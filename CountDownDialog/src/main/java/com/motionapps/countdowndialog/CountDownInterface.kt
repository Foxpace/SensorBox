package com.motionapps.countdowndialog

/**
 * interface for the other components to implement
 * refresh interval is 100 ms, but only whole seconds are passed
 */
interface CountDownInterface {
    fun onTick(seconds: String)
    fun onCountDownEnd()
}