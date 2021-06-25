package com.motionapps.sensorbox.intro

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.SlideBackgroundColorHolder
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.uiHandlers.PowerManagement

@RequiresApi(Build.VERSION_CODES.M)
/**
 * Asks user to add app to the whitelist
 *
 */
class PowerSaverFragment : Fragment(), SlideBackgroundColorHolder {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_power_saver, container, false)

        (view.findViewById<Button>(R.id.intro_ignore_battery)).also {
            it.setOnClickListener{
                PowerManagement.checkOptimisation(requireActivity())
            }
        }

        return view
    }




    override val defaultBackgroundColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.colorRedLT)

    override fun setBackgroundColor(backgroundColor: Int) {}
}
