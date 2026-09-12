package com.tomasrepcik.sensorbox.pairedrecording

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.sync.readBytes
import com.tomasrepcik.sensorbox.measurements.sync.selectedArchive
import com.tomasrepcik.sensorbox.wearoslib.connection.GooglePlayWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandExchange
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingSettings
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class PhonePairedRecordingEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun givenPairedEmulatorsWhenPhoneRecordsOnWearAndSyncsThenPhoneReceivesRealMeasurement() = runBlocking {
        // Given
        assumeFalse(
            "Paired recording test must run on the phone emulator",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH),
        )
        val measurementName = "000_PHONE_DRIVEN_WEAR_${System.currentTimeMillis()}"
        val sessionId = "phone-driven-wear-${System.currentTimeMillis()}"
        val archive = selectedArchive(context)
        val peer = PairedWearRecordingClient(context)
        peer.open()

        try {
            val accelerometer = peer.availableSensors()
                .firstOrNull { sensor -> sensor.type == Sensor.TYPE_ACCELEROMETER }
                ?: throw AssertionError("Wear emulator has no accelerometer")

            // When
            peer.start(
                sessionId = sessionId,
                request = WearRecordingRequest(
                    folderName = measurementName,
                    sensorIds = listOf(accelerometer.type),
                    includesGps = false,
                    durationMillis = RECORDING_MILLIS,
                    settings = WearRecordingSettings(0,
                        stopOnLowBattery = true,
                        useWakeLock = false,
                        gpsIntervalSeconds = 1,
                        gpsMinDistanceMeters = 0
                    ),
                ),
            ).getOrThrow()
            peer.awaitAutomaticStop(sessionId)
            peer.sync()
            val receivedFiles = awaitReceivedFiles(archive, "WEAR_$measurementName")

            // Then
            assertMeasurementMetadata(
                file = receivedFiles.getValue(METADATA_FILE_NAME),
                measurementName = measurementName,
                sensor = accelerometer,
            )
            assertAccelerometerSamples(receivedFiles.getValue(ACCELEROMETER_FILE_NAME))
            val metadata = JSONObject(receivedFiles.getValue(METADATA_FILE_NAME).readBytes(context).decodeToString())
            assertEquals(sessionId, metadata.getString("sessionId"))
            assertEquals("watch", metadata.getString("device"))
        } finally {
            peer.close()
            archive.findFile("WEAR_$measurementName")?.let { measurement ->
                check(measurement.delete()) { "Could not remove test measurement $measurementName" }
            }
        }
    }

    private suspend fun awaitReceivedFiles(
        archive: DocumentFile,
        measurementName: String,
    ): Map<String, DocumentFile> {
        repeat(FILE_WAIT_ATTEMPTS) {
            val files = archive.findFile(measurementName)?.listFiles().orEmpty().associateBy { checkNotNull(it.name) }
            val metadata = files[METADATA_FILE_NAME]
            val accelerometer = files[ACCELEROMETER_FILE_NAME]
            if (
                metadata?.length()?.let { length -> length > 0L } == true &&
                accelerometer?.readBytes(context)?.decodeToString()?.lines()?.size?.let { rows -> rows >= MINIMUM_CSV_ROWS } == true
            ) {
                return files
            }
            delay(FILE_WAIT_INTERVAL_MILLIS.milliseconds)
        }
        throw AssertionError(
            "Timed out waiting for transferred watch measurement $measurementName in ${archive.uri}",
        )
    }

    private fun assertMeasurementMetadata(file: DocumentFile, measurementName: String, sensor: WearSensorInfo) {
        val metadata = JSONObject(file.readBytes(context).decodeToString())
        assertEquals(measurementName, metadata.getString("folder"))
        val recordedTypes = metadata.getJSONArray("ranges").let { ranges ->
            buildSet {
                repeat(ranges.length()) { index -> add(ranges.getJSONObject(index).getInt("type")) }
            }
        }
        assertEquals(setOf(sensor.type), recordedTypes)
    }

    private fun assertAccelerometerSamples(file: DocumentFile) {
        val rows = file.readBytes(context).decodeToString().lineSequence().filter(String::isNotBlank).toList()
        assertTrue("Expected transferred Wear samples in $file, got $rows", rows.size >= MINIMUM_CSV_ROWS)
        assertEquals(ACCELEROMETER_HEADER, rows.first())
        val timestamps = rows.drop(1).map { row ->
            val columns = row.split(';')
            assertEquals("Malformed transferred Wear row: $row", ACCELEROMETER_COLUMN_COUNT, columns.size)
            assertTrue("Non-numeric transferred Wear row: $row", columns.all { value -> value.toDoubleOrNull() != null })
            columns.first().toLong()
        }
        assertEquals(timestamps.sorted(), timestamps)
    }

    private companion object {
        const val RECORDING_MILLIS = 3_000L
        const val FILE_WAIT_ATTEMPTS = 120
        const val FILE_WAIT_INTERVAL_MILLIS = 500L
        const val MINIMUM_CSV_ROWS = 2
        const val METADATA_FILE_NAME = "extra.json"
        const val ACCELEROMETER_FILE_NAME = "accelerometer.csv"
        const val ACCELEROMETER_HEADER = "t_sensor;x;y;z;accuracy"
        const val ACCELEROMETER_COLUMN_COUNT = 5
    }
}

