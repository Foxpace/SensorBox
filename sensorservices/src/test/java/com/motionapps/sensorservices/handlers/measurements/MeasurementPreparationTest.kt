package com.motionapps.sensorservices.handlers.measurements

import android.hardware.Sensor
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticLogger
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.MeasurementStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStream

class MeasurementPreparationTest {
    @Test
    fun `Given first sensor file failure When sensors prepare Then later sensor files are not opened`() {
        val storage = FailingMeasurementStorage()
        val measurement = SensorMeasurement(storage, DiagnosticLogger { }, EpochClock { 1L })

        val result = measurement.prepare(
            folderName = "session",
            useInternalStorage = true,
            sensorTypes = setOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE),
            samplingPeriod = 0,
        )

        assertTrue(result.isFailure)
        assertEquals(1, storage.openCalls)
    }

    @Test
    fun `Given GPS file failure When GPS prepares Then no output work follows`() {
        val storage = FailingMeasurementStorage()
        val measurement = GPSMeasurement(GPSHandler(), storage, EpochClock { 1L })

        val result = measurement.prepare(
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
