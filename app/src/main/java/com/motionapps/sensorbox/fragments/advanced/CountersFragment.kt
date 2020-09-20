package com.motionapps.sensorbox.fragments.advanced

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.services.MeasurementService.Companion.ENDLESS
import com.motionapps.sensorservices.services.MeasurementService.Companion.LONG
import com.motionapps.sensorservices.services.MeasurementService.Companion.SHORT
import com.shawnlin.numberpicker.NumberPicker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class CountersFragment : Fragment() {

    private var type: Int = ENDLESS

    /**
     * User can set up time of running for long measurement or for short measurement
     * Uses https://github.com/ShawnLin013/NumberPicker for number pickers
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_counters, container, false)

        // setting up pickers and text to them
        val pickerLeft: NumberPicker = view.findViewById(R.id.number_picker_left)
        val pickerRight: NumberPicker = view.findViewById(R.id.number_picker_right)
        val textViewLeft: TextView = view.findViewById(R.id.number_picker_textview_left)
        val textViewRight: TextView  = view.findViewById(R.id.number_picker_textview_rigth)

        val args: CountersFragmentArgs by navArgs()

        pickerLeft.minValue = 0
        pickerLeft.value = 0

        pickerRight.minValue = 0
        pickerRight.value = 0
        pickerRight.maxValue = 60

        if(args.longMeasurement){
            type = LONG
            setUpLongMeasurement(pickerLeft, textViewLeft, textViewRight)
        }else{
            type = SHORT
            setUpShortMeasurement(pickerLeft, textViewLeft, textViewRight)
        }

        (view.findViewById<Button>(R.id.number_picker_next)).also {
            it.setOnClickListener{
                val action: NavDirections = CountersFragmentDirections.actionCountersFragmentToExtraFragment(type, pickerLeft.value, pickerRight.value)
                if(type == LONG){
                    if(pickerLeft.value == 0 && pickerRight.value == 0){
                        Snackbar.make(requireView(), getString(R.string.advanced_setup_time), Snackbar.LENGTH_SHORT).show()
                    }else{
                        Navigation.findNavController(requireView()).navigate(action)
                    }
                }else{
                    if(pickerLeft.value == 0 || pickerRight.value == 0){
                        Snackbar.make(requireView(), getString(R.string.advanced_setup_time), Snackbar.LENGTH_SHORT).show()
                    }else{
                        Navigation.findNavController(requireView()).navigate(action)
                    }
                }
            }
        }

        return view
    }

    /**
     * Specifics for short measurement - picker on the right has always range 0 - 60,
     * only text needs to be changed
     *
     * @param pickerLeft - time to start measurement
     * @param textViewLeft
     * @param textViewRight
     */
    private fun setUpShortMeasurement(pickerLeft: NumberPicker, textViewLeft: TextView, textViewRight: TextView) {
        type = SHORT
        textViewLeft.setText(R.string.advanced_time_to_start)
        textViewRight.setText(R.string.advanced_time_to_measure)
        pickerLeft.maxValue = 20
    }

    /**
     * Specifics for long measurement
     *
     * @param pickerLeft - hours to measure
     * @param textViewLeft
     * @param textViewRight
     */
    private fun setUpLongMeasurement(pickerLeft: NumberPicker, textViewLeft: TextView, textViewRight: TextView) {
        type = LONG
        textViewLeft.setText(R.string.advanced_hours)
        textViewRight.setText(R.string.advanced_minutes)
        pickerLeft.maxValue = 24
    }
}