package com.motionapps.sensorbox.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.motionapps.sensorbox.core.error.AppErrorCode
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DataStoreAppPreferencesRepositoryTest {
    @Test
    fun `Given an empty legacy store When preferences are read Then every historical default is preserved`() = runTest {
        val repository = DataStoreAppPreferencesRepository(InMemoryPreferencesDataStore())

        val preferences = repository.preferences.first().getOrNull()

        assertEquals(OnboardingPreferences(), preferences?.onboarding)
        assertEquals(RecordingPreferences(), preferences?.recording)
        assertEquals(DisplayPreferences(), preferences?.display)
    }

    @Test
    fun `Given historical keys When preferences round trip Then names and values stay compatible`() = runTest {
        val dataStore = InMemoryPreferencesDataStore(
            mutablePreferencesOf(
                booleanPreferencesKey("completed_intro") to true,
                booleanPreferencesKey("accepted_policy") to true,
                intPreferencesKey("gps_interval_seconds") to 33,
                intPreferencesKey("gps_min_distance_meters") to 7,
                intPreferencesKey("sensor_sampling_period") to 2,
                booleanPreferencesKey("restrict_on_low_battery") to false,
                booleanPreferencesKey("use_wake_lock") to true,
                booleanPreferencesKey("keep_phone_display_on") to true,
                booleanPreferencesKey("keep_wear_display_on") to true,
            ),
        )
        val repository = DataStoreAppPreferencesRepository(dataStore)

        val before = repository.preferences.first().getOrNull()
        val update = repository.dispatch(AppPreferencesIntent.SetGpsInterval(44))
        val after = repository.preferences.first().getOrNull()

        assertTrue(update.isSuccess)
        assertTrue(before?.onboarding?.hasCompletedIntro == true)
        assertEquals(7, before?.recording?.gpsMinDistanceMeters)
        assertFalse(before?.recording?.restrictMeasurementOnLowBattery ?: true)
        assertTrue(before?.display?.keepPhoneDisplayOn == true)
        assertEquals(44, after?.recording?.gpsIntervalSeconds)
        assertEquals(44, dataStore.current[intPreferencesKey("gps_interval_seconds")])
    }

    @Test
    fun `Given a read failure When preferences are collected Then a stable preferences error is emitted`() = runTest {
        val dataStore = FlowPreferencesDataStore(
            flow { throw IOException("read failed") },
        )
        val repository = DataStoreAppPreferencesRepository(dataStore)

        val result = repository.preferences.first()

        assertEquals(AppErrorCode.PREFERENCES, result.errorOrNull()?.code)
    }

    @Test
    fun `Given cancellation When preferences are collected Then cancellation propagates`() = runTest {
        val dataStore = FlowPreferencesDataStore(
            flow { awaitCancellation() },
        )
        val repository = DataStoreAppPreferencesRepository(dataStore)
        var emitted = false

        val collection = launch {
            repository.preferences.collect { emitted = true }
        }
        runCurrent()
        collection.cancelAndJoin()

        assertFalse(emitted)
    }

    private class InMemoryPreferencesDataStore(initial: Preferences = mutablePreferencesOf()) :
        DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        val current: Preferences
            get() = state.value

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.value = transform(state.value)
            return state.value
        }
    }

    private class FlowPreferencesDataStore(override val data: Flow<Preferences>) : DataStore<Preferences> {
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            error("updateData is not used by this test")
    }
}
