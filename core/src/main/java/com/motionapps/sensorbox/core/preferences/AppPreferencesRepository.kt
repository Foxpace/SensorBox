package com.motionapps.sensorbox.core.preferences

import com.motionapps.sensorbox.core.error.AppResult
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val preferences: Flow<AppResult<AppPreferences>>

    suspend fun dispatch(intent: AppPreferencesIntent): AppResult<Unit>
}
