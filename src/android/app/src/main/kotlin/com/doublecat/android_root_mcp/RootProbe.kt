package com.doublecat.android_root_mcp

import java.util.concurrent.TimeUnit

class RootProbe {
    fun check(requestAuthorization: Boolean = false): Map<String, Any?> {
        return try {
            val process = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(
                if (requestAuthorization) 60 else 2,
                TimeUnit.SECONDS,
            )
            if (!completed) {
                process.destroyForcibly()
                return status("error", "root_probe_timeout", false, requestAuthorization)
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val isRoot = process.exitValue() == 0 && Regex("uid=0(?:\\D|$)").containsMatchIn(output)
            if (isRoot) {
                status("available", "su_uid_0", true, requestAuthorization)
            } else {
                status("unavailable", "su_uid_0_not_confirmed", false, requestAuthorization)
            }
        } catch (_: Exception) {
            status("unavailable", "su_command_unavailable", false, requestAuthorization)
        }
    }

    private fun status(
        state: String,
        reason: String,
        available: Boolean,
        requested: Boolean,
    ): Map<String, Any?> {
        return mapOf(
            "state" to state,
            "available" to available,
            "reason" to reason,
            "attempt" to if (requested) "authorization_request" else "status_check",
        )
    }
}
