package com.anicharging.animator.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Locale

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float,
    val temperatureFahrenheit: Float,
    val chargingSpeed: String,
    val pluggedType: String
)

object BatteryHelper {

    fun getBatteryInfo(context: Context): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10.0f
        val tempFahrenheit = (tempCelsius * 9.0f / 5.0f) + 32.0f

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val pluggedType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val chargingSpeed = calculateChargingSpeed(context, isCharging, plugged)

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            temperatureCelsius = tempCelsius,
            temperatureFahrenheit = tempFahrenheit,
            chargingSpeed = chargingSpeed,
            pluggedType = pluggedType
        )
    }

    private fun calculateChargingSpeed(context: Context, isCharging: Boolean, plugged: Int): String {
        if (!isCharging) return "Not Charging"

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val microAmps = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val milliAmps = Math.abs(microAmps) / 1000

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageVolts = voltageMv / 1000.0f

        if (milliAmps > 0 && voltageVolts > 0) {
            val watts = (milliAmps / 1000.0f) * voltageVolts
            if (watts >= 15.0f) {
                return String.format(Locale.US, "%.1fW Fast Charging", watts)
            } else if (watts > 0) {
                return String.format(Locale.US, "%.1fW Normal Charging", watts)
            }
        }

        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast Charging"
            BatteryManager.BATTERY_PLUGGED_USB -> "Standard Charging"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            else -> "Unknown"
        }
    }
}
