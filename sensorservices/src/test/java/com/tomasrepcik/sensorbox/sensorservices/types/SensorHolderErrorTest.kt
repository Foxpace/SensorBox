package com.tomasrepcik.sensorbox.sensorservices.types

import android.hardware.Sensor
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.OutputStream

class SensorHolderErrorTest {
    @Test
    fun `Given a failed output stream When holder closes Then storage AppError is returned`() = runBlocking {
        val spec = checkNotNull(SensorSpec.fromType(Sensor.TYPE_ACCELEROMETER))
        val logger = RecordingDiagnosticLogger()
        val holder = SensorHolder(spec, FailingOutputStream(), logger, EpochClock { 123L })

        val result = holder.close()

        val error = result.errorOrNull()
        assertTrue(error is AppError)
        assertEquals(AppErrorCode.STORAGE, (error as AppError).code)
        assertEquals("Write ${spec.fileName}", logger.events.single().operation)
    }

    private class FailingOutputStream : OutputStream() {
        override fun write(value: Int): Unit = throw IOException("disk full")
    }

    private class RecordingDiagnosticLogger : DiagnosticLogger {
        val events = mutableListOf<DiagnosticEvent>()

        override fun record(event: DiagnosticEvent) {
            events += event
        }
    }
}
