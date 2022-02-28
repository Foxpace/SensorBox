package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.HomeFragmentDirections
import com.motionapps.sensorbox.permissions.PermissionHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import kotlinx.coroutines.*


object StorageFunctions {
    /**
     * Checks if the main folder exists
     * in Android Oreo, is required permission from the user, so he is prompted to pick folder, if
     * the saved one is not available
     * in lower versions, this is automatic
     */
    fun checkMainFolder(context: Context, fragmentView: View) {
        if (!StorageHandler.isFolder(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Navigation.findNavController(fragmentView).navigate(
                    HomeFragmentDirections.actionNavHomeToPickFolderFragment(
                        ContextCompat.getColor(context, R.color.colorPrimaryDark)
                    )
                )
                return
            } else {
                StorageHandler.createMainFolder(context, null)
            }
        }
    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun checkStorageAccess(fragment: Fragment, permissionHandler: PermissionHandler): Boolean{
        if (!StorageHandler.isAccess(fragment.requireContext())) {
            return when {
                Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT -> {
                    Navigation.findNavController(fragment.requireView()).navigate(
                        HomeFragmentDirections.actionNavHomeToPickFolderFragment(
                            ContextCompat.getColor(fragment.requireContext(), R.color.colorPrimaryDark)
                        )
                    )
                    true
                }
                Build.VERSION_CODES.M <= Build.VERSION.SDK_INT -> {
                    permissionHandler.showDialogStorage(fragment)
                    true
                }
                else -> {
                    false
                }
            }
        }
        return false
    }
}