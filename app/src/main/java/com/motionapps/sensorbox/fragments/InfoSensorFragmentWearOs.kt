package com.motionapps.sensorbox.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.motionapps.sensorbox.fragments.displayers.SensorWearOsDisplayer
import com.motionapps.sensorbox.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@AndroidEntryPoint

/**
 * Same as InfoSensorFragment but with Wear Os
 * Stops sending of the Wear Os data onDestroy
 *
 */
class InfoSensorFragmentWearOs : Fragment() {

    val args: InfoSensorFragmentWearOsArgs by navArgs()
    private val mainViewModel: MainViewModel by viewModels(ownerProducer = {requireActivity()})

    private lateinit var sensorWearOsDisplayer: SensorWearOsDisplayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sensorWearOsDisplayer = SensorWearOsDisplayer(mainViewModel)
        return sensorWearOsDisplayer.getView(requireContext(), inflater, container, args)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.stopWearOsSensor(requireContext())
        sensorWearOsDisplayer.onDestroy()
    }
}