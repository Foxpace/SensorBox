package com.tomasrepcik.sensorbox.wearoslib.sync

import kotlinx.serialization.Serializable

@Serializable
data class WearFileMetadata(val measurementName: String, val fileName: String, val requestId: String) {
    val isCommit: Boolean get() = fileName == COMMIT_FILE

    companion object {
        const val COMMIT_FILE = "__sensorbox_commit__"
    }
}
