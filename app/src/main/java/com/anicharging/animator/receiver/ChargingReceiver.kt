package com.anicharging.animator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anicharging.animator.service.ChargingForegroundService

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                try {
                    val serviceIntent = Intent(context, ChargingForegroundService::class.java).apply {
                        action = ChargingForegroundService.ACTION_POWER_CONNECTED
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                try {
                    val serviceIntent = Intent(context, ChargingForegroundService::class.java).apply {
                        action = ChargingForegroundService.ACTION_POWER_DISCONNECTED
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // System boot completed - ensure ready
            }
        }
    }
}
