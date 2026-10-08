package com.doublecat.android_root_mcp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class McpForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        McpRuntime.initialize(this)
        createNotificationChannel()
        val chinese = McpRuntime.getLocale() == "zh"
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ARMCP")
            .setContentText(if (chinese) "MCP 服务正在运行" else "MCP server is running")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (McpRuntime.getMcpEnabled()) {
            if (McpRuntime.start()) return START_STICKY
            McpRuntime.setMcpEnabled(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        McpRuntime.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.armcp_mcp_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "armcp_mcp_server"
        const val NOTIFICATION_ID = 8787
    }
}
