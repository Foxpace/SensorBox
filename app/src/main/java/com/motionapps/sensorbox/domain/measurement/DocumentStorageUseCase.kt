package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface DocumentStorageGateway {
    fun hasStorage(): AppResult<Boolean>

    fun displayPath(): AppResult<String?>

    fun persist(resultIntent: Intent): AppResult<Unit>
}

class DocumentStorageUseCase @Inject constructor(@ApplicationContext private val context: Context) :
    DocumentStorageGateway {
    override fun hasStorage(): AppResult<Boolean> = NativeDocumentStorage.hasAppDirectory(context, APP_DIRECTORY)

    override fun displayPath(): AppResult<String?> = NativeDocumentStorage.displayPath(context, APP_DIRECTORY)

    override fun persist(resultIntent: Intent): AppResult<Unit> =
        NativeDocumentStorage.persistRootAccess(context, resultIntent, APP_DIRECTORY)

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
    }
}
