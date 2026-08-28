package com.tomasrepcik.sensorbox.sensorservices.types

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.suspendAppResult
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class SensorHolder(
    val spec: SensorSpec,
    outputStream: OutputStream,
    private val diagnosticLogger: DiagnosticLogger,
    private val onFailure: (AppError) -> Unit,
) : SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val samples = Channel<SensorSample>(capacity = SAMPLE_BUFFER_CAPACITY)
    private val writer = outputStream.bufferedWriter()
    private val acceptedSamples = AtomicLong()
    private val writtenSamples = AtomicLong()
    private val droppedSamples = AtomicLong()
    private val failure = AtomicReference<AppError?>()

    private val writerJob = scope.async { writeSamples() }

    override fun onSensorChanged(event: SensorEvent) {
        record(
            SensorSample(
                sensorTimestampNanos = event.timestamp,
                values = event.values.copyOf(spec.axisCount),
                accuracy = event.accuracy,
            ),
        )
    }

    internal fun record(sample: SensorSample) {
        val result = samples.trySend(sample)
        if (result.isSuccess) {
            acceptedSamples.incrementAndGet()
        } else if (result.exceptionOrNull() == null) {
            droppedSamples.incrementAndGet()
            val cause = IllegalStateException("Sensor sample buffer is full")
            val error = AppError.from(AppErrorCode.MEASUREMENT, "Buffer ${spec.fileName}", cause)
            reportFailure(error)
            samples.close(cause)
        } else if (failure.get() != null) {
            droppedSamples.incrementAndGet()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @Suppress("TooGenericExceptionCaught")
    private suspend fun writeSamples() {
        try {
            writer.append(spec.header)
            writer.flush()
            var lastFlushAt = 0L
            for (sample in samples) {
                writer.appendLine(sample.toCsv())
                writtenSamples.incrementAndGet()
                if (sample.sensorTimestampNanos - lastFlushAt >= FLUSH_INTERVAL_NANOS) {
                    writer.flush()
                    lastFlushAt = sample.sensorTimestampNanos
                }
            }
            writer.flush()
        } catch (error: Throwable) {
            reportFailure(AppError.from(AppErrorCode.STORAGE, "Write ${spec.fileName}", error))
            samples.close(error)
            throw error
        }
    }

    private fun reportFailure(error: AppError) {
        if (!failure.compareAndSet(null, error)) return
        diagnosticLogger.record(error.toDiagnosticEvent())
        onFailure(error)
    }

    suspend fun close(): AppResult<Unit> {
        samples.close()
        val results = mutableListOf<AppResult<*>>()
        results += suspendAppResult(AppErrorCode.STORAGE, "Finish ${spec.fileName} writer") { writerJob.await() }
        results += appResult(AppErrorCode.STORAGE, "Flush ${spec.fileName}") { writer.flush() }
        results += appResult(AppErrorCode.STORAGE, "Close ${spec.fileName}") { writer.close() }
        scope.cancel()
        return results.combineAppResults(AppErrorCode.STORAGE, "Close ${spec.fileName}")
    }

    fun stats(): SensorFileStats = SensorFileStats(
        fileName = spec.fileName,
        acceptedSamples = acceptedSamples.get(),
        writtenSamples = writtenSamples.get(),
        droppedSamples = droppedSamples.get(),
        failureOperation = failure.get()?.operation,
    )

    private companion object {
        const val SAMPLE_BUFFER_CAPACITY = 8_192
        const val FLUSH_INTERVAL_NANOS = 1_000_000_000L
    }
}

internal data class SensorSample(val sensorTimestampNanos: Long, val values: FloatArray, val accuracy: Int) {
    fun toCsv(): String = buildString {
        append(sensorTimestampNanos).append(';')
        values.forEach { value -> append(value).append(';') }
        append(accuracy)
    }
}

@Serializable
internal data class SensorFileStats(
    val fileName: String,
    val acceptedSamples: Long,
    val writtenSamples: Long,
    val droppedSamples: Long,
    val failureOperation: String?,
)
