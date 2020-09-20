package com.motionapps.sensorbox.uiHandlers

import android.graphics.Color
import com.jjoe64.graphview.*
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.motionapps.sensorservices.types.SensorNeeds
import java.util.*
import kotlin.collections.ArrayList

/**
 * Handles all the requests for the chart arrangement
 */
object GraphHandler {

    /**
     * Stylize the chart for needed look
     *
     * @param graph - view of the chart
     * @param title - title of the chart
     * @param sensorNeeds - formatting requirements for the sensor
     * @param type - INFO_VIEW - shows grid with units / MEASUREMENT - without grid and units
     * @param lineData - previous data to bind to chart
     * @return - reference to dataStorage of the chart - update lines
     */
    fun initChart(graph: GraphView, title: String, sensorNeeds: SensorNeeds, type: Int, lineData: ArrayList<LineGraphSeries<DataPoint>>? = null): ArrayList<LineGraphSeries<DataPoint>> {


        val data: ArrayList<LineGraphSeries<DataPoint>> = lineData ?: ArrayList()
        val titles = arrayOf("x", "y", "z", "0")
        val colors = intArrayOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW
        )

        if(data.isEmpty()){
            for (i in 0 until sensorNeeds.count) { // init of the series for the chart
                data.add(LineGraphSeries())
                data[i].color = colors[i]
                data[i].title = titles[i]
                graph.addSeries(data[i])
            }
        }else{
            for(series in data){ // adding previous data
                graph.addSeries(series)
            }
        }

        when(type){
            INFO_VIEW -> setupInfoView(
                graph,
                title,
                sensorNeeds
            )
            MEASUREMENT -> setupMeasurement(
                graph,
                data
            )
        }

        return data

    }

    /**
     * adds grid and specific formatting
     *
     * @param graph - view with the chart
     * @param title - to show
     * @param sensorNeeds - for units of the sensor
     */
    private fun setupInfoView(graph: GraphView, title: String, sensorNeeds: SensorNeeds){
        // title
        graph.title = "%s [%s]".format(title, sensorNeeds.unit)

        // grid
        val color = Color.WHITE
        graph.legendRenderer.textColor = color
        graph.titleColor = color
        graph.gridLabelRenderer.gridColor = color
        graph.gridLabelRenderer.horizontalLabelsColor = color
        graph.gridLabelRenderer.verticalLabelsColor = color

        // setting boundaries for X axis
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(10000.0)
        //padding and formatting
        graph.gridLabelRenderer.padding = 32
        graph.gridLabelRenderer.labelFormatter = object : LabelFormatter {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    String.format(
                        Locale.getDefault(),
                        "%.0f s",
                        value / 1_000
                    )
                } else String.format(Locale.getDefault(), "%.2f", value)
            }

            override fun setViewport(viewport: Viewport) {}
        }

        // legend
        graph.legendRenderer.isVisible = true
        graph.legendRenderer.align = LegendRenderer.LegendAlign.TOP
    }

    /**
     * removes grid and units
     *
     * @param graph - view of the chart
     * @param data - to stylize data
     */
    private fun setupMeasurement(
        graph: GraphView,
        data: ArrayList<LineGraphSeries<DataPoint>>
    ){
        //
        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.NONE
        graph.gridLabelRenderer.isVerticalLabelsVisible = false
        graph.gridLabelRenderer.isHorizontalLabelsVisible = false
        // setting boundaries for X axis
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(10000.0)

        graph.gridLabelRenderer.padding = 32
        graph.legendRenderer.isVisible = false

        for(d in data){
            d.thickness = 10
        }
    }

    const val INFO_VIEW = 0
    const val MEASUREMENT = 1
}