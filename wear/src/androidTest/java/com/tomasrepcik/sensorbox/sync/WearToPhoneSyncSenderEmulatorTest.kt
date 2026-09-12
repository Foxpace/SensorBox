package com.tomasrepcik.sensorbox.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tomasrepcik.sensorbox.wearoslib.sync.WearSyncEmulatorFixture
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class WearToPhoneSyncSenderEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scenario = WearSyncEmulatorFixture.scenario(
        requireNotNull(
            InstrumentationRegistry.getArguments().getString(WearSyncEmulatorFixture.SCENARIO_ARGUMENT),
        ) { "Missing ${WearSyncEmulatorFixture.SCENARIO_ARGUMENT} instrumentation argument" },
    )

    @Before
    fun createScenarioFixture() {
        val root = File(context.filesDir, WearSyncEmulatorFixture.APP_DIRECTORY)
        (scenario.files.map { it.measurementName } + scenario.emptyMeasurementNames).distinct().forEach {
            File(root, it).deleteRecursively()
        }
        scenario.emptyMeasurementNames.forEach { File(root, it).mkdirs() }
        scenario.files.forEach { fixture ->
            File(root, fixture.measurementName).also(File::mkdirs)
                .resolve(fixture.fileName)
                .writeBytes(fixture.content)
        }
    }

    @Test
    fun givenPairedEmulatorsWhenWearSyncsThenEverySupportedFileIsTransferred() = runBlocking {
        // Given
        android.util.Log.i("SensorBoxEmulatorTest", "READY_FOR_PHONE_SYNC")
        val root = File(context.filesDir, WearSyncEmulatorFixture.APP_DIRECTORY)

        // When
        val cleaned = kotlinx.coroutines.withTimeoutOrNull(60_000L.milliseconds) {
            while (scenario.transferredFiles.any { File(root, "${it.measurementName}/${it.fileName}").exists() }) {
                delay(100L.milliseconds)
            }
            true
        }

        // Then
        org.junit.Assert.assertTrue("Watch did not receive a committed-folder acknowledgement", cleaned == true)
        scenario.files.filterNot { it.shouldTransfer }.forEach {
            org.junit.Assert.assertTrue(File(root, "${it.measurementName}/${it.fileName}").exists())
        }
    }
}
