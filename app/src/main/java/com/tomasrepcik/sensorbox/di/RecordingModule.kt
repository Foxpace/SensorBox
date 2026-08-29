package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.data.measurements.AndroidMeasurementRepository
import com.tomasrepcik.sensorbox.domain.diagnostics.AndroidDiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.domain.diagnostics.DiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.domain.licenses.AndroidOpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.domain.licenses.OpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementRepository
import com.tomasrepcik.sensorbox.domain.preview.AndroidDevicePreviewRepository
import com.tomasrepcik.sensorbox.domain.preview.DevicePreviewRepository
import com.tomasrepcik.sensorbox.domain.recording.AndroidPhoneRecordingController
import com.tomasrepcik.sensorbox.domain.recording.DefaultRecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.recording.DocumentRecordingArchiveRepository
import com.tomasrepcik.sensorbox.domain.recording.PeerRecordingControl
import com.tomasrepcik.sensorbox.domain.recording.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.recording.PhoneRecordingSessionControl
import com.tomasrepcik.sensorbox.domain.recording.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.domain.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.recording.RecordingPermissionUseCase
import com.tomasrepcik.sensorbox.domain.recording.RecordingPermissionsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.AvailableRecordingSourcesUseCase
import com.tomasrepcik.sensorbox.domain.sensors.AvailableSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.GetAvailableSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.ReceiveWatchSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.RecordingSourceAvailability
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
    abstract fun bindRecordingArchiveRepository(
        implementation: DocumentRecordingArchiveRepository,
    ): RecordingArchiveRepository

    @Binds
    abstract fun bindAvailableSensorsUseCase(implementation: GetAvailableSensorsUseCase): AvailableSensorsUseCase

    @Binds
    abstract fun bindAvailableRecordingSourcesUseCase(
        implementation: RecordingSourceAvailability,
    ): AvailableRecordingSourcesUseCase

    @Binds
    abstract fun bindReceiveWatchSensorsUseCase(
        implementation: RecordingSourceAvailability,
    ): ReceiveWatchSensorsUseCase

    @Binds
    abstract fun bindRecordingPermissionsUseCase(
        implementation: RecordingPermissionUseCase,
    ): RecordingPermissionsUseCase

    @Binds
    abstract fun bindRecordingControlUseCase(implementation: DefaultRecordingControlUseCase): RecordingControlUseCase

    @Binds
    abstract fun bindPhoneRecordingSessionControl(
        implementation: DefaultRecordingControlUseCase,
    ): PhoneRecordingSessionControl

    @Binds
    abstract fun bindPeerRecordingControl(implementation: DefaultRecordingControlUseCase): PeerRecordingControl

    @Binds
    abstract fun bindMeasurementRepository(implementation: AndroidMeasurementRepository): MeasurementRepository

    @Binds
    abstract fun bindElapsedRealtimeClock(implementation: AndroidElapsedRealtimeClock): ElapsedRealtimeClock

    @Binds
    abstract fun bindDevicePreviewRepository(implementation: AndroidDevicePreviewRepository): DevicePreviewRepository

    @Binds
    abstract fun bindOpenSourceLicenseRepository(
        implementation: AndroidOpenSourceLicenseRepository,
    ): OpenSourceLicenseRepository

    @Binds
    abstract fun bindDiagnosticsShareFilePreparer(
        implementation: AndroidDiagnosticsShareFilePreparer,
    ): DiagnosticsShareFilePreparer
}
