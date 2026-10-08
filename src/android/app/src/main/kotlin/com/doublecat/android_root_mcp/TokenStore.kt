package com.doublecat.android_root_mcp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class TokenAccess(
    val id: String,
    val enabledTools: Set<String>,
)

class TokenStore(
    context: Context,
    private val validToolNames: Set<String>,
    private val defaultToolNames: Set<String>,
) {
    private val preferences = context.getSharedPreferences("android_root_mcp_tokens", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private val lock = Any()

    fun create(
        mode: String,
        durationSeconds: Long?,
        enabledTools: Collection<String>? = null,
    ): Map<String, Any?> {
        require(mode in MODES) { "unsupported_token_mode" }
        if (mode == "fixed_duration" && (durationSeconds == null || durationSeconds < 1)) {
            throw IllegalArgumentException("duration_required")
        }
        val tools = enabledTools?.toSet() ?: defaultToolNames
        if (!validToolNames.containsAll(tools)) throw IllegalArgumentException("unsupported_tool")

        val id = randomString(12)
        val secret = "armcp_${randomString(32)}"
        val now = System.currentTimeMillis()
        val expiresAt = if (mode == "fixed_duration") {
            now + (durationSeconds!! * 1000L)
        } else {
            null
        }
        val record = JSONObject()
            .put("id", id)
            .put("verifier", digest(secret))
            .put("mode", mode)
            .put("createdAt", now)
            .put("enabledTools", JSONArray(tools.toList().sorted()))
            .put("useCount", 0)
        if (expiresAt != null) record.put("expiresAt", expiresAt)

        synchronized(lock) {
            val records = records()
            records.put(record)
            save(records)
        }

        return metadata(record) + mapOf("secret" to secret)
    }

    fun authenticate(secret: String): TokenAccess? {
        val candidate = digest(secret)
        synchronized(lock) {
            val records = records()
            for (index in 0 until records.length()) {
                val record = records.getJSONObject(index)
                if (!constantTimeEquals(candidate, record.getString("verifier"))) continue
                val expiresAt = record.optLong("expiresAt", 0L)
                if (expiresAt > 0 && expiresAt <= System.currentTimeMillis()) return null
                if (record.getString("mode") == "one_time" && record.getInt("useCount") > 0) return null
                record.put("useCount", record.getInt("useCount") + 1)
                record.put("lastUsedAt", System.currentTimeMillis())
                save(records)
                return TokenAccess(record.getString("id"), enabledTools(record))
            }
        }
        return null
    }

    fun list(): List<Map<String, Any?>> = synchronized(lock) {
        val records = records()
        (0 until records.length()).map { metadata(records.getJSONObject(it)) }
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val records = records()
        for (index in 0 until records.length()) {
            if (records.getJSONObject(index).getString("id") == id) {
                records.remove(index)
                save(records)
                return@synchronized true
            }
        }
        false
    }

    fun updateTools(id: String, enabledTools: Collection<String>): Boolean = synchronized(lock) {
        val tools = enabledTools.toSet()
        if (!validToolNames.containsAll(tools)) throw IllegalArgumentException("unsupported_tool")
        val records = records()
        for (index in 0 until records.length()) {
            val record = records.getJSONObject(index)
            if (record.getString("id") == id) {
                record.put("enabledTools", JSONArray(tools.toList().sorted()))
                save(records)
                return@synchronized true
            }
        }
        false
    }

    private fun records(): JSONArray {
        val stored = JSONArray(preferences.getString(KEY, "[]"))
        val active = JSONArray()
        val allowedKeys = setOf(
            "id",
            "verifier",
            "mode",
            "createdAt",
            "expiresAt",
            "enabledTools",
            "useCount",
            "lastUsedAt",
        )
        for (index in 0 until stored.length()) {
            val record = stored.getJSONObject(index)
            // Discard records from older schemas instead of carrying legacy state fields forward.
            val keys = record.keys().asSequence().toSet()
            if (keys.all { it in allowedKeys }) active.put(record)
        }
        if (active.length() != stored.length()) save(active)
        return active
    }

    private fun save(records: JSONArray) {
        preferences.edit().putString(KEY, records.toString()).apply()
    }

    private fun metadata(record: JSONObject): Map<String, Any?> = mapOf(
        "id" to record.getString("id"),
        "mode" to record.getString("mode"),
        "createdAt" to record.getLong("createdAt"),
        "expiresAt" to nullableLong(record, "expiresAt"),
        "enabledTools" to enabledTools(record).toList().sorted(),
        "useCount" to record.getInt("useCount"),
    )

    private fun enabledTools(record: JSONObject): Set<String> {
        val stored = record.optJSONArray("enabledTools")
        if (stored == null) return defaultToolNames
        return (0 until stored.length())
            .map { stored.optString(it) }
            .filter { it in validToolNames }
            .toSet()
    }

    private fun nullableLong(record: JSONObject, key: String): Long? {
        return if (record.has(key)) record.optLong(key) else null
    }

    private fun randomString(bytes: Int): String {
        val value = ByteArray(bytes)
        random.nextBytes(value)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value)
    }

    private fun digest(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(
            left.toByteArray(Charsets.US_ASCII),
            right.toByteArray(Charsets.US_ASCII),
        )
    }

    companion object {
        private const val KEY = "tokens"
        private val MODES = setOf("one_time", "reusable", "fixed_duration", "unlimited")
    }
}
