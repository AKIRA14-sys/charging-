package com.anicharging.animator.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ani_charging_settings")

enum class PositionPreset {
    STATUS_AREA,
    CENTER,
    CUSTOM_DRAG
}

enum class BatteryBarDesign {
    NORMAL,
    RED_SHARINGAN,
    COMBINED
}

data class AppSettings(
    val animationCategory: String = "sharingan",
    val positionPreset: PositionPreset = PositionPreset.CENTER,
    val customX: Float = 0f,
    val customY: Float = 0f,
    val animationScale: Float = 1.0f,
    val displayDurationSeconds: Int = 10, // 0 means until unplugged
    val batteryBarDesign: BatteryBarDesign = BatteryBarDesign.RED_SHARINGAN,
    val performanceMode: Boolean = false, // FPS cap for low end hardware
    val showTemperature: Boolean = true,
    val showChargingSpeed: Boolean = true,
    val showDateTime: Boolean = true,
    val soundEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ANIMATION_CATEGORY = stringPreferencesKey("animation_category")
        val POSITION_PRESET = stringPreferencesKey("position_preset")
        val CUSTOM_X = floatPreferencesKey("custom_x")
        val CUSTOM_Y = floatPreferencesKey("custom_y")
        val ANIMATION_SCALE = floatPreferencesKey("animation_scale")
        val DISPLAY_DURATION = intPreferencesKey("display_duration")
        val BATTERY_BAR_DESIGN = stringPreferencesKey("battery_bar_design")
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val SHOW_TEMPERATURE = booleanPreferencesKey("show_temperature")
        val SHOW_CHARGING_SPEED = booleanPreferencesKey("show_charging_speed")
        val SHOW_DATE_TIME = booleanPreferencesKey("show_date_time")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            animationCategory = prefs[Keys.ANIMATION_CATEGORY] ?: "sharingan",
            positionPreset = PositionPreset.valueOf(
                prefs[Keys.POSITION_PRESET] ?: PositionPreset.CENTER.name
            ),
            customX = prefs[Keys.CUSTOM_X] ?: 0f,
            customY = prefs[Keys.CUSTOM_Y] ?: 0f,
            animationScale = prefs[Keys.ANIMATION_SCALE] ?: 1.0f,
            displayDurationSeconds = prefs[Keys.DISPLAY_DURATION] ?: 10,
            batteryBarDesign = BatteryBarDesign.valueOf(
                prefs[Keys.BATTERY_BAR_DESIGN] ?: BatteryBarDesign.RED_SHARINGAN.name
            ),
            performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: false,
            showTemperature = prefs[Keys.SHOW_TEMPERATURE] ?: true,
            showChargingSpeed = prefs[Keys.SHOW_CHARGING_SPEED] ?: true,
            showDateTime = prefs[Keys.SHOW_DATE_TIME] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true
        )
    }

    suspend fun updateAnimationCategory(category: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ANIMATION_CATEGORY] = category }
    }

    suspend fun updatePositionPreset(preset: PositionPreset) {
        context.dataStore.edit { prefs -> prefs[Keys.POSITION_PRESET] = preset.name }
    }

    suspend fun updateCustomPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_X] = x
            prefs[Keys.CUSTOM_Y] = y
        }
    }

    suspend fun updateAnimationScale(scale: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.ANIMATION_SCALE] = scale }
    }

    suspend fun updateDisplayDuration(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.DISPLAY_DURATION] = seconds }
    }

    suspend fun updateBatteryBarDesign(design: BatteryBarDesign) {
        context.dataStore.edit { prefs -> prefs[Keys.BATTERY_BAR_DESIGN] = design.name }
    }

    suspend fun updatePerformanceMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PERFORMANCE_MODE] = enabled }
    }
}
