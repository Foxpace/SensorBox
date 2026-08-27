package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.data.measurements.AndroidMeasurementRepository
import com.tomasrepcik.sensorbox.domain.measurement.AndroidPhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageGateway
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageUseCase
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementControlUseCase
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementPermissionUseCase
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementPermissionsUseCase
import com.tomasrepcik.sensorbox.domain.measurement.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.RecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementRepository
import com.tomasrepcik.sensorbox.domain.sensors.AvailableSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.GetAvailableSensorsUseCase
import com.tomasrepcik.sensorbox.presentation.main.AndroidElapsedRealtimeClock
import com.tomasrepcik.sensorbox.presentation.main.ElapsedRealtimeClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {
    @Binds
    abstract fun bindPhoneRecordingController(
        implementation: AndroidPhoneRecordingController,
    ): PhoneRecordingController

    @Binds
    abstract fun bindDocumentStorageGateway(implementation: DocumentStorageUseCase): DocumentStorageGateway

    @Binds
    abstract fun bindAvailableSensorsUseCase(implementation: GetAvailableSensorsUseCase): AvailableSensorsUseCase

    @Binds
    abstract fun bindMeasurementPermissionsUseCase(
        implementation: MeasurementPermissionUseCase,
    ): MeasurementPermissionsUseCase

    @Binds
    abstract fun bindRecordingControlUseCase(implementation: MeasurementControlUseCase): RecordingControlUseCase

    @Binds
    abstract fun bindMeasurementRepository(implementation: AndroidMeasurementRepository): MeasurementRepository

    @Binds
    abstract fun bindElapsedRealtimeClock(implementation: AndroidElapsedRealtimeClock): ElapsedRealtimeClock
}
