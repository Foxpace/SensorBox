package com.tomasrepcik.sensorbox.sync

import android.content.Context
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import com.tomasrepcik.sensorbox.core.failure.suspendFlatMap
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

interface SyncWatchMeasurementsUseCase {
    suspend operator fun invoke(requestId: String): AppResult<Int>

    fun cancel(requestId: String)

    fun available(): AppResult<Pair<Int, Int>>
}

@Singleton
class DefaultSyncWatchMeasurementsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionRepository: WearConnectionRepository,
    private val transferMeasurement: TransferWatchMeasurementUseCase,
    private val sessions: RecordingSessionStore,
    private val syncLock: MeasurementSyncLock,
    private val transferClient: WearFileTransferClient,
) : SyncWatchMeasurementsUseCase {
    private val transferLock = Mutex()
    private val jobs = ConcurrentHashMap<String, Job>()

    override fun cancel(requestId: String) {
        transferClient.cancel(requestId)
        jobs[requestId]?.cancel()
    }

    override fun available(): AppResult<Pair<Int, Int>> = appResult(AppErrorCode.STORAGE, "List watch measurements") {
        val files = measurementFiles()
        files.size to files.map { it.parent }.distinct().size
    }

    override suspend fun invoke(requestId: String): AppResult<Int> {
        syncLock.begin(requestId)
        try {
            return withContext(Dispatchers.IO) {
                jobs[requestId] = checkNotNull(currentCoroutineContext()[Job])
                withTimeoutOrNull(280_000L.milliseconds) {
                    transferLock.withLock { transferMeasurements(requestId) }
                } ?: AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Watch sync timed out"))
            }
        } finally {
            jobs.remove(requestId)
            transferClient.cancel(requestId)
            syncLock.finish(requestId)
        }
    }

    private suspend fun transferMeasurements(requestId: String): AppResult<Int> =
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Find phone") {
            connectionRepository.findNode(PHONE_APP_CAPABILITY)
        }.suspendFlatMap { node ->
            if (node == null) {
                return@suspendFlatMap AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Find connected phone"))
            }
            appResult(AppErrorCode.STORAGE, "List watch measurements", ::measurementFiles).suspendFlatMap { files ->
                transferFolders(node.id, files, requestId)
            }
        }

    private suspend fun transferFolders(nodeId: String, files: List<File>, requestId: String): AppResult<Int> {
        var transferred = 0
        for (measurement in files.groupBy { it.parent }.values) {
            val alreadyConfirmed = WatchMeasurementCleanup().isConfirmed(checkNotNull(measurement.first().parentFile))
            val result = transferMeasurement(nodeId, measurement, requestId)
            if (result.isFailure) return result.map { 0 }
            if (!alreadyConfirmed) transferred += measurement.size
        }
        return AppResult.success(transferred)
    }

    private fun measurementFiles(): List<File> {
        val session = sessions.state.value
        if (session == RecordingSessionState.Stopping) return emptyList()
        val activeFolder = (session as? RecordingSessionState.Running)?.folderName
        return File(context.filesDir, APP_DIRECTORY).listFiles().orEmpty()
            .filter { it.isDirectory && it.name != activeFolder }
            .flatMap { folder -> folder.listFiles().orEmpty().asIterable() }
            .filter { file -> file.isFile && file.extension.lowercase() in ALLOWED_EXTENSIONS }
            .sortedBy(File::getAbsolutePath)
    }

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
        val ALLOWED_EXTENSIONS = setOf("csv", "json", "txt")
    }
}
