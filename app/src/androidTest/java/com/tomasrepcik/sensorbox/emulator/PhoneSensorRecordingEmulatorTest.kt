package com.tomasrepcik.sensorbox.emulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneSensorRecordingEmulatorTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = RecordingEmulatorFixture(context)

    @Before
    fun prepareDevice() = fixture.prepareDevice()

    @After
    fun cleanUpDevice() = fixture.cleanUpDevice()

    @Test
    fun givenAWorkoutWithAnAnnotationWhenStoppedThenCsvAndSessionMetadataAreComplete() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_ANNOTATED_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                notes = listOf("Outdoor workout", "Phone in jacket pocket"),
            ),
        )
            .waitFor(1_000)
            .annotate("Reached the first checkpoint")
            .waitFor(1_000)
            .stop()
            .assertRecorded()
            .assertAnnotation("Reached the first checkpoint")
    }

    @Test
    fun givenTwoMotionSensorsAtGameSpeedWhenStoppedThenBothCsvFilesContainValidSamples() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_MULTI_SENSOR_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER, GYROSCOPE),
                samplingPeriod = SensorManager.SENSOR_DELAY_GAME,
                useWakeLock = true,
                notes = listOf("Short movement drill"),
            ),
        )
            .waitFor(2_000)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenATimedRecordingWhenDurationExpiresThenSamplesAreFinalized() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_TIMED_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST,
                durationMillis = 4_000,
                notes = listOf("Hands-free timed capture"),
            ),
        )
            .awaitAutomaticStop()
            .assertRecorded()
    }

    @Test
    fun givenARecordingWhenStoppedDirectlyThenFilesCloseWithSamples() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_CANCELLED_DELAYED_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER, GYROSCOPE),
                notes = listOf("Short direct capture"),
            ),
        )
            .waitFor(1_000)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenGpsAndMotionWhenLocationsChangeThenBothSourceFilesAreComplete() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_GPS_RECORDING_TEST",
                sessionId = "gps-session-from-phone",
                sensors = setOf(ACCELEROMETER),
                includesGps = true,
                expectGpsSamples = true,
                gpsIntervalSeconds = 1,
                gpsMinDistanceMeters = 0,
                notes = listOf("Walking route"),
            ),
        )
            .provideLocations(
                TestLocation(48.1486, 17.1077),
                TestLocation(48.1491, 17.1082),
                TestLocation(48.1498, 17.1090),
            )
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenActivityRecognitionAndDuplicateAlarmsWhenStoppedThenOptionsAreNormalizedAndSaved() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_ACTIVITY_AND_ALARM_TEST",
                sensors = emptySet(),
                alarmOffsetsSeconds = listOf(-3, 0, 0, 1),
                activityRecognition = true,
                activityRecognitionPeriodSeconds = 1,
                notes = listOf("Activity-only capture"),
            ),
        )
            .waitFor(2_500)
            .stop()
            .assertRecorded()
            .assertAlarmCount(2)
    }

    @Test
    fun givenLowBatteryProtectionWhenBatteryDropsThenTheRecordingFinalizes() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_LOW_BATTERY_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                samplingPeriod = SensorManager.SENSOR_DELAY_UI,
                stopOnLowBattery = true,
                notes = listOf("Battery-protected capture"),
            ),
        )
            .waitFor(1_000)
            .dropBatteryTo(1)
            .awaitAutomaticStop()
            .assertRecorded()
    }

    @Test
    fun givenSignificantMotionRecordingWhenStoppedThenThePreparedFileCloses() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_SIGNIFICANT_MOTION_FAILURE_TEST",
                sensors = emptySet(),
                significantMotion = true,
                notes = listOf("One-shot movement trigger"),
            ),
        )
            .waitFor(500)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenExternalStorageWithoutAPersistedDirectoryThenInternalFilesAreNotCreated() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_MISSING_EXTERNAL_STORAGE_TEST",
                sensors = setOf(ACCELEROMETER),
                useInternalStorage = false,
            ),
        )
            .assertStartRejected()
    }

    @Test
    fun givenANegativeDurationAndNoSourcesWhenStoppedThenASessionOnlyRecordingIsFinalized() {
        fixture.start(
            RecordingScenario(
                name = "PHONE_SESSION_ONLY_RECORDING_TEST",
                sensors = emptySet(),
                durationMillis = -5_000,
                notes = listOf("Metadata-only session"),
            ),
        )
            .waitFor(500)
            .stop()
            .assertRecorded()
    }

    private companion object {
        val ACCELEROMETER = RecordedSensor(Sensor.TYPE_ACCELEROMETER, "accelerometer.csv")
        val GYROSCOPE = RecordedSensor(Sensor.TYPE_GYROSCOPE, "gyroscope.csv")
    }
}
