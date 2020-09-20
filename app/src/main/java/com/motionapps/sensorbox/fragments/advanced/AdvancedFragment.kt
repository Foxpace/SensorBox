package com.motionapps.sensorbox.fragments.advanced

import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.HomeFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi

/**
 * Same as HomeFragment
 * The click of the button leads to fragment to pick more specific settings
 */
class AdvancedFragment : HomeFragment() {


    override fun initMainButton() {
        mainButton?.setText(R.string.next)
        mainButton?.setOnClickListener {
                val action: NavDirections = AdvancedFragmentDirections.actionAdvancedToPicker()
                Navigation.findNavController(requireView()).navigate(action)
        }
    }


}