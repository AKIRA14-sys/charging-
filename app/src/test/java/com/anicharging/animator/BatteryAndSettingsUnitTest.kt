package com.anicharging.animator

import com.anicharging.animator.battery.BatteryInfo
import com.anicharging.animator.settings.AppSettings
import com.anicharging.animator.settings.BatteryBarDesign
import com.anicharging.animator.settings.PositionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAndSettingsUnitTest {

    @Test
    fun testBatteryInfoDataClass() {
        val info = BatteryInfo(
            level = 85,
            isCharging = true,
            temperatureCelsius = 30.5f,
            temperatureFahrenheit = 86.9f,
            chargingSpeed = "25.0W Fast Charging",
            pluggedType = "AC Charger"
        )

        assertEquals(85, info.level)
        assertTrue(info.isCharging)
        assertEquals(30.5f, info.temperatureCelsius, 0.01f)
        assertEquals(86.9f, info.temperatureFahrenheit, 0.01f)
        assertEquals("25.0W Fast Charging", info.chargingSpeed)
        assertEquals("AC Charger", info.pluggedType)
    }

    @Test
    fun testAppSettingsDefaults() {
        val settings = AppSettings()

        assertEquals("sharingan", settings.animationCategory)
        assertEquals(PositionPreset.CENTER, settings.positionPreset)
        assertEquals(10, settings.displayDurationSeconds)
        assertEquals(BatteryBarDesign.RED_SHARINGAN, settings.batteryBarDesign)
        assertFalse(settings.performanceMode)
        assertTrue(settings.showTemperature)
        assertTrue(settings.showChargingSpeed)
        assertTrue(settings.showDateTime)
    }

    @Test
    fun testTemperatureConversion() {
        val tempCelsius = 25.0f
        val tempFahrenheit = (tempCelsius * 9.0f / 5.0f) + 32.0f
        assertEquals(77.0f, tempFahrenheit, 0.01f)
    }
}
