package com.tomasrepcik.sensorbox.wearoslib.files

import kotlinx.serialization.Serializable

@Serializable
data class WearFileMetadata(val measurementName: String, val fileName: String)
