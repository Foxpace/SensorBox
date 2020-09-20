package com.motionapps.sensorbox.communication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.preference.PreferenceManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.motionapps.sensorbox.rlRecording.RealTimeSensorService
import com.motionapps.sensorbox.activities.MainSettings
import com.motionapps.sensorbox.adapters.SettingsAdapter
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementService.Companion.getIntentWearOs
import com.motionapps.sensorservices.services.MeasurementService.MeasurementBinder
import com.motionapps.wearoslib.WearOsConstants.DELETE_ALL_MEASUREMENTS
import com.motionapps.wearoslib.WearOsConstants.DELETE_FOLDER
import com.motionapps.wearoslib.WearOsConstants.END_WEAR_SENSOR_REAL_TIME
import com.motionapps.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.WearOsConstants.SEND_WEAR_SENSOR_INFO
import com.motionapps.wearoslib.WearOsConstants.START_MEASUREMENT
import com.motionapps.wearoslib.WearOsConstants.START_WEAR_SENSOR_REAL_TIME
import com.motionapps.wearoslib.WearOsConstants.WEAR_KILL_APP
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_FILE
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_PATHS
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS
import com.motionapps.wearoslib.WearOsHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.nio.charset.StandardCharsets

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * listens to messages from the phone
 * message has its main part and other info divided by ;
 *
 */
class MsgListener : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WEAR_MESSAGE_PATH) {
            val data = String(messageEvent.data, StandardCharsets.UTF_8).split(";".toRegex())
                .toTypedArray()
            val action = data[0] // main part
            val intent: Intent
            val w = WearOsHandler()

            when (action) {

                WEAR_STATUS -> sendStatus(this, w) // sends status about memory

                START_MEASUREMENT -> { // starts measurement with specific sensors
                    val sensorsString = data[2].split("|").toTypedArray()
                    val sensors = IntArray(sensorsString.size)
                    var counter = 0
                    for (s in sensorsString) {
                        sensors[counter++] = s.toInt()
                    }
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val sensorSpeed =
                        sharedPreferences.getInt(SettingsAdapter.SAMPLING_PREFERENCE, 0)
                    val batteryRestriction =
                        sharedPreferences.getBoolean(MainSettings.BATTERY_RESTRICTION, true)
                    intent =
                        getIntentWearOs(this, data[1], sensors, sensorSpeed, batteryRestriction)
                    startService(intent)
                }

                SEND_WEAR_SENSOR_INFO -> { // sends attributes of the sensors
                    val sensorInfo = SensorTools.getSensorInfo(this)
                    w.sendMessageInstant(this, PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, sensorInfo)
                }

                START_WEAR_SENSOR_REAL_TIME -> { // starts service to send samples to phone
                    val startRealTimeService = Intent(this, RealTimeSensorService::class.java)
                    startRealTimeService.putExtra(RealTimeSensorService.SENSOR_ID, Integer.valueOf(data[1]))
                    startService(startRealTimeService)
                }

                // ends transmission of the sensor events
                END_WEAR_SENSOR_REAL_TIME -> sendBroadcast(Intent(END_WEAR_SENSOR_REAL_TIME))

                // ends measurement / realtime transmission
                WEAR_KILL_APP -> {
                    sendBroadcast(Intent(MeasurementService.STOP_SERVICE))
                    sendBroadcast(Intent(END_WEAR_SENSOR_REAL_TIME))
                }

                // sends paths to the files in internal storage
                WEAR_SEND_PATHS -> {
                    val paths = DataSync.getPaths(this)
                    DataSync.nullAsset(this)
                    w.sendMessageInstant(
                        this,
                        PHONE_APP_CAPABILITY,
                        PHONE_MESSAGE_PATH,
                        "$WEAR_SEND_PATHS;$paths"
                    )
                }
                // file is sent by requested path from phone
                WEAR_SEND_FILE -> {
                    val pathIntent = Intent(this, SendFileService::class.java)
                    pathIntent.putExtra(SendFileService.PATH_EXTRA, data[1])
                    SendFileService.enqueueWork(this, pathIntent)
                }
                // deletes all the folders in internal storage
                DELETE_ALL_MEASUREMENTS -> {
                    DataSync.nullAsset(this)
                    DataSync.deleteAllFolders(this)
                    sendStatus(this, w)
                }
                // deletes specific folder - mainly from short measurements
                DELETE_FOLDER -> DataSync.deleteFolder(this, data[1])
            }
        }
    }

    companion object {

        /**
         * status about internal storage and if the service is running
         *
         * @param context
         * @param wearOsHandler - to send message
         */
        fun sendStatus(context: Context, wearOsHandler: WearOsHandler) {
            val s = DataSync.dataAvailable(context) // internalStorage status
            context.bindService(
                Intent(context, MeasurementService::class.java),
                object : ServiceConnection {
                    override fun onServiceConnected(
                        componentName: ComponentName,
                        iBinder: IBinder
                    ) {
                        // if the service is running
                        val localBinder = iBinder as MeasurementBinder
                        val msg = if (localBinder.getService().running) {
                            "$WEAR_STATUS;1|$s"
                        } else {
                            "$WEAR_STATUS;0|$s"
                        }
                        wearOsHandler.sendMessageInstant(
                            context,
                            PHONE_APP_CAPABILITY,
                            PHONE_MESSAGE_PATH,
                            msg
                        )
                        context.unbindService(this)
                    }

                    override fun onServiceDisconnected(componentName: ComponentName) {}
                },
                BIND_AUTO_CREATE
            )
        }

        /**
         * can be used from service alone - for example if the status changes
         *
         * @param context
         * @param wearOsHandler
         * @param b - if the service runs
         */

        fun sendStatusStartingService(context: Context, wearOsHandler: WearOsHandler, b: Boolean){

            val s = DataSync.dataAvailable(context) // internalStorage status
            val running =  if(b){
                "1"
            }else{
                "0"
            }

            val msg = "$WEAR_STATUS;$running|$s"
            wearOsHandler.sendMessageInstant(
                context,
                PHONE_APP_CAPABILITY,
                PHONE_MESSAGE_PATH,
                msg
            )

        }
    }
}