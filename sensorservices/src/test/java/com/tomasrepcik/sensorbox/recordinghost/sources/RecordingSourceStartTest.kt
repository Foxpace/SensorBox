package com.tomasrepcik.sensorbox.recordinghost.sources

import android.hardware.Sensor
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GPSHandler
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GpsRecording
import com.tomasrepcik.sensorbox.recordinghost.sources.sensor.SensorRecording
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStream

class RecordingSourceStartTest {
    @Test
    fun `Given first sensor file failure When sensors start Then later sensor files are not opened`() {
        val storage = FailingMeasurementStorage()
        val recording = SensorRecording(storage, DiagnosticLogger { })

        val result = recording.start(
            folderName = "session",
            useInternalStorage = true,
            sensorTypes = setOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE),
            samplingPeriod = 0,
        )

        assertTrue(result.isFailure)
        assertEquals(1, storage.openCalls)
    }

    @Test
    fun `Given GPS file failure When GPS starts Then no output work follows`() {
        val storage = FailingMeasurementStorage()
        val recording = GpsRecording(GPSHandler(), storage, EpochClock { 1L })

        val result = recording.start(
            folderName = "session",
            useInternalStorage = true,
            intervalSeconds = 10,
            minimumDistanceMeters = 20,
        )

        assertTrue(result.isFailure)
        assertEquals(1, storage.openCalls)
    }

    private class FailingMeasurementStorage : MeasurementStorage {
        var openCalls = 0
            private set

        override fun createMeasurementDirectory(folderName: String, useInternalStorage: Boolean): AppResult<Unit> =
            AppResult.success(Unit)

        override fun openMeasurementFile(
            folderName: String,
            mimeType: String,
            fileName: String,
            useInternalStorage: Boolean,
        ): AppResult<OutputStream> {
            openCalls += 1
            return AppResult.failure(AppError(AppErrorCode.STORAGE, "Open fake file"))
        }
    }
}
