package com.motionapps.sensorbox.fragments.settings

import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.services.MeasurementService.Companion.ACTIVITY_RECOGNITION_PERIOD
import com.motionapps.sensorservices.services.MeasurementService.Companion.GPS_DISTANCE
import com.motionapps.sensorservices.services.MeasurementService.Companion.GPS_TIME
import com.motionapps.sensorservices.services.MeasurementService.Companion.PREFS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // preference to open fragment to change saving folder
        val storage: Preference ?= findPreference("folder_change")
        storage?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(SettingsFragmentDirections.actionNavSettingsToPickFolderFragment(
                ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)))
            true
        }

        // preference to open fragment to change annotations
        val annotations: Preference ?= findPreference("annots")
        annotations?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(SettingsFragmentDirections.actionNavSettingsToAnnotationFragment())
            true
        }
        // preference to open dialog about Google's activity regognition system
        val recognition: Preference? = findPreference("about_activity_recognition")
        recognition?.setOnPreferenceClickListener {
            MaterialDialog(this@SettingsFragment.requireContext()).show{
                cornerRadius(16f)
                title(R.string.settings_activity_recognition_title)
                message(R.string.settings_activity_recognition_text)
            }
            true
        }

        // settings for the service - number parameters
        for(key in PREFS) {
            val pref: EditTextPreference? = findPreference(key)
            pref?.setOnPreferenceChangeListener { preference: Preference, value: Any ->
                valueAssertion(preference.key, value)
            }
            pref?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER // only numbers
            }
        }
    }

    /**
     * Number assertion prior to storing the value
     *
     * @param key - key of the preference
     * @param any - usually string with number value
     * @return - if it is ok, to store value
     */
    private fun valueAssertion(key: String, any: Any): Boolean{
        if (any is String) {
            when (key) {
                ACTIVITY_RECOGNITION_PERIOD -> return checkValue(any.toString(), 1200)
                GPS_DISTANCE -> return checkValue(any.toString(), 10000)
                GPS_TIME -> return checkValue(any.toString(), 10000)
            }

        }
        return false
    }

    /**
     * Compares value with rule - Toast is risen, if the value is not in range
     *
     * @param value - value of the preference
     * @param toCompare - max acceptable value
     * @return
     */
    private fun checkValue(value: String, toCompare: Int): Boolean{
        val v = value.toLong()
        return if(v > toCompare){
            val s: String = getString(R.string.settings_high_value_warning)
            Snackbar.make(this@SettingsFragment.requireView(), s + toCompare.toString(), Snackbar.LENGTH_SHORT).show()
            false
        }else{
            true
        }
    }

    companion object{
        const val APP_FIRST_TIME = "APP_FIRST_TIME"
    }

}