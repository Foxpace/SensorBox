package com.motionapps.sensorbox.fragments.advanced

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.jaredrummler.materialspinner.MaterialSpinner
import com.motionapps.countdowndialog.CountDownStates
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.activities.MeasurementActivity
import com.motionapps.sensorbox.permissions.PermissionSettingsDialog
import com.motionapps.sensorbox.viewmodels.MainViewModel
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementService.Companion.SHORT
import com.motionapps.wearoslib.WearOsStates
import dagger.hilt.android.AndroidEntryPoint
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ExtraFragment : Fragment() {

    /**
     * Adds more specifications to advanced measurement like custom name, speed of sensors, alarms,
     * notes and other
     */

    // uses viewmodel from MainActivity and data from previous fragments
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    private val args: ExtraFragmentArgs by navArgs()
    private var dialog: Dialog? = null
    private var materialDialog: MaterialDialog? = null
    private var activityRecognitionBoolean = false
    private val activityRecognitionPermissionCallback =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isGranted) {
                    makeActivityRecognitionCheckEnabled(true)
                    activityRecognitionBoolean = true
                } else {
                    val rational =
                        shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
                    if (rational) {
                        showPermissionDialog()
                    } else {
                        PermissionSettingsDialog.showSettings(requireContext())
                    }
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPermissionDialog() {
        materialDialog = PermissionSettingsDialog.showPermissionRational(
            activityRecognitionPermissionCallback,
            this,
            R.string.advanced_recognition_permission,
            Manifest.permission.ACTIVITY_RECOGNITION,
            icon = R.drawable.ic_baseline_run
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // FolderName
        val view: View = inflater.inflate(R.layout.fragment_extra, container, false)
        val folderName: EditText = view.findViewById(R.id.extra_name_folder)
        folderName.addTextChangedListener(CustomTextWatcher())

        // sensorSpeed spinner setup
        val spinner = view.findViewById(R.id.spinner) as MaterialSpinner
        val items =
            requireContext().resources.getStringArray(R.array.sensor_speeds) as Array<String>
        spinner.setItems(items.toList())
        spinner.setOnItemSelectedListener { _, position, _, _ ->

            mainViewModel.positionSensorSpeed = arrayOf(
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_NORMAL
            )[position]
        }
        spinner.selectedIndex = mainViewModel.positionSensorSpeed

        // button to start measurement
        val startButton: Button = view.findViewById(R.id.extra_start_button)
        startButton.setOnClickListener {
            onStartClick()
        }


        if (args.typeMeasurement != SHORT) {
            // repeated measurement is only available with short measurement
            view.findViewById<CheckBox>(R.id.extra_checkbox_repeat).visibility = GONE
        } else {
            // sets up countdown
            dialog = mainViewModel.startCountdown(requireContext(), -1)
            dialog?.show()
            dialog?.findViewById<Button>(R.id.dialog_button_cancel)?.setOnClickListener {
                mainViewModel.cancelCountDown()
                dialog?.dismiss()
            }

            // on countDown action
            mainViewModel.countDown.observe(viewLifecycleOwner) { countDownState ->
                when (countDownState) {
                    is CountDownStates.OnTick -> {
                        dialog?.findViewById<TextView>(R.id.dialog_text_countdown)?.text =
                            countDownState.tick
                    }
                    is CountDownStates.OnCancel -> {
                        dialog?.dismiss()
                        dialog = null
                    }
                    is CountDownStates.OnFinish -> {
                        dialog?.dismiss()
                        dialog = null
                        startMeasurement()
                    }
                    else -> {
                    }
                }
            }
        }

        // check significant motion
        val mSensorManager =
            requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) == null) {
            view.findViewById<CheckBox>(R.id.extra_checkbox_significant_motion).visibility = GONE
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // if the permission for the activity recognition is missing
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                makeActivityRecognitionCheckDisabled()
            } else {
                makeActivityRecognitionCheckEnabled(activityRecognitionBoolean)
            }
        }
    }

    private fun makeActivityRecognitionCheckEnabled(checked: Boolean) {
        requireView().findViewById<CheckBox>(R.id.extra_checkbox_activity)?.let { checkBox ->
            checkBox.text = getString(R.string.extra_activity_recognition_checkbox)
            checkBox.isChecked = checked
            checkBox.isEnabled = true
            checkBox.setOnCheckedChangeListener{ _, click ->
                activityRecognitionBoolean = click
            }

            val white = ContextCompat.getColor(requireContext(), R.color.colorWhite)
            checkBox.setTextColor(white)

            checkBox.buttonTintList = ColorStateList.valueOf(white)
        }
        requireView().findViewById<Button>(R.id.extra_button_activity_recognition_permission)?.let {
            it.visibility = GONE
            it.setOnClickListener {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun makeActivityRecognitionCheckDisabled() {
        val checkBox = requireView().findViewById<CheckBox>(R.id.extra_checkbox_activity)
        checkBox.text =
            getString(R.string.extra_activity_recognition_checkbox_no_permission)
        checkBox.isChecked = false
        checkBox.isEnabled = false

        val gray = ContextCompat.getColor(requireContext(), R.color.colorGray)
        checkBox.setTextColor(gray)

        checkBox.buttonTintList = ColorStateList.valueOf(gray)
        requireView().findViewById<Button>(R.id.extra_button_activity_recognition_permission)
            ?.let {
                it.visibility = VISIBLE
                it.setOnClickListener {
                    showPermissionDialog()
                }
            }
    }

    /**
     * Starts dialog for Short or measurement for other types
     */
    private fun onStartClick() {
        if (args.typeMeasurement == SHORT) {
            dialog = mainViewModel.startCountdown(requireContext(), interval = args.firstTime)
            dialog?.show()
            dialog?.findViewById<Button>(R.id.dialog_button_cancel)?.setOnClickListener {
                mainViewModel.cancelCountDown()
                dialog?.dismiss()
                dialog = null
            }
        } else {
            startMeasurement()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        materialDialog?.dismiss()
    }


    /**
     * creates intents for service and MeasurementActivity from gather info from previous fragment
     * and this one
     */
    private fun startMeasurement() {

        val status = mainViewModel.wearOsStatus.value
        if (status is WearOsStates.Status) {
            if (status.running) {
                Toasty.warning(
                    requireContext(),
                    getString(R.string.wear_os_active_measurement),
                    Toasty.LENGTH_LONG,
                    true
                ).show()
                return
            }
        }

        val serviceIntent = Intent(requireActivity(), MeasurementService::class.java)
        val customName =
            requireView().findViewById<EditText>(R.id.extra_name_folder).text.toString()
        val folderName = MeasurementService.generateFolderName(args.typeMeasurement, customName)

        val wearOsSensors = mainViewModel.getSensorView().getWearOsToMeasure()
        wearOsSensors?.let { sensors ->
            mainViewModel.startWearOsMeasurement(
                requireContext(),
                folderName,
                sensors
            )
        }

        MeasurementService.addExtraToIntentAdvanced(
            folder = folderName,
            customName = customName,
            internalStorage = false, // used for the Wear Os, which stores info to internal storage
            intent = serviceIntent,
            speedSensor = mainViewModel.sensorSpeedArray,
            sensorsToMeasure = mainViewModel.getSensorView().getSensorsToMeasure(),
            gpsToMeasure = mainViewModel.getSensorView().gpsMeasurement,
            typeMeasurement = args.typeMeasurement,
            timeIntervals = arrayOf(args.firstTime, args.secondTime),
            notes = mainViewModel.getNotes(),
            alarms = mainViewModel.getAlarms(),
            checkCheckBoxes = checkCheckBoxes(), // another info like repeating, battery/activity check
            wearSensors = wearOsSensors // array of Wear Os sensors ID
        )

        val activityIntent = Intent(requireContext(), MeasurementActivity::class.java)
        activityIntent.putExtras(serviceIntent)

        ContextCompat.startForegroundService(requireContext(), serviceIntent)
        startActivity(activityIntent)
        requireActivity().finish()
    }

    /**
     * Refreshes views for notes, alarms, ...
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manageNoteViews()
        manageAlarmHandler()
        mainViewModel.refreshNotesAndAlarms(
//            requireView().findViewById(R.id.extra_container_alarms),
            requireView().findViewById(R.id.extra_container_notes),
            layoutInflater, args
        )
    }

    /**
     * sets up button for the dialog
     */
    private fun manageAlarmHandler() {
//        requireView().findViewById<Button>(R.id.extra_button_alarms).setOnClickListener {
//            dialog = AlarmHandler.CustomDialog(this, mainViewModel.getAlarmHandler(), args)
//            dialog!!.show()
//        }
    }

    /**
     * manges EditText, which result is added to viewModel
     *
     */
    private fun manageNoteViews() {
        val editText = requireView().findViewById<EditText>(R.id.extra_edittext_notes)
        editText.addTextChangedListener(CustomTextWatcher())
        requireView().findViewById<Button>(R.id.extra_button_notes).setOnClickListener {
            val s = editText.text.toString()
            if (s != "") {
                mainViewModel.onAddNote(
                    s,
                    requireView().findViewById(R.id.extra_container_notes),
                    layoutInflater,
                )
                editText.text.clear()
            }
        }
    }

    /**
     * gathers info about anothe setting for the measurement
     *
     * @return {Movement Activity, significant motion, battery log, lock CPU, repeating}
     */
    private fun checkCheckBoxes(): Array<Boolean> {
        return arrayOf(
            requireView().findViewById<CheckBox>(R.id.extra_checkbox_activity).isChecked,
            requireView().findViewById<CheckBox>(R.id.extra_checkbox_significant_motion).isChecked,
            requireView().findViewById<CheckBox>(R.id.extra_checkbox_battery).isChecked,
            requireView().findViewById<CheckBox>(R.id.extra_checkbox_cpu).isChecked,
            requireView().findViewById<CheckBox>(R.id.extra_checkbox_repeat).isChecked
        )
    }


    /**
     * Custom class of TextWatcher for the EditText class
     * Only letters and underscores are permitted at custom name of the folder
     *
     */
    inner class CustomTextWatcher : TextWatcher {
        private val re: Regex = "^[\\p{L}0-9_]*\$".toRegex()
        override fun afterTextChanged(s: Editable) {
            if (!re.matches(s.toString())) {
                s.delete(s.length - 1, s.length)
                Toasty.warning(
                    this@ExtraFragment.requireContext(),
                    R.string.extra_only_letters,
                    Toasty.LENGTH_LONG,
                    true
                ).show()
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

}