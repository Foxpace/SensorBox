package com.motionapps.wearoslib

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.motionapps.wearoslib.WearOsConstants.NUMBER_OF_FILES

/**
 * Dialog to show basic info about data stored at Wear Os
 * Number of measurements, size of them
 *
 * @param context
 */
class WearOsSyncDialog(context: Context): Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.wearos_sync_dialog)
    }


    override fun onStart() {
        super.onStart()
        // to resize dialog to match parent in width
        window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    /**
     *  After click on button, the SyncService will be started to transfer all the data
     *
     * @param status - data about storage
     */
    fun showStatus(status: WearOsStates.Status){
        show()
        findViewById<TextView>(R.id.wear_os_sync_measurement_count)?.apply {
            text = status.measurementsToSync.toString()
        }

        findViewById<TextView>(R.id.wear_os_sync_size_mb)?.apply{
            text = "%.3f MB".format(status.sizeOfData)
        }

        findViewById<Button>(R.id.wear_os_sync_button).setOnClickListener{
            val intent = Intent(it.context, WearOsSyncService::class.java)
            intent.putExtra(NUMBER_OF_FILES, status.totalNumberOfFiles)
            ContextCompat.startForegroundService(it.context, intent)
            dismiss()
        }
    }

}