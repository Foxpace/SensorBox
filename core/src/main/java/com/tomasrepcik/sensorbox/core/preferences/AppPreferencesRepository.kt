package com.tomasrepcik.sensorbox.core.preferences

import com.tomasrepcik.sensorbox.core.failure.AppResult
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val preferences: Flow<AppResult<AppPreferences>>

    suspend fun dispatch(intent: AppPreferencesIntent): AppResult<Unit>
}
