package com.motionapps.wearoslib.protocol

sealed interface WearCommand {
    data object LaunchPhone : WearCommand

    data class StartMeasurement(
        val folderName: String,
        val sensorIds: List<Int>,
        val includesGps: Boolean,
        val startAtEpochMillis: Long = System.currentTimeMillis(),
        val durationMillis: Long = 0L,
        val measurementType: String = "ENDLESS",
    ) : WearCommand

    data object StopMeasurement : WearCommand

    data object SyncMeasurements : WearCommand

    data object RequestSensorList : WearCommand

    data class SensorList(val sensors: List<WearSensorInfo>) : WearCommand
}

data class WearSensorInfo(val type: Int, val name: String, val vendor: String, val isHeartRate: Boolean)
