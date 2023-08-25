package com.motionapps.sensorbox.fragments.displayers

import android.content.Context
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavArgs
import com.jjoe64.graphview.GraphView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.InfoSensorFragmentArgs
import com.motionapps.sensorbox.fragments.SensorInfoView
import com.motionapps.sensorbox.types.SensorAttributes
import com.motionapps.sensorbox.types.SensorResources
import com.motionapps.sensorbox.uiHandlers.GraphHandler
import com.motionapps.sensorbox.uiHandlers.GraphHandler.INFO_VIEW
import com.motionapps.sensorbox.uiHandlers.GraphUpdater
import com.motionapps.sensorbox.uiHandlers.TextUpdater
import com.motionapps.sensorservices.types.SensorNeeds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
/**
 * Displays sensor information and time changes
 */
open class SensorDisplayer @Inject constructor(): Displayer, TextUpdater.TextUpdaterInterface {

    private var view: View? = null
    private var graphUpdater: GraphUpdater? = null
    var args: InfoSensorFragmentArgs? = null

    override fun getView(context: Context, inflater: LayoutInflater, viewGroup: ViewGroup?, args: NavArgs): View {
        this.args = args as InfoSensorFragmentArgs
        return setUpSensor(context, inflater, viewGroup)
    }

    private fun setUpSensor(context: Context, inflater: LayoutInflater, container: ViewGroup?): View {
        // get sensor
        val sensorNeeds: SensorNeeds = SensorNeeds.valueOf(args!!.type)
        val sensorResources: SensorResources = SensorResources.valueOf(args!!.type)

        // inflate main view
        view = inflater.inflate(pickSensorView(), container, false)
        val linearLayout: LinearLayout = view!!.findViewById(R.id.sensorinfo_container)

        // get views to add to container
        val viewsToAdd: ArrayList<View> = getInfoViewSensor(
                    context,
                    inflater,
                    sensorNeeds,
                    sensorResources.icon
                )

        for (sensorView: View in viewsToAdd) {
            linearLayout.addView(sensorView)
        }

        // update chart / textview
        setUpSensorUpdates(context)

        return view as View
    }

    override fun onDestroy() {
        view = null
        graphUpdater?.onDestroy()
    }

    /**
     * Sets up object to update chart / textview with actual values of the sensor
     * @param context
     */
    private fun setUpSensorUpdates(context: Context) {
        val sensorNeeds: SensorNeeds = SensorNeeds.valueOf(args!!.type)
        when (sensorNeeds.oneValueTextView) {
            SensorNeeds.Companion.TypeOfRepresentation.PLOT ->{ // chart version
                graphUpdater = GraphUpdater()
                view!!.findViewById<GraphView>(R.id.graph)?.let{
                    graphUpdater?.chartData = GraphHandler.initChart(it, context.getString(sensorNeeds.title), sensorNeeds, INFO_VIEW)
                    graphUpdater?.startSensing(context, sensorNeeds)
                }
            }
            SensorNeeds.Companion.TypeOfRepresentation.TEXTVIEW, // text version - for proximity, steps, ...
            SensorNeeds.Companion.TypeOfRepresentation.REALTIME_COUNTER -> {
                graphUpdater = TextUpdater()
                (graphUpdater as TextUpdater).onTextUpdater = this
                (graphUpdater as TextUpdater).startSensing(context, sensorNeeds)
            }
        }
    }

    /**
     * Changes contentView for specific sensor Chart / TextView
     * @return
     */
    private fun pickSensorView(): Int {
        return when (SensorNeeds.valueOf(args!!.type).oneValueTextView) {
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
     * Creates views with attributes of the sensor
     *
     * @param context
     * @param inflater
     * @param sensorNeeds - sensor required to show
     * @param icon - icon of the attribute
     * @return
     */
    private fun getInfoViewSensor(
        context: Context,
        inflater: LayoutInflater,
        sensorNeeds: SensorNeeds,
        icon: Int
    ): ArrayList<View> {

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(sensorNeeds.id)
        val array: ArrayList<View> = ArrayList()
        if (sensor == null) {
            return arrayListOf()
        }
        // pulls data from the sensor and creates views of the attributes to show
        for (sensorInfoView: SensorInfoView in SensorAttributes.getSensorInfoViews(context, sensor, icon, sensorNeeds)) {
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
     * updates textView with actual value of the sensor
     *
     * @param s - value to show
     */
    override fun onTextUpdate(s: String) {
        view!!.findViewById<TextView>(R.id.sensorinfo_textview)?.let {
            it.text = s
        }
    }

}