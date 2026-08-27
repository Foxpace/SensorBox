package com.tomasrepcik.sensorbox.emulator

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.domain.sync.SyncWearMeasurementsUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.GooglePlayWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.files.GooglePlayWearFileTransferClient
import com.tomasrepcik.sensorbox.wearoslib.files.WearSyncEmulatorFixture
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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
        root.deleteRecursively()
        scenario.emptyMeasurementNames.forEach { File(root, it).mkdirs() }
        scenario.files.forEach { fixture ->
            File(root, fixture.measurementName).also(File::mkdirs)
                .resolve(fixture.fileName)
                .writeBytes(fixture.content)
        }
    }

    @Test
    fun givenPairedEmulatorsWhenWearSyncsThenEverySupportedFileIsTransferred() = runBlocking {
        val sync = SyncWearMeasurementsUseCase(
            context = context,
            connectionRepository = GooglePlayWearConnectionRepository(context),
            transferClient = GooglePlayWearFileTransferClient(context),
        )

        assertEquals(scenario.transferredFiles.size, syncWhenPhoneBecomesReachable(sync))
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
        val message = "Phone emulator did not become reachable: $lastFailure; ${connectionDiagnostics()}"
        throw AssertionError(message, lastFailure?.cause)
    }

    private suspend fun connectionDiagnostics(): String = runCatching {
        val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes).joinToString { it.displayName }
        val capabilities = Tasks.await(
            Wearable.getCapabilityClient(context).getAllCapabilities(CapabilityClient.FILTER_ALL),
        )
            .mapValues { (_, info) -> info.nodes.map { it.displayName } }
        "connectedNodes=[$nodes], capabilities=$capabilities"
    }.getOrElse { "diagnostics failed: $it" }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val MAX_ATTEMPTS = 60
    }
}
