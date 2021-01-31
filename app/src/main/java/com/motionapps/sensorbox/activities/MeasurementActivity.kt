package com.motionapps.sensorbox.activities

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.motionapps.countdowndialog.CountDownStates
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.settings.AnnotationFragment
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.StorageHandler.getDate
import com.motionapps.sensorbox.uiHandlers.GraphHandler
import com.motionapps.sensorbox.viewmodels.MeasurementViewModel
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementService.Companion.ANDROID_SENSORS
import com.motionapps.sensorservices.services.MeasurementService.Companion.ANNOTATION
import com.motionapps.sensorservices.services.MeasurementService.Companion.CUSTOM_NAME
import com.motionapps.sensorservices.services.MeasurementService.Companion.ENDLESS
import com.motionapps.sensorservices.services.MeasurementService.Companion.FINISH_NOTIFICATION
import com.motionapps.sensorservices.services.MeasurementService.Companion.FOLDER_NAME
import com.motionapps.sensorservices.services.MeasurementService.Companion.LONG
import com.motionapps.sensorservices.services.MeasurementService.Companion.OTHER
import com.motionapps.sensorservices.services.MeasurementService.Companion.SHORT
import com.motionapps.sensorservices.services.MeasurementService.Companion.TIME_INTERVALS
import com.motionapps.sensorservices.services.MeasurementService.Companion.TYPE
import com.motionapps.sensorservices.services.MeasurementService.Companion.WEAR_SENSORS
import com.motionapps.sensorservices.services.Notify
import com.motionapps.sensorservices.services.MeasurementStates
import com.motionapps.sensorservices.types.SensorNeeds
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


/**
 * Activity is presented, when the measurement is active
 * The Activity has its own viewmodel class, which binds to SensorService
 */

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
class MeasurementActivity : AppCompatActivity() {

    private var measurementBinderB: Boolean = false // flag for binding to service
    private val viewModel: MeasurementViewModel by viewModels()

    private var dialogMaterial: MaterialDialog? = null // custom dialog library
    private var dialog: Dialog? = null  // default dialog

    // data shared with SensorService
    private var folderName = ""
    private var customName = ""
    private var type: Int = ENDLESS
    private var startInterval: Int = -1
    private var annotationTime: Long = -1L
    private var repeating: Boolean = false

