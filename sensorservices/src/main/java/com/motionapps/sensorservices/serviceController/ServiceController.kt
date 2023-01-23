package com.motionapps.sensorservices.serviceController

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.*
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementStates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * handles all the interactions of the service and controls the flow of the measurement
 *
 */
class ServiceController {

    // all handles in one place
    private val activityRecognition: ActivityRecognition = ActivityRecognition()
    private val alarmNoiseHandler: AlarmNoiseHandler = AlarmNoiseHandler()
    private val extraInfoHandler: ExtraInfoHandler = ExtraInfoHandler()
    private val gpsMeasurement: GPSMeasurement = GPSMeasurement(GPSHandler())
    private val sensorMeasurement: SensorMeasurement = SensorMeasurement()
    private val significantMotion: SignificantMotion = SignificantMotion()

    // basic parameters to use in service
    private var paramType: Int = MeasurementService.ENDLESS
    private var paramSensorId: IntArray? = null
    private var paramTimeIntervals: IntArray = intArrayOf(-1)
    private var paramChecks: BooleanArray = booleanArrayOf(false, false, false, false, false)

    private var paramFolderName = ""
    private var paramInternalStorage = false
    private var paramSensorSpeed: Int = SensorManager.SENSOR_DELAY_FASTEST
    private var paramGPSToMeasure: Boolean = false

    /**
     * on first run
     * get data from the intent and unpack it
     *
     * @param context
     * @param intent - unpack to fill parameters for the measurement
     */
    fun onInit(context: Context, intent: Intent?){

        intent?.let {
            it.extras?.let{b ->

                paramFolderName = b.getString(MeasurementService.FOLDER_NAME, "")
                paramInternalStorage = b.getBoolean(MeasurementService.INTERNAL_STORAGE, false)
                paramSensorId = b.getIntArray(MeasurementService.ANDROID_SENSORS)
                paramSensorSpeed = b.getInt(MeasurementService.ANDROID_SENSORS_SPEED, SensorManager.SENSOR_DELAY_FASTEST)
                paramType = b.getInt(MeasurementService.TYPE, MeasurementService.ENDLESS)
                paramTimeIntervals = b.getIntArray(MeasurementService.TIME_INTERVALS)!!
                paramGPSToMeasure = b.getBoolean(MeasurementService.GPS, false)
                paramChecks = b.getBooleanArray(MeasurementService.OTHER)!!

                extraInfoHandler.onStart(context, paramInternalStorage) // init of the notes and alarms
                extraInfoHandler.handleNotes(b.getStringArrayList(MeasurementService.NOTES))
            }
        }
    }

    /**
     * Stores annotation to handler
     *
     * @param time - milliseconds
     * @param text - text of the annotation
     */
    fun onAnnotation(time: Long, text: String) {
        extraInfoHandler.writeAnnotation(time, text)
    }

