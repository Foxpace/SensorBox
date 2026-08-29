package com.tomasrepcik.sensorbox.di

import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.RecordingCommandExchange
import com.tomasrepcik.sensorbox.wearoslib.protocol.RecordingCommandSender
import com.tomasrepcik.sensorbox.wearoslib.protocol.RecordingResultReceiver
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordingCommandExchangeModule {
    @Provides
    @Singleton
    fun provideRecordingCommandExchange(sendCommand: SendWearCommandUseCase) = RecordingCommandExchange(
        sendCommand = sendCommand,
        peerCapability = WEAR_APP_CAPABILITY,
        peerPath = WEAR_MESSAGE_PATH,
        peerName = "watch",
    )

    @Provides
    fun provideRecordingCommandSender(exchange: RecordingCommandExchange): RecordingCommandSender = exchange

    @Provides
    fun provideRecordingResultReceiver(exchange: RecordingCommandExchange): RecordingResultReceiver = exchange
}
