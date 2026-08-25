package com.tomasrepcik.sensorbox.emulator

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.tomasrepcik.sensorbox.core.time.SystemEpochClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.motionapps.sensorservices.intent.MeasurementIntentFactory
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.sensorservices.services.MeasurementService
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneSensorRecordingEmulatorTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val measurementDirectory = File(context.filesDir, "SensorBox/$MEASUREMENT_NAME")

    @Before
    fun clearPreviousMeasurement() {
        measurementDirectory.deleteRecursively()
    }

    @After
    fun stopMeasurement() {
        context.startService(stopIntent())
    }

    @Test
    fun givenVirtualAccelerometerWhenValuesChangeThenCsvContainsSamples() {
        assertTrue(hasAccelerometer())
        ContextCompat.startForegroundService(context, recordingIntent())
        Log.i(LOG_TAG, "READY_FOR_SENSOR_INJECTION")

        Thread.sleep(RECORDING_WINDOW_MILLIS)
        context.startService(stopIntent())

        val rows = awaitRecordedRows()
        assertEquals(EXPECTED_HEADER, rows.first())
        assertTrue("Expected injected accelerometer samples, got $rows", rows.size >= MINIMUM_ROWS)
    }

    private fun hasAccelerometer(): Boolean = context.getSystemService(SensorManager::class.java)
        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    private fun recordingIntent(): Intent = MeasurementIntentFactory(context, SystemEpochClock).create(
        MeasurementLaunchRequest(
            folderName = MEASUREMENT_NAME,
            useInternalStorage = true,
            sensorIds = setOf(Sensor.TYPE_ACCELEROMETER),
            sensorSamplingPeriod = SensorManager.SENSOR_DELAY_NORMAL,
            includesGps = false,
            stopOnLowBattery = false,
            useWakeLock = false,
            gpsIntervalSeconds = 10,
            gpsMinDistanceMeters = 20,
        ),
    )

    private fun stopIntent(): Intent = Intent(context, MeasurementService::class.java).apply {
        action = MeasurementService.ACTION_STOP
    }

    private fun awaitRecordedRows(): List<String> {
        val output = File(measurementDirectory, "accelerometer.csv")
        repeat(FILE_WAIT_ATTEMPTS) {
            val rows = output.takeIf(File::isFile)?.readLines().orEmpty()
            if (rows.size >= MINIMUM_ROWS) return rows
            Thread.sleep(FILE_WAIT_INTERVAL_MILLIS)
        }
        return output.takeIf(File::isFile)?.readLines().orEmpty()
    }

    private companion object {
        const val LOG_TAG = "SensorBoxEmulatorTest"
        const val MEASUREMENT_NAME = "PHONE_EMULATOR_SENSOR_TEST"
        const val EXPECTED_HEADER = "t_sensor;t_unix;x;y;z;accuracy"
        const val MINIMUM_ROWS = 2
        const val RECORDING_WINDOW_MILLIS = 8_000L
        const val FILE_WAIT_ATTEMPTS = 20
        const val FILE_WAIT_INTERVAL_MILLIS = 250L
    }
}
