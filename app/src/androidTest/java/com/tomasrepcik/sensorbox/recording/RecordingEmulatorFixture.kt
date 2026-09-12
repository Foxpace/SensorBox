package com.tomasrepcik.sensorbox.recording

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.tomasrepcik.sensorbox.core.time.SystemEpochClock
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingIntentFactory
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.session.RecordingService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import java.io.File

internal data class RecordedSensor(
    val type: Int,
    val fileName: String,
    val header: String = "t_sensor;x;y;z;accuracy",
    val columnCount: Int = 5,
)

internal data class RecordingScenario(
    val name: String,
    val sensors: Set<RecordedSensor>,
    val sessionId: String = "instrumentation-$name",
    val useInternalStorage: Boolean = true,
    val samplingPeriod: Int = SensorManager.SENSOR_DELAY_NORMAL,
    val includesGps: Boolean = false,
    val expectGpsSamples: Boolean = false,
    val stopOnLowBattery: Boolean = false,
    val useWakeLock: Boolean = false,
    val gpsIntervalSeconds: Int = 10,
    val gpsMinDistanceMeters: Int = 20,
    val durationMillis: Long = 0L,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
)

internal data class TestLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Int = 3,
)

internal class RecordingEmulatorFixture(private val context: Context) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private var mockGpsActive = false

    fun prepareDevice() {
        assumeFalse(
            "Phone recording scenarios do not run on Wear OS",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH),
        )
        grantRuntimePermissions()
        resetBattery()
    }

    fun cleanUpDevice() {
        stopAnyRecording()
        resetBattery()
        removeMockGps()
    }

    fun start(scenario: RecordingScenario): ActiveRecording {
        scenario.sensors.forEach { sensor ->
            assertTrue(
                "Emulator is missing sensor type ${sensor.type}",
                sensorManager.getDefaultSensor(sensor.type) != null,
            )
        }
        measurementDirectory(scenario.name).deleteRecursively()
        if (!scenario.useInternalStorage) clearDocumentStoragePermissions()
        if (scenario.includesGps) prepareMockGps()
        val scheduledStartMillis = SystemEpochClock.nowMillis()
        val request = RecordingRequest(
            sessionId = scenario.sessionId,
            folderName = scenario.name,
            useInternalStorage = scenario.useInternalStorage,
            sensorIds = scenario.sensors.mapTo(mutableSetOf(), RecordedSensor::type),
            sensorSamplingPeriod = scenario.samplingPeriod,
            includesGps = scenario.includesGps,
            stopOnLowBattery = scenario.stopOnLowBattery,
            useWakeLock = scenario.useWakeLock,
            gpsIntervalSeconds = scenario.gpsIntervalSeconds,
            gpsMinDistanceMeters = scenario.gpsMinDistanceMeters,
            durationMillis = scenario.durationMillis,
            notes = scenario.notes,
            alarmOffsetsSeconds = scenario.alarmOffsetsSeconds,
            activityRecognition = scenario.activityRecognition,
            activityRecognitionPeriodSeconds = scenario.activityRecognitionPeriodSeconds,
            significantMotion = scenario.significantMotion,
        )
        val intent = RecordingIntentFactory(context, SystemEpochClock).create(request)
        assertIntentMatches(intent, request)
        ContextCompat.startForegroundService(context, intent)
        val expectedRecordedTypes = scenario.sensors.mapTo(mutableSetOf(), RecordedSensor::type).apply {
            if (
                scenario.significantMotion &&
                sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
            ) {
                add(Sensor.TYPE_SIGNIFICANT_MOTION)
            }
        }
        return ActiveRecording(this, scenario, scheduledStartMillis, expectedRecordedTypes)
    }

    private fun assertIntentMatches(intent: Intent, request: RecordingRequest) {
        assertEquals(request.sessionId, intent.getStringExtra(RecordingService.SESSION_ID))
        assertEquals(request.folderName, intent.getStringExtra(RecordingService.FOLDER_NAME))
        assertEquals(request.useInternalStorage, intent.getBooleanExtra(RecordingService.INTERNAL_STORAGE, false))
        val sensorIds = intent.getIntArrayExtra(RecordingService.ANDROID_SENSORS) ?: intArrayOf()
        assertEquals(request.sensorIds, sensorIds.toSet())
        assertEquals(
            request.sensorSamplingPeriod,
            intent.getIntExtra(RecordingService.ANDROID_SENSORS_SPEED, Int.MIN_VALUE),
        )
        assertEquals(request.includesGps, intent.getBooleanExtra(RecordingService.GPS, false))
        assertEquals(request.stopOnLowBattery, intent.getBooleanExtra(RecordingService.STOP_ON_LOW_BATTERY, false))
        assertEquals(request.useWakeLock, intent.getBooleanExtra(RecordingService.USE_WAKE_LOCK, false))
        assertEquals(request.gpsIntervalSeconds, intent.getIntExtra(RecordingService.GPS_INTERVAL_SECONDS, -1))
        assertEquals(request.gpsMinDistanceMeters, intent.getIntExtra(RecordingService.GPS_DISTANCE_METERS, -1))
        assertEquals(request.durationMillis, intent.getLongExtra(RecordingService.DURATION_MILLIS, -1))
        assertEquals(request.notes, intent.getStringArrayListExtra(RecordingService.NOTES).orEmpty())
        assertEquals(
            request.alarmOffsetsSeconds,
            (intent.getIntArrayExtra(RecordingService.ALARM_OFFSETS_SECONDS) ?: intArrayOf()).toList(),
        )
        assertEquals(
            request.activityRecognition,
            intent.getBooleanExtra(RecordingService.ACTIVITY_RECOGNITION, false),
        )
        assertEquals(
            request.activityRecognitionPeriodSeconds,
            intent.getIntExtra(RecordingService.ACTIVITY_RECOGNITION_PERIOD_SECONDS, -1),
        )
        assertEquals(request.significantMotion, intent.getBooleanExtra(RecordingService.SIGNIFICANT_MOTION, false))
    }

    fun stopAnyRecording() {
        context.startService(serviceIntent(RecordingService.ACTION_STOP_RECORDING))
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

    private fun provideLocations(locations: List<TestLocation>) {
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

    private fun annotate(text: String) {
        context.startService(serviceIntent(RecordingService.ACTION_ANNOTATE).apply {
            putExtra(RecordingService.ANNOTATION_TIME, SystemEpochClock.nowMillis())
            putExtra(RecordingService.ANNOTATION_TEXT, text)
        })
    }

    private fun clearDocumentStoragePermissions() {
        context.contentResolver.persistedUriPermissions.forEach { permission ->
            var flags = 0
            if (permission.isReadPermission) flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (permission.isWritePermission) flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(permission.uri, flags)
        }
    }

    private fun complete(
        scenario: RecordingScenario,
        scheduledStartMillis: Long,
        expectedRecordedTypes: Set<Int>,
    ): RecordingOutput {
        val metadata = awaitMetadata(scenario.name)
        return RecordingOutput(
            scenario = scenario,
            scheduledStartMillis = scheduledStartMillis,
            expectedRecordedTypes = expectedRecordedTypes,
            metadata = metadata,
            files = measurementDirectory(scenario.name).listFiles().orEmpty().associateBy(File::getName),
        )
    }

    private fun awaitMetadata(measurementName: String): JSONObject {
        val output = File(measurementDirectory(measurementName), METADATA_FILE)
        repeat(FILE_WAIT_ATTEMPTS) {
            output.takeIf { it.isFile && it.length() > 0L }?.let { return JSONObject(it.readText()) }
            Thread.sleep(FILE_WAIT_INTERVAL_MILLIS)
        }
        assertTrue("Recording did not finalize $output", output.isFile)
        return JSONObject(output.readText())
    }

    private fun measurementDirectory(measurementName: String): File =
        File(context.filesDir, "SensorBox/$measurementName")

    private fun serviceIntent(actionName: String): Intent = Intent(context, RecordingService::class.java).apply {
        action = actionName
    }

    internal class ActiveRecording(
        private val fixture: RecordingEmulatorFixture,
        private val scenario: RecordingScenario,
        private val scheduledStartMillis: Long,
        private val expectedRecordedTypes: Set<Int>,
    ) {
        fun waitFor(delayMillis: Long): ActiveRecording = apply {
            Thread.sleep(delayMillis)
        }

        fun provideLocations(vararg locations: TestLocation): ActiveRecording = apply {
            fixture.provideLocations(locations.toList())
        }

        fun dropBatteryTo(level: Int): ActiveRecording = apply {
            fixture.setBatteryLevel(level)
        }

        fun annotate(text: String): ActiveRecording = apply {
            fixture.annotate(text)
        }

        fun stop(): RecordingOutput {
            fixture.stopAnyRecording()
            return fixture.complete(scenario, scheduledStartMillis, expectedRecordedTypes)
        }

        fun awaitAutomaticStop(): RecordingOutput =
            fixture.complete(scenario, scheduledStartMillis, expectedRecordedTypes)

        fun assertStartRejected(): ActiveRecording = apply {
            Thread.sleep(STORAGE_FAILURE_WAIT_MILLIS)
            assertFalse(fixture.measurementDirectory(scenario.name).exists())
        }
    }

    internal class RecordingOutput(
        private val scenario: RecordingScenario,
        private val scheduledStartMillis: Long,
        private val expectedRecordedTypes: Set<Int>,
        private val metadata: JSONObject,
        private val files: Map<String, File>,
    ) {
        fun assertRecorded(): RecordingOutput = apply {
            assertSessionMetadata()
            assertExactFiles()
            scenario.sensors.forEach { sensor -> assertValidCsv(files.getValue(sensor.fileName), sensor) }
            if (scenario.includesGps) assertGpsCsv(files.getValue(GPS_FILE), scenario.expectGpsSamples)
            if (scenario.activityRecognition) assertActivityFiles()
            if (scenario.significantMotion) assertHeader(files.getValue(SIGNIFICANT_MOTION_FILE), SIGNIFICANT_HEADER)
        }

        fun assertAnnotation(expected: String): RecordingOutput = apply {
            val annotations = metadata.getJSONArray("annotations")
            assertEquals(1, annotations.length())
            assertEquals(expected, annotations.getJSONObject(0).getString("annotation"))
        }

        fun assertAlarmCount(expected: Int): RecordingOutput = apply {
            assertEquals(expected, metadata.getJSONArray("alarms").length())
        }

        private fun assertSessionMetadata() {
            assertEquals(scenario.name, metadata.getString("folder"))
            assertTrue(metadata.getLong("millis") >= scheduledStartMillis - TIMESTAMP_TOLERANCE_MILLIS)
            assertTrue(metadata.getLong("nanos") > 0L)
            assertEquals(scenario.notes, metadata.getJSONArray("notes").toStringList())
            assertEquals(scenario.durationMillis.coerceAtLeast(0), metadata.getLong("durationMillis"))
            assertEquals(
                scenario.alarmOffsetsSeconds.filter { it >= 0 },
                metadata.getJSONArray("configuredAlarmOffsetsSeconds").toIntList(),
            )
            assertEquals(scenario.activityRecognition, metadata.getBoolean("activityRecognition"))
            assertEquals(scenario.significantMotion, metadata.getBoolean("significantMotion"))
            val recordedTypes = metadata.getJSONArray("ranges").let { ranges ->
                buildSet {
                    repeat(ranges.length()) { add(ranges.getJSONObject(it).getInt("type")) }
                }
            }
            assertEquals(expectedRecordedTypes, recordedTypes)
        }

        private fun assertExactFiles() {
            val expected = scenario.sensors.mapTo(mutableSetOf(), RecordedSensor::fileName).apply {
                add(METADATA_FILE)
                if (scenario.includesGps) add(GPS_FILE)
                if (scenario.activityRecognition) {
                    add(ACTIVITY_UPDATES_FILE)
                    add(ACTIVITY_TRANSITIONS_FILE)
                }
                if (scenario.significantMotion) add(SIGNIFICANT_MOTION_FILE)
            }
            assertEquals(expected, files.keys)
        }

        private fun assertGpsCsv(file: File, expectSamples: Boolean) {
            val rows = file.readLines()
            assertEquals(GPS_HEADER, rows.first())
            if (!expectSamples) return
            assertTrue("Expected injected GPS samples in $file, got $rows", rows.size >= MINIMUM_ROWS)
            rows.drop(1).forEach { row ->
                val columns = row.split(';')
                assertEquals("Malformed GPS row: $row", GPS_COLUMN_COUNT, columns.size)
                assertTrue("Malformed GPS timestamp: $row", columns[0].toLongOrNull() != null)
                assertTrue("Malformed GPS coordinates: $row", columns.slice(1..6).all { it.toDoubleOrNull() != null })
                assertTrue("Missing GPS provider: $row", columns[7].isNotBlank())
            }
        }

        private fun assertActivityFiles() {
            assertHeader(files.getValue(ACTIVITY_UPDATES_FILE), ACTIVITY_UPDATES_HEADER)
            assertHeader(files.getValue(ACTIVITY_TRANSITIONS_FILE), ACTIVITY_TRANSITIONS_HEADER)
        }

        private fun assertHeader(file: File, expected: String) {
            assertEquals(expected, file.readLines().first())
        }

        private fun assertValidCsv(file: File, sensor: RecordedSensor) {
            val rows = file.readLines()
            assertTrue("Expected a header and recorded samples in $file, got $rows", rows.size >= MINIMUM_ROWS)
            assertEquals(sensor.header, rows.first())
            val samples = rows.drop(1).map { row ->
                val columns = row.split(';')
                assertEquals("Malformed CSV row: $row", sensor.columnCount, columns.size)
                assertTrue("Non-numeric CSV row: $row", columns.all { it.toDoubleOrNull() != null })
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
        const val GPS_FILE = "GPS.csv"
        const val GPS_HEADER = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider"
        const val GPS_COLUMN_COUNT = 8
        const val ACTIVITY_UPDATES_FILE = "activity_updates.csv"
        const val ACTIVITY_UPDATES_HEADER = "t_elapsed;still;on_foot;walking;running;vehicle;bike;unknown;tilting"
        const val ACTIVITY_TRANSITIONS_FILE = "activity_transitions.csv"
        const val ACTIVITY_TRANSITIONS_HEADER = "t_nanos;activity;enter_exit"
        const val SIGNIFICANT_MOTION_FILE = "significant_motion.csv"
        const val SIGNIFICANT_HEADER = "t_sensor;event"
        const val MINIMUM_ROWS = 2
        const val TIMESTAMP_TOLERANCE_MILLIS = 250L
        const val FILE_WAIT_ATTEMPTS = 40
        const val FILE_WAIT_INTERVAL_MILLIS = 250L
        const val STORAGE_FAILURE_WAIT_MILLIS = 1_000L
        const val LOCATION_STARTUP_MILLIS = 1_000L
        const val LOCATION_UPDATE_MILLIS = 1_100L
    }
}

private fun org.json.JSONArray.toStringList(): List<String> =
    List(length()) { index -> getString(index) }

private fun org.json.JSONArray.toIntList(): List<Int> =
    List(length()) { index -> getInt(index) }
