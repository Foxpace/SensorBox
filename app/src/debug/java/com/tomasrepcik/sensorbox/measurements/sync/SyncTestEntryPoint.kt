package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncTestEntryPoint {
    fun sync(): WatchSyncRepo
    fun commands(): SendWearCommandUseCase
    fun storage(): DocumentStorage
    fun transfers(): WearFileTransferClient
    fun destination(): WatchFileDestination
}
