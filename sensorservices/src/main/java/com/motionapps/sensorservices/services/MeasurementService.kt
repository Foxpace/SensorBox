package com.motionapps.sensorservices.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.motionapps.sensorservices.R
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.serviceController.ServiceController
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsHandler
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class MeasurementService : Service(), WearOsListener {


    private val serviceBroadcastReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null){
                // stops service and pending intent to stop, or on low battery
                if(intent.action == STOP_SERVICE || intent.action == Intent.ACTION_BATTERY_LOW){
                    if(!intent.getBooleanExtra(USER, false)){
                        sendOnFinishNotification()
                    }
                    context?.sendBroadcast(Intent(STOP_ACTIVITY))
                    cancelService()

                }else if(intent.action == ANNOTATION){ // to pass annotation from the measurementActivity
                    intent.extras?.let {
                        val time: Long = it.getLong(ANNOTATION_TIME, -1L)
                        val text: String = it.getString(ANNOTATION_TEXT, "")
                        if(time != -1L){
                            serviceController.onAnnotation(time, text)
                        }
                    }
                }
//                else if(intent.action == STATUS && running){
//                    sendBroadcast(Intent(RUNNING))
//                }
            }
        }
    }

    inner class MeasurementBinder : Binder() {
        fun getService(): MeasurementService {
            return this@MeasurementService
        }

    }

    override fun onBind(p0: Intent?): IBinder {
        return MeasurementBinder()
    }

    interface OnMeasurementStateListener{
        fun onServiceState(measurementStates: MeasurementStates)
    }

    // can pass information directly to the listener
    // used mainly for the short measurement to pass countdown
    var onMeasurementStateListener: OnMeasurementStateListener? = null

    var running = false
    var intent: Intent? = null
    val startTime: Long = SystemClock.elapsedRealtime()

    private var wakeLock: PowerManager.WakeLock? = null
    private var receiver: Boolean = false
    private var shortJob: Job? = null

    // params
    var paramType: Int = ENDLESS
    var paramSensorId: IntArray? = null
    private var paramInternalStorage = false
    var paramTimeIntervals: IntArray = intArrayOf(-1)
    private var paramChecks: BooleanArray = booleanArrayOf(false, false, false, false, false)

    // main controller for measurement
    private val serviceController: ServiceController = ServiceController()

    // wear Os references
    private val wearOsHandler: WearOsHandler = WearOsHandler()
    private var wearOsPresence: WearOsStates.PresenceResult? = null
    private var wearOsJob: Job? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true

        intent?.let {
            unwrapIntent(it)
        }

        if (!paramInternalStorage) { // show notification at phone only
            startForeground(
                ONGOING_NOTIFICATION_ID, Notify.createNotification(
                    applicationContext, getString(
                        R.string.notification_title
                    ), getString(R.string.notification_content)
                )
            )

            // search for Wear Os
            wearOsJob = CoroutineScope(Dispatchers.Main).launch {
                wearOsHandler.searchForWearOs(
                    this@MeasurementService,
                    this@MeasurementService,
                    WearOsConstants.WEAR_APP_CAPABILITY
                )
            }

        }

        handleAndroid()

        serviceController.onInit(this, intent)

        if(!serviceController.onStart(this)){
            cancelService()
        }

        when(paramType){
            SHORT -> createShortTimer()
            LONG -> createLongTimer()
            ENDLESS -> START_REDELIVER_INTENT
        }
        return START_NOT_STICKY
    }

    /**
     * checks wakeLock and registers receiver
     *
     */
    private fun handleAndroid(){
        wakeLockHandle()
        createBroadcastReceiver()
    }

    /**
     * picks necessary parameters from the intent
     *
     * @param intent - formated by companion methods below
     */
    private fun unwrapIntent(intent: Intent) {

        this.intent = intent
        intent.extras?.let {
            paramSensorId = it.getIntArray(ANDROID_SENSORS)
            paramType = it.getInt(TYPE, ENDLESS)
            paramTimeIntervals = it.getIntArray(TIME_INTERVALS)!!
            paramChecks = it.getBooleanArray(OTHER)!!
            paramInternalStorage = it.getBoolean(INTERNAL_STORAGE)
        }
    }

    /**
     * registers receiver for the intent to stop service / write annotation
     * also can registers intents for battery status and controls the battery %
     */
    private fun createBroadcastReceiver(){
        val intentFilter = IntentFilter(STOP_SERVICE)
        intentFilter.addAction(ANNOTATION)
//        intentFilter.addAction(STATUS)

        if(paramChecks[2]){
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW)
        }

        registerReceiver(serviceBroadcastReceiver, intentFilter)
        receiver = true
    }

    /**
     * uses own kotlin flow to countdown SHORT measurement with delay of 1000 ms
     *
     */
    private fun createShortTimer() {
        shortJob = CoroutineScope(Dispatchers.Default).launch {
            serviceController.shortTimer().onEach { state ->
                withContext(Dispatchers.Main) {

                    if (state is MeasurementStates.OnShortEnd) { // on the end of the countdown
                        onMeasurementStateListener?.onServiceState(
                            MeasurementStates.OnEndMeasurement(
                                paramType,
                                paramChecks[4] // repetition
                            )
                        )
                        onMeasurementStateListener?.onServiceState(MeasurementStates.StateNothing)
                        sendOnFinishNotification()
                        serviceController.onStop(this@MeasurementService)
                        cancelService()
                    }else{
                        // passing seconds left
                        onMeasurementStateListener?.onServiceState(state)
                    }

                }
            }.launchIn(this)
        }

    }

    private fun createLongTimer(){
        serviceController.longTimer(this)
    }

    /**
     * acquires wakelock with time limit for LONG/SHORT measurement
     *
     */
    @SuppressLint("WakelockTimeout")
    private fun wakeLockHandle(){
        if (paramChecks[3]) { // wakelock flag
            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorBox::Measuring").apply {
                    when (paramType) {
                        SHORT -> {
                            acquire(paramTimeIntervals[1] * 1000L)
                        }
                        LONG -> {
                            acquire(((paramTimeIntervals[0] * 3600 + paramTimeIntervals[1] * 60) * 1000).toLong())
                        }
                        else -> {
                            acquire()
                        }
                    }
                }
            }
        }
    }

    /**
     * finishing notification after stopping of the service
     *
     */
    private fun sendOnFinishNotification(){
        Notify.updateNotification(
            this, FINISH_NOTIFICATION,
            Notify.endingNotification(
                this,
                getString(R.string.notification_finish_title),
                getString(R.string.notification_finish_content)
            )
        )
    }

    /**
     * method stop measurement from the service
     *
     */
    private fun cancelService(){
        if(running){
            running = false

            onMeasurementStateListener?.onServiceState( // ending to activity
                MeasurementStates.OnEndMeasurement(
                    paramType,
                    paramChecks[4]
                )
            )

            onMeasurementStateListener?.onServiceState(MeasurementStates.StateNothing)

            shortJob?.cancel()

            serviceController.onStop(this) // stops measurement
            stopForeground(true)
            onMeasurementStateListener = null
        }
        wearOsJob?.cancel() // cancels Wear Os
        wearOsHandler.onDestroy()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)

        if(wakeLock != null){
            if(wakeLock!!.isHeld){
                wakeLock!!.release()
            }
        }

        wearOsPresence?.let {
            if(!paramInternalStorage && it.present){ // If there is internal storage - Wear Os usage
                removeWearOs()
            }
        }



        if(receiver){
            unregisterReceiver(serviceBroadcastReceiver)
            receiver = false
        }
        running = false
    }

    /**
     * callback for Wear Os presence
     *
     * @param wearOsStates - PresenceResult for Wear Os
     */
    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if(wearOsStates is WearOsStates.PresenceResult){
            wearOsPresence = wearOsStates
        }
    }

    /**
     * sends message to Wear Os to stop all the operations
     * laced mainly here, so the service can be standalone and the Wear Os measurement is stopped
     * by actual service in phone
     */
    private fun removeWearOs() {
        wearOsHandler.sendMsg(
            this,
            WearOsConstants.WEAR_MESSAGE_PATH,
            WearOsConstants.WEAR_KILL_APP,
            silent = true
        )
        wearOsHandler.onDestroy()
    }


    companion object {


        // notifications
        const val FINISH_NOTIFICATION = 465
        private const val ONGOING_NOTIFICATION_ID = 729

        // parameters
        const val FOLDER_NAME = "FOLDER_NAME"
        const val CUSTOM_NAME = "CUSTOM_NAME"
        const val INTERNAL_STORAGE = "INTERNAL_STORAGE"
        const val OTHER = "OTHER"
        internal const val ALARMS = "ALARMS"
        internal const val NOTES = "NOTES"
        const val TIME_INTERVALS = "TIME_INTERVALS"
        const val ANDROID_SENSORS = "ANDROID_SENSORS"
        const val ANDROID_SENSORS_SPEED = "ANDROID_SENSORS_SPEED"
        const val GPS = "GPS_MEASURE"
        const val TYPE = "TYPE"
        const val WEAR_SENSORS = "WEAR_SENSORS"

        // types
        const val SHORT = 0
        const val ENDLESS = 1
        const val LONG = 2

        const val SHORT_STRING = "SHORT"
        const val ENDLESS_STRING = "ENDLESS"
        const val LONG_STRING = "LONG"


        // intents
        const val USER = "USER"
        const val STOP_ACTIVITY = "STOP_ACTIVITY"
        const val STOP_SERVICE = "STOP_SERVICE"
        const val ANNOTATION = "ANNOTATION"
//        const val STATUS = "STATUS"
        const val RUNNING = "RUNNING"
        const val ANNOTATION_TIME = "ANNOTATION_TIME"
        const val ANNOTATION_TEXT = "ANNOTATION_TEXT"

        /**
         * used for basic measurement in HomeFragment
         *
         * @param intent - intent for MeasurementService
         * @param internalStorage - to use in Wear Os - true
         * @param sensorsToMeasure // default
         * @param gpsToMeasure - true to use GPS
         * @param nameOfFolder - string with formatted name ENDLESS_10_9_2020_20_50_30
         * @param wearSensors - intArray of sensor Ids for Wear Os - can be null
         * @return filled intent with extras based on string on top of the companion object
         */
        fun addExtraToIntentBasic(
            intent: Intent,
            internalStorage: Boolean,
            sensorsToMeasure: IntArray,
            gpsToMeasure: Boolean,
            nameOfFolder: String,
            wearSensors: IntArray?
        ): Intent {
            return intent.apply {
                putExtra(FOLDER_NAME, nameOfFolder)
                putExtra(INTERNAL_STORAGE, internalStorage)
                putExtra(ANDROID_SENSORS, sensorsToMeasure)
                putExtra(WEAR_SENSORS, wearSensors)
                putExtra(ANDROID_SENSORS_SPEED, SensorManager.SENSOR_DELAY_FASTEST) // default
                putExtra(GPS, gpsToMeasure)
                putExtra(TYPE, ENDLESS)
                putExtra(TIME_INTERVALS, intArrayOf(-1)) // not used
                putExtra(NOTES, arrayListOf("")) // not used
                putExtra(ALARMS, intArrayOf(-1)) // not used
                putExtra(OTHER, booleanArrayOf(false, false, false, false, false)) // default
            }

        }

        /**
         * used in Advanced Measurement to pack all the parameters
         *
         * @param intent - intent to MeasurementService
         * @param folder - whole folder name
         * @param customName - pure custom string with name for folder
         * @param internalStorage - to use in Wear Os, this should be true
         * @param sensorsToMeasure - intArray with sensors Ids to measure
         * @param speedSensor - SensorManager.SENSOR_DELAY_FAST, ...
         * @param gpsToMeasure - true to measure GPS
         * @param typeMeasurement - SHORT / ENDLESS / LONG
         * @param timeIntervals - SHORT [time to start in seconds, time to measure in seconds], LONG [hours, minutes]
         * @param notes - arrayList of strings to store
         * @param alarms - arrayList of ints with seconds in which to launch alarms
         * @param checkCheckBoxes - another parameters - [extra_checkbox_activity, significant_motion, battery, cpu, repeat]
         * @param wearSensors - intArray of ids for sensors in Wear Os
         * @return - filled intent
         */
        fun addExtraToIntentAdvanced(
            intent: Intent,
            folder: String,
            customName: String,
            internalStorage: Boolean,
            sensorsToMeasure: IntArray,
            speedSensor: Int,
            gpsToMeasure: Boolean,
            typeMeasurement: Int,
            timeIntervals: Array<Int>,
            notes: ArrayList<String>,
            alarms: ArrayList<Int>,
            checkCheckBoxes: Array<Boolean>,
            wearSensors: IntArray?
        ): Intent {

            return intent.apply {
                putExtra(FOLDER_NAME, folder)
                putExtra(CUSTOM_NAME, customName)
                putExtra(INTERNAL_STORAGE, internalStorage)
                putExtra(ANDROID_SENSORS, sensorsToMeasure)
                putExtra(ANDROID_SENSORS_SPEED, speedSensor)
                putExtra(GPS, gpsToMeasure)
                putExtra(TYPE, typeMeasurement)
                putExtra(TIME_INTERVALS, timeIntervals.toIntArray())
                putExtra(NOTES, notes)
                putExtra(ALARMS, alarms.toIntArray())
                putExtra(OTHER, checkCheckBoxes.toBooleanArray())
                wearSensors?.let {
                    putExtra(WEAR_SENSORS, wearSensors)
                }
            }
        }

        /**
         * Wear Os intent - customized for specific needs - creates ENDLESS measurement
         *
         * @param context
         * @param path - to store data
         * @param sensors - intArray of sensors to use
         * @param sensorSpeed - SensorManager.SENSOR_DELAY_FAST, ...
         * @param battery - stop on low battery - true
         * @param wakeLock - lock the cpu - true
         * @return filled Intent with  params
         */
        fun getIntentWearOs(
            context: Context,
            path: String,
            sensors: IntArray,
            sensorSpeed: Int,
            gps: Boolean,
            battery: Boolean,
            wakeLock: Boolean
        ): Intent{
            val intent = Intent(context, MeasurementService::class.java)

            return addExtraToIntentAdvanced(
                intent,
                path,
                "",
                true,
                sensors,
                sensorSpeed,
                gps,
                ENDLESS,
                arrayOf(-1),
                arrayListOf(""),
                arrayListOf(-1),
                arrayOf(
                    false,
                    false,
                    battery,
                    wakeLock,
                    false
                ),
                null
            )
        }

        /**
         * generates folder name with current timestamp
         *
         * @param type - SHORT, ENDLESS, LONG
         * @param customName - can be empty, if it is not, then the name replaces the type in name
         * @return - folder name
         */
        fun generateFolderName(type: Int, customName: String = ""): String{
            val date = StorageHandler.getDate(System.currentTimeMillis(), "dd_MM_yyyy_HH_mm_ss")
            return  if(customName != ""){
                "%s_%s".format(customName, date)
            }else{
                val measurement = when(type){
                    SHORT -> SHORT_STRING
                    LONG -> LONG_STRING
                    else -> ENDLESS_STRING
                }
                "%s_%s".format(measurement, date)
            }

        }

        // settings keys
        const val ACTIVITY_RECOGNITION_PERIOD = "activity_recognition_period"
        const val GPS_DISTANCE = "gps_distance"
        const val GPS_TIME = "gps_time"

        val PREFS = arrayOf(
            ACTIVITY_RECOGNITION_PERIOD,
            GPS_DISTANCE,
            GPS_TIME
        )

    }


}