    /**
     * passes parameters to handlers and starts all the measurements
     *
     * @param context
     * @return
     */
    fun onStart(context: Context): Boolean {
        val date = StorageHandler.getDate(System.currentTimeMillis(), "dd_MM_yyyy_HH_mm_ss")

        val measurement = when(paramType){
            MeasurementService.SHORT -> MeasurementService.SHORT_STRING
//            MeasurementService.LONG -> MeasurementService.LONG_STRING
            else -> MeasurementService.ENDLESS_STRING
        }

        extraInfoHandler.folderName = paramFolderName
        extraInfoHandler.date = date
        extraInfoHandler.measurementType = measurement

        if(paramInternalStorage) {
            if (!StorageHandler.createInternalStorageMeasurementFolder(context, paramFolderName)) {
                return false
            }
        }else{
            if (!StorageHandler.createFolderMeasurement(context, paramFolderName)) {
                return false
            }
        }

        // Sensors
        paramSensorId?.let {
            val params: Bundle = Bundle().apply {
                putString(MeasurementInterface.FOLDER_NAME, paramFolderName)
                putBoolean(MeasurementInterface.INTERNAL_STORAGE, paramInternalStorage)
                putIntArray(MeasurementInterface.SENSOR_ID, paramSensorId)
                putInt(MeasurementInterface.SENSOR_SPEED, paramSensorSpeed)
            }
            sensorMeasurement.initMeasurement(context, params)
            sensorMeasurement.startMeasurement(context)
        }

        // GPS
        if(paramGPSToMeasure){
            val params: Bundle = Bundle().apply {
                putString(MeasurementInterface.FOLDER_NAME, paramFolderName)
                putBoolean(MeasurementInterface.INTERNAL_STORAGE, paramInternalStorage)
            }
            gpsMeasurement.initMeasurement(context, params)
            gpsMeasurement.startMeasurement(context)
        }

        // activity recognition
        if(paramChecks[0]){
            val params: Bundle = Bundle().apply {
                putString(MeasurementInterface.FOLDER_NAME, paramFolderName)
                putBoolean(MeasurementInterface.INTERNAL_STORAGE, paramInternalStorage)
            }
            activityRecognition.initMeasurement(context, params)
            activityRecognition.startMeasurement(context)
        }

        // significant motion
        if(paramChecks[1]){
            val params: Bundle = Bundle().apply {
                putString(MeasurementInterface.FOLDER_NAME, paramFolderName)
                putBoolean(MeasurementInterface.INTERNAL_STORAGE, paramInternalStorage)
            }
            significantMotion.initMeasurement(context, params)
            significantMotion.startMeasurement(context)
        }

        return true
    }

    /**
     * internal counter for the SHORT measurement based on flow and delay
     *
     */
    suspend fun shortTimer() = flow {

        for(i in paramTimeIntervals[1]-1 downTo 0){
            delay(1000L)
            alarmNoiseHandler.onShortTick(i*1000L)
            emit(MeasurementStates.OnTick(i))
        }
        emit(MeasurementStates.OnShortEnd)
    }

//    /**
//     * Sets intent with AlarmManager
//     *
//     * @param context
//     */
//    fun longTimer(context: Context){
//
//        val addedTime: Long = ((paramTimeIntervals[0]*3600 + paramTimeIntervals[1] * 60) * 1000).toLong()
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val pendingIntent: PendingIntent = getAlarmIntent(context)
//
//        if (SDK_INT >= Build.VERSION_CODES.M) {
//            alarmManager.setExactAndAllowWhileIdle(
//                AlarmManager.RTC_WAKEUP,
//                System.currentTimeMillis() + addedTime, pendingIntent
//            )
//        } else {
//            alarmManager.setExact(
//                AlarmManager.RTC_WAKEUP,
//                System.currentTimeMillis() + addedTime, pendingIntent
//            )
//        }
//
//        alarmNoiseHandler.setLongAlarms(context, alarmManager)
//
//    }

    /**
     * @param context
     * @return creates intent to stop Service
     */
    private fun getAlarmIntent(context: Context): PendingIntent{
        val intent = Intent(MeasurementService.STOP_SERVICE)
        intent.flags = Intent.FLAG_RECEIVER_FOREGROUND

        val flags =
            if (SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        return PendingIntent.getBroadcast(
            context, 50, intent,
            flags
        )
    }

    /**
     * creates JSON and all the handler are saved into csv files
     *
     * @param context
     */
    suspend fun onStop(context: Context) {
        extraInfoHandler.addAlarms(alarmNoiseHandler)
        extraInfoHandler.writeExtra(context, paramInternalStorage)

        val saving = CoroutineScope(Dispatchers.Main).launch {
            sensorMeasurement.onDestroyMeasurement(context)
            gpsMeasurement.onDestroyMeasurement(context)
            activityRecognition.onDestroyMeasurement(context)
        }
        saving.join()
//        if(paramType == MeasurementService.LONG){
//            cancelAlarm(context)
//        }
    }

    /**
     * alarm is removed for Long measurement
     *
     * @param context
     */
    private fun cancelAlarm(context: Context){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent: PendingIntent = getAlarmIntent(context)
        alarmManager.cancel(pendingIntent)
        alarmNoiseHandler.onDestroy()
    }
}