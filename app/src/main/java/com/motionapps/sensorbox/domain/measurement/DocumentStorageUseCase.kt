package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DocumentStorageUseCase @Inject constructor(@ApplicationContext private val context: Context) {
    fun hasStorage(): Result<Boolean> = NativeDocumentStorage.hasAppDirectory(context, APP_DIRECTORY)

    fun displayPath(): Result<String?> = NativeDocumentStorage.displayPath(context, APP_DIRECTORY)

    fun persist(resultIntent: Intent): Result<Unit> =
        NativeDocumentStorage.persistRootAccess(context, resultIntent, APP_DIRECTORY)

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
    }
}
