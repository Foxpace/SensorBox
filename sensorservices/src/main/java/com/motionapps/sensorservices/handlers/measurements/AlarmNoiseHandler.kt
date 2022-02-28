package com.motionapps.sensorservices.handlers.measurements

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import com.motionapps.sensorservices.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AlarmNoiseHandler {

    var json: JSONArray = JSONArray()
    private lateinit var mediaPlayerStart: MediaPlayer
    private lateinit var values: ArrayList<Int>
    private var valid = false
    private var requestCodes = 500

    private var registered: Boolean = false
    private val alarmBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            mediaPlayerStart.start()
            addJSON()
        }
    }

    /**
     * Sorts the intArray, creates mediaplayer and inits json object
     *
     * @param context
     * @param array intArray of the seconds
     */
    fun initHandler(context: Context, array: IntArray?){
        if (array != null) {
            if (array[0] != -1) {
                values = array.toCollection(ArrayList())
                values.sort()
                values.reverse()
                valid = true
            } else {
                values = ArrayList()
            }
        } else {
            values = ArrayList()
        }

        json = JSONArray()
        mediaPlayerStart = MediaPlayer.create(context, R.raw.alert)
    }

    /**
     * Loops through the ArrayList with CountDown -
     * if the seconds are below the value, the alarm is triggered
     *
     * @param tick
     */
    suspend fun onShortTick(tick: Long) {
        if(valid){
            if(values.isNotEmpty()){
                if(values[0] * 1000L > tick){
                    withContext(Dispatchers.Main){
                        mediaPlayerStart.start()
                    }
                    values.remove(values[0])
                    addJSON()
                }
            }
        }
    }

    /**
     * Intents for LONG measurement and for the AlarmHandler
     *
     * @param context
     * @param code
     * @return
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getIntent(context: Context, code: Int = requestCodes++): PendingIntent{
        val intent = Intent(ON_LONG_ALARM)
        intent.flags = Intent.FLAG_RECEIVER_FOREGROUND

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }else{
            PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    /**
     * registers intents for the LONG measurement with use of AlarmManager
     *
     * @param context
     * @param alarmManager - alarmManager from Android Os - can be used for other purposes too
     */
    fun setLongAlarms(context: Context, alarmManager: AlarmManager) {
        if (valid) {
            registered = true
            context.registerReceiver(alarmBroadcastReceiver, IntentFilter(ON_LONG_ALARM))

            for (time in values) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + time * 1000L, getIntent(context)
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + time * 1000L, getIntent(context)
                    )
                }
            }
        }
    }

    /**
     * cancels all the alarms for the Long Measurement
     *
     * @param context
     * @param alarmManager - alarmManager from Android Os - can be used for other purposes too
     */
    private fun cancelLongAlarms(context: Context, alarmManager: AlarmManager) {
        if(valid){
            for(code in 500 until requestCodes){
                alarmManager.cancel(getIntent(context, code))
            }
        }
    }

    /**
     * add current time into JSON object
     *
     */
    private fun addJSON(){
        json.put(System.currentTimeMillis())
    }


    /**
     * cancels all the alarms, empties all the alarm values and releases mediaPlayer
     *
     * @param context
     * @param alarmManager- alarmManager from Android Os - can be used for other purposes too
     */
    fun onDestroy(context: Context, alarmManager: AlarmManager){
        cancelLongAlarms(context, alarmManager)
        if(registered){
            context.unregisterReceiver(alarmBroadcastReceiver)
        }
        mediaPlayerStart.release()
        values = ArrayList()
    }

    companion object{
        const val ON_LONG_ALARM = "ON_LONG_ALARM"
    }


}