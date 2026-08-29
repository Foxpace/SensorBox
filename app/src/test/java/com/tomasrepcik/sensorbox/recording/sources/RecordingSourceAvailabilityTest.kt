package com.tomasrepcik.sensorbox.recording.sources

import com.tomasrepcik.sensorbox.wearoslib.connection.FakeWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connection.WearNode
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingSourceAvailabilityTest {
    @Test
    fun `Given connected watch When sources are observed Then discovery is requested`() = runTest {
        // Given
        val repository = connectedRepository()
        val availability = availability(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            availability.observe().collect()
        }

        // When
        advanceUntilIdle()

        // Then
        val command = WearCommandCodec.decode(repository.sentMessages.single().payload).getOrThrow()
        assertEquals(WearCommand.RequestAvailableSensors, command)
        assertFalse(availability.current.isWatchConnected)
    }

    @Test
    fun `Given an empty watch response When it is received Then watch is available`() = runTest {
        // Given
        val availability = availability(connectedRepository())

        // When
        availability.receive(emptyList())

        // Then
        assertTrue(availability.current.isWatchConnected)
        assertTrue(availability.current.watchSensors.isEmpty())
    }

    @Test
    fun `Given watch sensors When received Then details are sorted and retained`() = runTest {
        // Given
        val availability = availability(connectedRepository())

        // When
        availability.receive(listOf(sensor(2, "Z sensor"), sensor(1, "A sensor"), sensor(1, "Duplicate")))

        // Then
        assertEquals(listOf(1, 2), availability.current.watchSensors.map(SensorDescriptor::type))
        assertEquals("A sensor", availability.current.watchSensors.first().name)
    }

    @Test
    fun `Given available watch sources When watch disconnects Then watch sources clear`() = runTest {
        // Given
        val repository = connectedRepository()
        val availability = availability(repository)
        availability.receive(listOf(sensor(1, "Sensor")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            availability.observe().collect()
        }

        // When
        repository.emit(WearConnection.Disconnected)
        advanceUntilIdle()

        // Then
        assertFalse(availability.current.isWatchConnected)
        assertTrue(availability.current.watchSensors.isEmpty())
    }

    private fun availability(repository: FakeWearConnectionRepository) = RecordingSourceAvailability(
        phoneSensors = AvailableSensorsUseCase { emptyList() },
        observeWatchCapability = ObserveWearCapabilityUseCase(repository),
        sendWatchCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
    )

    private fun connectedRepository() = FakeWearConnectionRepository(
        WearConnection.Connected(WearNode("watch", "Watch", isNearby = true)),
    )

    private fun sensor(type: Int, name: String) = WearSensorInfo(
        type = type,
        name = name,
        vendor = "Fixture",
        version = 1,
        stringType = "sensor.$type",
        maximumRange = 1f,
        resolution = 1f,
        power = 1f,
        minimumDelayMicros = 1,
        maximumDelayMicros = 1,
        reportingMode = 0,
        isWakeUpSensor = false,
    )
}
