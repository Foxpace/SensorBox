package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.communication.WearRecordingEnvironment
import com.tomasrepcik.sensorbox.communication.WearRecordingRequirementsUseCase
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementControlUseCase
import com.tomasrepcik.sensorbox.domain.measurement.WearRecordingControlUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {
    @Binds
    abstract fun bindWearRecordingControlUseCase(
        implementation: WearMeasurementControlUseCase,
    ): WearRecordingControlUseCase

    @Binds
    abstract fun bindWearRecordingRequirementsUseCase(
        implementation: WearRecordingEnvironment,
    ): WearRecordingRequirementsUseCase
}
