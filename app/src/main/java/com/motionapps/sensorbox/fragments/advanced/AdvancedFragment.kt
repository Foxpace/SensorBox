package com.motionapps.sensorbox.fragments.advanced

import android.os.Build
import androidx.core.content.ContextCompat
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.HomeFragment
import com.motionapps.sensorbox.fragments.HomeFragmentDirections
import com.motionapps.sensorservices.handlers.StorageHandler
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
            if (!StorageHandler.isAccess(requireContext())) {
                if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
                    Navigation.findNavController(requireView()).navigate(
                        HomeFragmentDirections.actionNavHomeToPickFolderFragment(
                            ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                        )
                    )
                    return@setOnClickListener
                } else if(Build.VERSION_CODES.M <= Build.VERSION.SDK_INT && permissionHandler != null){
                    permissionHandler?.showDialogStorage(this)
                    return@setOnClickListener
                }
            }
            val action: NavDirections = AdvancedFragmentDirections.actionAdvancedToPicker()
            Navigation.findNavController(requireView()).navigate(action)
        }
    }
}