package com.motionapps.wearoslib.files

object WearSyncEmulatorFixture {
    const val APP_DIRECTORY = "SensorBox"
    const val MEASUREMENT_NAME = "EMULATOR_SYNC_TEST"
    const val RECEIVED_MEASUREMENT_NAME = "WEAR_$MEASUREMENT_NAME"
    const val FILE_NAME = "accelerometer.csv"
    const val CONTENT =
        "t_sensor;t_unix;x;y;z;accuracy\n" +
            "123;456;1.0;2.0;3.0;3\n"
}
