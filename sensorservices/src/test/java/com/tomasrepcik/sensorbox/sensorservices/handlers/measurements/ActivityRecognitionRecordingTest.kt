package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class ActivityRecognitionRecordingTest {
    @Test
    fun `Given fake storage and platform When activity recording runs Then lifecycle has no Android types`() =
        runBlocking {
            val storage = FakeMeasurementStorage()
            val platform = FakeActivityRecognitionPlatform()
            val recording = ActivityRecognitionRecording(storage, platform)

            assertTrue(
                recording.start(
                    folderName = "session",
                    useInternalStorage = true,
                    periodSeconds = 15,
                ).isSuccess,
            )
            platform.emitUpdate(ActivityUpdate(123L, listOf(1, 2, 3, 4, 5, 6, 7, 8)))
            platform.emitTransitions(listOf(ActivityTransitionSample(456L, 2, 1)))
            val stopped = recording.stop()

            assertTrue(stopped.isSuccess)
            assertEquals(15, platform.startedPeriodSeconds)
            assertEquals(1, platform.stopCalls)
            assertTrue(storage.text("activity_updates.csv").contains("123;1;2;3;4;5;6;7;8"))
            assertTrue(storage.text("activity_transitions.csv").contains("456;2;1"))
        }

    @Test
    fun `Given transition storage failure When started Then platform initialization does not run`() = runBlocking {
        val storage = FakeMeasurementStorage(failOnCall = 2)
        val platform = FakeActivityRecognitionPlatform()
        val recording = ActivityRecognitionRecording(storage, platform)

        val result = recording.start(
            folderName = "session",
            useInternalStorage = false,
            periodSeconds = 15,
        )

        assertTrue(result.isFailure)
        assertEquals(0, platform.startCalls)
        assertEquals(2, storage.openCalls)
    }

    private class FakeMeasurementStorage(private val failOnCall: Int? = null) : MeasurementStorage {
        private val outputs = mutableMapOf<String, ByteArrayOutputStream>()
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
            if (openCalls == failOnCall) {
                return AppResult.failure(AppError(AppErrorCode.STORAGE, "Open fake file"))
            }
            return AppResult.success(ByteArrayOutputStream().also { outputs[fileName] = it })
        }

        fun text(fileName: String): String = outputs.getValue(fileName).toString(Charsets.UTF_8.name())
    }

    private class FakeActivityRecognitionPlatform : ActivityRecognitionPlatform {
        var startCalls = 0
            private set
        var stopCalls = 0
            private set
        var startedPeriodSeconds: Int? = null
            private set
        private var updateCallback: ((ActivityUpdate) -> Unit)? = null
        private var transitionCallback: ((List<ActivityTransitionSample>) -> Unit)? = null

        override suspend fun start(
            periodSeconds: Int,
            onUpdate: (ActivityUpdate) -> Unit,
            onTransitions: (List<ActivityTransitionSample>) -> Unit,
        ): AppResult<Unit> {
            startCalls += 1
            startedPeriodSeconds = periodSeconds
            updateCallback = onUpdate
            transitionCallback = onTransitions
            return AppResult.success(Unit)
        }

        override suspend fun stop(): AppResult<Unit> {
            stopCalls += 1
            return AppResult.success(Unit)
        }

        fun emitUpdate(update: ActivityUpdate) {
            checkNotNull(updateCallback).invoke(update)
        }

        fun emitTransitions(transitions: List<ActivityTransitionSample>) {
            checkNotNull(transitionCallback).invoke(transitions)
        }
    }
}
