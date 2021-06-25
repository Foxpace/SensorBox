package com.motionapps.sensorbox.permissions


import android.Manifest
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

    @RequiresApi(Build.VERSION_CODES.M)
    val storageCallback = fragment.registerForActivityResult(
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

    @RequiresApi(Build.VERSION_CODES.M)
    val gpsCallback = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val action: NavDirections = HomeFragmentDirections.homeInfoAction(SensorNeeds.GPS)
            Navigation.findNavController(fragment.requireView()).navigate(action)
        } else {
            if (fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showDialogFineLocation(fragment)
            } else {
//              R.string.permission_gps_show,
                PermissionSettingsDialog.showSettings(fragment.requireContext())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    val bodySensors = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted && fragment.shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS)) {
            showDialogBodySensors(fragment)
        } else {
//          R.string.heart_rate_permission_rational,
            PermissionSettingsDialog.showSettings(fragment.requireContext())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    fragment.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showDialogFineLocation(fragment)
                return true
            }
        }
        return false
    }


    /**
     * Creates prominent disclosure for GPS for apps under Android Q
     *
     * @return material dialog
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun showDialogFineLocation(fragment: Fragment) {
        dialog = PermissionSettingsDialog.showPermissionRational(
            permissionCallback = gpsCallback,
            fragment = fragment,
            messageText = R.string.permission_gps,
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            icon = R.drawable.ic_baseline_location,
        )
    }

//    /**
//     * Creates prominent disclosure for GPS for apps for Android Q and above
//     *
//     * @return material dialog
//     */
//    @RequiresApi(Build.VERSION_CODES.Q)
//    @ExperimentalCoroutinesApi
//    @InternalCoroutinesApi
//    fun showDialogBackgroundLocation(
//        fragment: Fragment,
//    ) {
//        dialog = PermissionSettingsDialog.showPermissionSettings(
//            fragment = fragment,
//            messageText = R.string.permission_gps_android_Q,
//            icon = R.drawable.ic_baseline_location,
//        )
//    }

    fun checkHeartRateSensor(fragment: Fragment, sensorId: Int): Boolean {
        if (sensorId == Sensor.TYPE_HEART_RATE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && ActivityCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showDialogBodySensors(fragment)
            return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    fun showDialogBodySensors(fragment: Fragment) {
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