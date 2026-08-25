package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.domain.measurement.AndroidPhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.AndroidRecordingWorkflowGateway
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageGateway
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageUseCase
import com.tomasrepcik.sensorbox.domain.measurement.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.RecordingWorkflowGateway
import com.tomasrepcik.sensorbox.domain.paired.PhoneWearCommandHandler
import com.tomasrepcik.sensorbox.domain.paired.PhoneWearCommandPolicy
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
    abstract fun bindRecordingWorkflowGateway(
        implementation: AndroidRecordingWorkflowGateway,
    ): RecordingWorkflowGateway

    @Binds
    abstract fun bindDocumentStorageGateway(implementation: DocumentStorageUseCase): DocumentStorageGateway

    @Binds
    abstract fun bindPhoneWearCommandPolicy(implementation: PhoneWearCommandHandler): PhoneWearCommandPolicy
}
