package com.tomasrepcik.sensorbox.wearoslib.sync

import kotlinx.serialization.Serializable

@Serializable
data class WearFileMetadata(val measurementName: String, val fileName: String)
