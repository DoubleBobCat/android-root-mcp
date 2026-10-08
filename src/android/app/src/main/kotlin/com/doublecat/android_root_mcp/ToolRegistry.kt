package com.doublecat.android_root_mcp

import org.json.JSONArray
import org.json.JSONObject

data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JSONObject,
    val requiredCapabilities: List<String>,
    val timeoutMs: Long,
    val sensitiveOutput: Boolean,
    val readOnly: Boolean = true,
) {
    fun asMcpJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("description", description)
        .put("inputSchema", inputSchema)
        .put("requiredCapabilities", JSONArray(requiredCapabilities))
        .put("timeoutMs", timeoutMs)
        .put("sensitiveOutput", sensitiveOutput)
        .put("readOnly", readOnly)
}

class ToolRegistry {
    private val tools = listOf(
        ToolDefinition(
            name = "root_status",
            description = "Read the current Root access status",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("root.probe"),
            timeoutMs = 2_500,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "list_apps",
            description = "List installed Android applications",
            inputSchema = JSONObject().put("type", "object").put("properties", JSONObject()
                .put("includeSystem", JSONObject().put("type", "boolean"))),
            requiredCapabilities = listOf("app.read"),
            timeoutMs = 5_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "app_permissions",
            description = "Read requested and granted permissions for an installed application",
            inputSchema = JSONObject().put("type", "object").put("properties", JSONObject()
                .put("packageName", JSONObject().put("type", "string").put("pattern", "^[A-Za-z][A-Za-z0-9_.]*$")))
                .put("required", JSONArray().put("packageName")),
            requiredCapabilities = listOf("permission.read"),
            timeoutMs = 3_000,
            sensitiveOutput = true,
        ),
        ToolDefinition(
            name = "location_status",
            description = "Read location service and permission status without collecting a location",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("location.read"),
            timeoutMs = 2_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "get_location",
            description = "Read the latest available location when location access is granted",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("location.read"),
            timeoutMs = 3_000,
            sensitiveOutput = true,
        ),
        ToolDefinition(
            name = "battery_status",
            description = "Read current battery level, charging state, and temperature",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("device.read"),
            timeoutMs = 1_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "network_status",
            description = "Read the active network and connection status",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("device.read"),
            timeoutMs = 1_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "display_info",
            description = "Read the display size and density",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("device.read"),
            timeoutMs = 1_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "device_status",
            description = "Read Android device and system status",
            inputSchema = objectSchema(),
            requiredCapabilities = listOf("device.read"),
            timeoutMs = 1_000,
            sensitiveOutput = false,
        ),
        ToolDefinition(
            name = "launch_app",
            description = "Open an installed Android application by package name",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put(
                    "packageName",
                    JSONObject().put("type", "string").put("pattern", "^[A-Za-z][A-Za-z0-9_.]*$"),
                ))
                .put("required", JSONArray().put("packageName")),
            requiredCapabilities = listOf("app.launch"),
            timeoutMs = 3_000,
            sensitiveOutput = false,
            readOnly = false,
        ),
        ToolDefinition(
            name = "shell_exec",
            description = "Run a command with Root access",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject()
                    .put("command", JSONObject().put("type", "string").put("minLength", 1))
                    .put("timeoutMs", JSONObject().put("type", "integer").put("minimum", 100).put("maximum", 10_000)))
                .put("required", JSONArray().put("command")),
            requiredCapabilities = listOf("root.exec"),
            timeoutMs = 10_000,
            sensitiveOutput = true,
            readOnly = false,
        ),
        ToolDefinition(
            name = "screenshot",
            description = "Capture the current screen",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject()),
            requiredCapabilities = listOf("screen.capture", "root.exec"),
            timeoutMs = 10_000,
            sensitiveOutput = true,
        ),
        ToolDefinition(
            name = "view_tree",
            description = "Read the current screen hierarchy as XML with bounds, size, and center coordinates",
            inputSchema = JSONObject().put("type", "object").put("properties", JSONObject()
                .put("filter", JSONObject().put("type", "string").put("maxLength", 256))),
            requiredCapabilities = listOf("ui.inspect"),
            timeoutMs = 10_000,
            sensitiveOutput = true,
        ),
        ToolDefinition(
            name = "view_content",
            description = "Read the current screen hierarchy as JSON with optional text and package filters",
            inputSchema = JSONObject().put("type", "object").put("properties", JSONObject()
                .put("filter", JSONObject().put("type", "string").put("maxLength", 256))),
            requiredCapabilities = listOf("ui.inspect"),
            timeoutMs = 10_000,
            sensitiveOutput = true,
        ),
        ToolDefinition(
            name = "tap",
            description = "Tap a screen coordinate",
            inputSchema = coordinateSchema("x", "y"),
            requiredCapabilities = listOf("input.inject", "root.exec"),
            timeoutMs = 3_000,
            sensitiveOutput = false,
            readOnly = false,
        ),
        ToolDefinition(
            name = "random_tap_by_xpath",
            description = "Tap a random point inside the element selected by XPath",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put(
                    "xpath",
                    JSONObject().put("type", "string").put("minLength", 1).put("maxLength", 1_024),
                ))
                .put("required", JSONArray().put("xpath")),
            requiredCapabilities = listOf("ui.inspect", "input.inject", "root.exec", "random.secure"),
            timeoutMs = 10_000,
            sensitiveOutput = false,
            readOnly = false,
        ),
        ToolDefinition(
            name = "swipe",
            description = "Swipe between two screen coordinates",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject()
                    .put("x1", integerSchema(0, 100_000))
                    .put("y1", integerSchema(0, 100_000))
                    .put("x2", integerSchema(0, 100_000))
                    .put("y2", integerSchema(0, 100_000))
                    .put("durationMs", integerSchema(1, 10_000)))
                .put("required", JSONArray().put("x1").put("y1").put("x2").put("y2")),
            requiredCapabilities = listOf("input.inject", "root.exec"),
            timeoutMs = 10_000,
            sensitiveOutput = false,
            readOnly = false,
        ),
        ToolDefinition(
            name = "input_text",
            description = "Enter text in the focused field",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put(
                    "text",
                    JSONObject().put("type", "string").put("minLength", 1).put("maxLength", 4_096),
                ))
                .put("required", JSONArray().put("text")),
            requiredCapabilities = listOf("input.inject", "root.exec"),
            timeoutMs = 3_000,
            sensitiveOutput = true,
            readOnly = false,
        ),
        ToolDefinition(
            name = "key_event",
            description = "Send an Android key event",
            inputSchema = JSONObject()
                .put("type", "object")
                .put("properties", JSONObject().put("keyCode", integerSchema(0, 10_000)))
                .put("required", JSONArray().put("keyCode")),
            requiredCapabilities = listOf("input.inject", "root.exec"),
            timeoutMs = 3_000,
            sensitiveOutput = false,
            readOnly = false,
        ),
    )

    fun list(): List<ToolDefinition> = tools

    fun names(): Set<String> = tools.map { it.name }.toSet()

    fun defaultNames(): Set<String> = tools.filter { it.readOnly }.map { it.name }.toSet()

    fun find(name: String): ToolDefinition? = tools.firstOrNull { it.name == name }

    private companion object {
        fun objectSchema(): JSONObject = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject())

        fun integerSchema(minimum: Int, maximum: Int): JSONObject = JSONObject()
            .put("type", "integer")
            .put("minimum", minimum)
            .put("maximum", maximum)

        fun coordinateSchema(first: String, second: String): JSONObject = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject()
                .put(first, integerSchema(0, 100_000))
                .put(second, integerSchema(0, 100_000)))
            .put("required", JSONArray().put(first).put(second))
    }
}
