package com.motionapps.sensorbox.communication

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService

/**
 * Sends one file to Phone by required path
 * Uses  JobIntentService required for Android 11
 * IntentService is deprecated
 */
class SendFileService: JobIntentService(), DataSync.StatusListener {

    override fun onHandleWork(intent: Intent) {
        val path = intent.getStringExtra(PATH_EXTRA)
        DataSync.sendFile(this, path, this)
    }

    /**
     * response after sending the file
     * meanwhile not handled, because it is one way sending
     *
     * @param status
     */
    override fun onStatusChange(status: Boolean) {
        if (!status) {
            //TODO what happens if the path is wrong / file is missing
            //     w.sendMessageInstant(this, PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, WEAR_SEND_PATHS+";"+paths);
        }
    }

    companion object{
        const val PATH_EXTRA = "PATH_EXTRA"

        /**
         * creates JobIntentService with intent that holds path to file
         *
         * @param context
         * @param intent
         */
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, SendFileService::class.java, 1, intent)
        }

    }
}