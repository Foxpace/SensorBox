package com.motionapps.sensorbox.charts

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import com.jjoe64.graphview.GraphView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.activities.PickSensorShow
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * passes sensorEvents to GraphHandler with use of the HandlerThread
 *
 */
class GraphViewer : AppCompatActivity(), SensorEventListener, AmbientModeSupport.AmbientCallbackProvider {

    private var mSensorThread: HandlerThread? = null
    private var mSensorHandler: Handler? = null
    private var graphHandler: GraphHandler? = null
    private var sensorManager: SensorManager? = null
    private var sensorType = 0
    var paused = false

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph_viewer)

        // get sensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (sensorManager != null) {
            registerSensor()
        } else {
            Toast.makeText(this, R.string.sensor_registration_failure, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PickSensorShow::class.java))
        }

        // set up GraphHandler to prepare chart
        val bundle = intent.extras
        graphHandler = GraphHandler(
            findViewById<View>(R.id.graph) as GraphView,
            getString(
                bundle!!.getInt(PickSensorShow.GET_EXTRA_NAME)
            ),
            10000
        )

        // double tap on chart will pause him
        sensorType = bundle.getInt(PickSensorShow.GET_EXTRA_TYPE)
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            //here is the method for double tap
            override fun onDoubleTap(e: MotionEvent): Boolean {
                paused = if (paused) {
                    registerSensor()
                    false
                } else {
                    unregisterSensor()
                    true
                }
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return true
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })

        //here yourView is the View on which you want to set the double tap action

        findViewById<View>(R.id.graph).setOnTouchListener { _, event ->
            gd.onTouchEvent(event)
            true
        }
        Toast.makeText(this, R.string.pause_graph, Toast.LENGTH_LONG).show()

        // sensor registration
        registerSensor()
        ambientController = AmbientModeSupport.attach(this)
    }

    /**
     * starts HandlerThread and registers required sensor on it
     */
    private fun registerSensor() {
        mSensorThread = HandlerThread("SensorThread", Thread.MAX_PRIORITY)
        mSensorThread!!.start()
        mSensorHandler = Handler(mSensorThread!!.looper) //Blocks until looper is prepared, which is fairly quick

        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(sensorType),
            SensorManager.SENSOR_DELAY_UI,
            mSensorHandler
        )
    }

    /**
     * cancels thread and unregisters the sensor
     *
     */
    private fun unregisterSensor() {
        if (sensorManager != null) {
            sensorManager?.unregisterListener(this)
            mSensorThread?.quitSafely()
            mSensorThread = null
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensor()
    }

    override fun onSensorChanged(event: SensorEvent) {
        CoroutineScope(Dispatchers.Main).launch {
            graphHandler!!.addPoint(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()

    inner class MyAmbientCallback : AmbientModeSupport.AmbientCallback() {

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            unregisterSensor()
        }

        override fun onExitAmbient() {
            registerSensor()
        }

        override fun onUpdateAmbient() {

        }
    }
}