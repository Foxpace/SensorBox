package com.motionapps.sensorbox.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.motionapps.sensorbox.fragments.displayers.GPSDisplayer
import com.motionapps.sensorbox.fragments.displayers.SensorDisplayer
import com.motionapps.sensorservices.types.SensorNeeds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint
/**
 * used to show sensor attributes or GPS
 * assisted mainly by displayers
 * Android sensor / GPS only, Wear Os has its own implementation
 */
class InfoSensorFragment : Fragment() {

    private val args: InfoSensorFragmentArgs by navArgs()

    @Inject
    lateinit var gpsDisplayer: GPSDisplayer

    @Inject
    lateinit var sensorDisplayer: SensorDisplayer


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return when(args.type){
            SensorNeeds.GPS -> {
                gpsDisplayer.getView(requireContext(), inflater, container, args)
            }
            else -> {
                sensorDisplayer.getView(requireContext(), inflater, container, args)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorDisplayer.onDestroy()
        gpsDisplayer.onDestroy()
    }
}