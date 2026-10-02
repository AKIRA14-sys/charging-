package com.anicharging.animator.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anicharging.animator.service.ChargingForegroundService

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val serviceIntent = Intent(context, ChargingForegroundService::class.java).apply {
                    action = ChargingForegroundService.ACTION_POWER_CONNECTED
                }
                context.startForegroundService(serviceIntent)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val serviceIntent = Intent(context, ChargingForegroundService::class.java).apply {
                    action = ChargingForegroundService.ACTION_POWER_DISCONNECTED
                }
                context.startForegroundService(serviceIntent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // System boot completed - ensure ready
            }
        }
    }
}
