package com.tomasrepcik.sensorbox.emulator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.domain.sync.SyncWearMeasurementsUseCase
import com.motionapps.wearoslib.connectivity.GooglePlayWearConnectionRepository
import com.motionapps.wearoslib.files.GooglePlayWearFileTransferClient
import com.motionapps.wearoslib.files.WearSyncEmulatorFixture
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearToPhoneSyncSenderEmulatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun createSingleMeasurementFixture() {
        val root = File(context.filesDir, WearSyncEmulatorFixture.APP_DIRECTORY)
        root.deleteRecursively()
        val measurementDirectory = File(root, WearSyncEmulatorFixture.MEASUREMENT_NAME)
        check(measurementDirectory.mkdirs())
        File(measurementDirectory, WearSyncEmulatorFixture.FILE_NAME)
            .writeText(WearSyncEmulatorFixture.CONTENT)
    }

    @Test
    fun givenPairedEmulatorsWhenWearSyncsThenOneCsvIsTransferred() = runBlocking {
        val sync = SyncWearMeasurementsUseCase(
            context = context,
            connectionRepository = GooglePlayWearConnectionRepository(context),
            transferClient = GooglePlayWearFileTransferClient(context),
        )

        assertEquals(1, syncWhenPhoneBecomesReachable(sync))
    }

    private suspend fun syncWhenPhoneBecomesReachable(sync: SyncWearMeasurementsUseCase): Int {
        var lastFailure: AppError? = null
        repeat(MAX_ATTEMPTS) {
            when (val result = sync()) {
                is AppResult.Success -> return result.value
                is AppResult.Failure -> lastFailure = result.error
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        val message = "Phone emulator did not become reachable: ${lastFailure?.code}"
        throw AssertionError(message, lastFailure?.cause)
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val MAX_ATTEMPTS = 60
    }
}
