package com.tomasrepcik.sensorbox.wearoslib.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearFileTransferModule {
    @Binds
    @Singleton
    abstract fun bindWearFileTransferClient(implementation: GooglePlayWearFileTransferClient): WearFileTransferClient
}
