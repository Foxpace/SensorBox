package com.tomasrepcik.sensorbox.recordinghost.sources.sensor

import android.hardware.Sensor
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class SensorHolderErrorTest {
    @Test
    fun `Given a failed output stream When holder closes Then storage AppError is returned`() = runBlocking {
        val spec = checkNotNull(SensorSpec.fromType(Sensor.TYPE_ACCELEROMETER))
        val logger = RecordingDiagnosticLogger()
        val failures = mutableListOf<AppError>()
        val holder = SensorHolder(spec, FailingOutputStream(), logger, failures::add)

        val result = holder.close()

        val error = result.errorOrNull()
        assertTrue(error is AppError)
        assertEquals(AppErrorCode.STORAGE, (error as AppError).code)
        assertEquals("Write ${spec.fileName}", logger.events.single().operation)
        assertEquals("Write ${spec.fileName}", failures.single().operation)
    }

    @Test
    fun `Given a large sensor burst When holder closes Then every sample is written in order`() = runBlocking {
        // Given
        val spec = checkNotNull(SensorSpec.fromType(Sensor.TYPE_ACCELEROMETER))
        val output = ByteArrayOutputStream()
        val failures = mutableListOf<AppError>()
        val holder = SensorHolder(spec, output, DiagnosticLogger { }, failures::add)

        // When
        repeat(SAMPLE_COUNT) { index ->
            holder.record(
                SensorSample(
                    sensorTimestampNanos = index.toLong(),
                    values = floatArrayOf(index.toFloat(), 2f, 3f),
                    accuracy = 3,
                ),
            )
        }
        val result = holder.close()

        // Then
        val lines = output.toString(Charsets.UTF_8.name()).lineSequence().filter(String::isNotBlank).toList()
        assertTrue(result.isSuccess)
        assertEquals(SAMPLE_COUNT + 1, lines.size)
        assertEquals("t_sensor;x;y;z;accuracy", lines.first())
        assertEquals("0;0.0;2.0;3.0;3", lines[1])
        assertEquals("${SAMPLE_COUNT - 1};${SAMPLE_COUNT - 1}.0;2.0;3.0;3", lines.last())
        assertEquals(SAMPLE_COUNT.toLong(), holder.stats().writtenSamples)
        assertTrue(failures.isEmpty())
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

    private companion object {
        const val SAMPLE_COUNT = 5_000
    }
}
