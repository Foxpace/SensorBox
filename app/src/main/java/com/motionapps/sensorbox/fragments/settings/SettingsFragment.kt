package com.motionapps.sensorbox.fragments.settings

import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.permissions.PermissionSettingsDialog
import com.motionapps.sensorbox.uiHandlers.PowerManagement
import com.motionapps.sensorservices.services.MeasurementService.Companion.ACTIVITY_RECOGNITION_PERIOD
import com.motionapps.sensorservices.services.MeasurementService.Companion.GPS_DISTANCE
import com.motionapps.sensorservices.services.MeasurementService.Companion.GPS_TIME
import com.motionapps.sensorservices.services.MeasurementService.Companion.PREFS
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SettingsFragment : PreferenceFragmentCompat() {

    private var dialog: MaterialDialog? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // preference to open fragment to change saving folder
        val storage: Preference ?= findPreference(FOLDER_CHANGE)
        storage?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(SettingsFragmentDirections.actionNavSettingsToPickFolderFragment(
                ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)))
            true
        }

        // preference to open fragment to change annotations
        val annotations: Preference ?= findPreference(ANNOTS)
        annotations?.setOnPreferenceClickListener {
            Navigation.findNavController(requireView()).navigate(SettingsFragmentDirections.actionNavSettingsToAnnotationFragment())
            true
        }
        // preference to open dialog about Google's activity regognition system
        val recognition: Preference? = findPreference(ABOUT_ACTIVITY_RECOGNITION)
        recognition?.setOnPreferenceClickListener {
            dialog = MaterialDialog(this@SettingsFragment.requireContext()).show{
                cornerRadius(16f)
                title(R.string.settings_activity_recognition_title)
                message(R.string.settings_activity_recognition_text)
                negativeButton(R.string.cancel) {
                    it.dismiss()
                }
            }
            true
        }

        val storageDetails: Preference? = findPreference(STORAGE_DETAILS)
        storageDetails?.setOnPreferenceClickListener {
            dialog = MaterialDialog(this@SettingsFragment.requireContext()).show{
                cornerRadius(16f)
                title(R.string.settings_csv_formatting)
                message(R.string.settings_csv_formatting_text)
                negativeButton(R.string.cancel) {
                    it.dismiss()
                }
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

        val androidSettings: Preference? = findPreference(ANDROID_SETTINGS)
        androidSettings?.let {
            it.setOnPreferenceClickListener {
                PermissionSettingsDialog.showSettings(this@SettingsFragment.requireContext())
                true
            }
        }

        val batterySettings: Preference? = findPreference(BATTERY_SETTINGS)
        batterySettings?.let {
            it.setOnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManagement.checkOptimisation(this@SettingsFragment.requireActivity())
                } else{
                    Toasty.info(requireContext(), R.string.settings_no_optimisations_required, Toasty.LENGTH_LONG).show()
                }
                true
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
        try {
            if(value.isBlank()){
                val s: String = getString(R.string.settings_empty_string)
                Toasty.warning(this@SettingsFragment.requireContext(), s, Toasty.LENGTH_LONG).show()
                return false
            }
            return if(value.toLong() > toCompare){
                val s: String = getString(R.string.settings_high_value_warning)
                Toasty.warning(this@SettingsFragment.requireContext(), s + toCompare.toString(), Toasty.LENGTH_LONG).show()
                false
            }else{
                true
            }
        }catch (e: Error){
            Toasty.error(requireContext(), getString(R.string.settings_wrong_input_number), Toasty.LENGTH_LONG).show()
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
        dialog = null
    }

    companion object{
        const val APP_FIRST_TIME = "APP_FIRST_TIME"
        const val POLICY_AGREED = "POLICY_AGREED"
        const val FOLDER_CHANGE = "folder_change"
        const val ABOUT_ACTIVITY_RECOGNITION = "about_activity_recognition"
        const val ANNOTS = "annots"
        const val STORAGE_DETAILS = "storage_details"
        const val ANDROID_SETTINGS = "android_settings"
        const val BATTERY_SETTINGS = "battery_settings"
    }

}