package com.motionapps.sensorbox.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.suspendAppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesRepository(private val dataStore: DataStore<Preferences>) : AppPreferencesRepository {
    override val preferences: Flow<AppResult<AppPreferences>> = dataStore.data
        .map { values -> AppResult.success(values.toAppPreferences()) }
        .catch { error ->
            if (error is CancellationException) throw error
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
        hasCompletedIntro = this[Keys.COMPLETED_INTRO] ?: false,
        hasAcceptedPolicy = this[Keys.ACCEPTED_POLICY] ?: false,
        gpsIntervalSeconds = this[Keys.GPS_INTERVAL] ?: 10,
        gpsMinDistanceMeters = this[Keys.GPS_DISTANCE] ?: 20,
        sensorSamplingPeriod = this[Keys.SAMPLING_PERIOD] ?: 0,
        restrictMeasurementOnLowBattery = this[Keys.LOW_BATTERY] ?: true,
        useWakeLock = this[Keys.WAKE_LOCK] ?: false,
        keepPhoneDisplayOn = this[Keys.KEEP_PHONE_DISPLAY_ON] ?: false,
        keepWearDisplayOn = this[Keys.KEEP_DISPLAY_ON] ?: false,
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.write(value: AppPreferences) {
        this[Keys.COMPLETED_INTRO] = value.hasCompletedIntro
        this[Keys.ACCEPTED_POLICY] = value.hasAcceptedPolicy
        this[Keys.GPS_INTERVAL] = value.gpsIntervalSeconds
        this[Keys.GPS_DISTANCE] = value.gpsMinDistanceMeters
        this[Keys.SAMPLING_PERIOD] = value.sensorSamplingPeriod
        this[Keys.LOW_BATTERY] = value.restrictMeasurementOnLowBattery
        this[Keys.WAKE_LOCK] = value.useWakeLock
        this[Keys.KEEP_PHONE_DISPLAY_ON] = value.keepPhoneDisplayOn
        this[Keys.KEEP_DISPLAY_ON] = value.keepWearDisplayOn
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
    }
}
