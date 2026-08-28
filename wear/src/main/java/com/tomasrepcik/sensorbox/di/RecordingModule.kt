package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.communication.WearRecordingEnvironment
import com.tomasrepcik.sensorbox.communication.WearRecordingRequirementsUseCase
import com.tomasrepcik.sensorbox.domain.recording.DefaultWatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.recording.WatchRecordingControlUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {
    @Binds
    abstract fun bindWatchRecordingControlUseCase(
        implementation: DefaultWatchRecordingControlUseCase,
    ): WatchRecordingControlUseCase

    @Binds
    abstract fun bindWearRecordingRequirementsUseCase(
        implementation: WearRecordingEnvironment,
    ): WearRecordingRequirementsUseCase
}
