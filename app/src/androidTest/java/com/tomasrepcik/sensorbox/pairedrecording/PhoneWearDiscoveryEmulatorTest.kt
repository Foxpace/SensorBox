package com.tomasrepcik.sensorbox.pairedrecording

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.wearoslib.connection.GooglePlayWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOutcome
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingSettings
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class PhoneWearDiscoveryEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun givenPairedWearAppWhenPhoneRequestsSensorsThenWearReturnsItsCatalog() = runBlocking {
        val response = CompletableDeferred<WearCommand.AvailableSensors>()
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == PHONE_MESSAGE_PATH) {
                val command = WearCommandCodec.decode(event.data).getOrNull()
                if (command is WearCommand.AvailableSensors) response.complete(command)
            }
        }

        Tasks.await(messageClient.addListener(listener))
        try {
            val payload = WearCommandCodec.encode(WearCommand.RequestAvailableSensors).getOrThrow()
            GooglePlayWearConnectionRepository(context)
                .sendMessage(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, payload)
                .getOrThrow()

            val catalog = withTimeout(RESPONSE_TIMEOUT_MILLIS.milliseconds) { response.await() }
            assertTrue("Wear emulator returned no sensors", catalog.sensors.isNotEmpty())
            assertTrue(
                "Wear emulator returned an incomplete sensor catalog",
                catalog.sensors.all { sensor ->
                    sensor.stringType.isNotBlank() &&
                        sensor.maximumRange.isFinite() &&
                        sensor.resolution.isFinite() &&
                        sensor.power.isFinite()
                },
            )
        } finally {
            Tasks.await(messageClient.removeListener(listener))
        }
    }

    @Test
    fun givenWearNotificationsDeniedWhenPhoneStartsSensorRecordingThenWearAcceptsIt() = runBlocking {
        val commands = Channel<WearCommand>(Channel.UNLIMITED)
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == PHONE_MESSAGE_PATH) {
                WearCommandCodec.decode(event.data).getOrNull()?.let(commands::trySend)
            }
        }
        val connection = GooglePlayWearConnectionRepository(context)
        val sessionId = "permission-free-sensor-session"

        Tasks.await(messageClient.addListener(listener))
        try {
            connection.sendMessage(
                WEAR_APP_CAPABILITY,
                WEAR_MESSAGE_PATH,
                WearCommandCodec.encode(WearCommand.RequestAvailableSensors).getOrThrow(),
            ).getOrThrow()
            val catalog = awaitCommand<WearCommand.AvailableSensors>(commands)

            val start = WearCommand.StartRecording(
                sessionId = sessionId,
                request = WearRecordingRequest(
                    folderName = "paired_sensor_permission_test",
                    sensorIds = listOf(catalog.sensors.first().type),
                    includesGps = false,
                    settings = WearRecordingSettings(0,
                        stopOnLowBattery = true,
                        useWakeLock = false,
                        gpsIntervalSeconds = 1,
                        gpsMinDistanceMeters = 0
                    ),
                ),
            )
            connection.sendMessage(
                WEAR_APP_CAPABILITY,
                WEAR_MESSAGE_PATH,
                WearCommandCodec.encode(start).getOrThrow(),
            ).getOrThrow()

            val result = awaitCommand<WearCommand.RecordingResult>(commands) {
                it.sessionId == sessionId && it.operation == WearRecordingOperation.START
            }
            assertEquals(WearRecordingOutcome.SUCCEEDED, result.outcome)
        } finally {
            connection.sendMessage(
                WEAR_APP_CAPABILITY,
                WEAR_MESSAGE_PATH,
                WearCommandCodec.encode(
                    WearCommand.StopRecording(sessionId, WearStopReason.USER_REQUEST),
                ).getOrThrow(),
            )
            Tasks.await(messageClient.removeListener(listener))
        }
    }

    @Test
    fun givenWearLocationDeniedWhenPhoneStartsGpsThenResultExplainsPermission() = runBlocking {
        val commands = Channel<WearCommand>(Channel.UNLIMITED)
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == PHONE_MESSAGE_PATH) {
                WearCommandCodec.decode(event.data).getOrNull()?.let(commands::trySend)
            }
        }
        val connection = GooglePlayWearConnectionRepository(context)
        val sessionId = "gps-permission-diagnostic-session"

        Tasks.await(messageClient.addListener(listener))
        try {
            connection.sendMessage(
                WEAR_APP_CAPABILITY,
                WEAR_MESSAGE_PATH,
                WearCommandCodec.encode(
                    WearCommand.StartRecording(
                        sessionId = sessionId,
                        request = WearRecordingRequest(
                            folderName = "paired_gps_permission_test",
                            sensorIds = emptyList(),
                            includesGps = true,
                            settings = WearRecordingSettings(0,
                                stopOnLowBattery = true,
                                useWakeLock = false,
                                gpsIntervalSeconds = 1,
                                gpsMinDistanceMeters = 0
                            ),
                        ),
                    ),
                ).getOrThrow(),
            ).getOrThrow()

            val result = awaitCommand<WearCommand.RecordingResult>(commands) {
                it.sessionId == sessionId && it.operation == WearRecordingOperation.START
            }
            assertEquals(WearRecordingOutcome.FAILED, result.outcome)
            assertEquals("Validate watch recording permissions", result.errorOperation)
            assertEquals("Wear OS is missing required recording permissions", result.errorMessage)
            assertTrue(
                result.errorContext["missingPermissions"]
                    .orEmpty()
                    .contains("android.permission.ACCESS_FINE_LOCATION"),
            )
        } finally {
            Tasks.await(messageClient.removeListener(listener))
        }
    }

    private suspend inline fun <reified T : WearCommand> awaitCommand(
        commands: Channel<WearCommand>,
        crossinline matches: (T) -> Boolean = { true },
    ): T = withTimeout(RESPONSE_TIMEOUT_MILLIS.milliseconds) {
        commands.receiveAsFlow().filterIsInstance<T>().first { matches(it) }
    }

    private companion object {
        const val RESPONSE_TIMEOUT_MILLIS = 15_000L
    }
}
