package com.motionapps.sensorbox

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.motionapps.sensorbox.activities.MainActivity
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_SENSOR_INFO
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_SENSOR_INFO_EXTRA
import com.motionapps.wearoslib.WearOsConstants.START_MAIN_ACTIVITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED
import com.motionapps.wearoslib.WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED_BOOLEAN
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_PATHS
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_PATHS_EXTRA
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS_EXTRA
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.nio.charset.StandardCharsets

@InternalCoroutinesApi
@ExperimentalCoroutinesApi

/**
 * Receives messages from the Wear Os
 * Messages contains 2 part
 * 1. part - main info, which was sent
 * 2. other additional information
 * these parts are divided by ;, otherwise by | in second part
 * Messages are then sent as broadcasts to activities and services
 *
 * Suggestion - change this 2 part system to paths of the WearOs framework, meanwhile only one path is used
 */
class MsgListener : WearableListenerService() {


    override fun onMessageReceived(messageEvent: MessageEvent) {

        if (messageEvent.path == PHONE_MESSAGE_PATH) {

            val data = String(messageEvent.data, StandardCharsets.UTF_8).split(";".toRegex())

            when (data[0]) {
                WEAR_STATUS ->{  // status of the Wear os - memory status
                    Intent(WEAR_STATUS).also {
                        it.putExtra(WEAR_STATUS_EXTRA, data[1])
                        sendBroadcast(it)
                    }
                }

                WEAR_SEND_SENSOR_INFO ->{ // sensor info to parse for HomeFragment
                    Intent(WEAR_SEND_SENSOR_INFO).also {
                        it.putExtra(WEAR_SEND_SENSOR_INFO_EXTRA, data[1])
                        sendBroadcast(it)
                    }
                }

                START_MAIN_ACTIVITY -> { // shows the app
                    Intent(this, MainActivity::class.java).also{
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_MULTIPLE_TASK
                        startActivity(it)
                    }
                }

                WEAR_SEND_PATHS -> { // paths to synchronize between Phone and Wear Os
                    Intent(WEAR_SEND_PATHS).also{
                        it.putExtra(WEAR_SEND_PATHS_EXTRA, data[1])
                        sendBroadcast(it)
                    }
                }

                WEAR_HEART_RATE_PERMISSION_REQUIRED ->{
                    Intent(WEAR_HEART_RATE_PERMISSION_REQUIRED).also{
                        it.putExtra(WEAR_HEART_RATE_PERMISSION_REQUIRED_BOOLEAN, data[1] == "1")
                        sendBroadcast(it)
                    }
                }
            }
        }
    }
}