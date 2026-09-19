package com.tomasrepcik.sensorbox.recording.archive

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import javax.inject.Inject

sealed interface RecordingArchiveSelection {
    data class Selected(val uri: String, val grantFlags: Int) : RecordingArchiveSelection
    data object Cancelled : RecordingArchiveSelection
}

interface RecordingArchiveRepository {
    fun isSelected(): AppResult<Boolean>

    fun path(): AppResult<String?>

    fun select(selection: RecordingArchiveSelection.Selected): AppResult<Unit>
}

class DocumentRecordingArchiveRepository @Inject constructor(private val storage: DocumentStorage) :
    RecordingArchiveRepository {
    override fun isSelected(): AppResult<Boolean> = storage.hasConfiguredDirectory()

    override fun path(): AppResult<String?> = storage.displayPath()

    override fun select(selection: RecordingArchiveSelection.Selected): AppResult<Unit> =
        storage.persistRootAccess(selection.uri, selection.grantFlags)
}
