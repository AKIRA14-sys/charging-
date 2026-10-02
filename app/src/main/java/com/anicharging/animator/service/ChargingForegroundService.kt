package com.anicharging.animator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.anicharging.animator.battery.BatteryHelper

class ChargingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ani_charging_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_POWER_CONNECTED = "com.anicharging.animator.POWER_CONNECTED"
        const val ACTION_POWER_DISCONNECTED = "com.anicharging.animator.POWER_DISCONNECTED"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_POWER_CONNECTED -> {
                val notification = createNotification("Ani Charging Service Active")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                val batteryInfo = BatteryHelper.getBatteryInfo(this)
                updateNotification("Charger Connected (${batteryInfo.level}%) - ${batteryInfo.chargingSpeed}")

                // Trigger overlay animation service
                val overlayIntent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_OVERLAY
                }
                startService(overlayIntent)
            }
            ACTION_POWER_DISCONNECTED -> {
                // Hide overlay animation service
                val overlayIntent = Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_HIDE_OVERLAY
                }
                startService(overlayIntent)

                // Stop foreground service and self cleanup
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ani Charging Animation Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows notifications when device is charging and animating overlays"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ani Charging Animator")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(contentText))
    }
}
