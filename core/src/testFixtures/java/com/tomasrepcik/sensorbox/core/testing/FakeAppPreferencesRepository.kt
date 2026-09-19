package com.tomasrepcik.sensorbox.core.testing

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesReducer
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppPreferencesRepository(
    initial: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {
    private val mutablePreferences = MutableStateFlow(initial)

    override val preferences = mutablePreferences.map(AppResult.Companion::success)

    override suspend fun dispatch(intent: AppPreferencesIntent): AppResult<Unit> {
        mutablePreferences.value = AppPreferencesReducer.reduce(mutablePreferences.value, intent)
        return AppResult.success(Unit)
    }
}
