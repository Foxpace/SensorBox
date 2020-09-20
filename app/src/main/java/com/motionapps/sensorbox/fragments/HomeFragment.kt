package com.motionapps.sensorbox.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.motionapps.sensorbox.activities.MainActivity
import com.motionapps.sensorbox.activities.MeasurementActivity
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.types.SensorResources
import com.motionapps.sensorbox.uiHandlers.SensorViewHandler
import com.motionapps.sensorbox.viewmodels.MainViewModel
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.types.SensorNeeds
import com.motionapps.sensorservices.types.SensorNeeds.Companion.getSensorByIdWearOs
import com.motionapps.sensorservices.types.SensorNeeds.Companion.getSensors
import com.motionapps.wearoslib.WearOsStates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
/**
 * Main fragment to choose sensors and picking them to measure
 */
open class HomeFragment : Fragment() {

    private var container: LinearLayout ?= null
    var mainButton: Button ?= null

    private val wearOsViews: ArrayList<View> = ArrayList() // storing Wear Os views - can be deleted
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = {requireActivity()})


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_home, container, false)
        this.container = root.findViewById(R.id.home_container)
        this.mainButton = root.findViewById(R.id.home_mainbutton)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkMainFolder()
        initSensorViews()
        initGPSView()
        checkSensorsToMeasure()
        initMainButton()
        setObservers()
        mainViewModel.clearHandlers()
    }

    /**
     * Checks if the main folder exists
     * in Android Oreo, is required permission from the user, so he is prompted to pick folder, if
     * the saved one is not available
     * in lower versions, this is automatic
     */
    private fun checkMainFolder(){
        if(!StorageHandler.isFolder(requireContext())){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                Navigation.findNavController(requireView()).navigate(
                    HomeFragmentDirections.actionNavHomeToPickFolderFragment(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)))
                return
            }else{
                StorageHandler.createMainFolder(requireContext(), null)
            }
        }
    }

    /**
     * if the Wear Os sensors are available - show them
     * Otherwise remove them, if they are available
     */
    private fun setObservers() {
        mainViewModel.wearOsContacted.observe(requireActivity(), { info ->
            if (info.isNotEmpty()) {
                createWearViews(info)
            }else{
                removeWearOs()
            }
        })
    }

    /**
     * inflate views for the Wear Os
     * data are obtained from Enumeration of basic sensor
     * @param info - hashMap <if of the sensor, list with attributes>
     */
    private fun createWearViews(info: HashMap<Int, List<String>>) {
        val sensorViewHandler: SensorViewHandler = mainViewModel.getSensorView()

        for (key in info.keys) {
            val properties = info[key]
            val sensorNeeds = getSensorByIdWearOs(properties!![0].toInt())
            val view = inflateOneSensor(sensorNeeds, true)

            wearOsViews.add(view)
            sensorViewHandler.addWearOsSensor(sensorNeeds)

            // handling the button to add or remove to sensor for measurement
            // sensorViewHandler manages all sensors to measure
            // specific object for Wear Os sensors
            val imageButton = view.findViewById<ImageButton>(R.id.sensorrow_save)
            imageButton.setOnClickListener {
                sensorViewHandler.sensorsWearOsToRecord[sensorNeeds.id] =
                    !sensorViewHandler.sensorsWearOsToRecord[sensorNeeds.id]!!
                if (sensorViewHandler.sensorsWearOsToRecord[sensorNeeds.id]!!) {
                    imageButton.setImageResource(R.drawable.ic_ok)
                } else {
                    imageButton.setImageResource(R.drawable.ic_circle)
                }
                checkSensorsToMeasure()
            }

            // for refresh reasons - button is updated by sensorViewHandler from ViewModel
            if (sensorViewHandler.sensorsWearOsToRecord[sensorNeeds.id]!!) {
                imageButton.setImageResource(R.drawable.ic_ok)
            } else {
                imageButton.setImageResource(R.drawable.ic_circle)
            }
        }
    }

    /**
     * Gathers all the sensors, inflates views and manages buttons
     */
    private fun initSensorViews() {
        val sensorsNeeds: ArrayList<SensorNeeds> = getSensors(requireContext())
        val imageButtons: ArrayList<ImageButton> = inflateSensorsViews(sensorsNeeds)
        val sensorViewHandler: SensorViewHandler = mainViewModel.getSensorView()

        for (i in 0 until imageButtons.size) {

            // handling the button to add or remove to sensor for measurement
            // sensorViewHandler manages all sensors to measure
            imageButtons[i].setOnClickListener {
                sensorViewHandler.sensorsToRecord[sensorsNeeds[i].id] =
                    !sensorViewHandler.sensorsToRecord[sensorsNeeds[i].id]!!
                if (sensorViewHandler.sensorsToRecord[sensorsNeeds[i].id]!!) {
                    imageButtons[i].setImageResource(R.drawable.ic_ok)
                } else {
                    imageButtons[i].setImageResource(R.drawable.ic_circle)
                }
                checkSensorsToMeasure()
            }

            // for refresh reasons - button is updated by sensorViewHandler from ViewModel
            if (sensorViewHandler.sensorsToRecord[sensorsNeeds[i].id]!!) {
                imageButtons[i].setImageResource(R.drawable.ic_ok)
            } else {
                imageButtons[i].setImageResource(R.drawable.ic_circle)
            }
        }
    }

    /**
     *
     * Next step of inflation of the view - buttons are gathered, because they need to be configured
     * @param sensorsNeeds - list of all available sensors - SensorNeeds objects
     * @return list of buttons
     */
    private fun inflateSensorsViews(sensorsNeeds: ArrayList<SensorNeeds>): ArrayList<ImageButton> {

        val sensorButtons: ArrayList<ImageButton> = ArrayList()
        for (sensorNeeds: SensorNeeds in sensorsNeeds) {

            val view = inflateOneSensor(sensorNeeds, false)
            sensorButtons.add(view.findViewById(R.id.sensorrow_save))
        }

        return sensorButtons
    }

    /**
     * inflates one sensor to LinearLayout - used for the Phone sensors and Wear Os sensors
     *
     * @param sensorNeeds - requirements for the sensor
     * @param wearOs - if the view belongs to Wear Os
     * @return - created view
     */
    private fun inflateOneSensor(sensorNeeds: SensorNeeds, wearOs: Boolean): View {
        val resourceName = sensorNeeds.name
        val resources = SensorResources.valueOf(resourceName)
        val view = layoutInflater.inflate(R.layout.item_layout_sensorrow, null)
        val icon: ImageView = view.findViewById(R.id.sensorrow_icon)
        val textView: TextView = view.findViewById(R.id.sensorrow_info_title)

        val infoButton: ImageButton = view.findViewById(R.id.sensorrow_info)

        if(wearOs){ // Wear Os has its own displayer
            infoButton.setOnClickListener {
                val action: NavDirections = HomeFragmentDirections.actionNavHomeToNavWearOsInfo(resourceName)
                Navigation.findNavController(requireView()).navigate(action)
            }
        }else{
            infoButton.setOnClickListener {
                val action: NavDirections = HomeFragmentDirections.homeInfoAction(resourceName)
                Navigation.findNavController(requireView()).navigate(action)
            }
        }


        icon.setImageResource(resources.icon)
        textView.setText(resources.title)

        container?.addView(view)
        return view
    }

    /**
     * If the GPS is available, then it is added to sensors
     * Has its own place among the senors
     *
     */
    private fun initGPSView() {
        if(requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){

            val sensorViewHandler = this.mainViewModel.getSensorView()

            val view = layoutInflater.inflate(R.layout.item_layout_sensorrow, null)
            val icon: ImageView = view.findViewById(R.id.sensorrow_icon)
            val textView: TextView = view.findViewById(R.id.sensorrow_info_title)
            val imageButton: ImageButton = view.findViewById(R.id.sensorrow_save)
            val infoButton: ImageButton = view.findViewById(R.id.sensorrow_info)

            infoButton.setOnClickListener {
                val action: NavDirections = HomeFragmentDirections.homeInfoAction(SensorNeeds.GPS)
                Navigation.findNavController(requireView()).navigate(action)
            }
            icon.setImageResource(R.drawable.ic_gps)
            textView.setText(R.string.gps)

            if (sensorViewHandler.gpsMeasurement) {
                imageButton.setImageResource(R.drawable.ic_ok)
            } else {
                imageButton.setImageResource(R.drawable.ic_circle)
            }

            imageButton.setOnClickListener {
                sensorViewHandler.gpsMeasurement = !sensorViewHandler.gpsMeasurement
                if (sensorViewHandler.gpsMeasurement) {
                    imageButton.setImageResource(R.drawable.ic_ok)
                } else {
                    imageButton.setImageResource(R.drawable.ic_circle)
                }
                checkSensorsToMeasure()
            }

            container?.addView(view)
        }
    }

    /**
     * Inits all the checks before measurement takes place and sends the MeasurementService intent
     * to start service and MeasurementActivity
     *
     */
    open fun initMainButton() {
        mainButton?.setOnClickListener {

            val status = mainViewModel.wearOsStatus.value
            if(status is WearOsStates.Status){ // stops, if the wear os is measuring
                if(status.running){
                    Toast.makeText(requireContext(), getString(R.string.wear_os_active), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }
            // intent, sensors gathering and folder generation
            val serviceIntent = Intent(requireContext(), MeasurementService::class.java)
            val sensorViewModel = mainViewModel.getSensorView()
            val folderName = MeasurementService.generateFolderName(MeasurementService.ENDLESS)

            // Wear Os - sends message to Wearable to start measurement
            val wearOsSensors = mainViewModel.getSensorView().getWearOsToMeasure()
            wearOsSensors?.let {sensors -> mainViewModel.startWearOsMeasurement(requireContext(), folderName, sensors) }

            MeasurementService.addExtraToIntentBasic(
                serviceIntent,
                false, // false - Phone storage, true - Wearable storage
                sensorViewModel.getSensorsToMeasure(), // phone sensors
                sensorViewModel.gpsMeasurement, // GPS
                folderName,
                wearOsSensors // Wear Os sensors - can be null
            )

            val mainActivity = requireActivity() as MainActivity
            mainActivity.finish()

            val activityIntent = Intent(requireContext(), MeasurementActivity::class.java)
            activityIntent.putExtras(serviceIntent)

            ContextCompat.startForegroundService(requireContext(), serviceIntent)
            startActivity(activityIntent)
        }
    }

    /**
     * removes WearOs views
     *
     */
    private fun removeWearOs(){
        for(v in wearOsViews){
            container?.removeView(v)
        }
       wearOsViews.clear()
       checkSensorsToMeasure()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeWearOs()

        container = null
        mainButton = null
    }

    /**
     * Makes main button available, if there is sensor to measure
     *
     */
    private fun checkSensorsToMeasure() {
        if (mainViewModel.getSensorView().isSomethingToMeasure()) {
            mainButton?.alpha = 1f
            mainButton?.isClickable = true
            mainButton?.isEnabled = true
        } else {
            mainButton?.alpha = 0.5f
            mainButton?.isClickable = false
            mainButton?.isEnabled = false
        }
    }
}