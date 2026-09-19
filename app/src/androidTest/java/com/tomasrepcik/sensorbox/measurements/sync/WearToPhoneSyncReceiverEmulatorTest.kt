package com.tomasrepcik.sensorbox.measurements.sync

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tomasrepcik.sensorbox.wearoslib.sync.WearSyncEmulatorFixture
import com.tomasrepcik.sensorbox.wearoslib.sync.WearSyncFileFixture
import dagger.hilt.android.EntryPointAccessors
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearToPhoneSyncReceiverEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scenario = WearSyncEmulatorFixture.scenario(
        requireNotNull(
            InstrumentationRegistry.getArguments().getString(WearSyncEmulatorFixture.SCENARIO_ARGUMENT),
        ) { "Missing ${WearSyncEmulatorFixture.SCENARIO_ARGUMENT} instrumentation argument" },
    )
    private val root by lazy { selectedArchive(context) }

    @Before
    fun preparePhoneDestination() {
        scenario.files.map { it.receivedMeasurementName }.distinct().forEach { name ->
            root.findFile(name)?.delete()
        }
        scenario.transferredFiles.forEach { fixture ->
            fixture.existingPhoneContent?.let { existingContent ->
                val folder = root.findFile(fixture.receivedMeasurementName)
                    ?: checkNotNull(root.createDirectory(fixture.receivedMeasurementName))
                val file = checkNotNull(folder.createFile("application/octet-stream", fixture.fileName))
                checkNotNull(context.contentResolver.openOutputStream(file.uri)).use { it.write(existingContent) }
            }
        }
    }

    @Test
    fun givenPairedEmulatorsWhenWearSendsFilesThenPhoneReceivesExactBytes() {
        val dependencies = EntryPointAccessors.fromApplication(context, SyncTestEntryPoint::class.java)
        val viewModel = WatchSyncViewModel(
            dependencies.sync(), dependencies.commands(), dependencies.storage(), kotlinx.coroutines.Dispatchers.IO,
            dependencies.transfers(), dependencies.destination(),
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.accept(WatchSyncIntent.COPY) }
        Log.i(LOG_TAG, "READY_FOR_WEAR_TRANSFER")

        scenario.transferredFiles.forEach { fixture ->
            assertTrue("Timed out waiting for ${fixture.fileName}", waitFor(fixture))
            val received = checkNotNull(receivedFile(fixture))
            assertArrayEquals("Wrong bytes in $received", fixture.content, received.readBytes(context))
        }
        scenario.files.filterNot(WearSyncFileFixture::shouldTransfer).forEach { fixture ->
            assertTrue("Unsupported file was transferred: ${receivedFile(fixture)}", receivedFile(fixture) == null)
        }
    }

    private fun receivedFile(fixture: WearSyncFileFixture): DocumentFile? =
        root.findFile(fixture.receivedMeasurementName)?.findFile(fixture.fileName)

    private fun waitFor(fixture: WearSyncFileFixture): Boolean {
        repeat(MAX_ATTEMPTS) {
            val received = receivedFile(fixture)
            if (received?.isFile == true && received.readBytes(context).contentEquals(fixture.content)) return true
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    private companion object {
        const val LOG_TAG = "SensorBoxEmulatorTest"
        const val POLL_INTERVAL_MILLIS = 500L
        const val MAX_ATTEMPTS = 120
    }
}
