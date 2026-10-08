package com.doublecat.android_root_mcp

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.content.pm.PackageManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private val channelName = "com.doublecat.android_root_mcp/capabilities"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        McpRuntime.initialize(this)
        executor.execute {
            McpRuntime.ensureUiInspectionService()
            mainHandler.post {
                if (McpRuntime.getMcpEnabled()) startMcpService()
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        McpRuntime.initialize(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getHealth" -> result.success(McpRuntime.health())
                    "getRootStatus", "attemptRoot" -> {
                        executor.execute {
                            val status = McpRuntime.checkRoot(call.method == "attemptRoot")
                            mainHandler.post { result.success(status) }
                        }
                    }
                    "getPermissionStatus" -> result.success(manifestPermissionStatus())
                    "grantAllPermissions" -> executor.execute {
                        val outcome = McpRuntime.grantAllPermissions()
                        mainHandler.post { result.success(outcome) }
                    }
                    "openAccessibilitySettings" -> {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        result.success(true)
                    }
                    "getAccessibilityStatus" -> result.success(McpRuntime.accessibilityStatus())
                    "setTalkBackAdaptation" -> {
                        val enabled = call.argument<Boolean>("enabled")
                        if (enabled == null) {
                            result.error("INVALID_ARGUMENT", "enabled is required", null)
                        } else {
                            result.success(McpRuntime.setTalkBackAdaptation(enabled))
                        }
                    }
                    "getRuntimeMode" -> result.success(McpRuntime.getRuntimeMode())
                    "setRuntimeMode" -> {
                        val mode = call.argument<String>("mode")
                        if (mode == null || !McpRuntime.setRuntimeMode(mode)) {
                            result.error("INVALID_ARGUMENT", "mode must be root or non_root", null)
                        } else result.success(mode)
                    }
                    "getMcpEnabled" -> result.success(McpRuntime.getMcpEnabled())
                    "getAutoGrantPermissions" -> result.success(McpRuntime.getAutoGrantPermissions())
                    "setAutoGrantPermissions" -> {
                        val enabled = call.argument<Boolean>("enabled")
                        if (enabled == null) {
                            result.error("INVALID_ARGUMENT", "enabled is required", null)
                        } else {
                            McpRuntime.setAutoGrantPermissions(enabled)
                            result.success(enabled)
                        }
                    }
                    "getLocale" -> result.success(McpRuntime.getLocale())
                    "setLocale" -> {
                        val languageCode = call.argument<String>("languageCode")
                        if (languageCode == null || !McpRuntime.setLocale(languageCode)) {
                             result.error("INVALID_ARGUMENT", "languageCode must be system, en, or zh", null)
                        } else {
                            result.success(null)
                        }
                    }
                    "setMcpEnabled" -> {
                        val enabled = call.argument<Boolean>("enabled")
                        if (enabled == null) {
                            result.error("INVALID_ARGUMENT", "enabled is required", null)
                        } else {
                            McpRuntime.setMcpEnabled(enabled)
                            if (enabled) {
                                executor.execute {
                                    McpRuntime.ensureUiInspectionService()
                                    mainHandler.post { startMcpService() }
                                }
                            } else {
                                stopService(Intent(this, McpForegroundService::class.java))
                                McpRuntime.stop()
                            }
                            result.success(enabled)
                        }
                    }
                    "createToken" -> {
                        try {
                            val mode = call.argument<String>("mode") ?: "reusable"
                            val duration = call.argument<Int>("durationSeconds")?.toLong()
                            val enabledTools = call.argument<List<String>>("enabledTools")
                            result.success(McpRuntime.tokenStore().create(mode, duration, enabledTools))
                        } catch (error: IllegalArgumentException) {
                            result.error("INVALID_ARGUMENT", error.message, null)
                        }
                    }
                    "listTokens" -> result.success(McpRuntime.tokenStore().list())
                    "listTokenTools" -> result.success(McpRuntime.tokenTools())
                    "setTokenTools" -> {
                        try {
                            val id = call.argument<String>("id") ?: ""
                            val enabledTools = call.argument<List<String>>("enabledTools") ?: emptyList()
                            result.success(McpRuntime.tokenStore().updateTools(id, enabledTools))
                        } catch (error: IllegalArgumentException) {
                            result.error("INVALID_ARGUMENT", error.message, null)
                        }
                    }
                    "deleteToken" -> result.success(McpRuntime.tokenStore().delete(call.argument<String>("id") ?: ""))
                    else -> result.notImplemented()
                }
            }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun startMcpService() {
        val intent = Intent(this, McpForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun manifestPermissionStatus(): List<Map<String, Any>> {
        val permissions = packageManager
            .getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.filterNot { it.contains("BIND_ACCESSIBILITY_SERVICE") }
            ?: emptyList()
        return permissions.map { permission ->
            mapOf(
                "name" to permission,
                "granted" to (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED),
            )
        }
    }

}
