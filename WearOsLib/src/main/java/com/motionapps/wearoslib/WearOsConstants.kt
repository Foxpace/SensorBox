package com.motionapps.wearoslib

/**
 * Constants, which are used by both sides at phone and
 */
object WearOsConstants {

    // these paths needs to be the same in manifest
    // also both of them needs XML file with these capabilities
    const val PHONE_APP_CAPABILITY = "phone_app"
    const val PHONE_MESSAGE_PATH = "/com.motionapps.sensorbox.Activity" // in phone app

    const val WEAR_APP_CAPABILITY = "wear_app"
    const val WEAR_MESSAGE_PATH = "/com.motionapps.sensorbox.Sensors" // in wear app

    // basic messages to send
    const val WEAR_STATUS = "SEND_STATUS"
    const val WEAR_STATUS_EXTRA = "SEND_STATUS_EXTRA"
    const val WEAR_KILL_APP = "KILL_WEAR_APP"
    const val WEAR_SEND_PATHS = "WEAR_SEND_PATHS"
    const val WEAR_SEND_PATHS_EXTRA = "WEAR_SEND_PATHS_EXTRA"
    const val WEAR_SEND_FILE = "WEAR_SEND_FILE"
    const val WEAR_HEART_RATE_PERMISSION_REQUIRED = "HEART_RATE_PERMISSION"
    const val WEAR_HEART_RATE_PERMISSION_REQUIRED_BOOLEAN = "HEART_RATE_PERMISSION_BOOLEAN"

    const val START_MAIN_ACTIVITY = "START_MAIN_ACTIVITY"
    const val START_MEASUREMENT = "START_MEASUREMENT"
    const val STOP_SYNC = "STOP_SYNC"
    const val NUMBER_OF_FILES = "NUMBER_OF_FILES"
    const val DELETE_ALL_MEASUREMENTS = "DELETE_ALL_MEASUREMENTS"
    const val DELETE_FOLDER = "DELETE_FOLDER"

    // Realtime broadcasting of samples to chart
    const val WEAR_START_SENSOR_REAL_TIME = "START_WEAR_SENSOR_REAL_TIME"
    const val WEAR_END_SENSOR_REAL_TIME = "END_WEAR_SENSOR_REAL_TIME"
    const val WEAR_SEND_SENSOR_INFO = "SEND_WEAR_SENSOR_INFO"
    const val WEAR_SEND_SENSOR_INFO_EXTRA = "SEND_WEAR_SENSOR_INFO_EXTRA"

    // to send sample from sensor
    const val SAMPLE_PATH = "/com.motionapps.sensorbox."
    const val SAMPLE_PATH_VALUE = "/com.motionapps.sensorbox.sample."
    const val SAMPLE_PATH_TIME = "/com.motionapps.sensorbox.sample.time"
    const val FILE_TO_TRANSFER_PATH = "/com.motionapps.sensorbox.file_to_transfer"


}