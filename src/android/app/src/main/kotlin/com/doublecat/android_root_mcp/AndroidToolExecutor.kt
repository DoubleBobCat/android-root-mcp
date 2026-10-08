package com.doublecat.android_root_mcp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.Locale
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException

data class ToolExecutionResult(
    val content: JSONArray,
    val isError: Boolean = false,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val details: JSONObject = JSONObject(),
)

class AndroidToolExecutor(
    private val context: Context,
    private val rootProbe: RootProbe,
    private val mcpRunning: () -> Boolean,
    private val runtimeMode: () -> String = { "root" },
    private val rootPermissionManager: RootPermissionManager? = null,
) {
    fun execute(name: String, arguments: JSONObject?): ToolExecutionResult {
        val args = arguments ?: JSONObject()
        return when (name) {
            "root_status" -> success(JSONObject(rootProbe.check()))
            "device_status" -> deviceStatus()
            "list_apps" -> listApps(args)
            "app_permissions" -> appPermissions(args)
            "location_status" -> locationStatus()
            "get_location" -> getLocation()
            "battery_status" -> batteryStatus()
            "network_status" -> networkStatus()
            "display_info" -> displayInfo()
            "launch_app" -> launchApp(args)
            "shell_exec" -> shellExec(args)
            "screenshot" -> screenshot()
            "tap" -> inputCommand(args, "tap")
            "random_tap_by_xpath" -> randomTapByXPath(args)
            "swipe" -> inputCommand(args, "swipe")
            "input_text" -> inputText(args)
            "key_event" -> inputCommand(args, "keyevent")
            "view_tree" -> viewDump(args, asJson = false)
            "view_content" -> viewDump(args, asJson = true)
            else -> failure("CAPABILITY_UNSUPPORTED", "This tool is unavailable", JSONObject().put("tool", name))
        }
    }

    private fun deviceStatus(): ToolExecutionResult {
        val status = JSONObject()
            .put("platform", "android")
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("androidVersion", Build.VERSION.RELEASE ?: "unknown")
            .put("apiLevel", Build.VERSION.SDK_INT)
            .put("applicationId", context.packageName)
            .put("mcpRunning", mcpRunning())
            .put("runtimeMode", runtimeMode())
        return success(status)
    }

    private fun listApps(arguments: JSONObject): ToolExecutionResult {
        val includeSystem = arguments.optBoolean("includeSystem", false)
        return try {
            val flags = PackageManager.GET_META_DATA
            val apps = context.packageManager.getInstalledApplications(flags)
                .asSequence()
                .filter { includeSystem || (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase(Locale.ROOT) }
                .map { app ->
                    JSONObject()
                        .put("packageName", app.packageName)
                        .put("label", context.packageManager.getApplicationLabel(app).toString())
                        .put("system", (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
                        .put("enabled", app.enabled)
                }
                .toList()
            success(JSONObject()
                .put("count", apps.size)
                .put("includeSystem", includeSystem)
                .put("apps", JSONArray(apps)))
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "Installed applications could not be read", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun appPermissions(arguments: JSONObject): ToolExecutionResult {
        val packageName = arguments.optString("packageName").trim()
        if (!packageName.matches(PACKAGE_PATTERN)) return failure("INVALID_ARGUMENT", "packageName is invalid")
        return try {
            val info = context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val permissions = JSONArray()
            val names = info.requestedPermissions ?: emptyArray()
            val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
            names.forEachIndexed { index, name ->
                permissions.put(JSONObject()
                    .put("name", name)
                    .put("granted", index < flags.size && (flags[index] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0))
            }
            success(JSONObject().put("packageName", packageName).put("permissions", permissions))
        } catch (_: PackageManager.NameNotFoundException) {
            failure("NOT_FOUND", "The application was not found", JSONObject().put("packageName", packageName))
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "Application permissions could not be read", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun locationStatus(): ToolExecutionResult {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return success(locationStatusValue(manager))
    }

    private fun locationStatusValue(manager: LocationManager): JSONObject {
        val fine = Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return JSONObject()
            .put("finePermission", fine)
            .put("coarsePermission", coarse)
            .put("locationEnabled", if (Build.VERSION.SDK_INT >= 28) manager.isLocationEnabled else null)
            .put("gpsProvider", manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            .put("networkProvider", manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    private fun getLocation(): ToolExecutionResult {
        val fine = Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return failure("PERMISSION_REQUIRED", "Location permission is not granted", JSONObject()
            .put("permission", "ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION"))
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            val location = providers.asSequence().mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }
            if (location == null) failure("NO_DATA", "No recent location is available", JSONObject()
                .put("status", locationStatusValue(manager)))
            else success(JSONObject()
                .put("latitude", location.latitude)
                .put("longitude", location.longitude)
                .put("accuracyMeters", location.accuracy)
                .put("altitudeMeters", location.altitude)
                .put("timeMs", location.time)
                .put("provider", location.provider ?: JSONObject.NULL))
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "The latest location could not be read", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun batteryStatus(): ToolExecutionResult {
        val intent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return failure("UNAVAILABLE", "Battery status is not available")
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return success(JSONObject()
            .put("level", level)
            .put("scale", scale)
            .put("percentage", if (level >= 0 && scale > 0) level.toDouble() / scale * 100 else JSONObject.NULL)
            .put("status", intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .put("plugged", intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
            .put("temperature", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0))
    }

    private fun networkStatus(): ToolExecutionResult {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork
        val capabilities = network?.let { manager.getNetworkCapabilities(it) }
        return success(JSONObject()
            .put("connected", network != null)
            .put("validated", capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false)
            .put("wifi", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false)
            .put("cellular", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false)
            .put("ethernet", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false))
    }

    private fun displayInfo(): ToolExecutionResult {
        val metrics = context.resources.displayMetrics
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(size)
        return success(JSONObject()
            .put("widthPixels", size.x)
            .put("heightPixels", size.y)
            .put("density", metrics.density)
            .put("densityDpi", metrics.densityDpi)
            .put("scaledDensity", metrics.scaledDensity)
            .put("refreshRateHz", windowManager.defaultDisplay.refreshRate))
    }

    private fun viewDump(arguments: JSONObject, asJson: Boolean): ToolExecutionResult {
        val filter = arguments.optString("filter").trim()
        return synchronized(UI_INSPECTION_LOCK) {
            val initial = inspectViewDump(filter, asJson)
            if (shouldUseTemporaryTalkBack(initial)) {
                val session = rootPermissionManager?.beginTemporaryTalkBack()
                if (session != null) {
                    try {
                        Thread.sleep(TALKBACK_START_DELAY_MS)
                        return@synchronized inspectViewDump(filter, asJson)
                    } finally {
                        rootPermissionManager.endTemporaryTalkBack(session)
                    }
                }
            }
            initial
        }
    }

    private fun inspectViewDump(filter: String, asJson: Boolean): ToolExecutionResult {
        val accessibilityService = ArmcpAccessibilityService.awaitInstance()
        val accessibilitySnapshot = accessibilityService?.snapshot()
        return if (accessibilitySnapshot != null) {
                uiSnapshotResult(filter, accessibilitySnapshot, "accessibility_service", asJson)
            } else if (accessibilityService != null) {
                failure(
                    "UI_INSPECTION_UNAVAILABLE",
                    "Screen content could not be read from the active app",
                    JSONObject()
                        .put("backend", "accessibility_service")
                        .put("serviceConnected", true),
                )
        } else inspectWithRootUiAutomator(filter, asJson)
    }

    private fun inspectWithRootUiAutomator(filter: String, asJson: Boolean): ToolExecutionResult {
        return executeRootCommand(
                "rm -f /data/local/tmp/armcp_window.xml; uiautomator dump --compressed /data/local/tmp/armcp_window.xml 2>&1; dump_rc=\$?; if [ \$dump_rc -ne 0 ]; then rm -f /data/local/tmp/armcp_window.xml; exit \$dump_rc; fi; cat /data/local/tmp/armcp_window.xml; cat_rc=\$?; rm -f /data/local/tmp/armcp_window.xml; exit \$cat_rc",
                MAX_SHELL_TIMEOUT_MS,
                null,
            ) { output, exitCode, _ ->
                if (exitCode != 0) {
                    val diagnostic = output.toString(Charsets.UTF_8).take(MAX_UI_DIAGNOSTIC_BYTES)
                    val conflict = exitCode == 137 || diagnostic.contains("already registered", ignoreCase = true)
                    failure(
                        if (conflict) "UI_AUTOMATION_CONFLICT" else "UI_INSPECTION_UNAVAILABLE",
                    if (conflict) "Another screen automation session is active"
                        else "Screen content could not be read",
                        JSONObject().put("exitCode", exitCode)
                            .put("backend", "root_uiautomator_dump")
                            .put("diagnostic", diagnostic),
                    )
                } else {
                    uiSnapshotResult(filter, UiSnapshot(output.toString(Charsets.UTF_8)), "root_uiautomator_dump", asJson)
                }
            }
    }

    private fun shouldUseTemporaryTalkBack(result: ToolExecutionResult): Boolean {
        return runtimeMode() == "root" &&
            result.errorCode in setOf("UI_INSPECTION_UNAVAILABLE", "UI_AUTOMATION_CONFLICT") &&
            rootPermissionManager != null &&
            McpRuntime.isTalkBackAutoUseAllowed() &&
            screenCaptureHasContent()
    }

    private fun screenCaptureHasContent(): Boolean {
        val result = executeRootCommand(
            "screencap -p",
            MAX_SCREENSHOT_TIMEOUT_MS,
            MAX_SCREENSHOT_BYTES,
        ) { output, exitCode, truncated ->
            val bitmap = if (exitCode == 0 && !truncated && output.isNotEmpty()) {
                android.graphics.BitmapFactory.decodeByteArray(output, 0, output.size)
            } else {
                null
            }
            val hasVisibleContent = bitmap?.let { image ->
                val stepX = maxOf(1, image.width / 32)
                val stepY = maxOf(1, image.height / 32)
                var visibleSamples = 0
                var samples = 0
                var y = 0
                while (y < image.height) {
                    var x = 0
                    while (x < image.width) {
                        val pixel = image.getPixel(x, y)
                        if ((pixel and 0x00FFFFFF) != 0) visibleSamples++
                        samples++
                        x += stepX
                    }
                    y += stepY
                }
                image.recycle()
                samples > 0 && visibleSamples >= 4
            } ?: false
            if (hasVisibleContent) success(JSONObject())
            else failure("SCREEN_EMPTY", "No visible screen content was found")
        }
        return !result.isError
    }

    private fun uiSnapshotResult(
        filter: String,
        snapshot: UiSnapshot,
        backend: String,
        asJson: Boolean,
    ): ToolExecutionResult {
        return try {
            val formatted = UiHierarchyFormatter.format(
                xml = snapshot.xml,
                filter = filter,
                backend = backend,
                asJson = asJson,
            )
            ToolExecutionResult(
                content = JSONArray().put(JSONObject()
                    .put("type", "text")
                    .put("text", formatted)
                    .put("_meta", JSONObject()
                        .put("mimeType", if (asJson) "application/json" else "application/xml")
                        .put("backend", backend)
                        .put("privilege", if (backend == "accessibility_service") {
                            "root_enabled_accessibility_service"
                        } else {
                            "root_shell"
                        })
                        .put("filter", filter)
                        .put("filtered", filter.isNotEmpty()))),
            )
        } catch (error: Exception) {
            failure("UI_FORMAT_FAILED", "Screen content could not be formatted", JSONObject()
                .put("backend", backend)
                .put("reason", error.javaClass.simpleName))
        }
    }

    private fun launchApp(arguments: JSONObject): ToolExecutionResult {
        val packageName = arguments.optString("packageName").trim()
        if (!packageName.matches(PACKAGE_PATTERN)) {
            return failure("INVALID_ARGUMENT", "packageName is invalid")
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return failure("EXECUTION_FAILED", "This app cannot be opened", JSONObject().put("packageName", packageName))
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            success(JSONObject().put("packageName", packageName).put("launched", true))
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "The app could not be opened", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun shellExec(arguments: JSONObject): ToolExecutionResult {
        val command = arguments.optString("command").trim()
        if (command.isEmpty()) return failure("INVALID_ARGUMENT", "command is required")
        val timeoutMs = arguments.optLong("timeoutMs", DEFAULT_SHELL_TIMEOUT_MS)
            .coerceIn(MIN_SHELL_TIMEOUT_MS, MAX_SHELL_TIMEOUT_MS)
        val root = rootProbe.check()
        if (root["available"] != true) {
            return failure("ROOT_UNAVAILABLE", "Root access is unavailable", JSONObject(root))
        }

        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = ByteArrayOutputStream()
            val reader = Thread {
                process.inputStream.use { input ->
                    val buffer = ByteArray(4_096)
                    var remaining = MAX_SHELL_OUTPUT_BYTES
                    while (remaining > 0) {
                        val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        remaining -= count
                    }
                }
            }.apply {
                name = "android-root-mcp-shell-output"
                isDaemon = true
                start()
            }
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) process.destroyForcibly()
            reader.join(SHELL_READER_JOIN_MS)
            val text = output.toString(Charsets.UTF_8.name())
            if (!completed) {
                failure("TIMEOUT", "Shell command timed out", JSONObject().put("timeoutMs", timeoutMs))
            } else {
                success(JSONObject()
                    .put("exitCode", process.exitValue())
                    .put("output", text)
                    .put("truncated", output.size() >= MAX_SHELL_OUTPUT_BYTES))
            }
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "The command could not be completed", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun screenshot(): ToolExecutionResult {
        return executeRootCommand(
            command = "screencap -p",
            timeoutMs = MAX_SHELL_TIMEOUT_MS,
            maxOutputBytes = MAX_SCREENSHOT_BYTES + 1,
        ) { output, exitCode, truncated ->
            if (exitCode != 0) {
                failure("EXECUTION_FAILED", "The screen could not be captured", JSONObject().put("exitCode", exitCode))
            } else {
                if (truncated || output.size > MAX_SCREENSHOT_BYTES) {
                    failure("OUTPUT_LIMIT", "Screenshot exceeded the output limit")
                } else {
                    ToolExecutionResult(
                        content = JSONArray().put(JSONObject()
                            .put("type", "image")
                            .put("data", Base64.encodeToString(output, Base64.NO_WRAP))
                            .put("mimeType", "image/png")),
                    )
                }
            }
        }
    }

    private fun inputCommand(arguments: JSONObject, operation: String): ToolExecutionResult {
        val command = when (operation) {
            "tap" -> {
                val x = coordinate(arguments, "x") ?: return failure("INVALID_ARGUMENT", "x must be a non-negative integer")
                val y = coordinate(arguments, "y") ?: return failure("INVALID_ARGUMENT", "y must be a non-negative integer")
                "input tap $x $y"
            }
            "swipe" -> {
                val x1 = coordinate(arguments, "x1") ?: return failure("INVALID_ARGUMENT", "x1 must be a non-negative integer")
                val y1 = coordinate(arguments, "y1") ?: return failure("INVALID_ARGUMENT", "y1 must be a non-negative integer")
                val x2 = coordinate(arguments, "x2") ?: return failure("INVALID_ARGUMENT", "x2 must be a non-negative integer")
                val y2 = coordinate(arguments, "y2") ?: return failure("INVALID_ARGUMENT", "y2 must be a non-negative integer")
                val duration = arguments.optLong("durationMs", 300L).coerceIn(1L, 10_000L)
                "input swipe $x1 $y1 $x2 $y2 $duration"
            }
            "keyevent" -> {
                val keyCode = coordinate(arguments, "keyCode") ?: return failure("INVALID_ARGUMENT", "keyCode must be a non-negative integer")
                "input keyevent $keyCode"
            }
            else -> return failure("INVALID_ARGUMENT", "Unsupported input operation")
        }
        if (runtimeMode() == "non_root") {
            val service = ArmcpAccessibilityService.awaitInstance()
                ?: return failure("ACCESSIBILITY_REQUIRED", "Enable ARMCP screen reading in Android settings", JSONObject().put("mode", runtimeMode()))
            val success = when (operation) {
                "tap" -> service.dispatchTap(
                    arguments.optInt("x"),
                    arguments.optInt("y"),
                )
                "swipe" -> service.dispatchSwipe(
                    arguments.optInt("x1"),
                    arguments.optInt("y1"),
                    arguments.optInt("x2"),
                    arguments.optInt("y2"),
                    arguments.optLong("durationMs", 300L).coerceIn(1L, 10_000L),
                )
                else -> false
            }
            return if (success) success(JSONObject().put("executed", true).put("backend", "accessibility_service"))
            else failure("ACCESSIBILITY_ACTION_FAILED", "The screen did not accept this action", JSONObject().put("backend", "accessibility_service"))
        }
        return executeRootCommand(command, 3_000L, MAX_COMMAND_OUTPUT_BYTES) { _, exitCode, _ ->
            if (exitCode == 0) success(JSONObject().put("executed", true).put("operation", operation))
            else failure("EXECUTION_FAILED", "The input action failed", JSONObject().put("exitCode", exitCode))
        }
    }

    private fun randomTapByXPath(arguments: JSONObject): ToolExecutionResult {
        val xpath = arguments.optString("xpath").trim()
        if (xpath.isEmpty() || xpath.length > MAX_XPATH_LENGTH) {
            return failure("INVALID_ARGUMENT", "xpath must contain 1 to 1024 characters")
        }

        return synchronized(UI_INSPECTION_LOCK) {
            val accessibilityService = ArmcpAccessibilityService.awaitInstance()
            val accessibilitySnapshot = accessibilityService?.snapshot()
            if (accessibilitySnapshot != null) {
                randomTapFromSnapshot(xpath, accessibilitySnapshot, "accessibility_service")
            } else if (accessibilityService != null) {
                failure(
                    "UI_INSPECTION_UNAVAILABLE",
                    "Screen content could not be read from the active app",
                    JSONObject()
                        .put("backend", "accessibility_service")
                        .put("serviceConnected", true),
                )
            } else executeRootCommand(
                "rm -f /data/local/tmp/armcp_window.xml; uiautomator dump --compressed /data/local/tmp/armcp_window.xml 2>&1; dump_rc=\$?; if [ \$dump_rc -ne 0 ]; then rm -f /data/local/tmp/armcp_window.xml; exit \$dump_rc; fi; cat /data/local/tmp/armcp_window.xml; cat_rc=\$?; rm -f /data/local/tmp/armcp_window.xml; exit \$cat_rc",
                MAX_SHELL_TIMEOUT_MS,
                null,
            ) { output, exitCode, _ ->
                if (exitCode != 0) {
                    val diagnostic = output.toString(Charsets.UTF_8).take(MAX_UI_DIAGNOSTIC_BYTES)
                    val conflict = exitCode == 137 || diagnostic.contains("already registered", ignoreCase = true)
                    failure(
                        if (conflict) "UI_AUTOMATION_CONFLICT" else "UI_INSPECTION_UNAVAILABLE",
                        if (conflict) "Another screen automation session is active"
                        else "Screen content could not be read",
                        JSONObject().put("exitCode", exitCode)
                            .put("backend", "root_uiautomator_dump")
                            .put("diagnostic", diagnostic),
                    )
                } else {
                    randomTapFromSnapshot(
                        xpath,
                        UiSnapshot(output.toString(Charsets.UTF_8)),
                        "root_uiautomator_dump",
                    )
                }
            }
        }
    }

    private fun randomTapFromSnapshot(
        xpath: String,
        snapshot: UiSnapshot,
        backend: String,
    ): ToolExecutionResult {
        val element = try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
                runCatching { isXIncludeAware = false }
                setOptionalFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setOptionalFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setOptionalFeature("http://xml.org/sax/features/external-general-entities", false)
                setOptionalFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setOptionalFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val document = factory.newDocumentBuilder().apply {
                setEntityResolver { _, _ ->
                    throw SAXException("External entities are disabled")
                }
            }.parse(InputSource(StringReader(snapshot.xml)))
            val nodes = XPathFactory.newInstance().newXPath().evaluate(
                xpath,
                document,
                XPathConstants.NODESET,
            ) as org.w3c.dom.NodeList
            when {
                nodes.length == 0 -> return failure(
                    "XPATH_NOT_FOUND",
                    "XPath did not select an element",
                    JSONObject().put("xpath", xpath).put("backend", backend),
                )
                nodes.length > 1 -> return failure(
                    "XPATH_MULTIPLE_MATCHES",
                    "XPath selected more than one element",
                    JSONObject().put("xpath", xpath).put("matches", nodes.length).put("backend", backend),
                )
                nodes.item(0) !is Element -> return failure(
                    "XPATH_NOT_ELEMENT",
                    "XPath did not select an element node",
                    JSONObject().put("xpath", xpath).put("backend", backend),
                )
                else -> nodes.item(0) as Element
            }
        } catch (error: Exception) {
            return failure(
                "INVALID_XPATH",
                "XPath could not be evaluated",
                JSONObject().put("xpath", xpath).put("backend", backend)
                    .put("reason", error.javaClass.simpleName),
            )
        }

        val bounds = parseBounds(element.getAttribute("bounds"))
            ?: return failure(
                "BOUNDS_UNAVAILABLE",
                "Selected element has no valid bounds",
                JSONObject().put("xpath", xpath).put("backend", backend),
            )
        if (bounds.left < 0 || bounds.top < 0 || bounds.right <= bounds.left || bounds.bottom <= bounds.top ||
            bounds.width > MAX_COORDINATE || bounds.height > MAX_COORDINATE
        ) {
            return failure(
                "BOUNDS_UNSUPPORTED",
                "Selected element bounds are outside the supported screen range",
                JSONObject().put("xpath", xpath).put("backend", backend),
            )
        }

        return executeRootCommand(
            "dd if=/dev/hwrng bs=1 count=$HARDWARE_RANDOM_BYTES 2>/dev/null",
            MAX_HARDWARE_RANDOM_TIMEOUT_MS,
            HARDWARE_RANDOM_BYTES + 1,
        ) { output, exitCode, truncated ->
            val randomBytes = if (exitCode == 0 && !truncated && output.size >= HARDWARE_RANDOM_BYTES) {
                RandomBytes(output.copyOf(HARDWARE_RANDOM_BYTES), "linux_hwrng")
            } else {
                val fallback = ByteArray(HARDWARE_RANDOM_BYTES)
                SecureRandom().nextBytes(fallback)
                RandomBytes(fallback, "android_secure_random")
            }
            run {
                val xOffset = uniformOffset(randomBytes.bytes, bounds.width)
                val yOffset = uniformOffset(randomBytes.bytes, bounds.height, xOffset.second)
                if (xOffset.first == null || yOffset.first == null) {
                    failure(
                        "HARDWARE_RANDOM_READ_FAILED",
                        "Random source did not yield a bounded sample",
                        JSONObject().put("xpath", xpath).put("backend", backend)
                            .put("randomSource", randomBytes.source),
                    )
                } else {
                    val x = bounds.left + xOffset.first!!
                    val y = bounds.top + yOffset.first!!
                    val tapResult = inputCommand(JSONObject().put("x", x).put("y", y), "tap")
                    if (tapResult.isError) {
                        failure(
                            tapResult.errorCode ?: "EXECUTION_FAILED",
                            tapResult.errorMessage ?: "Random tap failed",
                            JSONObject().put("xpath", xpath).put("backend", backend)
                                .put("randomSource", randomBytes.source)
                                .put("x", x).put("y", y),
                        )
                    } else {
                        success(JSONObject()
                            .put("executed", true)
                            .put("xpath", xpath)
                            .put("backend", backend)
                            .put("randomSource", randomBytes.source)
                            .put("bounds", JSONObject()
                                .put("left", bounds.left).put("top", bounds.top)
                                .put("right", bounds.right).put("bottom", bounds.bottom))
                            .put("x", x).put("y", y))
                    }
                }
            }
        }
    }

    private fun DocumentBuilderFactory.setOptionalFeature(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private fun parseBounds(value: String): Bounds? {
        val match = BOUNDS_PATTERN.matchEntire(value) ?: return null
        return Bounds(
            left = match.groupValues[1].toInt(),
            top = match.groupValues[2].toInt(),
            right = match.groupValues[3].toInt(),
            bottom = match.groupValues[4].toInt(),
        )
    }

    private fun uniformOffset(bytes: ByteArray, bound: Int, start: Int = 0): Pair<Int?, Int> {
        if (bound <= 0) return null to start
        val range = 1L shl 32
        val limit = range - (range % bound.toLong())
        var cursor = start
        while (cursor + 4 <= bytes.size) {
            val value = ((bytes[cursor].toLong() and 0xffL) shl 24) or
                ((bytes[cursor + 1].toLong() and 0xffL) shl 16) or
                ((bytes[cursor + 2].toLong() and 0xffL) shl 8) or
                (bytes[cursor + 3].toLong() and 0xffL)
            cursor += 4
            if (value < limit) return (value % bound).toInt() to cursor
        }
        return null to cursor
    }

    private fun inputText(arguments: JSONObject): ToolExecutionResult {
        val text = arguments.optString("text")
        if (text.isEmpty() || text.length > MAX_INPUT_TEXT_LENGTH) {
            return failure("INVALID_ARGUMENT", "text must contain 1 to 4096 characters")
        }
        if (runtimeMode() == "non_root") {
            val service = ArmcpAccessibilityService.awaitInstance()
                ?: return failure("ACCESSIBILITY_REQUIRED", "Enable ARMCP screen reading in Android settings")
            return if (service.setFocusedText(text)) {
                success(JSONObject().put("executed", true).put("backend", "accessibility_service"))
            } else {
                failure("ACCESSIBILITY_ACTION_FAILED", "No focused text field accepted the text")
            }
        }
        val escaped = text
            .replace("%", "%25")
            .replace(" ", "%s")
            .replace("'", "'\\''")
        return executeRootCommand("input text '$escaped'", 3_000L, MAX_COMMAND_OUTPUT_BYTES) { _, exitCode, _ ->
            if (exitCode == 0) success(JSONObject().put("executed", true))
            else failure("EXECUTION_FAILED", "Text input failed", JSONObject().put("exitCode", exitCode))
        }
    }


    private fun coordinate(arguments: JSONObject, key: String): Long? {
        if (!arguments.has(key)) return null
        val value = arguments.optLong(key, -1L)
        return value.takeIf { it in 0L..100_000L }
    }

    private fun executeRootCommand(
        command: String,
        timeoutMs: Long,
        maxOutputBytes: Int?,
        mapper: (ByteArray, Int, Boolean) -> ToolExecutionResult,
    ): ToolExecutionResult {
        if (runtimeMode() == "non_root") {
            return failure(
                "ROOT_MODE_REQUIRED",
                "This operation requires Root mode",
                JSONObject().put("runtimeMode", runtimeMode()),
            )
        }
        val root = rootProbe.check()
        if (root["available"] != true) {
            return failure("ROOT_UNAVAILABLE", "Root capability is unavailable", JSONObject(root))
        }
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = ByteArrayOutputStream()
            val reader = Thread {
                process.inputStream.use { input ->
                    val buffer = ByteArray(4_096)
                    var remaining = maxOutputBytes
                    while (remaining == null || remaining!! > 0) {
                        val readSize = remaining?.let { minOf(buffer.size, it) } ?: buffer.size
                        val count = input.read(buffer, 0, readSize)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        if (remaining != null) remaining -= count
                    }
                }
            }.apply { isDaemon = true; start() }
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!completed) process.destroyForcibly()
            reader.join(SHELL_READER_JOIN_MS)
            if (!completed) failure("TIMEOUT", "Root command timed out", JSONObject().put("timeoutMs", timeoutMs))
            else mapper(
                output.toByteArray(),
                process.exitValue(),
                maxOutputBytes != null && output.size() >= maxOutputBytes,
            )
        } catch (error: Exception) {
            failure("EXECUTION_FAILED", "Root command failed", JSONObject().put("reason", error.javaClass.simpleName))
        }
    }

    private fun success(value: JSONObject): ToolExecutionResult = ToolExecutionResult(
        content = JSONArray().put(JSONObject().put("type", "text").put("text", value.toString())),
    )

    private fun failure(code: String, message: String, details: JSONObject = JSONObject()): ToolExecutionResult = ToolExecutionResult(
        content = JSONArray().put(JSONObject().put("type", "text").put("text", details.toString())),
        isError = true,
        errorCode = code,
        errorMessage = message,
        details = details,
    )

    private companion object {
        val PACKAGE_PATTERN = Regex("^[A-Za-z][A-Za-z0-9_.]*$")
        val BOUNDS_PATTERN = Regex("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]")
        const val DEFAULT_SHELL_TIMEOUT_MS = 5_000L
        const val MIN_SHELL_TIMEOUT_MS = 100L
        const val MAX_SHELL_TIMEOUT_MS = 10_000L
        const val MAX_SHELL_OUTPUT_BYTES = 64 * 1024
        const val MAX_SCREENSHOT_BYTES = 8 * 1024 * 1024
        const val MAX_SCREENSHOT_TIMEOUT_MS = 5_000L
        const val MAX_COMMAND_OUTPUT_BYTES = 4 * 1024
        const val MAX_UI_DIAGNOSTIC_BYTES = 2_048
        const val MAX_INPUT_TEXT_LENGTH = 4_096
        const val MAX_XPATH_LENGTH = 1_024
        const val MAX_COORDINATE = 100_000
        const val HARDWARE_RANDOM_BYTES = 64
        const val MAX_HARDWARE_RANDOM_TIMEOUT_MS = 3_000L
        const val SHELL_READER_JOIN_MS = 1_000L
        const val TALKBACK_START_DELAY_MS = 1_000L
        val UI_INSPECTION_LOCK = Any()
    }

    private data class Bounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    private data class RandomBytes(
        val bytes: ByteArray,
        val source: String,
    )
}
