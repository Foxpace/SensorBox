package com.tomasrepcik.sensorbox.domain.recording

import android.content.Intent
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import javax.inject.Inject

interface RecordingArchiveRepository {
    fun isSelected(): AppResult<Boolean>

    fun path(): AppResult<String?>

    fun select(resultIntent: Intent): AppResult<Unit>
}

class DocumentRecordingArchiveRepository @Inject constructor(private val storage: DocumentStorage) :
    RecordingArchiveRepository {
    override fun isSelected(): AppResult<Boolean> = storage.hasConfiguredDirectory()

    override fun path(): AppResult<String?> = storage.displayPath()

    override fun select(resultIntent: Intent): AppResult<Unit> = storage.persistRootAccess(resultIntent)
}
