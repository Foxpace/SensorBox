package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandExchange
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandSender
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingResultReceiver
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
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
        peerCapability = PHONE_APP_CAPABILITY,
        peerPath = PHONE_MESSAGE_PATH,
        peerName = "phone",
    )

    @Provides
    fun provideRecordingCommandSender(exchange: RecordingCommandExchange): RecordingCommandSender = exchange

    @Provides
    fun provideRecordingResultReceiver(exchange: RecordingCommandExchange): RecordingResultReceiver = exchange
}
