package com.doublecat.android_root_mcp

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.TimeUnit

class RootPermissionManager(
    private val context: Context,
    private val rootProbe: RootProbe,
) {
    data class TemporaryTalkBackSession(
        val previousServices: String,
        val previousAccessibilityEnabled: String,
        val changed: Boolean,
    )

    fun accessibilityStatus(talkBackAdaptation: Boolean): Map<String, Any?> {
        val resolver = context.contentResolver
        val enabled = android.provider.Settings.Secure.getString(
            resolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val armcp = "${context.packageName}/${ArmcpAccessibilityService::class.java.name}"
        val talkBackPackages = listOf(
            "com.google.android.marvin.talkback",
            "com.google.android.accessibility.talkback",
        )
        val installed = talkBackPackages.any { packageName ->
            runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.isSuccess
        }
        val talkBackEnabled = talkBackPackages.any { enabled.contains(it) }
        return mapOf(
            "serviceEnabled" to enabled.split(':').contains(armcp),
            "talkBackEnabled" to talkBackEnabled,
            "talkBackInstalled" to installed,
            "talkBackAdaptation" to talkBackAdaptation,
            "requiresSettings" to (
                !enabled.split(':').contains(armcp) ||
                    (talkBackAdaptation && !talkBackEnabled)
                ),
            "reason" to if (talkBackEnabled) "talkback_enabled" else "talkback_not_enabled",
        )
    }

    fun beginTemporaryTalkBack(): TemporaryTalkBackSession? {
        if (rootProbe.check()["available"] != true) return null
        return try {
            val currentServices = runRootCommand("settings get secure enabled_accessibility_services")
                .output.trim().takeUnless { it == "null" }.orEmpty()
            val previousAccessibilityEnabled = runRootCommand("settings get secure accessibility_enabled")
                .output.trim().takeUnless { it == "null" }.orEmpty()
            val talkBack = talkBackComponent() ?: return null
            val services = currentServices.split(':').filter { it.isNotBlank() }.toMutableSet()
            if (talkBack in services && previousAccessibilityEnabled == "1") {
                return TemporaryTalkBackSession(
                    previousServices = currentServices,
                    previousAccessibilityEnabled = previousAccessibilityEnabled,
                    changed = false,
                )
            }
            services += talkBack
            val command = "settings put secure enabled_accessibility_services ${shellQuote(services.joinToString(":"))}; settings put secure accessibility_enabled 1"
            val nextServices = services.joinToString(":")
            val result = runRootCommand(command)
            val servicesEnabled = runRootCommand("settings get secure enabled_accessibility_services")
                .output.contains(talkBack)
            val accessibilityEnabled = runRootCommand("settings get secure accessibility_enabled")
                .output.trim() == "1"
            if (result.exitCode != 0 || !servicesEnabled || !accessibilityEnabled) null
            else TemporaryTalkBackSession(
                previousServices = currentServices,
                previousAccessibilityEnabled = previousAccessibilityEnabled,
                changed = currentServices != nextServices || previousAccessibilityEnabled != "1",
            )
        } catch (_: Exception) {
            null
        }
    }

    fun endTemporaryTalkBack(session: TemporaryTalkBackSession) {
        if (!session.changed) return
        runCatching {
            val services = shellQuote(session.previousServices)
            val enabled = if (session.previousAccessibilityEnabled.isBlank()) "0" else session.previousAccessibilityEnabled
            runRootCommand(
                "settings put secure enabled_accessibility_services $services; " +
                    "settings put secure accessibility_enabled ${shellQuote(enabled)}",
            )
        }
    }

    private fun talkBackComponent(): String? = listOf(
        "com.google.android.marvin.talkback/.TalkBackService",
        "com.google.android.accessibility.talkback/com.google.android.accessibility.talkback.TalkBackService",
    ).firstOrNull { component ->
        runCatching {
            context.packageManager.getApplicationInfo(component.substringBefore('/'), 0)
        }.isSuccess
    }
    fun grantAll(): Map<String, Any?> {
        val root = rootProbe.check()
        if (root["available"] != true) {
            return mapOf(
                "success" to false,
                "state" to "root_unavailable",
                "reason" to root["reason"],
                "permissions" to emptyList<Map<String, Any?>>(),
            )
        }

        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val outcomes = requested.map { permission -> grant(permission) }.toMutableList()
        outcomes += ensureUiInspectionService()
        return mapOf(
            "success" to outcomes.all { it["status"] == "granted" || it["status"] == "already_granted" },
            "state" to "completed",
            "reason" to if (outcomes.all { it["status"] == "granted" || it["status"] == "already_granted" }) {
                "all_declared_permissions_granted"
            } else {
                "some_permissions_refused"
            },
            "permissions" to outcomes,
        )
    }

    private fun grant(permission: String): Map<String, Any?> {
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return mapOf("name" to permission, "status" to "already_granted")
        }

        return try {
            val command = "pm grant ${shellQuote(context.packageName)} ${shellQuote(permission)}"
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(3, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                mapOf("name" to permission, "status" to "failed", "reason" to "grant_timeout")
            } else if (process.exitValue() == 0 &&
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            ) {
                mapOf("name" to permission, "status" to "granted")
            } else {
                mapOf("name" to permission, "status" to "failed", "reason" to "android_refused_grant")
            }
        } catch (_: Exception) {
            mapOf("name" to permission, "status" to "failed", "reason" to "grant_command_failed")
        }
    }

    /** Enable the declared service through Root so UI inspection does not need
     * to compete for Android's singleton UiAutomation registration. */
    fun ensureUiInspectionService(): Map<String, Any?> {
        val component = "${context.packageName}/${ArmcpAccessibilityService::class.java.name}"
        return try {
            val current = runRootCommand("settings get secure enabled_accessibility_services")
                .output.trim().takeUnless { it == "null" }.orEmpty()
            val enabled = current.split(':').filter { it.isNotBlank() }
            if (component in enabled) {
                mapOf("name" to "ACCESSIBILITY_SERVICE", "status" to "already_granted")
            } else {
                val next = (enabled + component).joinToString(":")
                val command = "settings put secure enabled_accessibility_services ${shellQuote(next)}; settings put secure accessibility_enabled 1"
                val result = runRootCommand(command)
                val verified = runRootCommand("settings get secure enabled_accessibility_services")
                    .output.contains(component)
                if (result.exitCode == 0 && verified) {
                    mapOf("name" to "ACCESSIBILITY_SERVICE", "status" to "granted")
                } else {
                    mapOf("name" to "ACCESSIBILITY_SERVICE", "status" to "failed", "reason" to "android_refused_enable")
                }
            }
        } catch (_: Exception) {
            mapOf("name" to "ACCESSIBILITY_SERVICE", "status" to "failed", "reason" to "enable_command_failed")
        }
    }

    private fun runRootCommand(command: String): RootCommandResult {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val completed = process.waitFor(3, TimeUnit.SECONDS)
        if (!completed) process.destroyForcibly()
        return RootCommandResult(output, if (completed) process.exitValue() else 124)
    }

    private data class RootCommandResult(val output: String, val exitCode: Int)

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
