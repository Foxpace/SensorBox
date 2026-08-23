package com.motionapps.sensorservices.types

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticLogger
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.suspendAppResult
import com.motionapps.sensorbox.core.error.toDiagnosticEvent
import com.motionapps.sensorbox.core.time.EpochClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.OutputStream

class SensorHolder(
    val spec: SensorSpec,
    outputStream: OutputStream,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
) : SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val samples = Channel<SensorSample>(capacity = Channel.BUFFERED)
    private val writer = outputStream.bufferedWriter()
    private val writerJob = scope.async {
        try {
            writer.append(spec.header)
            writer.flush()
            for (sample in samples) {
                writer.appendLine(sample.toCsv(spec.axisCount))
                writer.flush()
            }
        } catch (error: IOException) {
            throw reportWriterFailure(error)
        } catch (error: IllegalStateException) {
            throw reportWriterFailure(error)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val result = samples.trySend(
            SensorSample(
                sensorTimestampNanos = event.timestamp,
                unixTimestampMillis = clock.nowMillis(),
                values = event.values.copyOf(spec.axisCount),
                accuracy = event.accuracy,
            ),
        )
        if (result.isFailure && result.exceptionOrNull() == null) {
            samples.close(IllegalStateException("Sensor sample buffer is full"))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun <T : Throwable> reportWriterFailure(error: T): T {
        val appError = AppError.from(AppErrorCode.STORAGE, "Write ${spec.fileName}", error)
        diagnosticLogger.record(appError.toDiagnosticEvent())
        samples.close(error)
        return error
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
}

class SensorSample(
    val sensorTimestampNanos: Long,
    val unixTimestampMillis: Long,
    val values: FloatArray,
    val accuracy: Int,
) {
    fun toCsv(axisCount: Int): String = buildString {
        append(sensorTimestampNanos).append(';')
        append(unixTimestampMillis).append(';')
        values.take(axisCount).forEach { value -> append(value).append(';') }
        append(accuracy)
    }
}
