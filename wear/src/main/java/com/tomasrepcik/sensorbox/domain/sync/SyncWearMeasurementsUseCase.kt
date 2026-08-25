package com.tomasrepcik.sensorbox.domain.sync

import android.content.Context
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.suspendAppResult
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.files.WearFileMetadata
import com.tomasrepcik.sensorbox.wearoslib.files.WearFileTransferClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SyncWearMeasurementsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionRepository: WearConnectionRepository,
    private val transferClient: WearFileTransferClient,
) {
    suspend operator fun invoke(): AppResult<Int> = suspendAppResult(AppErrorCode.CONNECTIVITY, "Find phone") {
        connectionRepository.findNode(PHONE_APP_CAPABILITY)
    }.suspendFlatMap { node ->
        if (node == null) {
            return@suspendFlatMap AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Find connected phone"))
        }
        appResult(AppErrorCode.STORAGE, "List Wear measurements", ::measurementFiles).suspendFlatMap { files ->
            var transferResult: AppResult<Unit> = AppResult.success(Unit)
            for (file in files) {
                if (transferResult.isFailure) break
                transferResult = sendFile(node.id, file)
            }
            transferResult.map { files.size }
        }
    }.withAppError(AppErrorCode.CONNECTIVITY, "Sync Wear measurements")

    private suspend fun sendFile(nodeId: String, file: File): AppResult<Unit> {
        val measurementName = file.parentFile?.name
            ?: return AppResult.failure(AppError(AppErrorCode.STORAGE, "Read Wear measurement folder"))
        return transferClient.send(
            nodeId = nodeId,
            metadata = WearFileMetadata(measurementName, file.name),
            input = file::inputStream,
        )
    }

    private fun measurementFiles(): List<File> {
        val root = File(context.filesDir, APP_DIRECTORY)
        return root.listFiles().orEmpty()
            .filter(File::isDirectory)
            .flatMap { folder -> folder.listFiles().orEmpty().asIterable() }
            .filter { file -> file.isFile && file.extension.lowercase() in ALLOWED_EXTENSIONS }
            .sortedBy(File::getAbsolutePath)
    }

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
        val ALLOWED_EXTENSIONS = setOf("csv", "json", "txt")
    }
}
