package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.communication.AndroidWearCommandEnvironment
import com.tomasrepcik.sensorbox.communication.WearCommandEnvironment
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementControlUseCase
import com.tomasrepcik.sensorbox.domain.measurement.WearRecordingController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {
    @Binds
    abstract fun bindWearRecordingController(implementation: WearMeasurementControlUseCase): WearRecordingController

    @Binds
    abstract fun bindWearCommandEnvironment(implementation: AndroidWearCommandEnvironment): WearCommandEnvironment
}