private class PairedWearRecordingClient(context: Context) {
    private val incomingCommands = Channel<WearCommand>(Channel.UNLIMITED)
    private val sendCommand = SendWearCommandUseCase(
        SendWearMessageUseCase(GooglePlayWearConnectionRepository(context)),
    )
    private val recordingExchange = RecordingCommandExchange(
        sendCommand = sendCommand,
        peerCapability = WEAR_APP_CAPABILITY,
        peerPath = WEAR_MESSAGE_PATH,
        peerName = "watch",
    )
    private val messageClient = Wearable.getMessageClient(context)
    private val listener = MessageClient.OnMessageReceivedListener { event ->
        if (event.path != PHONE_MESSAGE_PATH) return@OnMessageReceivedListener
        when (val command = WearCommandCodec.decode(event.data).getOrNull()) {
            is WearCommand.RecordingResult -> recordingExchange.receive(command)
            null -> Unit
            else -> incomingCommands.trySend(command)
        }
    }
    fun open() {
        Tasks.await(messageClient.addListener(listener))
    }

    fun close() {
        Tasks.await(messageClient.removeListener(listener))
        incomingCommands.close()
    }

    suspend fun availableSensors(): List<WearSensorInfo> {
        var lastFailure: AppError? = null
        repeat(CATALOG_ATTEMPTS) {
            when (val sent = sendOneWay(WearCommand.RequestAvailableSensors)) {
                is AppResult.Failure -> lastFailure = sent.error
                is AppResult.Success -> {
                    val catalog = withTimeoutOrNull(CATALOG_ATTEMPT_TIMEOUT_MILLIS.milliseconds) {
                        incomingCommands.receiveAsFlow().filterIsInstance<WearCommand.AvailableSensors>().first()
                    }
                    if (catalog != null) return catalog.sensors
                }
            }
        }
        throw AssertionError("Wear sensor catalog did not arrive: $lastFailure", lastFailure?.cause)
    }

    suspend fun start(sessionId: String, request: WearRecordingRequest): AppResult<Unit> =
        recordingExchange.send(WearCommand.StartRecording(sessionId, request))

    suspend fun awaitAutomaticStop(sessionId: String) {
        val stopped = withTimeoutOrNull(AUTOMATIC_STOP_TIMEOUT_MILLIS.milliseconds) {
            incomingCommands.receiveAsFlow()
                .filterIsInstance<WearCommand.StopRecording>()
                .first { command -> command.sessionId == sessionId }
        }
        if (stopped == null) throw AssertionError("Wear recording $sessionId did not stop automatically")
        if (stopped.reason != WearStopReason.DURATION_EXPIRED) {
            throw AssertionError("Wear recording $sessionId stopped because ${stopped.reason}")
        }
    }

    suspend fun sync() {
        val requestId = "sync-${System.currentTimeMillis()}"
        sendOneWay(WearCommand.CopyWatchMeasurements(requestId)).getOrThrow()
        val completed = awaitSyncStatus(requestId)
        assertTrue("Watch did not finish syncing", completed.finished)
        assertTrue("Watch could not save and remove measurements", !completed.failed)
        val checkId = "$requestId-check"
        sendOneWay(WearCommand.CheckWatchMeasurements(checkId)).getOrThrow()
        val remaining = awaitSyncStatus(checkId)
        assertTrue("Watch could not list remaining measurements", !remaining.failed)
        assertEquals("Saved measurements must be removed from the watch", 0, remaining.measurementCount)
    }

    private suspend fun awaitSyncStatus(requestId: String): WearCommand.WatchMeasurementsStatus =
        withTimeoutOrNull(SYNC_TIMEOUT_MILLIS.milliseconds) {
            incomingCommands.receiveAsFlow()
                .filterIsInstance<WearCommand.WatchMeasurementsStatus>()
                .first { it.requestId == requestId }
        } ?: throw AssertionError("Watch sync status did not arrive")

    private suspend fun sendOneWay(command: WearCommand): AppResult<Unit> =
        sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, command)

    private companion object {
        const val SYNC_TIMEOUT_MILLIS = 60_000L
        const val CATALOG_ATTEMPTS = 6
        const val CATALOG_ATTEMPT_TIMEOUT_MILLIS = 5_000L
        const val AUTOMATIC_STOP_TIMEOUT_MILLIS = 15_000L
    }
}
