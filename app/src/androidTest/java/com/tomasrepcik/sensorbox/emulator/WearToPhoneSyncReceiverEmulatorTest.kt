package com.tomasrepcik.sensorbox.emulator

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motionapps.wearoslib.files.WearSyncEmulatorFixture
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearToPhoneSyncReceiverEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val receivedFile = File(
        context.filesDir,
        "${WearSyncEmulatorFixture.APP_DIRECTORY}/" +
            "${WearSyncEmulatorFixture.RECEIVED_MEASUREMENT_NAME}/${WearSyncEmulatorFixture.FILE_NAME}",
    )

    @Before
    fun removePreviousTransfer() {
        receivedFile.parentFile?.deleteRecursively()
    }

    @Test
    fun givenPairedEmulatorsWhenWearSendsCsvThenPhoneReceivesExactBytes() {
        Log.i(LOG_TAG, "READY_FOR_WEAR_TRANSFER")

        assertTrue("Timed out waiting for $receivedFile", waitForReceivedFile())
        assertArrayEquals(WearSyncEmulatorFixture.CONTENT.encodeToByteArray(), receivedFile.readBytes())
    }

    private fun waitForReceivedFile(): Boolean {
        repeat(MAX_ATTEMPTS) {
            if (receivedFile.isFile && receivedFile.length() > 0L) return true
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
