package com.motionapps.sensorservices.handlers.measurements

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.tasks.OnSuccessListener
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.FOLDER_NAME
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.INTERNAL_STORAGE
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.io.OutputStream

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * Uses Google movement recognition api to write changes in movement
 *
 */
class ActivityRecognition : MeasurementInterface {

    private var registered = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, p1: Intent?) {
            p1?.let { intent: Intent ->
                when (intent.action) {
                    // called on change of state
                    MOVING_STATE_TRANSITION -> { // from activity API
                        if (ActivityTransitionResult.hasResult(intent)) {
                            ActivityTransitionResult.extractResult(intent)?.let {
                                // parsing info to csv
                                for(transition in it.transitionEvents){
                                    outputStreamTransitions?.write("${transition.elapsedRealTimeNanos};${transition.activityType};${transition.transitionType}\n".toByteArray())
                                }
                            }
                        }
                    }
                    // updates during the specific amount of time
                    MOVING_STATE_UPDATES -> { // from activity API
                        if (ActivityRecognitionResult.hasResult(intent)) {
                            ActivityRecognitionResult.extractResult(intent)?.let {
                                // parsing info to csv
                                var row: String = it.elapsedRealtimeMillis.toString() + ";"
                                for (activity in ALL_ACTIVITIES) {
                                    row += it.getActivityConfidence(activity).toString()
                                    if (DetectedActivity.TILTING != activity) {
                                        row += ";"
                                    }
                                }
                                row += "\n"
                                outputStreamUpdates?.write(row.toByteArray())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun registerActivityRecognitionReceiver(context: Context){
        val intentFilter = IntentFilter()
        registerIntents(intentFilter)
        context.registerReceiver(broadcastReceiver, intentFilter)
        registered = true
    }

    private var running: Boolean = false
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    // intents used by Client
    private var pendingIntentTransition: PendingIntent? = null
    private var pendingIntentUpdates: PendingIntent? = null

    // outputStreams to csv files
    private var outputStreamTransitions: OutputStream? = null
    private var outputStreamUpdates: OutputStream? = null

    // headers
    private val transitionHeader: String = "t;activity;enter_exit\n"
    private val updateHeader: String = "t;still;on_foot;walking;running;vehicle;bike;unknown;tilting\n"

    /**
     * creates pending intent, which are passed to model, when they are triggered
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntentUpdates(context: Context): PendingIntent {
        val intent = Intent(MOVING_STATE_UPDATES)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_UPDATES,
                intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_UPDATES,
                intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getPendingIntentTransition(context: Context): PendingIntent {
        val intentTransitions = Intent(MOVING_STATE_TRANSITION)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_TRANSITION,
                intentTransitions, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context, REQUEST_CODE_TRANSITION,
                intentTransitions, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    /**
     * Initialization of the Recognition
     *
     * @param context
     * @param params
     */
    override fun initMeasurement(context: Context, params: Bundle) {
        registerActivityRecognitionReceiver(context)

        if(params.getBoolean(INTERNAL_STORAGE)) { // internal storage for Wear Os
            outputStreamTransitions = StorageHandler.createFileInInternalFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "activity_transitions.csv"
            )
            outputStreamUpdates = StorageHandler.createFileInInternalFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "activity_updates.csv"
            )
        }else{ // external storage for phone
            outputStreamTransitions = StorageHandler.createFileInFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "csv", "activity_transitions.csv"
            )
            outputStreamUpdates = StorageHandler.createFileInFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "csv", "activity_updates.csv"
            )
        }
        // add headers
        outputStreamTransitions?.write(transitionHeader.toByteArray())
        outputStreamUpdates?.write(updateHeader.toByteArray())

    }

    /**
     * registration of the activity recognition
     *
     * @param context
     */
    override fun startMeasurement(context: Context) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED){
            return
        }

        activityRecognitionClient = ActivityRecognition.getClient(context)

        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val timeToUpdate = sharedPreferences.getString(MeasurementService.ACTIVITY_RECOGNITION_PERIOD, "30")!!.toLong()

        registerUpdates(context, timeToUpdate*1000L)
        registerTransitions(context, MOVING_PERSON_ACTIVITIES)
        running = true
    }

    /**
     * pauses the recognition client
     *
     * @param context
     */

    override fun pauseMeasurement(context: Context) {
        onDestroy()
        if(registered){
            context.unregisterReceiver(broadcastReceiver)
            registered = false
        }
    }

    /**
     * removes all updates from Activity recognition client
     */
    private fun onDestroy() {
        if(running) {
            pendingIntentTransition?.let{
                activityRecognitionClient.removeActivityTransitionUpdates(it)
                pendingIntentTransition = null
            }

            pendingIntentUpdates?.let{
                activityRecognitionClient.removeActivityUpdates(it)
                pendingIntentUpdates = null
            }
            running = false
        }
        Log.i(TAG, "unregistering")
    }

    /**
     * closes all the outputStreams
     *
     * @param context
     */
    override fun saveMeasurement(context: Context) {
        outputStreamTransitions?.flush()
        outputStreamTransitions?.close()
        outputStreamUpdates?.flush()
        outputStreamUpdates?.close()

    }

    /**
     * csv files are saved and client is unregistered
     *
     * @param context
     */
    override fun onDestroyMeasurement(context: Context) {
        pauseMeasurement(context)
        saveMeasurement(context)
    }

    /**
     * @param context
     * @param activities - wanted activities in int arraylist
     * overloaded method, without custom callbacks
     */

    private fun registerTransitions(context: Context, activities: IntArray) {

        pendingIntentTransition = getPendingIntentTransition(context)
        pendingIntentTransition?.let {
            activityRecognitionClient.requestActivityTransitionUpdates(
                ActivityTransitionRequest(
                    getTransitions(activities)),
                it
            ).addOnSuccessListener {
                OnSuccessListener<Void> {
                    Log.i(TAG, "successfully registered activity transitions")
                }
            }.addOnFailureListener {
                pendingIntentTransition = null
                Log.w(TAG, "Registration of activity transitions failed")
            }
        }

    }

    private fun getTransitions(activities: IntArray): ArrayList<ActivityTransition>{
        val transitions: ArrayList<ActivityTransition> = ArrayList()
        for (activity in activities){

            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        return transitions
    }

    /**
     * @param context
     * @param timeToUpdate - time, in which intent will be delivered - it is not guaranteed by system
     *
     * intent with probabilities of every category of movement is sent
     */


    private fun registerUpdates(context: Context, timeToUpdate: Long) {

        pendingIntentUpdates = getPendingIntentUpdates(context)
        pendingIntentUpdates?.let{ pendingIntent: PendingIntent ->
            activityRecognitionClient.requestActivityUpdates(timeToUpdate, pendingIntent).
            addOnSuccessListener {
                OnSuccessListener<Void> {
                    Log.i(TAG, "successfully registered activity updates")
                }
            }.addOnFailureListener { exception: Exception ->
                pendingIntentUpdates = null
                Log.w(TAG, "Registration of activity updates failed: $exception")
            }
        }
    }



    companion object{


        private const val MOVING_STATE_TRANSITION: String = "com.motionapps.MOVING_STATE_TRANSITION"
        private const val MOVING_STATE_UPDATES: String = "com.motionapps.MOVING_STATE_UPDATES"
        private val INTENTS: Array<String> = arrayOf(MOVING_STATE_TRANSITION, MOVING_STATE_UPDATES)

        fun registerIntents(intentFilter: IntentFilter){
            for(s in INTENTS){
                intentFilter.addAction(s)
            }
        }

        //  more options
        val MOVING_PERSON_ACTIVITIES: IntArray = intArrayOf(
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE
        )

        val ALL_ACTIVITIES: IntArray = intArrayOf(
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.UNKNOWN,
            DetectedActivity.TILTING

        )

//        fun convertTransitionToString(transition: Int): String {
//            return when (transition) {
//                DetectedActivity.IN_VEHICLE -> "Vehicle"
//                DetectedActivity.ON_BICYCLE -> "Bike"
//                DetectedActivity.ON_FOOT -> "Foot"
//                DetectedActivity.STILL -> "Still"
//                DetectedActivity.WALKING -> "Walking"
//                DetectedActivity.RUNNING -> "Run"
//                DetectedActivity.UNKNOWN -> "Unknown"
//                DetectedActivity.TILTING -> "Tilting"
//                else -> "Not classified"
//            }
//        }

        private const val TAG: String = "ActivityManager"
        private const val REQUEST_CODE_TRANSITION = 1654
        private const val REQUEST_CODE_UPDATES = 457
    }
}