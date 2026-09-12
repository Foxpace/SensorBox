package com.tomasrepcik.sensorbox.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesRepository(private val dataStore: DataStore<Preferences>) : AppPreferencesRepository {
    override val preferences: Flow<AppResult<AppPreferences>> = dataStore.data
        .map { values -> AppResult.success(values.toAppPreferences()) }
        .catch { error ->
            emit(AppResult.failure(AppError.from(AppErrorCode.PREFERENCES, "Read preferences", error)))
        }

    override suspend fun dispatch(intent: AppPreferencesIntent): AppResult<Unit> = suspendAppResult(
        AppErrorCode.PREFERENCES,
        "Update preferences",
    ) {
        dataStore.edit { values ->
            val updated = AppPreferencesReducer.reduce(values.toAppPreferences(), intent)
            values.write(updated)
        }
    }

    private fun Preferences.toAppPreferences(): AppPreferences = AppPreferences(
        onboarding = OnboardingPreferences(
            hasCompletedIntro = this[Keys.COMPLETED_INTRO] ?: false,
            hasAcceptedPolicy = this[Keys.ACCEPTED_POLICY] ?: false,
        ),
        recording = RecordingPreferences(
            gpsIntervalSeconds = this[Keys.GPS_INTERVAL] ?: 10,
            gpsMinDistanceMeters = this[Keys.GPS_DISTANCE] ?: 20,
            sensorSamplingPeriod = this[Keys.SAMPLING_PERIOD] ?: 0,
            stopRecordingOnLowBattery = this[Keys.LOW_BATTERY] ?: true,
            useWakeLock = this[Keys.WAKE_LOCK] ?: false,
        ),
        display = DisplayPreferences(
            keepPhoneDisplayOn = this[Keys.KEEP_PHONE_DISPLAY_ON] ?: false,
            keepWearDisplayOn = this[Keys.KEEP_DISPLAY_ON] ?: false,
            themeMode = themeMode(),
            dynamicColors = this[Keys.DYNAMIC_COLORS] ?: true,
        ),
    )

    private fun Preferences.themeMode(): AppThemeMode {
        val stored = this[Keys.THEME_MODE]
        return AppThemeMode.entries.firstOrNull { it.name == stored } ?: AppThemeMode.AUTOMATIC
    }

    private fun MutablePreferences.write(value: AppPreferences) {
        this[Keys.COMPLETED_INTRO] = value.onboarding.hasCompletedIntro
        this[Keys.ACCEPTED_POLICY] = value.onboarding.hasAcceptedPolicy
        this[Keys.GPS_INTERVAL] = value.recording.gpsIntervalSeconds
        this[Keys.GPS_DISTANCE] = value.recording.gpsMinDistanceMeters
        this[Keys.SAMPLING_PERIOD] = value.recording.sensorSamplingPeriod
        this[Keys.LOW_BATTERY] = value.recording.stopRecordingOnLowBattery
        this[Keys.WAKE_LOCK] = value.recording.useWakeLock
        this[Keys.KEEP_PHONE_DISPLAY_ON] = value.display.keepPhoneDisplayOn
        this[Keys.KEEP_DISPLAY_ON] = value.display.keepWearDisplayOn
        this[Keys.THEME_MODE] = value.display.themeMode.name
        this[Keys.DYNAMIC_COLORS] = value.display.dynamicColors
    }

    private object Keys {
        val COMPLETED_INTRO = booleanPreferencesKey("completed_intro")
        val ACCEPTED_POLICY = booleanPreferencesKey("accepted_policy")
        val GPS_INTERVAL = intPreferencesKey("gps_interval_seconds")
        val GPS_DISTANCE = intPreferencesKey("gps_min_distance_meters")
        val SAMPLING_PERIOD = intPreferencesKey("sensor_sampling_period")
        val LOW_BATTERY = booleanPreferencesKey("restrict_on_low_battery")
        val WAKE_LOCK = booleanPreferencesKey("use_wake_lock")
        val KEEP_PHONE_DISPLAY_ON = booleanPreferencesKey("keep_phone_display_on")
        val KEEP_DISPLAY_ON = booleanPreferencesKey("keep_wear_display_on")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }
}
