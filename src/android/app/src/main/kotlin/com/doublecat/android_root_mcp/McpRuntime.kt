package com.doublecat.android_root_mcp

import android.content.Context
import android.content.SharedPreferences

/** Process-level owner for the MCP server, independent of the Flutter activity. */
object McpRuntime {
    private const val SETTINGS = "android_root_mcp_settings"

    private lateinit var appContext: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var rootProbe: RootProbe
    private lateinit var permissionManager: RootPermissionManager
    private lateinit var tokenStore: TokenStore
    private lateinit var server: McpHttpServer
    private lateinit var toolExecutor: AndroidToolExecutor

    @Synchronized
    fun initialize(context: Context) {
        if (::server.isInitialized) return
        appContext = context.applicationContext
        preferences = appContext.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
        rootProbe = RootProbe()
        permissionManager = RootPermissionManager(appContext, rootProbe)
        val registry = ToolRegistry()
        tokenStore = TokenStore(appContext, registry.names(), registry.defaultNames())
        server = McpHttpServer(
            tokenStore = tokenStore,
            rootProbe = rootProbe,
            localeProvider = ::getEffectiveLocale,
            localeSetter = ::setLocale,
            toolExecutorProvider = { toolExecutor },
        )
        toolExecutor = AndroidToolExecutor(
            appContext,
            rootProbe,
            { server.isRunning() },
            { getRuntimeMode() },
            permissionManager,
        )
    }

    @Synchronized
    fun start(): Boolean {
        checkInitialized()
        return server.start()
    }

    @Synchronized
    fun stop() {
        if (::server.isInitialized) server.stop()
    }

    fun isRunning(): Boolean = ::server.isInitialized && server.isRunning()

    fun health(): Map<String, Any?> {
        checkInitialized()
        return mapOf(
            "platform" to "android",
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            "applicationId" to appContext.packageName,
            "operationPermission" to "read_only",
            "debugUrl" to server.debugUrl,
            "mcpUrl" to server.mcpUrl,
            "mcpRunning" to server.isRunning(),
        )
    }

    fun checkRoot(requestAuthorization: Boolean = false): Map<String, Any?> {
        checkInitialized()
        return rootProbe.check(requestAuthorization)
    }

    fun grantAllPermissions(): Map<String, Any?> {
        checkInitialized()
        if (getRuntimeMode() != "root") {
            return mapOf(
                "success" to false,
                "state" to "root_mode_required",
                "reason" to "switch_to_root_mode",
                "permissions" to emptyList<Map<String, Any?>>(),
            )
        }
        return permissionManager.grantAll()
    }

    fun ensureUiInspectionService(): Map<String, Any?> {
        checkInitialized()
        return permissionManager.ensureUiInspectionService()
    }

    fun getMcpEnabled(): Boolean = preferences.getBoolean("mcp_enabled", false)

    fun setMcpEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("mcp_enabled", enabled).apply()
    }

    fun getAutoGrantPermissions(): Boolean = preferences.getBoolean("auto_grant_permissions", false)

    fun setAutoGrantPermissions(enabled: Boolean) {
        preferences.edit().putBoolean("auto_grant_permissions", enabled).apply()
    }

    fun accessibilityStatus(): Map<String, Any?> = permissionManager.accessibilityStatus(
        preferences.getBoolean("talkback_adaptation", false),
    )

    fun setTalkBackAdaptation(enabled: Boolean): Map<String, Any?> {
        preferences.edit().putBoolean("talkback_adaptation", enabled).apply()
        return accessibilityStatus()
    }

    fun isTalkBackAutoUseAllowed(): Boolean =
        getRuntimeMode() == "root" && preferences.getBoolean("talkback_adaptation", false)

    fun getRuntimeMode(): String = preferences.getString("runtime_mode", "root") ?: "root"

    fun setRuntimeMode(mode: String): Boolean {
        if (mode != "root" && mode != "non_root") return false
        return preferences.edit().putString("runtime_mode", mode).commit()
    }

    fun getLocale(): String = preferences.getString("language_code", "system") ?: "system"

    fun getEffectiveLocale(): String {
        val preference = getLocale()
        if (preference != "system") return preference
        return if (java.util.Locale.getDefault().language == "zh") "zh" else "en"
    }

    fun setLocale(languageCode: String): Boolean {
        if (languageCode != "system" && languageCode != "en" && languageCode != "zh") return false
        return preferences.edit().putString("language_code", languageCode).commit()
    }

    fun tokenStore(): TokenStore {
        checkInitialized()
        return tokenStore
    }

    fun tokenTools(): List<Map<String, Any?>> {
        checkInitialized()
        return ToolRegistry().list().map { tool ->
            mapOf(
                "name" to tool.name,
                "readOnly" to tool.readOnly,
            )
        }
    }

    fun serverUrls(): Pair<String, String> {
        checkInitialized()
        return server.debugUrl to server.mcpUrl
    }

    private fun checkInitialized() {
        check(::server.isInitialized) { "mcp_runtime_not_initialized" }
    }
}
