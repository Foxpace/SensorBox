package com.motionapps.countdowndialog

/**
 * CountDown states, which can be used for own use
 *
 */
sealed class CountDownStates {

    class OnTick(val tick: String): CountDownStates()
    object OnCancel: CountDownStates()
    object OnNothing: CountDownStates()
    object OnFinish: CountDownStates()

}