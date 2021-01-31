package com.motionapps.sensorbox.charts

import android.graphics.Color
import android.hardware.SensorEvent
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.LabelFormatter
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.*

/**
 * Class that manages chart representation - stores data and stylize them
 *
 * @property max number of samples to show in 10 seconds
 * @constructor
 *
 * @param graphView - view with chart
 * @param title - title of the chart
 */
class GraphHandler(graphView: GraphView, title: String, private val max: Int) {

    private val start: Long
    private var reducer: Long = 0 // reduces frequency of updates - only one per 1 ms
    private val data: Array<LineGraphSeries<DataPoint>?> = arrayOf(
        LineGraphSeries(),
        LineGraphSeries(),
        LineGraphSeries()
    )

    init {
        setGraph(graphView, title)
        start = System.currentTimeMillis()
    }

    /**
     * addition of sensorEvent, which is plotted on chart
     *
     * @param sensorEvent - gravity, acceleration, ...
     */
    fun addPoint(sensorEvent: SensorEvent) {
        if (reducer == System.currentTimeMillis()) {
            return
        }

        for (i in sensorEvent.values.indices) {
            data[i]!!.appendData(
                DataPoint((System.currentTimeMillis() - start).toDouble(), sensorEvent.values[i].toDouble()),
                true, max
            )
            reducer = System.currentTimeMillis()
        }
    }

    /**
     * setups style of chart
     *
     * @param graph - view with chart
     * @param title - string
     */
    private fun setGraph(graph: GraphView, title: String) {

        graph.title = title
        graph.titleColor = Color.WHITE

        //manual range
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0)
        graph.viewport.setMaxX(max.toDouble())

        // own grid formatting
        graph.gridLabelRenderer.labelFormatter = object : LabelFormatter {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    String.format(Locale.getDefault(), "%.0f s", value / 1000)
                } else String.format(Locale.getDefault(), "%.2f", value)
            }

            override fun setViewport(viewport: Viewport) {}
        }
        graph.gridLabelRenderer.numHorizontalLabels = 3
        graph.gridLabelRenderer.textSize = 18f
        graph.gridLabelRenderer.labelsSpace = 15
        graph.gridLabelRenderer.gridColor = Color.WHITE
        graph.gridLabelRenderer.verticalLabelsColor = Color.WHITE
        graph.gridLabelRenderer.horizontalLabelsColor = Color.WHITE

        // style to linecharts
        val titles = arrayOf("x", "y", "z", "0")
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA)

        for (i in data.indices) {
            data[i] = LineGraphSeries()
            data[i]!!.color = colors[i]
            data[i]!!.title = titles[i]
            graph.addSeries(data[i])
        }
    }


}