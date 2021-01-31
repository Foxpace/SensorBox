package com.motionapps.sensorbox.viewmodels

import android.app.Dialog
import android.content.Context
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.motionapps.countdowndialog.CountDownInterface
import com.motionapps.countdowndialog.CountDownStates
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.advanced.ExtraFragmentArgs
import com.motionapps.sensorbox.fragments.advanced.extrahandlers.AlarmHandler
import com.motionapps.sensorbox.uiHandlers.SensorViewHandler
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsConstants.START_MEASUREMENT
import com.motionapps.wearoslib.WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import com.motionapps.wearoslib.WearOsSyncDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.withContext


@InternalCoroutinesApi
@ExperimentalCoroutinesApi
/**
 * Interaction of MainActivity and its fragments with main objects to handle start of the measurement
 *
 * @property repository - consists -sensorViewHandler, countDownMain, noteHandler, alarmHandler, wearOsHandler
 * @property savedStateHandle
 */
class MainViewModel
@ViewModelInject constructor(
    private val repository: MainRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel(), CountDownInterface, WearOsListener {

    // index of speed to use
    var positionSensorSpeed: Int = 0
    val sensorSpeedArray: Int // sensor speed to pick
        get() {
        return arrayOf(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL
        )[positionSensorSpeed]
    }

    // States of the CountDown
    private var _countDown: MutableLiveData<CountDownStates> = MutableLiveData()
    val countDown: LiveData<CountDownStates>
        get() = _countDown


    // Wear Os presence
    private var _wearOsPresence: MutableLiveData<WearOsStates> = MutableLiveData()
    val wearOsPresence: LiveData<WearOsStates>
        get() = _wearOsPresence

    // Wear Os sensor info
    private var _wearOsContacted: MutableLiveData<HashMap<Int, List<String>>> = MutableLiveData()
    val wearOsContacted: LiveData<HashMap<Int, List<String>>>
        get() = _wearOsContacted

    // Wear Os status - wait for result, offline, sync info
    private var _wearOsStatus: MutableLiveData<WearOsStates> = MutableLiveData()
    val wearOsStatus: LiveData<WearOsStates>
        get() = _wearOsStatus

    var isHeartRatePermissionRequired = false

    /**
     * Start info with specific interval
     *
     * @param context
     * @param interval in seconds, use -1 to get dialog
     * @return Dialog if the request is valid
     */
    fun startCountdown(context: Context, interval: Int): Dialog?{
        if(repository.countDownMain.running && interval == -1){
            return repository.countDownMain.getDialog(context)
        }else if (interval == -1){
            return null
        }
        repository.countDownMain.startCountDown(this, interval)
        return repository.countDownMain.getDialog(context)
    }

    /**
     * Cancels countdown in main object and sends event
     *
     */
    fun cancelCountDown(){
        _countDown.value = CountDownStates.OnCancel
        repository.countDownMain.cancel()
    }

    fun getSensorView(): SensorViewHandler {
        return repository.sensorViewHandler
    }

    /**
     * passes event of the tick from countDown
     *
     * @param seconds
     */
    override fun onTick(seconds: String) {
        _countDown.value = CountDownStates.OnTick(seconds)
    }

    /**
     * passes finishing event on the end of the countDown
     *
     */
    override fun onCountDownEnd() {
        _countDown.value = CountDownStates.OnFinish
    }

    /**
     * Call this, if the note is added
     *
     * @param s - note value
     * @param linearLayout - linearLayout to which the view will be added
     * @param layoutInflater
     */
    fun onAddNote(s: String,
                  linearLayout: LinearLayout,
                  layoutInflater: LayoutInflater){

        repository.noteHandler.addNote(s, linearLayout, layoutInflater, true)

    }

    /**
     * Already added values of the notes and alarms will be added
     *
     * @param alarmLayout - linearLayouts to add views
     * @param notesLayout - linearLayouts to add views
     * @param layoutInflater
     * @param args - needs to distinguish Long and Short measurement
     */
    fun refreshNotesAndAlarms(alarmLayout: LinearLayout, notesLayout: LinearLayout, layoutInflater: LayoutInflater, args: ExtraFragmentArgs) {
        repository.noteHandler.refreshLayout(notesLayout, layoutInflater)
        repository.alarmHandler.refreshLayout(alarmLayout, layoutInflater, args)
    }

    /**
     * Removes all stored values for next measurement
     *
     */
    fun clearHandlers(){
        repository.noteHandler.notes.clear()
        repository.alarmHandler.alarmsList.clear()
        positionSensorSpeed = 0
    }

    fun getAlarmHandler(): AlarmHandler {
        return repository.alarmHandler
    }

    fun getNotes(): ArrayList<String> {
        return repository.noteHandler.notes
    }

    fun getAlarms(): ArrayList<Int> {
        return repository.alarmHandler.getAlarms()
    }

    fun onDestroy() {
        repository.wearOsHandler.onDestroy()
    }

    // ============================= Wear Os =============================

    /**
     * Searches for presence of the Wearable device
     *
     * @param context
     */
    suspend fun getWearPresenceAndStatus(context: Context){
        if(repository.wearOsHandler.isNode){
            onWearOsStates(WearOsStates.PresenceResult(true))

        }else{
            repository.wearOsHandler.searchForWearOs(context, this, WearOsConstants.WEAR_APP_CAPABILITY)
        }
    }

    /**
     * Updates live data on WearOsStatus change
     *
     * @param wearOsStates
     */
    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        withContext(Dispatchers.Main){
            _wearOsPresence.value = wearOsStates
            when(wearOsStates){
                is WearOsStates.PresenceResult -> {
                    if(!wearOsStates.present){
                        _wearOsContacted.value = hashMapOf()
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * passes click on Wear Os button
     * Gets Info about sensors / removes Wear Os
     * @param context
     */
    fun onWearPresentClick(context: Context) {
        if(_wearOsContacted.value.isNullOrEmpty()){
            _wearOsStatus.value = WearOsStates.AwaitResult
            repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, WearOsConstants.WEAR_SEND_SENSOR_INFO)
        }else{
            onRemoveWearOs(context)
        }
    }

    /**
     * Removes Wear Os - sends message to wear os to stop all operations
     * removes all the views
     * updates livedata to change UI
     * @param context
     */
    private fun onRemoveWearOs(context: Context){
        repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, WearOsConstants.WEAR_KILL_APP)
        repository.sensorViewHandler.removeActiveWearOsMeasurements()
        _wearOsContacted.value = hashMapOf()
        _wearOsStatus.value = WearOsStates.Offline
        isHeartRatePermissionRequired = false
    }

    /**
     * Shows dialog to proceed with Wear Os data synchronization
     *
     * @param context
     * @return Dialog to show
     */
    fun onWearSyncClick(context: Context): Dialog? {

        if(wearOsStatus.value is WearOsStates.Status && repository.wearOsHandler.nodeId != null){
            val wearOsDialog = WearOsSyncDialog(context)
            wearOsDialog.showStatus(wearOsStatus.value as WearOsStates.Status)
            return wearOsDialog
        }
        Toast.makeText(context, context.getString(R.string.wear_os_sync_unavailable_restart), Toast.LENGTH_LONG).show()
        return null
    }

    /**
     * If the Wear Os attributes of sensors come - they are parsed
     * -1 - data is not available
     * Line for the sensor:
     * sensor id | name | version | vendor | resolution | power | maximal range | minimal range | maximal range
     *
     * @param context
     * @param stringExtra - sensors are separated \n
     */
    fun onWearOsProperties(context: Context, stringExtra: String?) {
        if(stringExtra != null && stringExtra.isNotBlank()){

            val sensorMap = hashMapOf<Int, List<String>>()

            for(line in stringExtra.split("\n")){
                if(line.isNotBlank()){
                    val sensorValues = line.split("|")
                    sensorMap[sensorValues[0].toInt()] = sensorValues
                }
            }

            _wearOsContacted.value = sensorMap
            _wearOsStatus.value = WearOsStates.PresenceResult(true)
            repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, WEAR_STATUS)

        }else{
            // if the string is empty - data are removed
            _wearOsContacted.value = hashMapOf()
            _wearOsStatus.value = WearOsStates.PresenceResult(false)
        }
    }

    /**
     * Sends message to broadcast samples from Wear Os
     *
     * @param context
     * @param id - id of the sensor to broadcast
     */
    fun startWearOsSensor(context: Context, id: Int) {
        repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, "${WearOsConstants.WEAR_START_SENSOR_REAL_TIME};$id")
    }

    /**
     * Stops all the broadcasts
     *
     * @param context
     */
    fun stopWearOsSensor(context: Context) {
        repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, WearOsConstants.WEAR_END_SENSOR_REAL_TIME)
    }

    /**
     * Status of the memory in Wearable, which is send from the watch
     * running | measurementsToSync | sizeOfData in MB | totalNumberOfFiles
     * @param stringExtra
     */
    fun onWearOsStatus(stringExtra: String?){
        stringExtra?.let{
            val values = it.split("|")
            _wearOsStatus.value = WearOsStates.Status(values[0] == "1", values[1].toInt(), values[2].toDouble(), values[3].toInt())
        }
    }

    /**
     * Starts Service at Wearable
     * parts of the message are divided by ;
     * START_MEASUREMENT;path_to_save;integers|of|sensors
     * @param context
     * @param path - path
     * @param sensors - intArray, which is merged by |
     */
    fun startWearOsMeasurement(context: Context, path: String, sensors: IntArray) {
        repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, "$START_MEASUREMENT;$path;${sensors.joinToString("|")}")
    }

    fun askHeartRatePermission(context: Context){
        repository.wearOsHandler.sendMsg(context, WEAR_MESSAGE_PATH, WEAR_HEART_RATE_PERMISSION_REQUIRED)
    }

    /**
     * flag for requirement of the hear rate sensor
     * @param required - true if it is required - setter
     */
    fun onWearOsHearRatePermissionRequired(required: Boolean) {
        isHeartRatePermissionRequired = required
    }


}