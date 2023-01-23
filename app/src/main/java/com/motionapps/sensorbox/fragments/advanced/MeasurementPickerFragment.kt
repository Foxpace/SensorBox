package com.motionapps.sensorbox.fragments.advanced

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.services.MeasurementService.Companion.ENDLESS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * 3 buttons to pick kind of the measurement LONG / ENDLESS / SHORT
 */
class MeasurementPickerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_measurement_picker, container, false)

//        (view.findViewById<LinearLayout>(R.id.picker_measurement_long)).also {
//            it.setOnClickListener {
//                val action: NavDirections = MeasurementPickerFragmentDirections.actionMeasurementPickerFragmentToCountersFragment(true)
//                Navigation.findNavController(requireView()).navigate(action)
//            }
//        }

        (view.findViewById<LinearLayout>(R.id.picker_measurement_endless)).also {
            it.setOnClickListener {
                val action: NavDirections = MeasurementPickerFragmentDirections.actionMeasurementPickerFragmentToExtraFragment(ENDLESS)
                Navigation.findNavController(requireView()).navigate(action)
            }
        }

        (view.findViewById<LinearLayout>(R.id.picker_measurement_short)).also {
            it.setOnClickListener {
                val action: NavDirections = MeasurementPickerFragmentDirections.actionMeasurementPickerFragmentToCountersFragment(false)
                Navigation.findNavController(requireView()).navigate(action)
            }
        }
        return view
    }

}