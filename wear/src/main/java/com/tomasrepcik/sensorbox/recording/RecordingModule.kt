package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.pairedrecording.WearRecordingEnvironment
import com.tomasrepcik.sensorbox.pairedrecording.WearRecordingRequirementsUseCase
import com.tomasrepcik.sensorbox.recording.DefaultWatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.WatchRecordingControlUseCase
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
