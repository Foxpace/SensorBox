package com.motionapps.sensorbox.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.motionapps.sensorbox.R
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsHandler
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * opens app on the phone side
 *
 */
class MoveToMain: AppCompatActivity(), WearOsListener {
    private var imageButton: ImageButton? = null
    private val wearOsHandler = WearOsHandler()
    private var present: Boolean = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.move_to_main)

        imageButton = findViewById(R.id.imageButton)
        imageButton?.setOnClickListener {
            imageButton?.isClickable = false
            // if phone device is available - sends message to open MainActivity
            if (!present) {
                Toast.makeText(this, R.string.norespond, Toast.LENGTH_LONG).show()
            }else{
                wearOsHandler.sendMsg(this, WearOsConstants.PHONE_MESSAGE_PATH, WearOsConstants.START_MAIN_ACTIVITY, true)
                Toast.makeText(this, R.string.open_module, Toast.LENGTH_LONG).show()
            }
            imageButton?.isClickable = true
        }

        // searches for the phone
        job = CoroutineScope(Dispatchers.Main).launch {
            wearOsHandler.searchForWearOs(this@MoveToMain, this@MoveToMain, WearOsConstants.PHONE_APP_CAPABILITY)
        }
    }

    /**
     * awaits results for phone
     *
     * @param wearOsStates
     */
    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if(wearOsStates is WearOsStates.PresenceResult){
            present = wearOsStates.present
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        wearOsHandler.onDestroy()
    }
}