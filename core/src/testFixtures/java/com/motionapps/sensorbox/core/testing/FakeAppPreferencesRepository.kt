package com.motionapps.sensorbox.core.testing

import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.core.preferences.AppPreferencesIntent
import com.motionapps.sensorbox.core.preferences.AppPreferencesReducer
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppPreferencesRepository(
    initial: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {
    private val mutablePreferences = MutableStateFlow(initial)

    override val preferences = mutablePreferences.map(Result.Companion::success)

    override suspend fun dispatch(intent: AppPreferencesIntent): Result<Unit> {
        mutablePreferences.value = AppPreferencesReducer.reduce(mutablePreferences.value, intent)
        return Result.success(Unit)
    }
}
