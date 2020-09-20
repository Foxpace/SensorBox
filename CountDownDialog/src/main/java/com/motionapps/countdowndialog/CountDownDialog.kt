package com.motionapps.countdowndialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window

/**
 * Dialog composes of textView, where the seconds left are shown
 * CountDown can be canceled by cancel button in the dialog
 * @param context
 */
class CountDownDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.countdown_timer)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
}
