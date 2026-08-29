package com.tomasrepcik.sensorbox.measurements.sync

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tomasrepcik.sensorbox.wearoslib.sync.WearSyncEmulatorFixture
import com.tomasrepcik.sensorbox.wearoslib.sync.WearSyncFileFixture
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WearToPhoneSyncReceiverEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scenario = WearSyncEmulatorFixture.scenario(
        requireNotNull(
            InstrumentationRegistry.getArguments().getString(WearSyncEmulatorFixture.SCENARIO_ARGUMENT),
        ) { "Missing ${WearSyncEmulatorFixture.SCENARIO_ARGUMENT} instrumentation argument" },
    )
    private val root = File(context.filesDir, WearSyncEmulatorFixture.APP_DIRECTORY)

    @Before
    fun preparePhoneDestination() {
        root.deleteRecursively()
        scenario.transferredFiles.forEach { fixture ->
            fixture.existingPhoneContent?.let { existingContent ->
                receivedFile(fixture).apply {
                    parentFile?.mkdirs()
                    writeBytes(existingContent)
                }
            }
        }
    }

    @Test
    fun givenPairedEmulatorsWhenWearSendsFilesThenPhoneReceivesExactBytes() {
        Log.i(LOG_TAG, "READY_FOR_WEAR_TRANSFER")

        scenario.transferredFiles.forEach { fixture ->
            val received = receivedFile(fixture)
            assertTrue("Timed out waiting for $received", waitFor(received, fixture.content))
            assertArrayEquals("Wrong bytes in $received", fixture.content, received.readBytes())
        }
        scenario.files.filterNot(WearSyncFileFixture::shouldTransfer).forEach { fixture ->
            assertTrue("Unsupported file was transferred: ${receivedFile(fixture)}", !receivedFile(fixture).exists())
        }
    }

    private fun receivedFile(fixture: WearSyncFileFixture) =
        File(root, "${fixture.receivedMeasurementName}/${fixture.fileName}")

    private fun waitFor(receivedFile: File, expectedContent: ByteArray): Boolean {
        repeat(MAX_ATTEMPTS) {
            if (receivedFile.isFile && receivedFile.contentEquals(expectedContent)) return true
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    private fun File.contentEquals(expectedContent: ByteArray): Boolean =
        length() == expectedContent.size.toLong() && readBytes().contentEquals(expectedContent)

    private companion object {
        const val LOG_TAG = "SensorBoxEmulatorTest"
        const val POLL_INTERVAL_MILLIS = 500L
        const val MAX_ATTEMPTS = 120
    }
}
