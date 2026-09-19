package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.about.AndroidOpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.about.OpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.diagnostics.AndroidDiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.measurements.storage.AndroidMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import com.tomasrepcik.sensorbox.recording.AndroidPhoneRecordingController
import com.tomasrepcik.sensorbox.recording.DefaultRecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.PeerRecordingControl
import com.tomasrepcik.sensorbox.recording.PhoneRecordingController
import com.tomasrepcik.sensorbox.recording.PhoneRecordingSessionControl
import com.tomasrepcik.sensorbox.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.RecordingPermissionUseCase
import com.tomasrepcik.sensorbox.recording.RecordingPermissionsUseCase
import com.tomasrepcik.sensorbox.recording.active.AndroidElapsedRealtimeClock
import com.tomasrepcik.sensorbox.recording.active.ElapsedRealtimeClock
import com.tomasrepcik.sensorbox.recording.archive.DocumentRecordingArchiveRepository
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.recording.preview.AndroidDevicePreviewRepository
import com.tomasrepcik.sensorbox.recording.preview.DevicePreviewRepository
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSourcesUseCase
import com.tomasrepcik.sensorbox.recording.sources.AvailableSensorsUseCase
import com.tomasrepcik.sensorbox.recording.sources.GetAvailableSensorsUseCase
import com.tomasrepcik.sensorbox.recording.sources.ReceiveWatchSensorsUseCase
import com.tomasrepcik.sensorbox.recording.sources.RecordingSourceAvailability
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
