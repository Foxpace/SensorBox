package com.tomasrepcik.sensorbox.measurements.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile

internal fun selectedArchive(context: Context): DocumentFile {
    val permission = context.contentResolver.persistedUriPermissions
        .filter { it.isReadPermission && it.isWritePermission }
        .maxByOrNull { it.persistedTime }
        ?: error("Choose a recording folder in the phone app before running sync emulator tests")
    return checkNotNull(DocumentFile.fromTreeUri(context, permission.uri))
}

internal fun DocumentFile.readBytes(context: Context): ByteArray =
    checkNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes() }
