package com.doublecat.android_root_mcp

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetAddress
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class McpHttpServer(
    private val tokenStore: TokenStore,
    private val rootProbe: RootProbe,
    private val port: Int = 8787,
    private val localeProvider: () -> String = { "en" },
    private val localeSetter: (String) -> Boolean = { false },
    private val toolExecutorProvider: (() -> AndroidToolExecutor)? = null,
) {
    val debugUrl: String
        get() = "http://$advertisedHost:$port/debug/"
    val mcpUrl: String
        get() = "http://$advertisedHost:$port/mcp"

    private val advertisedHost: String
        get() = findNonLoopbackIpv4() ?: "127.0.0.1"

    private var clients: ExecutorService = newClientExecutor()
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private val toolRegistry = ToolRegistry()

    @Synchronized
    fun start(): Boolean {
        if (serverSocket != null) return true
        return try {
            if (clients.isShutdown) clients = newClientExecutor()
            val socket = ServerSocket(port, 16, InetAddress.getByName("0.0.0.0"))
            serverSocket = socket
            acceptThread = Thread {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        try {
                            clients.execute { handle(client) }
                        } catch (_: RejectedExecutionException) {
                            client.close()
                        }
                    } catch (_: Exception) {
                        if (!socket.isClosed) continue
                    }
                }
            }.apply {
                name = "android-root-mcp-http"
                start()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun stop() {
        serverSocket?.close()
        serverSocket = null
        acceptThread = null
        clients.shutdownNow()
    }

    fun isRunning(): Boolean = serverSocket?.isClosed == false

    private fun handle(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 5_000
                val input = BufferedInputStream(client.getInputStream())
                val output = BufferedOutputStream(client.getOutputStream())
                val requestLine = readLine(input) ?: return
                val parts = requestLine.split(" ", limit = 3)
                if (parts.size != 3) return

                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readLine(input) ?: return
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) {
                        headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
                    }
                }
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength < 0 || contentLength > MAX_REQUEST_BODY_BYTES) {
                    respond(output, 413, "application/json", "{\"error\":\"REQUEST_TOO_LARGE\"}")
                    return
                }
                val body = if (contentLength > 0) {
                    input.readNBytes(contentLength).toString(StandardCharsets.UTF_8)
                } else {
                    ""
                }
                val path = parts[1].substringBefore('?')
                when {
                    parts[0] == "GET" && path == "/debug/" -> respond(
                        output,
                        200,
                        "text/html; charset=utf-8",
                        McpDebugPage.html(localeProvider()),
                    )
                    parts[0] == "POST" && path == "/debug/locale" -> handleLocale(output, body)
                    parts[0] == "GET" && path == "/mcp" -> handleMcpSse(output, headers)
                    parts[0] == "GET" && path == "/.well-known/oauth-protected-resource" -> respond(
                        output,
                        200,
                        "application/json",
                        "{\"resource\":\"$mcpUrl\",\"authorization_servers\":[\"http://$advertisedHost:$port\"]}",
                    )
                    parts[0] == "POST" && path == "/mcp" -> {
                        val contentType = headers["content-type"]?.substringBefore(';')?.trim()?.lowercase()
                        if (contentType != "application/json") {
                            respond(output, 415, "application/json", "{\"error\":\"CONTENT_TYPE_REQUIRED\"}")
                        } else {
                            handleMcp(output, headers, body)
                        }
                    }
                    else -> respond(output, 404, "application/json", "{\"error\":\"not_found\"}")
                }
            } catch (_: Exception) {
                // The client may disconnect before a response is writable.
            }
        }
    }

    private fun handleMcp(output: BufferedOutputStream, headers: Map<String, String>, body: String) {
        val authorization = headers["authorization"] ?: ""
        val token = if (
            authorization.length > 7 &&
            authorization.regionMatches(0, "Bearer ", 0, 7, ignoreCase = true)
        ) {
            authorization.substring(7)
        } else {
            null
        }
        val access = token?.takeIf { it.isNotBlank() }?.let { tokenStore.authenticate(it) }
        if (access == null) {
            respondUnauthorized(output)
            return
        }

        try {
            val request = try {
                JSONObject(body)
            } catch (_: Exception) {
                respond(
                    output,
                    400,
                    "application/json",
                    jsonRpcError(null, -32700, "Parse error").toString(),
                )
                return
            }
            if (request.optString("jsonrpc") != "2.0" || request.optString("method").isBlank()) {
                respond(
                    output,
                    400,
                    "application/json",
                    jsonRpcError(request.opt("id"), -32600, "Invalid Request").toString(),
                )
                return
            }
            val method = request.optString("method")
            val id = request.opt("id")
            if (method.startsWith("notifications/")) {
                respond(output, 202, "application/json", "")
                return
            }

            val response = when (method) {
                // `ping` was part of the 2025-11-25 MCP utilities and is still
                // used by a number of clients as a connection health probe.
                // Keep it as a harmless compatibility method even though it
                // was removed from the 2026-07-28 protocol.
                "ping" -> result(id, JSONObject())
                // Modern stateless clients use discovery instead of the
                // legacy initialize handshake. Supporting both eras keeps a
                // client probe from being reported as an unavailable method.
                "server/discover" -> result(id, JSONObject()
                    .put("resultType", "complete")
                    .put("supportedVersions", JSONArray()
                        .put("2026-07-28")
                        .put("2025-11-25"))
                    .put("capabilities", JSONObject().put("tools", JSONObject()))
                    .put("_meta", JSONObject().put(
                        "io.modelcontextprotocol/serverInfo",
                        JSONObject().put("name", "android-root-mcp").put("version", "0.1.0"),
                    )))
                "initialize" -> result(id, JSONObject()
                    .put(
                        "protocolVersion",
                        request.optJSONObject("params")?.optString("protocolVersion")
                            ?.takeIf { it.isNotBlank() } ?: "2026-07-28",
                    )
                    .put("capabilities", JSONObject().put("tools", JSONObject()))
                    .put("serverInfo", JSONObject().put("name", "android-root-mcp").put("version", "0.1.0")))
                "tools/list" -> result(id, JSONObject().put(
                    "tools",
                    JSONArray().apply {
                        toolRegistry.list()
                            .filter { it.name in access.enabledTools }
                            .forEach { put(it.asMcpJson()) }
                    },
                ))
                "tools/call" -> callTool(id, request.optJSONObject("params"), access)
                else -> error(id, -32601, "Method not found")
            }
            respond(output, 200, "application/json", response.toString())
        } catch (_: Exception) {
            respond(
                output,
                500,
                "application/json",
                jsonRpcError(null, -32603, "Internal error").toString(),
            )
        }
    }

    private fun handleMcpSse(output: BufferedOutputStream, headers: Map<String, String>) {
        val authorization = headers["authorization"] ?: ""
        val token = if (
            authorization.length > 7 &&
            authorization.regionMatches(0, "Bearer ", 0, 7, ignoreCase = true)
        ) {
            authorization.substring(7)
        } else {
            null
        }
        val access = token?.takeIf { it.isNotBlank() }?.let { tokenStore.authenticate(it) }
        if (access == null) {
            respondUnauthorized(output)
            return
        }

        // A stateless server can satisfy the Streamable HTTP GET contract by
        // opening an SSE stream and immediately priming it. Clients can then
        // reconnect with GET; JSON-RPC requests still arrive via POST /mcp.
        val event = "id: mcp-${System.currentTimeMillis()}\ndata: \n\n"
            .toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/event-stream\r\n" +
            "Cache-Control: no-cache\r\n" +
            "Connection: close\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "\r\n"
        output.write(header.toByteArray(StandardCharsets.UTF_8))
        output.write(event)
        output.flush()
    }

    private fun respondUnauthorized(output: BufferedOutputStream) {
        respond(
            output,
            401,
            "application/json",
            "{\"error\":\"AUTH_REQUIRED\"}",
            mapOf(
                "WWW-Authenticate" to "Bearer",
                "Link" to "</.well-known/oauth-protected-resource>; rel=\"protected-resource\"",
            ),
        )
    }

    private fun handleLocale(output: BufferedOutputStream, body: String) {
        val languageCode = try {
            JSONObject(body).optString("languageCode")
        } catch (_: Exception) {
            ""
        }
        if (languageCode != "en" && languageCode != "zh") {
            respond(output, 400, "application/json", "{\"error\":\"INVALID_LANGUAGE\"}")
            return
        }
        if (!localeSetter(languageCode)) {
            respond(output, 500, "application/json", "{\"error\":\"LOCALE_PERSIST_FAILED\"}")
            return
        }
        respond(output, 200, "application/json", JSONObject().put("languageCode", languageCode).toString())
    }

    private fun callTool(id: Any?, params: JSONObject?, access: TokenAccess): JSONObject {
        val name = params?.optString("name") ?: ""
        val definition = toolRegistry.find(name)
        if (definition == null) {
            return error(id, -32602, "Unknown tool")
        }
        if (name !in access.enabledTools) {
            return toolError(id, "TOKEN_TOOL_DISABLED", "Tool is disabled for this token")
        }
        val executor = toolExecutorProvider?.invoke()
            ?: return toolError(id, "EXECUTION_FAILED", "Tool executor is unavailable")
        val execution = executor.execute(name, params?.optJSONObject("arguments"))
        val value = JSONObject()
            .put("content", execution.content)
            .put("isError", execution.isError)
        if (execution.isError) {
            value.put(
                "error",
                JSONObject()
                    .put("code", execution.errorCode ?: "EXECUTION_FAILED")
                    .put("message", execution.errorMessage ?: "Tool execution failed")
                    .put("retryable", execution.errorCode == "TIMEOUT")
                    .put("details", execution.details),
            )
        }
        return result(id, value)
    }

    private fun toolError(id: Any?, code: String, message: String): JSONObject = result(
        id,
        JSONObject()
            .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", message)))
            .put("isError", true)
            .put("error", JSONObject().put("code", code).put("message", message).put("retryable", false)),
    )

    private fun result(id: Any?, value: JSONObject): JSONObject = JSONObject()
        .put("jsonrpc", "2.0")
        .put("id", id ?: JSONObject.NULL)
        .put("result", value)

    private fun error(id: Any?, code: Int, message: String): JSONObject = JSONObject()
        .put("jsonrpc", "2.0")
        .put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))

    private fun jsonRpcError(id: Any?, code: Int, message: String): JSONObject = error(id, code, message)

    private fun respond(
        output: BufferedOutputStream,
        status: Int,
        contentType: String,
        body: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ").append(status).append(reasonPhrase(status)).append("\r\n")
            append("Content-Type: ").append(contentType).append("\r\n")
            append("Content-Length: ").append(bytes.size).append("\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            extraHeaders.forEach { (key, value) -> append(key).append(": ").append(value).append("\r\n") }
            append("\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        output.write(header)
        output.write(bytes)
        output.flush()
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val value = input.read()
            if (value < 0) return if (bytes.isEmpty()) null else bytes.toByteArray().toString(StandardCharsets.UTF_8)
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes.add(value.toByte())
            if (bytes.size > 8_192) return null
        }
        return bytes.toByteArray().toString(StandardCharsets.UTF_8)
    }

    private fun reasonPhrase(status: Int): String = when (status) {
        200 -> " OK"
        202 -> " Accepted"
        400 -> " Bad Request"
        401 -> " Unauthorized"
        405 -> " Method Not Allowed"
        413 -> " Payload Too Large"
        415 -> " Unsupported Media Type"
        404 -> " Not Found"
        500 -> " Internal Server Error"
        else -> " Error"
    }

    private fun newClientExecutor(): ExecutorService {
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, "android-root-mcp-client").apply { isDaemon = true }
        }
        return ThreadPoolExecutor(
            CLIENT_CORE_THREADS,
            CLIENT_MAX_THREADS,
            30,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(CLIENT_QUEUE_CAPACITY),
            threadFactory,
            ThreadPoolExecutor.AbortPolicy(),
        )
    }

    private fun findNonLoopbackIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address &&
                        !address.isLoopbackAddress &&
                        !address.isLinkLocalAddress
                    ) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val CLIENT_CORE_THREADS = 1
        const val CLIENT_MAX_THREADS = 4
        const val CLIENT_QUEUE_CAPACITY = 32
        const val MAX_REQUEST_BODY_BYTES = 1_048_576
    }
}
