package com.motionapps.sensorbox.domain.sync

import android.content.Context
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.suspendAppResult
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.motionapps.wearoslib.connectivity.WearConnectionRepository
import com.motionapps.wearoslib.files.WearFileMetadata
import com.motionapps.wearoslib.files.WearFileTransferClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SyncWearMeasurementsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionRepository: WearConnectionRepository,
    private val transferClient: WearFileTransferClient,
) {
    suspend operator fun invoke(): Result<Int> = suspendAppResult(AppError.Kind.CONNECTIVITY, "Find phone") {
        connectionRepository.findNode(PHONE_APP_CAPABILITY)
    }.suspendFlatMap { node ->
        if (node == null) {
            return@suspendFlatMap Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Find connected phone"))
        }
        appResult(AppError.Kind.STORAGE, "List Wear measurements", ::measurementFiles).suspendFlatMap { files ->
            var transferResult: Result<Unit> = Result.success(Unit)
            for (file in files) {
                if (transferResult.isFailure) break
                transferResult = sendFile(node.id, file)
            }
            transferResult.map { files.size }
        }
    }.withAppError(AppError.Kind.CONNECTIVITY, "Sync Wear measurements")

    private suspend fun sendFile(nodeId: String, file: File): Result<Unit> {
        val measurementName = file.parentFile?.name
            ?: return Result.failure(AppError(AppError.Kind.STORAGE, "Read Wear measurement folder"))
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
