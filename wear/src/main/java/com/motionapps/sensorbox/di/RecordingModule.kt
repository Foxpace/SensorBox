package com.motionapps.sensorbox.di

import com.motionapps.sensorbox.communication.AndroidWearCommandEnvironment
import com.motionapps.sensorbox.communication.WearCommandEnvironment
import com.motionapps.sensorbox.domain.measurement.WearMeasurementControlUseCase
import com.motionapps.sensorbox.domain.measurement.WearRecordingController
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
