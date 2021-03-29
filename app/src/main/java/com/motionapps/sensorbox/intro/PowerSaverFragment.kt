package com.motionapps.sensorbox.intro

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.appintro.SlideBackgroundColorHolder
import com.motionapps.sensorbox.R

@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("BatteryLife")
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
                checkOptimisation()
            }
        }

        return view
    }


    /**
     * request to add app to whitelist
     *
     */
    private fun checkOptimisation() {

        val pm = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager?
        if (pm != null) {
            if (!pm.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:" + requireContext().packageName)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.intro_ignored_optimisations_error), Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(requireContext(), getString(R.string.intro_ignored_optimisations), Toast.LENGTH_LONG).show()
            }
        }else{
            Toast.makeText(requireContext(), getString(R.string.intro_ignored_optimisations_error), Toast.LENGTH_LONG).show()
        }
    }

    override val defaultBackgroundColor: Int
        get() = ContextCompat.getColor(requireContext(), R.color.colorRedLT)

    override fun setBackgroundColor(backgroundColor: Int) {}
}
