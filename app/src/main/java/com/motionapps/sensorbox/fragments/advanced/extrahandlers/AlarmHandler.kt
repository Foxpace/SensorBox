package com.motionapps.sensorbox.fragments.advanced.extrahandlers

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.fragment.app.Fragment
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.advanced.ExtraFragmentArgs
import com.motionapps.sensorservices.services.MeasurementService.Companion.ENDLESS
import com.motionapps.sensorservices.services.MeasurementService.Companion.LONG
import com.motionapps.sensorservices.services.MeasurementService.Companion.SHORT
import com.shawnlin.numberpicker.NumberPicker
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@ActivityRetainedScoped
/**
 * Handles custom alarms during measurement
 * Handles not only time points, but also UI - dialogs
 * Customised for LONG and SHORT measurements
 */
class AlarmHandler @Inject constructor() {

    val alarmsList: ArrayList<Int> = ArrayList() // stores alarms

    /**
     * Called by activity, when is restarted
     * Inflates all the views into linearLayout
     * @param linearLayout - layout to inflate views
     * @param layoutInflater - inflater to use
     * @param args - args from ExtraFragment, could be replaced by type integer
     */

    fun refreshLayout(linearLayout: LinearLayout, layoutInflater: LayoutInflater, args: ExtraFragmentArgs){
        for(i in alarmsList)
        if (args.typeMeasurement == SHORT) {
            createAlarmShort(linearLayout, layoutInflater, i, false)
        }else{
            val hours = TimeUnit.SECONDS.toHours(i.toLong()).toInt()
            val minutes = TimeUnit.SECONDS.toMinutes((i - hours*3600).toLong()).toInt()
            createAlarmLong(linearLayout, layoutInflater, hours, minutes, false)
        }
    }

    /**
     * Views are customised for seconds
     *
     * @param linearLayout - layout to inflate views
     * @param layoutInflater - inflater to use
     * @param value - seconds to show
     * @param addToList - true if the stamp is added recently, false for the refreshing
     */
    private fun createAlarmShort(linearLayout: LinearLayout, layoutInflater: LayoutInflater, value: Int, addToList: Boolean) {
        // inflation
        val view: View = layoutInflater.inflate(R.layout.item_layout_annotation, linearLayout, false)

        // data insertion
        (view.findViewById<TextView>(R.id.annot_row_text)).also {
            it.text = linearLayout.context.getString(R.string.extra_seconds, value)
        }
        (view.findViewById<ImageButton>(R.id.annot_row_button)).also {
            it.setOnClickListener {
                removeNote(linearLayout, view, value)
            }
        }

        // to add stamp into list
        if(addToList){
            alarmsList.add(value)
        }

        linearLayout.addView(view)
    }

    /**
     * Removes timestamp from list and linearLayout
     *
     * @param linearLayout - linearLayout from which the view is removed
     * @param view - view to remove
     * @param value - value in seconds to remove
     */
    private fun removeNote(linearLayout: LinearLayout, view: View, value: Int) {
        linearLayout.removeView(view)
        alarmsList.remove(value)
    }

    /**
     *  Views are customised for seconds
     *
     * @param linearLayout - linearLayout to add views
     * @param layoutInflater - to inflate
     * @param hours - integer
     * @param mins - integer
     * @param addToList - true if the stamp is added recently, false for the refreshing
     */
    private fun createAlarmLong(linearLayout: LinearLayout, layoutInflater: LayoutInflater, hours: Int, mins: Int, addToList: Boolean) {


        val view: View = layoutInflater.inflate(R.layout.item_layout_annotation, linearLayout, false)
        val value = 3600 * hours + 60 * mins // recalculated to seconds

        (view.findViewById<TextView>(R.id.annot_row_text)).also {
            it.text = linearLayout.context.getString(R.string.extra_hours_mins, hours, mins)
        }
        (view.findViewById<ImageButton>(R.id.annot_row_button)).also {
            it.setOnClickListener {
                removeNote(linearLayout, view, value)
            }
        }
        if(addToList){
            alarmsList.add(value)
        }
        linearLayout.addView(view)
    }


    /**
     * @return ArrayList of alarms in seconds
     */
    fun getAlarms(): ArrayList<Int> {
        if(alarmsList.isEmpty()){
            return arrayListOf(-1)
        }
        return alarmsList
    }


    /**
     * Dialog to handle insertion of the alarm - called by some button
     *
     * @property fragment - fragment upon which the dialog is shown
     * @property alarmHandler - alarmHandler of the fragment to store values
     * @property type - Value from ExtraFragment
     */
    class CustomDialog(
        val fragment: Fragment,
        private val alarmHandler: AlarmHandler,
        val type: ExtraFragmentArgs
    ) : Dialog(fragment.requireContext()) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            // picked by type
            if (type.typeMeasurement == SHORT) {
                setContentView(R.layout.template_picker) // only for seconds
                createDialogShort(context, type)
            } else {
                setContentView(R.layout.template_double_picker) // hours and minutes
                createDialogLong(context, type)
            }

        }

        private fun createDialogShort(context: Context, args: ExtraFragmentArgs) {
            // set restrictions for number picker
            val numberPicker: NumberPicker = findViewById(R.id.number_picker_right)
            numberPicker.maxValue = args.secondTime
            numberPicker.minValue = 1


            (findViewById<Button>(R.id.extra_adding_button)).also {
                it.setOnClickListener {
                    // checks if the time is not registered already
                    if (numberPicker.value in alarmHandler.alarmsList) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.extra_alarm_registered),
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }

                    // adding to linearlayout and storage
                    val linearLayout = fragment.requireView().findViewById<LinearLayout>(R.id.extra_container_alarms)
                    alarmHandler.createAlarmShort(linearLayout, fragment.layoutInflater, numberPicker.value, true)
                    dismiss()
                }
            }
        }


        private fun createDialogLong(context: Context, args: ExtraFragmentArgs) {
            // set restrictions for number picker
            val leftNumberPicker: NumberPicker = findViewById(R.id.number_picker_left)
            val rightNumberPicker: NumberPicker = findViewById(R.id.number_picker_right)

            leftNumberPicker.minValue = 0
            rightNumberPicker.minValue = 0

            if(args.typeMeasurement == ENDLESS){
                leftNumberPicker.maxValue = 100
            }else{
                leftNumberPicker.maxValue = args.firstTime
            }

            rightNumberPicker.maxValue = 60

            (findViewById<Button>(R.id.extra_adding_button)).also {
                it.setOnClickListener {

                    // checks if the time is not registered already
                    if ((leftNumberPicker.value * 3600 + rightNumberPicker.value * 60) in alarmHandler.alarmsList) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.extra_alarm_registered),
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                        // time was not set
                    } else if (leftNumberPicker.value == 0 && rightNumberPicker.value == 0) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.advanced_setup_time),
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                        // time is beyond stated time for measurement
                    } else if((leftNumberPicker.value * 3600 + rightNumberPicker.value * 60) > (args.firstTime*3600 + args.secondTime*60) && args.typeMeasurement == LONG){
                        Toast.makeText(
                            context,
                            context.getString(R.string.advanced_time_pass_limit),
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    val linearLayout = fragment.requireView().findViewById<LinearLayout>(R.id.extra_container_alarms)
                    alarmHandler.createAlarmLong(linearLayout, fragment.layoutInflater, leftNumberPicker.value, rightNumberPicker.value, true)
                    dismiss()

                }
            }
        }
    }
}