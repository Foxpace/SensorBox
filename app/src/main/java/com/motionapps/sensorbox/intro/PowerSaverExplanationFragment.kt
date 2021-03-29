package com.motionapps.sensorbox.intro

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.SlideBackgroundColorHolder
import com.motionapps.sensorbox.R

@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("BatteryLife")
/**
 * Needed custom layout and fragment object for background colour
 *
 */
class PowerSaverExplanationFragment : Fragment(), SlideBackgroundColorHolder {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_explanation_power_saver, container, false)
    }

    override val defaultBackgroundColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.colorRedLT)

    override fun setBackgroundColor(backgroundColor: Int) {}
}
