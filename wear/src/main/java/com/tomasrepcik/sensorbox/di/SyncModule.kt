package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.domain.sync.DefaultSyncWatchMeasurementsUseCase
import com.tomasrepcik.sensorbox.domain.sync.SyncWatchMeasurementsUseCase
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
