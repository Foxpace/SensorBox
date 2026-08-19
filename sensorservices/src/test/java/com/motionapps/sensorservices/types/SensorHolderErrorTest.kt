package com.motionapps.sensorservices.types

import android.hardware.Sensor
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
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
        val holder = SensorHolder(spec, FailingOutputStream())

        val result = holder.close()

        val error = result.errorOrNull()
        assertTrue(error is AppError)
        assertEquals(AppErrorCode.STORAGE, (error as AppError).code)
    }

    private class FailingOutputStream : OutputStream() {
        override fun write(value: Int): Unit = throw IOException("disk full")
    }
}
