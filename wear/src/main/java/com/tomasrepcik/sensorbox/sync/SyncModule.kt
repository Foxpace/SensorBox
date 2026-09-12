package com.tomasrepcik.sensorbox.sync

import com.tomasrepcik.sensorbox.sync.DefaultSyncWatchMeasurementsUseCase
import com.tomasrepcik.sensorbox.sync.SyncWatchMeasurementsUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindSyncWatchMeasurementsUseCase(
        implementation: DefaultSyncWatchMeasurementsUseCase,
    ): SyncWatchMeasurementsUseCase
}
