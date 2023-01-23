package com.motionapps.sensorbox.permissions


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.os.Build
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import com.afollestad.materialdialogs.MaterialDialog
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.HomeFragmentDirections
import com.motionapps.sensorbox.uiHandlers.StorageFunctions
import com.motionapps.sensorservices.types.SensorNeeds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class PermissionHandler(fragment: Fragment) {

    var dialog: MaterialDialog? = null

    private val storageCallback = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            StorageFunctions.checkMainFolder(fragment.requireContext(), fragment.requireView())
            fragment.requireView().findViewById<Button>(R.id.home_mainbutton).callOnClick()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val action: NavDirections =
                    HomeFragmentDirections.actionNavHomeToPickFolderFragment(Color.BLACK)
                Navigation.findNavController(fragment.requireView()).navigate(action)
            } else {
                showDialogStorage(fragment)
            }
        }
    }

    private val gpsCallback = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val action: NavDirections = HomeFragmentDirections.homeInfoAction(SensorNeeds.GPS)
            Navigation.findNavController(fragment.requireView()).navigate(action)
        } else {
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showDialogFineLocation(fragment)
            } else {
                PermissionSettingsDialog.showSettings(fragment.requireContext())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val notificationHandler = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            StorageFunctions.checkMainFolder(fragment.requireContext(), fragment.requireView())
            fragment.requireView().findViewById<Button>(R.id.home_mainbutton).callOnClick()
        } else {
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showDialogNotification(fragment)
            } else {
                PermissionSettingsDialog.showSettings(fragment.requireContext())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val gpsCallbackS = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                val action: NavDirections = HomeFragmentDirections.homeInfoAction(SensorNeeds.GPS)
                Navigation.findNavController(fragment.requireView()).navigate(action)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                val action: NavDirections = HomeFragmentDirections.homeInfoAction(SensorNeeds.GPS)
                Navigation.findNavController(fragment.requireView()).navigate(action)
            }
            else -> {
                // permission is denied
                if (permissions.keys.any { fragment.shouldShowRequestPermissionRationale(it) }) {
                    showDialogFineLocation(fragment)
                } else {
//              R.string.permission_gps_show,
                    PermissionSettingsDialog.showSettings(fragment.requireContext())
                }
            }
        }
    }

    private val bodySensors = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted && fragment.shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS)) {
            showDialogBodySensors(fragment)
        } else {
//          R.string.heart_rate_permission_rational,
            PermissionSettingsDialog.showSettings(fragment.requireContext())
        }
    }

    fun showDialogStorage(fragment: Fragment) {
        dialog = PermissionSettingsDialog.showPermissionRational(
            permissionCallback = storageCallback,
            fragment = fragment,
            messageText = R.string.permission_storage,
            permission = Manifest.permission.READ_EXTERNAL_STORAGE,
            icon = R.drawable.ic_baseline_folder,
        )
    }

    fun checkGPSPermission(fragment: Fragment): Boolean {
        if (!isAnyGpsPermission(fragment.requireContext())) {
            showDialogFineLocation(fragment)
            return false
        }
        return true
    }

    fun checkNotificationPermission(fragment: Fragment): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        if (ActivityCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showDialogNotification(fragment)
            return false
        }
        return true
    }

    private fun isAnyGpsPermission(context: Context): Boolean {
        val coarse = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return coarse || fine
    }

    /**
     * Creates prominent disclosure for GPS for apps - modified for Android S
     * Asks for fine location for Android below SDK 31
     *
     * @return material dialog
     */
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun showDialogFineLocation(fragment: Fragment) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            dialog = PermissionSettingsDialog.showPermissionRational(
                permissionCallback = gpsCallback,
                fragment = fragment,
                messageText = R.string.permission_gps,
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                icon = R.drawable.ic_baseline_location,
            )
        } else {
            dialog = PermissionSettingsDialog.showPermissionRational(
                permissionCallback = gpsCallbackS,
                fragment = fragment,
                messageText = R.string.permission_gps,
                permissions = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                icon = R.drawable.ic_baseline_location,
            )
        }

    }

    /**
     * Creates prominent disclosure for the notification for Android 13 - SDK 33 and higher
     * @return material dialog
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun showDialogNotification(fragment: Fragment) {
        dialog = PermissionSettingsDialog.showPermissionRational(
            permissionCallback = notificationHandler,
            fragment = fragment,
            messageText = R.string.permission_notification,
            permission = Manifest.permission.POST_NOTIFICATIONS,
            icon = R.drawable.ic_bell,
        )
    }


    fun checkHeartRateSensor(fragment: Fragment, sensorId: Int): Boolean {
        if (sensorId == Sensor.TYPE_HEART_RATE && ActivityCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showDialogBodySensors(fragment)
            return true
        }
        return false
    }

    private fun showDialogBodySensors(fragment: Fragment) {
        dialog = MaterialDialog(fragment.requireContext()).show {
            title(R.string.heart_rate_permission_title)
            message(R.string.heart_rate_permission_text)
            cancelable(true)
            cancelOnTouchOutside(false)
            cornerRadius(16f)
            positiveButton(R.string.show_permission) {
                bodySensors.launch(Manifest.permission.BODY_SENSORS)
                dismiss()
            }
        }
    }

    fun onDestroy() {
        dialog?.dismiss()
        dialog = null
    }

}