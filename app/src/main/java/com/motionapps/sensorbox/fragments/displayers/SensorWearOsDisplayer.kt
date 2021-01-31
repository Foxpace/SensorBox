package com.motionapps.sensorbox.fragments.displayers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavArgs
import com.google.android.gms.wearable.*
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.InfoSensorFragmentWearOsArgs
import com.motionapps.sensorbox.fragments.SensorInfoView
import com.motionapps.sensorbox.types.SensorAttributes
import com.motionapps.sensorbox.types.SensorResources
import com.motionapps.sensorbox.uiHandlers.GraphHandler
import com.motionapps.sensorbox.uiHandlers.TextUpdater
import com.motionapps.sensorbox.viewmodels.MainViewModel
import com.motionapps.sensorservices.types.SensorNeeds
import com.motionapps.wearoslib.WearOsConstants.SAMPLE_PATH_TIME
import com.motionapps.wearoslib.WearOsConstants.SAMPLE_PATH_VALUE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * Similar to SensorDisplayer
 * Data are pulled from the Wear Os device through Wearable.DataClient
 * Attributes of the sensor are gathered at synchronization with Wear Os device, when all the information
 * are passed as one string divided by \n (for sensor) and | (for sensor attribute) symbols
 *
 * @property mainViewModel
 */
class SensorWearOsDisplayer(private val mainViewModel: MainViewModel) : Displayer, TextUpdater.TextUpdaterInterface,
    OnDataChangedListener {

    private var dataClient: DataClient? = null
    private var view: View? = null
    private var startTime = -1L
    private var maxSensorPoints = -1
    private var chartData: ArrayList<LineGraphSeries<DataPoint>> = ArrayList()
    private var idView = 0

    override fun getView(
        context: Context,
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        args: NavArgs
    ): View? {

        // data gathered from Wear Os about sensors
        val data = mainViewModel.wearOsContacted.value
        data?.let {

            idView = pickSensorView(args as InfoSensorFragmentWearOsArgs)

            view = inflater.inflate(idView, viewGroup, false)

            // data are gathered from viewModel
            for (v in createSensorAttributes(context, data, inflater, args)){
                view?.findViewById<LinearLayout>(R.id.sensorinfo_container)?.addView(v)
            }

            if(R.layout.fragment_info_sensor == idView){
                // sends message to start Wear Os service to send sensor data
                view?.findViewById<GraphView>(R.id.graph)?.let{
                    initWearOsSensorChart(context, it, args)
                }
            }else{
                initWearOsSensorChart(context, args)
            }

            return view
        }

        return null
    }

    /**
     * Changes contentView for specific sensor Chart / TextView
     * @return
     */
    private fun pickSensorView(args: InfoSensorFragmentWearOsArgs): Int {
        return when (SensorNeeds.valueOf(args.type).oneValueTextView) {
            SensorNeeds.Companion.TypeOfRepresentation.PLOT -> {
                R.layout.fragment_info_sensor
            }
            SensorNeeds.Companion.TypeOfRepresentation.REALTIME_COUNTER,
            SensorNeeds.Companion.TypeOfRepresentation.TEXTVIEW -> {
                R.layout.fragment_info_textview

            }
            else -> 0
        }
    }


    /**
     * inflates sensor attributes and adds them to linearLayout
     *
     * @param context
     * @param data - data gathered from Wear Os about sensors
     * @param inflater
     * @param args - type of the sensor to show
     * @return
     */
    private fun createSensorAttributes(
        context: Context,
        data: HashMap<Int, List<String>>,
        inflater: LayoutInflater,
        args: InfoSensorFragmentWearOsArgs
    ): ArrayList<View> {
        // sensor descriptions required for chart
        val sensorNeeds: SensorNeeds = SensorNeeds.valueOf(args.type)
        val ourSensor: List<String> = data[sensorNeeds.id]!!
        val array = ArrayList<View>()

        // max number of points to show 10 s
        maxSensorPoints = (10_000.toDouble()/(ourSensor[7].toDouble()/1000.0)).toInt()

        // provides similar info like sensor, but string is parsed into same objects
        val sensorViews = SensorAttributes.getViewSensorInfoWearOs(context, ourSensor, SensorResources.valueOf(args.type).icon, sensorNeeds)

        // inflation of the attribute views
        for (sensorInfoView: SensorInfoView in sensorViews) {
            val view = inflater.inflate(R.layout.item_layout_sensorrow_info, null)
            view.findViewById<TextView>(R.id.sensorrow_info_title)
                .also { it.text = sensorInfoView.title }
            view.findViewById<TextView>(R.id.sensorrow_info_value)
                .also { it.text = sensorInfoView.value }
            view.findViewById<ImageView>(R.id.sensorrow_icon)
                .also { it.setImageResource(sensorInfoView.icon) }
            array.add(view)
        }

        return array
    }

    /**
     * sends message to Wear Os device to send sensor samples to show on chart
     *
     * @param context
     * @param graphView - chart to use
     * @param args - picked sensor passed from fragment
     */
    private fun initWearOsSensorChart(
        context: Context,
        graphView: GraphView,
        args: InfoSensorFragmentWearOsArgs
    ) {
        val sensorNeeds: SensorNeeds = SensorNeeds.valueOf(args.type)
        // chart initialization
        chartData = GraphHandler.initChart(
            graphView, context.getString(sensorNeeds.title), sensorNeeds,
            GraphHandler.INFO_VIEW
        )

        connectToWearOs(context, sensorNeeds.id)
    }

    /**
     * sends message to Wear Os device to send sensor samples to show on textview
     *
     * @param context
     * @param args - picked sensor passed from fragment
     */
    private fun initWearOsSensorChart(
        context: Context, args: InfoSensorFragmentWearOsArgs) {
        val sensorNeeds: SensorNeeds = SensorNeeds.valueOf(args.type)
        connectToWearOs(context, sensorNeeds.id)
    }

    /**
     * inits DataClient to gather info from wearOS
     * also the message to Wear Os is sent
     * @param context
     * @param id
     */
    private fun connectToWearOs(context: Context, id: Int){
        dataClient = Wearable.getDataClient(context)
        dataClient?.addListener(this)
        startTime = System.currentTimeMillis()
        mainViewModel.startWearOsSensor(context, id)
    }

    override fun onDestroy() {
        view = null
        dataClient?.removeListener(this)
        dataClient = null
        chartData.clear()
    }

    /**
     * DataEvents are handled - samples are put on chart
     *
     * @param events
     */
    override fun onDataChanged(events: DataEventBuffer) {

        for(event in events){
            if(event.type == DataEvent.TYPE_CHANGED){
                // unwrap time stamp
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val time = dataMap.getLong(SAMPLE_PATH_TIME)

                if(time == startTime){
                    continue
                }

                // unwrap values
                val values = dataMap.getFloatArray(SAMPLE_PATH_VALUE)

                if (idView == R.layout.fragment_info_sensor) {
                    for (i in 0 until values.size - 2) {
                        chartData[i].appendData(
                            DataPoint(
                                (System.currentTimeMillis() - startTime).toDouble(),
                                values[i].toDouble()
                            ), true, maxSensorPoints
                        )
                    }
                } else {
                    onTextUpdate("%d %s".format(values[0].toInt(), "bpm"))
                }
            }
        }
    }

    override fun onTextUpdate(s: String) {
        view!!.findViewById<TextView>(R.id.sensorinfo_textview)?.let {
            it.text = s
        }
    }
}