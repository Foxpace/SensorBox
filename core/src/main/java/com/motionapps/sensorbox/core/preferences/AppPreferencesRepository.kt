package com.motionapps.sensorbox.core.preferences

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val preferences: Flow<Result<AppPreferences>>

    suspend fun dispatch(intent: AppPreferencesIntent): Result<Unit>
}
