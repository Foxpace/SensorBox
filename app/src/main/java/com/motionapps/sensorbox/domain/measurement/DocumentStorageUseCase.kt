package com.motionapps.sensorbox.domain.measurement

import android.content.Intent
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.storage.DocumentStorage
import javax.inject.Inject

interface DocumentStorageGateway {
    fun hasStorage(): AppResult<Boolean>

    fun displayPath(): AppResult<String?>

    fun persist(resultIntent: Intent): AppResult<Unit>
}

class DocumentStorageUseCase @Inject constructor(private val storage: DocumentStorage) : DocumentStorageGateway {
    override fun hasStorage(): AppResult<Boolean> = storage.hasConfiguredDirectory()

    override fun displayPath(): AppResult<String?> = storage.displayPath()

    override fun persist(resultIntent: Intent): AppResult<Unit> = storage.persistRootAccess(resultIntent)
}
