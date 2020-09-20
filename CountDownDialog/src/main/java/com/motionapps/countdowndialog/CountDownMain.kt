package com.motionapps.countdowndialog

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer

/**
 * Uses native CountDownTimer of the Android
 * Refresh rate is 100 ms, but whole seconds are formatted as String
 */
class CountDownMain {


    private var countDownInterface: CountDownInterface? = null

    // tones can be added
    var mediaPlayerBeep: MediaPlayer? = null
    var mediaPlayerStart: MediaPlayer? = null

    private var timer: Timer? = null
    private var playedFirst = false
    private var playedSecond = false
    var running: Boolean = false
    var playing: Boolean = false

    /**
     * Start of CountDown - dialog is not returned, as this component can be used for internal
     * functionality too
     * To use dialog, create one by method and update it through interface updates
     *
     * @param countDownInterface
     * @param interval - in seconds
     */
    fun startCountDown(countDownInterface: CountDownInterface, interval: Int){
        running = true
        this.countDownInterface = countDownInterface
        timer = Timer(interval.toLong(), 100L)
        timer?.start()
    }


    private inner class Timer constructor(interval: Long, tick: Long): CountDownTimer(interval * 1000L, tick) {


        override fun onFinish() {

            running = false

            mediaPlayerStart?.start()
            playing = true

            countDownInterface?.onCountDownEnd()
            mediaPlayerStart?.setOnCompletionListener {
                playing = false
                onDestroy()
            }
        }

        override fun onTick(millis: Long) {
            countDownInterface?.onTick("%d s".format((millis / 1000).toInt()))
            when {
                millis <= 2000 && !playedFirst -> { // peep soudn to create on 2 and 1 second
                    mediaPlayerBeep?.start()
                    playedFirst = true
                }
                millis <= 1000 && !playedSecond -> {
                    mediaPlayerBeep?.start()
                    playedSecond = true
                }
            }
        }
    }

    fun cancel(){
        timer?.cancel()
        playedFirst = false
        playedSecond = false
        running = false
    }


    fun onDestroy(){
        if(!playing){
            cancel()
        }
    }

    fun getDialog(context: Context): Dialog{
        return CountDownDialog(context)
    }

}