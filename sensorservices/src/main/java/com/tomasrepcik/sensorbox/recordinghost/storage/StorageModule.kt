package com.tomasrepcik.sensorbox.recordinghost.storage

import android.content.Context
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.core.storage.NativeDocumentStorage
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.core.time.SystemEpochClock
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideDocumentStorage(@ApplicationContext context: Context, syncLock: MeasurementSyncLock): DocumentStorage =
        NativeDocumentStorage(context, syncLock)

    @Provides
    @Singleton
    fun provideMeasurementSyncLock(): MeasurementSyncLock = MeasurementSyncLock()

    @Provides
    @Singleton
    fun provideEpochClock(): EpochClock = SystemEpochClock

    @Provides
    @Singleton
    fun provideRecordingSessionStore(): RecordingSessionStore = RecordingSessionStore()
}
