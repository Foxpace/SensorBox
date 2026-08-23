package com.motionapps.sensorbox.di

import com.motionapps.sensorbox.domain.measurement.AndroidPhoneRecordingController
import com.motionapps.sensorbox.domain.measurement.AndroidRecordingWorkflowGateway
import com.motionapps.sensorbox.domain.measurement.DocumentStorageGateway
import com.motionapps.sensorbox.domain.measurement.DocumentStorageUseCase
import com.motionapps.sensorbox.domain.measurement.PhoneRecordingController
import com.motionapps.sensorbox.domain.measurement.RecordingWorkflowGateway
import com.motionapps.sensorbox.domain.paired.PhoneWearCommandHandler
import com.motionapps.sensorbox.domain.paired.PhoneWearCommandPolicy
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
