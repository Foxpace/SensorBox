package com.motionapps.sensorbox.viewmodels

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.motionapps.countdowndialog.CountDownInterface
import com.motionapps.countdowndialog.CountDownStates
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementStates
import com.motionapps.sensorservices.types.SensorNeeds
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@ActivityRetainedScoped
/**
 * Communicates with MeasurementActivity and stores required values
 *
 * @property repository
 * @property savedStateHandle
 */
class MeasurementViewModel
@ViewModelInject constructor(
    private val repository: MeasurementRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel(), CountDownInterface, MeasurementService.OnMeasurementStateListener, WearOsListener {

    // data for the chart
    var lineData: ArrayList<LineGraphSeries<DataPoint>> = repository.graphUpdater.chartData
    private var wearOsJob: Job? = null

    // countdown events
    private var _countDown: MutableLiveData<CountDownStates> = MutableLiveData()
    val countDown: LiveData<CountDownStates>
        get() = _countDown

    // info from the MeasurementService
    private var _measurementState: MutableLiveData<MeasurementStates> = MutableLiveData()
    val measurementState: LiveData<MeasurementStates>
        get() = _measurementState

    // binder to the MeasurementService
    private var _measurementBinder: MutableLiveData<MeasurementService.MeasurementBinder?> = MutableLiveData()
    val measurementBinder: MutableLiveData<MeasurementService.MeasurementBinder?>
        get() = _measurementBinder

    // flags to show dialogs if required
    var showRepeating: Boolean = false
    var showAnnotation: Boolean = false
    var deleteMeasurement: Boolean = false

    // ongoing countdown
    val isCounting: Boolean
    get() = repository.countDownMain.running

    // connection to MeasurementService - listens to the changes in MeasurementService
    // MeasurementService is independent - activity is not required
    val connectionMeasurement: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            _measurementBinder.value = binder as MeasurementService.MeasurementBinder
            binder.getService().onMeasurementStateListener = this@MeasurementViewModel

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            _measurementBinder.value = null
        }
    }

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
     * Cancels countdown in main object and sends events
     *
     */
    fun cancelCountDown(){
        _countDown.value = CountDownStates.OnCancel
        _countDown.value = CountDownStates.OnNothing
        repository.countDownMain.cancel()
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
        _countDown.value = CountDownStates.OnNothing
    }


    override fun onServiceState(measurementStates: MeasurementStates) {
        _measurementState.value = measurementStates
    }

    /**
     * Start showing the chart data
     *
     * @param context
     * @param sensorNeeds - required sensor
     */
    fun startSensing(context: Context, sensorNeeds: SensorNeeds) {
        repository.graphUpdater.startSensing(context, sensorNeeds)
    }

    /**
     * proceeds with showing the chart
     * also starts to search for Wear Os
     * @param context
     */
    fun resumeSensing(context: Context){
        repository.graphUpdater.onResume()
        wearOsJob = CoroutineScope(Dispatchers.Main).launch {
            tryToGetWearOs(context)
        }

    }

    /**
     * Pauses charts
     * cancels search for the Wear Os
     */
    fun pauseSensing(){
        repository.graphUpdater.onPause()
        wearOsJob?.cancel()
        wearOsJob = null
    }

    fun destroySensing() {
        repository.graphUpdater.onDestroy()
        repository.wearOsHandler.onDestroy()
    }

    /**
     * Searches for the Wear Os
     *
     * @param context
     */
    private suspend fun tryToGetWearOs(context: Context){
        repository.wearOsHandler.searchForWearOs(context, this, WearOsConstants.WEAR_APP_CAPABILITY)
    }

    /**
     * Starts Service at Wearable
     * parts of the message are divided by ;
     * START_MEASUREMENT;path_to_save;integers|of|sensors
     * @param context
     * @param path - path
     * @param sensors - intArray, which is merged by |
     */
    fun startWearOsMeasurement(context: Context, path: String, sensors: IntArray?) {
        sensors?.let {
            repository.wearOsHandler.sendMsg(context,
                WearOsConstants.WEAR_MESSAGE_PATH, "${WearOsConstants.START_MEASUREMENT};$path;${sensors.joinToString("|")}")
        }
    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        // not relevant for the MeasurementActivity
        // at the end of the service, service sends message to end all operations
        Log.i("MeasurementActivity", wearOsStates.toString())
    }

    /**
     * Deletes folder, if the user decides to do so in repeating mode
     *
     * @param context
     * @param folderName - foldername of the previous measurement
     */
    fun deleteWearOsFolder(context: Context, folderName: String) {
        repository.wearOsHandler.sendMsg(context,
            WearOsConstants.WEAR_MESSAGE_PATH, "${WearOsConstants.DELETE_FOLDER};$folderName")
    }
}