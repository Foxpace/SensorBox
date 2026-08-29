package com.tomasrepcik.sensorbox.domain.diagnostics

import android.content.Context
import androidx.core.content.FileProvider
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class DiagnosticsShareFile(val contentUri: String, val displayName: String, val mimeType: String)

interface DiagnosticsShareFilePreparer {
    fun prepare(text: String): AppResult<DiagnosticsShareFile>
}

class AndroidDiagnosticsShareFilePreparer @Inject constructor(@ApplicationContext private val context: Context) :
    DiagnosticsShareFilePreparer {
    override fun prepare(text: String): AppResult<DiagnosticsShareFile> = appResult(
        AppErrorCode.STORAGE,
        "Prepare diagnostics share file",
    ) {
        val directory = context.filesDir.resolve("diagnostics").apply { mkdirs() }
        val file = directory.resolve(FILE_NAME).apply { writeText(text) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        DiagnosticsShareFile(uri.toString(), file.name, MIME_TYPE)
    }

    private companion object {
        const val FILE_NAME = "sensorbox-diagnostics-export.jsonl"
        const val MIME_TYPE = "application/x-ndjson"
    }
}
