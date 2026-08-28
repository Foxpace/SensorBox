package com.tomasrepcik.sensorbox.wearoslib.files

data class WearSyncFileFixture(
    val measurementName: String,
    val fileName: String,
    val content: ByteArray,
    val shouldTransfer: Boolean = true,
    val existingPhoneContent: ByteArray? = null,
) {
    val receivedMeasurementName: String = "WEAR_$measurementName"
}

data class WearSyncScenario(
    val id: String,
    val files: List<WearSyncFileFixture>,
    val emptyMeasurementNames: List<String> = emptyList(),
) {
    val transferredFiles: List<WearSyncFileFixture> = files.filter(WearSyncFileFixture::shouldTransfer)
}

object WearSyncEmulatorFixture {
    const val APP_DIRECTORY = "SensorBox"
    const val SCENARIO_ARGUMENT = "syncScenario"

    val scenarioIds: List<String> get() = scenarios.map(WearSyncScenario::id)

    fun scenario(id: String): WearSyncScenario = scenarios.singleOrNull { it.id == id }
        ?: error("Unknown sync scenario '$id'. Available: ${scenarioIds.joinToString()}")

    private val scenarios = listOf(
        WearSyncScenario(
            id = "single_csv",
            files = listOf(
                textFile(
                    measurement = "EMULATOR_SINGLE_CSV",
                    name = "accelerometer.csv",
                    text = "t_sensor;x;y;z;accuracy\n123;1.0;2.0;3.0;3\n",
                ),
            ),
        ),
        WearSyncScenario(
            id = "mixed_formats",
            files = listOf(
                textFile("MORNING WALK", "accelerometer.csv", "time;x;y;z\n1;0.1;0.2;0.3\n"),
                textFile("MORNING WALK", "summary.json", "{\"activity\":\"chôdza 🚶\",\"samples\":1}"),
                textFile("EVENING REST", "notes.txt", "Pokojné meranie — bez pohybu.\n"),
            ),
        ),
        WearSyncScenario(
            id = "empty_and_ignored",
            files = listOf(
                textFile("EMPTY DATA", "empty.txt", ""),
                WearSyncFileFixture(
                    measurementName = "EMPTY DATA",
                    fileName = "raw.bin",
                    content = byteArrayOf(0x00, 0x01, 0x7F),
                    shouldTransfer = false,
                ),
            ),
            emptyMeasurementNames = listOf("EMPTY DIRECTORY"),
        ),
        WearSyncScenario(
            id = "overwrite_existing",
            files = listOf(
                textFile(
                    measurement = "REPEATED SYNC",
                    name = "location.csv",
                    text = "time;latitude;longitude\n2;48.1486;17.1077\n",
                    existingPhoneText = "stale phone content",
                ),
            ),
        ),
        WearSyncScenario(
            id = "same_name_different_sessions",
            files = listOf(
                textFile("SESSION ONE", "accelerometer.csv", "session;value\none;1\n"),
                textFile("SESSION TWO", "accelerometer.csv", "session;value\ntwo;2\n"),
            ),
        ),
        WearSyncScenario(
            id = "large_payload",
            files = listOf(
                WearSyncFileFixture(
                    measurementName = "LONG RECORDING",
                    fileName = "sensor.txt",
                    content = ByteArray(256 * 1024) { index -> (index % 251).toByte() },
                ),
            ),
        ),
    )

    private fun textFile(
        measurement: String,
        name: String,
        text: String,
        existingPhoneText: String? = null,
    ) = WearSyncFileFixture(
        measurementName = measurement,
        fileName = name,
        content = text.encodeToByteArray(),
        existingPhoneContent = existingPhoneText?.encodeToByteArray(),
    )
}
