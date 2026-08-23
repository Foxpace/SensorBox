package com.motionapps.sensorservices.di

import android.content.Context
import com.motionapps.sensorbox.core.storage.DocumentStorage
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorbox.core.time.SystemEpochClock
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
    fun provideDocumentStorage(@ApplicationContext context: Context): DocumentStorage = NativeDocumentStorage(context)

    @Provides
    @Singleton
    fun provideEpochClock(): EpochClock = SystemEpochClock
}
