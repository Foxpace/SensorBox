package com.tomasrepcik.sensorbox.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.RecordingPreferences
import com.tomasrepcik.sensorbox.core.time.SystemEpochClock
import com.tomasrepcik.sensorbox.recording.DefaultWatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingIntentFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import java.io.File

internal data class WearRecordedSensor(
    val type: Int,
    val fileName: String,
    val header: String = "t_sensor;x;y;z;accuracy",
    val columnCount: Int = 5,
)

internal data class WearRecordingScenario(
    val name: String,
    val sensors: Set<WearRecordedSensor>,
    val sessionId: String = "wear-instrumentation-$name",
    val samplingIndex: Int = 0,
    val includesGps: Boolean = false,
    val stopOnLowBattery: Boolean = false,
    val useWakeLock: Boolean = false,
    val gpsIntervalSeconds: Int = 10,
    val gpsMinDistanceMeters: Int = 20,
    val durationMillis: Long = 0L,
)

internal data class WearTestLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Int = 3,
)

internal class WearRecordingEmulatorFixture(private val context: Context) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val controller = DefaultWatchRecordingControlUseCase(
        context = context,
        intentFactory = RecordingIntentFactory(context, SystemEpochClock),
    )
    private var mockGpsActive = false

    fun prepareDevice() {
        assumeTrue(
            "watch recording scenarios require Wear OS",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH),
        )
        grantRuntimePermissions()
        resetBattery()
    }

    fun cleanUpDevice() {
        controller.stop()
        resetBattery()
        removeMockGps()
    }

    fun batteryAt(level: Int): WearRecordingEmulatorFixture = apply {
        setBatteryLevel(level)
    }

    fun start(scenario: WearRecordingScenario): ActiveWearRecording {
        scenario.sensors.forEach { sensor ->
            assertTrue(
                "Wear device is missing sensor type ${sensor.type}",
                sensorManager.getDefaultSensor(sensor.type) != null,
            )
        }
        measurementDirectory(scenario.name).deleteRecursively()
        if (scenario.includesGps) prepareMockGps()
        val scheduledStartMillis = SystemEpochClock.nowMillis()
        val preferences = AppPreferences(
            recording = RecordingPreferences(
                gpsIntervalSeconds = scenario.gpsIntervalSeconds,
                gpsMinDistanceMeters = scenario.gpsMinDistanceMeters,
                sensorSamplingPeriod = scenario.samplingIndex,
                stopRecordingOnLowBattery = scenario.stopOnLowBattery,
                useWakeLock = scenario.useWakeLock,
            ),
        )
        controller.start(
            sensorIds = scenario.sensors.mapTo(mutableSetOf(), WearRecordedSensor::type),
            includesGps = scenario.includesGps,
            preferences = preferences,
            sessionId = scenario.sessionId,
            folderName = scenario.name,
            durationMillis = scenario.durationMillis,
        ).getOrThrow()
        return ActiveWearRecording(this, scenario, scheduledStartMillis)
    }

    private fun grantRuntimePermissions() {
        val requestedPermissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()
        RUNTIME_PERMISSIONS
            .filter(requestedPermissions::contains)
            .forEach { permission ->
                instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
            }
    }

    private fun prepareMockGps() {
        shell("appops set 2000 android:mock_location allow")
        shell(
            "cmd location providers add-test-provider gps " +
                "--requiresSatellite --supportsAltitude --supportsSpeed --supportsBearing",
        )
        shell("cmd location providers set-test-provider-enabled gps true")
        mockGpsActive = true
    }

    private fun provideLocations(locations: List<WearTestLocation>) {
        Thread.sleep(LOCATION_STARTUP_MILLIS)
        locations.forEach { location ->
            shell(
                "cmd location providers set-test-provider-location gps " +
                    "--location ${location.latitude},${location.longitude} " +
                    "--accuracy ${location.accuracyMeters} --time ${SystemEpochClock.nowMillis()}",
            )
            Thread.sleep(LOCATION_UPDATE_MILLIS)
        }
    }

    private fun removeMockGps() {
        if (!mockGpsActive) return
        shell("cmd location providers remove-test-provider gps")
        shell("appops set 2000 android:mock_location deny")
        mockGpsActive = false
    }

    private fun setBatteryLevel(level: Int) {
        require(level in 0..100) { "Battery level must be between 0 and 100" }
        shell("dumpsys battery unplug -f")
        shell("dumpsys battery set -f level $level")
    }

    private fun resetBattery() {
        shell("dumpsys battery reset -f")
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader()
            .use { it.readText() }

    private fun complete(scenario: WearRecordingScenario, scheduledStartMillis: Long): WearRecordingOutput {
        val directory = measurementDirectory(scenario.name)
        return WearRecordingOutput(
            scenario = scenario,
            scheduledStartMillis = scheduledStartMillis,
            metadata = awaitMetadata(directory),
            files = directory.listFiles().orEmpty().associateBy(File::getName),
        )
    }

    private fun rows(scenario: WearRecordingScenario, sensor: WearRecordedSensor): List<String> {
        val output = File(measurementDirectory(scenario.name), sensor.fileName)
        repeat(FILE_WAIT_ATTEMPTS) {
            val rows = output.takeIf(File::isFile)?.readLines().orEmpty()
            if (rows.isNotEmpty()) return rows
            Thread.sleep(FILE_WAIT_INTERVAL_MILLIS)
        }
        return output.takeIf(File::isFile)?.readLines().orEmpty()
    }

    private fun awaitMetadata(directory: File): JSONObject {
        val output = File(directory, METADATA_FILE)
        repeat(FILE_WAIT_ATTEMPTS) {
            output.takeIf { it.isFile && it.length() > 0L }?.let { return JSONObject(it.readText()) }
            Thread.sleep(FILE_WAIT_INTERVAL_MILLIS)
        }
        assertTrue("watch recording did not finalize $output", output.isFile)
        return JSONObject(output.readText())
    }

    private fun measurementDirectory(name: String): File = File(context.filesDir, "SensorBox/$name")

    internal class ActiveWearRecording(
        private val fixture: WearRecordingEmulatorFixture,
        private val scenario: WearRecordingScenario,
        private val scheduledStartMillis: Long,
    ) {
        fun waitFor(delayMillis: Long): ActiveWearRecording = apply {
            Thread.sleep(delayMillis)
        }

        fun provideLocations(vararg locations: WearTestLocation): ActiveWearRecording = apply {
            fixture.provideLocations(locations.toList())
        }

        fun dropBatteryTo(level: Int): ActiveWearRecording = apply {
            fixture.setBatteryLevel(level)
        }

        fun assertOnlyHeaders(): ActiveWearRecording = apply {
            scenario.sensors.forEach { sensor ->
                assertEquals(listOf(sensor.header), fixture.rows(scenario, sensor))
            }
        }

        fun stop(): WearRecordingOutput {
            fixture.controller.stop().getOrThrow()
            return fixture.complete(scenario, scheduledStartMillis)
        }

        fun awaitAutomaticStop(): WearRecordingOutput = fixture.complete(scenario, scheduledStartMillis)
    }

    internal class WearRecordingOutput(
        private val scenario: WearRecordingScenario,
        private val scheduledStartMillis: Long,
        private val metadata: JSONObject,
        private val files: Map<String, File>,
    ) {
        fun assertRecorded(): WearRecordingOutput = apply {
            assertSessionMetadata()
            assertExactFiles()
            scenario.sensors.forEach { sensor -> assertValidCsv(files.getValue(sensor.fileName), sensor) }
            if (scenario.includesGps) assertGpsCsv(files.getValue(GPS_FILE))
        }

        fun assertCancelledBeforeStart(): WearRecordingOutput = apply {
            assertSessionMetadata()
            assertExactFiles()
            scenario.sensors.forEach { sensor ->
                assertEquals(listOf(sensor.header), files.getValue(sensor.fileName).readLines())
            }
        }

        private fun assertSessionMetadata() {
            assertEquals(scenario.name, metadata.getString("folder"))
            assertTrue(metadata.getLong("millis") >= scheduledStartMillis - TIMESTAMP_TOLERANCE_MILLIS)
            assertTrue(metadata.getLong("nanos") > 0L)
            assertEquals(scenario.durationMillis.coerceAtLeast(0), metadata.getLong("durationMillis"))
            val recordedTypes = metadata.getJSONArray("ranges").let { ranges ->
                buildSet {
                    repeat(ranges.length()) { add(ranges.getJSONObject(it).getInt("type")) }
                }
            }
            assertEquals(scenario.sensors.mapTo(mutableSetOf(), WearRecordedSensor::type), recordedTypes)
        }

        private fun assertExactFiles() {
            val expected = scenario.sensors.mapTo(mutableSetOf(), WearRecordedSensor::fileName).apply {
                add(METADATA_FILE)
                if (scenario.includesGps) add(GPS_FILE)
            }
            assertEquals(expected, files.keys)
        }

        private fun assertGpsCsv(file: File) {
            val rows = file.readLines()
            assertEquals(GPS_HEADER, rows.first())
            assertTrue("Expected Wear GPS samples in $file, got $rows", rows.size >= MINIMUM_ROWS)
            rows.drop(1).forEach { row ->
                val columns = row.split(';')
                assertEquals("Malformed Wear GPS row: $row", GPS_COLUMN_COUNT, columns.size)
                assertTrue("Malformed Wear GPS timestamp: $row", columns[0].toLongOrNull() != null)
                assertTrue("Malformed Wear GPS coordinates: $row", columns.slice(1..6).all { it.toDoubleOrNull() != null })
                assertTrue("Missing Wear GPS provider: $row", columns[7].isNotBlank())
            }
        }

        private fun assertValidCsv(file: File, sensor: WearRecordedSensor) {
            val rows = file.readLines()
            assertTrue("Expected Wear sensor samples in $file, got $rows", rows.size >= MINIMUM_ROWS)
            assertEquals(sensor.header, rows.first())
            val samples = rows.drop(1).map { row ->
                val columns = row.split(';')
                assertEquals("Malformed Wear CSV row: $row", sensor.columnCount, columns.size)
                assertTrue("Non-numeric Wear CSV row: $row", columns.all { it.toDoubleOrNull() != null })
                columns
            }
            val sensorTimestamps = samples.map { it[0].toLong() }
            assertEquals(sensorTimestamps.sorted(), sensorTimestamps)
        }
    }

    private companion object {
        val RUNTIME_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        const val METADATA_FILE = "extra.json"
        const val GPS_FILE = "gps.csv"
        const val GPS_HEADER = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider"
        const val GPS_COLUMN_COUNT = 8
        const val MINIMUM_ROWS = 2
        const val TIMESTAMP_TOLERANCE_MILLIS = 250L
        const val FILE_WAIT_ATTEMPTS = 40
        const val FILE_WAIT_INTERVAL_MILLIS = 250L
        const val LOCATION_STARTUP_MILLIS = 1_000L
        const val LOCATION_UPDATE_MILLIS = 1_100L
    }
}
