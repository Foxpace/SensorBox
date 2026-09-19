package com.tomasrepcik.sensorbox.recording

import android.content.Context
import android.hardware.Sensor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearSensorRecordingEmulatorTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = WearRecordingEmulatorFixture(context)

    @Before
    fun prepareDevice() = fixture.prepareDevice()

    @After
    fun cleanUpDevice() = fixture.cleanUpDevice()

    @Test
    fun givenAnEverydayAccelerometerRecordingWhenStoppedThenItsCsvAndMetadataAreComplete() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_ACCELEROMETER_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                samplingIndex = 0,
            ),
        )
            .waitFor(2_000)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenTwoMotionSensorsAndAWakeLockWhenStoppedThenBothFilesContainSamples() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_MULTI_SENSOR_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER, GYROSCOPE),
                samplingIndex = 1,
                useWakeLock = true,
            ),
        )
            .waitFor(2_000)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenAWalkingRouteWhenLocationsChangeThenGpsAndMotionFilesAreComplete() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_GPS_RECORDING_TEST",
                sessionId = "gps-session-from-watch",
                sensors = setOf(ACCELEROMETER),
                samplingIndex = 2,
                includesGps = true,
                gpsIntervalSeconds = 1,
                gpsMinDistanceMeters = 0,
            ),
        )
            .provideLocations(
                WearTestLocation(48.1486, 17.1077),
                WearTestLocation(48.1491, 17.1082),
                WearTestLocation(48.1498, 17.1090),
            )
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenATimedRecordingWhenDurationExpiresThenSamplesAreSaved() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_TIMED_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                samplingIndex = 3,
                durationMillis = 2_000,
            ),
        )
            .awaitAutomaticStop()
            .assertRecorded()
    }

    @Test
    fun givenARecordingWhenStoppedDirectlyThenSamplesAreSaved() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_CANCELLED_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER, GYROSCOPE),
            ),
        )
            .waitFor(1_000)
            .stop()
            .assertRecorded()
    }

    @Test
    fun givenLowBatteryProtectionWhenBatteryDropsThenTheRecordingFinalizes() {
        fixture
            .batteryAt(100)
            .start(
                WearRecordingScenario(
                    name = "WEAR_LOW_BATTERY_RECORDING_TEST",
                    sensors = setOf(ACCELEROMETER),
                    stopOnLowBattery = true,
                ),
            )
            .waitFor(1_000)
            .dropBatteryTo(1)
            .awaitAutomaticStop()
            .assertRecorded()
    }

    @Test
    fun givenAnUnknownSamplingIndexWhenStoppedThenFastestFallbackStillRecords() {
        fixture.start(
            WearRecordingScenario(
                name = "WEAR_UNKNOWN_SAMPLING_RECORDING_TEST",
                sensors = setOf(ACCELEROMETER),
                samplingIndex = Int.MAX_VALUE,
            ),
        )
            .waitFor(1_000)
            .stop()
            .assertRecorded()
    }

    private companion object {
        val ACCELEROMETER = WearRecordedSensor(Sensor.TYPE_ACCELEROMETER, "accelerometer.csv")
        val GYROSCOPE = WearRecordedSensor(Sensor.TYPE_GYROSCOPE, "gyroscope.csv")
    }
}
