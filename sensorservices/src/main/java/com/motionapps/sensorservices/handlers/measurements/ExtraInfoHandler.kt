package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.types.SensorNeeds
import org.json.JSONArray
import org.json.JSONObject

/**
 * creates extra.json
 * aggregates all the ranges from the sensors,starting times, notes, alarms, annotations, ...
 * as result, the json file is created
 */
class ExtraInfoHandler {
    // basic info
    var folderName: String = ""
    var measurementType: String = ""
    var date: String = ""

    private var triggered: Boolean = false // to prevent double write

    // starting times
    private var timeMillis: Long? = null
    private var timeNanos: Long? = null


    private val annotations: ArrayList<Annotations> = ArrayList() // by user
    private val ranges: ArrayList<SensorRange> = ArrayList() // from sensors

    private var notes: ArrayList<String>? = null // from ExtraFragment
    private var alarms: JSONArray? = null // from AlarmNoiseHandler

    /**
     * Extra is started - beginning time stamps
     *
     * @param context
     * @param internal - to avoid Wear Os or not
     */
    fun onStart(context: Context, internal: Boolean){
        triggered = false

        //write start
        timeMillis = System.currentTimeMillis()
        timeNanos = SystemClock.elapsedRealtimeNanos()

        createSensorRanges(context, internal)
    }

    /**
     * addition of notes from ExtraFragment
     *
     * @param list
     */
    fun handleNotes(list: ArrayList<String>?) {
        list?.let {
            notes = it
        }
    }

    /**
     * writes annotation from MeasurementActivity
     *
     * @param time - timestamp
     * @param text - text to store
     */
    fun writeAnnotation(time: Long, text: String) {
        if(time != -1L && text != ""){
            annotations.add(Annotations(time, text))
        }
    }

    /**
     * iterates through all sensors and picks available maximum ranges to store in json
     *
     * @param context
     * @param internal
     */
    private fun createSensorRanges(context: Context, internal: Boolean) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        for (sensor: SensorNeeds in SensorNeeds.values()) {
            if (internal && "WEAR" in sensor.name) {
                sensorManager.getDefaultSensor(sensor.id)?.let {
                    ranges.add(SensorRange(sensor.name, it.maximumRange))
                }
            } else if (!internal && "WEAR" !in sensor.name) {
                sensorManager.getDefaultSensor(sensor.id)?.let {
                    ranges.add(SensorRange(sensor.name, it.maximumRange))
                }
            }
        }
    }

    /**
     * creation of the JSON file
     *
     * @param context
     * @param internal - if it is internal storage or not
     */
    fun writeExtra(context: Context, internal: Boolean){
        Log.i("SensorController", "Writing JSON file")
        if(triggered){
            return
        }

        triggered = true
        val jsonObject = JSONObject()
        jsonObject.put("millis", timeMillis)
        jsonObject.put("nanos", timeNanos)
        jsonObject.put("type", measurementType)
        jsonObject.put("date", date)

        notes?.let {
            putNotes(jsonObject)
        }

        if(annotations.isNotEmpty()){
            putAnnots(jsonObject)
        }

        if(ranges.isNotEmpty()){
            putRanges(jsonObject)
        }

        alarms?.let {
            jsonObject.put("alarms", it)
        }

        val name = "extra.json"

        val outputStream = if(internal){
            StorageHandler.createFileInInternalFolder(context, folderName, name)
        }else{
            StorageHandler.createFileInFolder(context, folderName, "json", name)
        }

        outputStream?.let {
            it.write(jsonObject.toString().toByteArray())
            it.flush()
            it.close()
        }

    }

    /**
     * all notes are looped and writes as JSON array
     * @param main - JSON object
     */
    private fun putNotes(main: JSONObject){
        val arrayJson = JSONArray()
        for(note in notes!!){
            arrayJson.put(note)
        }
        main.put("notes", arrayJson)
    }

    /**
     * annotations are separated JSON objects in array with timestamp and value
     *
     * @param main - JSON object
     */
    private fun putAnnots(main: JSONObject){
        val arrayJson = JSONArray()
        for(annot in annotations){
            val childJson = JSONObject()
            childJson.put("timestamp", annot.time)
            childJson.put("annotation", annot.text)
            arrayJson.put(childJson)
        }
        main.put("annotations", arrayJson)
    }

    /**
     * separate JSON objects with sensor and its range in JSON array
     *
     * @param main - JSON object
     */
    private fun putRanges(main: JSONObject){
        val arrayJson = JSONArray()
        for(sensorRange in ranges){
            val childJson = JSONObject()
            childJson.put("sensor", sensorRange.sensor)
            childJson.put("range", sensorRange.range)
            arrayJson.put(childJson)
        }
        main.put("ranges", arrayJson)
    }

    /**
     * adds alarm JSON to the our main JSON
     */
    fun addAlarms(alarmNoiseHandler: AlarmNoiseHandler) {
        alarms = alarmNoiseHandler.json
    }


    class Annotations(val time: Long, val text: String)
    class SensorRange(val sensor: String, val range: Float)

}