    // receiver for broadcast of SensorService / notification to shut down activity
    // can be used with Wear Os messages
    private var receiverRegistered: Boolean = false
    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == MeasurementService.STOP_ACTIVITY) {
                finish()
                startActivity(Intent(this@MeasurementActivity, MainActivity::class.java))
            }else if(intent.action == WEAR_STATUS){
                //TODO handle Wear status - not important in MeasurementActivity
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpView() // changes view based on the type of the measurement
        setUpObservers() // observers for livedata
        setUpGraph() // sensor data projection to chart

        // binding to service and registration of the receiver
        measurementBinderB = true
        Intent(this, MeasurementService::class.java).also { intent ->
            bindService(intent, viewModel.connectionMeasurement, Context.BIND_AUTO_CREATE)
        }

        registerReceiver(mBroadcastReceiver, IntentFilter(MeasurementService.STOP_ACTIVITY))
        receiverRegistered = true
    }

    /**
     * Picks from 2 different layouts for SHORT and other types of the measurement
     * Handles onClickEvents
     */
    private fun setUpView(){

        intent.extras?.let {
            type = it.getInt(TYPE)
            repeating = it.getBooleanArray(OTHER)?.get(4) ?: false
            startInterval = it.getIntArray(TIME_INTERVALS)?.get(0) ?: -1
            folderName = it.getString(FOLDER_NAME, "")
            customName = it.getString(CUSTOM_NAME, "")
        }

        // 2 layouts
        when(type){
            SHORT -> {
                setContentView(R.layout.activity_measurement_textview)
            }
            ENDLESS, LONG -> {
                setContentView(R.layout.activity_measurement_chronometer)
            }
        }

        // ending button
        (findViewById<Button>(R.id.measurement_stop_button)).setOnClickListener{
            if(repeating){
                sendBroadcast(Intent(MeasurementService.STOP_SERVICE))
                showDialogRepeating()
            }else{
                endMeasurement()
            }

        }

        // annotation button
        (findViewById<Button>(R.id.measurement_annotation_button)).setOnClickListener{
            showDialogAnnotations()
        }

        // if the dialog was created - show it on screen rotation / restarting activity
        if(viewModel.showAnnotation && dialogMaterial == null){
            showDialogAnnotations() // annotation dialog
        }

        if(viewModel.showRepeating && dialogMaterial == null){
            showDialogRepeating() // repeating dialog
        }
    }

    /**
     * observers for countdown, binder, measurementState - countdown, end of the measurement, ...
     */
    private fun setUpObservers() {
        // dialog is shown, if it is running - interval is irrelevant -> -1
        dialog = viewModel.startCountdown(this, -1)
        dialog?.show()
        dialog?.findViewById<Button>(R.id.dialog_button_cancel)?.setOnClickListener{
            viewModel.cancelCountDown()
        }

        // state of the countdown - different library - not the same as SensorServices lib
        viewModel.countDown.observe(this, { countDownState ->
            when (countDownState) {

                is CountDownStates.OnTick -> {
                    dialog?.findViewById<TextView>(R.id.dialog_text_countdown)?.text = countDownState.tick
                }

                is CountDownStates.OnCancel -> {
                    dialog?.dismiss()
                    dialog = null
                    showDialogRepeating()
                }

                is CountDownStates.OnFinish -> {
                    dialog?.dismiss()
                    dialog = null
                    onCountDownEnd()
                }
                else -> {}
            }
        })

        // on binder -> set up time / end the activity, if the measurement is not running
        viewModel.measurementBinder.observe(this, { binder ->
            binder?.let {b ->
                val service = b.getService()
                if(!service.running && !viewModel.showRepeating && !viewModel.isCounting){
                    endMeasurement()
                }
                setUpTime(b)

            }
        })

        // state of the measurement
        viewModel.measurementState.observe(this, { measurementState ->
            when(measurementState){
                is MeasurementStates.OnTick -> {
                    findViewById<TextView>(R.id.measurement_time)?.let {
                        it.text = "${measurementState.tick} s"
                    }
                }
                is MeasurementStates.OnEndMeasurement ->{
                    if(measurementState.repeat){
                        showDialogRepeating()
                    }else{
                        endMeasurement()
                    }
                }
                else -> {}
            }
        })
    }

    /**
     * Picks first sensor in chosen sensors by user and shows it to user in chart
     */
    private fun setUpGraph(){
        var sensorNeeds: SensorNeeds = SensorNeeds.ACG

        intent.extras?.let {
            it.getIntArray(ANDROID_SENSORS)?.let { array ->
                if (array.isNotEmpty()) {
                    sensorNeeds = SensorNeeds.getSensorByIdForChart(array[0])
                }
            }
        }

        // inits linedata and chart based on the requirements
        viewModel.lineData = GraphHandler.initChart(findViewById(R.id.measurement_graph), "", sensorNeeds, GraphHandler.INFO_VIEW, viewModel.lineData)
        viewModel.startSensing(this, sensorNeeds)

    }

    /**
     * Sets up start time for chronometer by MeasurementService
     * @param measurementBinder
     */
    private fun setUpTime(measurementBinder: MeasurementService.MeasurementBinder) {

        when(type){
            ENDLESS -> {
                val time = measurementBinder.getService().startTime
                (findViewById<Chronometer>(R.id.measurement_time)).also {
                    it.base = time
                    it.start()
                }
            }
            LONG -> {
                val time = measurementBinder.getService().startTime
                (findViewById<Chronometer>(R.id.measurement_time)).also {
                    it.base = time
                    it.start()
                }
                val timeIntervals = measurementBinder.getService().paramTimeIntervals
                val stopTime = System.currentTimeMillis() + (timeIntervals[0]*3600 + timeIntervals[1]*60) * 1000

                (findViewById<TextView>(R.id.measurement_time_title)).also{
                    it.text = getString(R.string.measurement_format_long).format(getDate(stopTime))
                }
            }
        }
    }


    /**
     * handles custom material-dialog by: https://github.com/afollestad/material-dialogs
     * shows if the type is SHORT and repeating option is checked
     */
    private fun showDialogRepeating() {
        if (dialogMaterial == null) {
            viewModel.showRepeating = true
            dialogMaterial = MaterialDialog(this)
            dialogMaterial?.show {
                // cosmetics
                cornerRadius(16f)
                cancelable(false)
                cancelOnTouchOutside(false)
                title(R.string.measurement_title_repeat)

                // to delete recent folder
                checkBoxPrompt(R.string.measurement_delete_previous) { checked ->
                    viewModel.deleteMeasurement = checked
                }

                // repeat measurement
                positiveButton(R.string.measurement_repeat) {

                    // delete recent folder
                    if (viewModel.deleteMeasurement && folderName != "") {
                        StorageHandler.deleteByNameOfFolder(this@MeasurementActivity, folderName)
                        viewModel.deleteWearOsFolder(this@MeasurementActivity, folderName)
                    }

                    viewModel.deleteMeasurement = false
                    viewModel.showRepeating = false

                    it.dismiss()
                    dialogMaterial = null

                    // start countdown
                    dialog = viewModel.startCountdown(this@MeasurementActivity, startInterval)
                    dialog?.show()
                    dialog?.findViewById<Button>(R.id.dialog_button_cancel)?.setOnClickListener {
                        viewModel.cancelCountDown()
                        dialog?.dismiss()
                    }
                }

                // stops measurement process
                negativeButton(R.string.measurement_stop) {
                    it.dismiss()
                    dialogMaterial = null
                    viewModel.showRepeating = false
                    endMeasurement()
                }

                onDismiss {
                    viewModel.deleteMeasurement = false
                }
            }
        }
    }

    /**
     * Annotation dialog to add notes during measurement
     */
    private fun showDialogAnnotations(){
        // gets info from sharedPreferences - customized in settings
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@MeasurementActivity)
        val annotations = preferences.getStringSet(AnnotationFragment.ANNOTS, null)

        if(annotations != null){
            viewModel.showAnnotation = true
            annotationTime = System.currentTimeMillis()

            dialogMaterial = MaterialDialog(this)
            dialogMaterial?.show {
                cornerRadius(16f)
                title(R.string.measurement_annot_title)

                listItemsSingleChoice(items = annotations.toList()) { dialog, _, text ->

                    val intentAnnotation = Intent(ANNOTATION)
                    intentAnnotation.putExtra(MeasurementService.ANNOTATION_TIME, annotationTime)
                    intentAnnotation.putExtra(MeasurementService.ANNOTATION_TEXT, text)
                    sendBroadcast(intentAnnotation)

                    dialog.dismiss()
                    viewModel.showAnnotation = false
                }

                onDismiss {
                    viewModel.showAnnotation = false
                }
            }
        }else{
            Toast.makeText(this@MeasurementActivity, getString(R.string.measurement_annot_empty), Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        when(type){
            ENDLESS, LONG -> {
                (findViewById<Chronometer>(R.id.measurement_time)).also { it.start() }
            }
        }
        // starts chart
        viewModel.resumeSensing(this@MeasurementActivity)
    }

    override fun onStop() {
        super.onStop()
        when(type){
            ENDLESS, LONG -> {
                (findViewById<Chronometer>(R.id.measurement_time)).also {
                    it.stop()
                }
            }
        }
        // stops chart
        viewModel.pauseSensing()
    }

    override fun onDestroy() {
        super.onDestroy()

        if(isFinishing){
            viewModel.deleteMeasurement = false
        }

        viewModel.destroySensing()

        if(measurementBinderB){
            unbindService(viewModel.connectionMeasurement)
            measurementBinderB = false
        }


        if(receiverRegistered){
            unregisterReceiver(mBroadcastReceiver)
        }

        dialogMaterial?.let {
            if(it.isShowing){
                it.dismiss()
            }
        }
        dialogMaterial = null
    }

    /**
     * Starts new measurement
     */
    private fun onCountDownEnd() {
        // canceling UI stuff
        viewModel.showAnnotation = false
        viewModel.showRepeating = false
        dialogMaterial?.dismiss()
        Notify.cancelNotification(this, FINISH_NOTIFICATION)

        val serviceIntent = Intent(this, MeasurementService::class.java)
        val folderPath = MeasurementService.generateFolderName(type, customName)

        // passing new folder
        intent.putExtra(FOLDER_NAME, folderPath)
        serviceIntent.putExtras(intent)

        // sending info also to Wear, it it is present
        viewModel.startWearOsMeasurement(this, folderPath, intent.getIntArrayExtra(WEAR_SENSORS))
        ContextCompat.startForegroundService(this, serviceIntent)

        // recreated later - It takes time to start service
        Handler(Looper.getMainLooper()).postDelayed({
            recreate()
        }, 750)
    }

    /**
     * Ends everything and start MainActivity
     */
    private fun endMeasurement(){

        if(viewModel.isCounting){
            viewModel.cancelCountDown()
        }

        val intent = Intent(MeasurementService.STOP_SERVICE)
        intent.putExtra(MeasurementService.USER, true)
        sendBroadcast(intent)
        startActivity(Intent(this@MeasurementActivity, MainActivity::class.java))
        finish()
    }
}