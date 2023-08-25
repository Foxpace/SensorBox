package com.motionapps.sensorbox.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.communication.SensorTools
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsHandler
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class PermissionActivity : ComponentActivity(), WearOsListener {

    private var permission: String? = null
    private val wearOsHandler = WearOsHandler()
    private var present = false
    private var job: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        onPermissionsResult(permissions)
        if (permissions.all { it.value }) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        job = CoroutineScope(Dispatchers.Main).launch {
            wearOsHandler.searchForWearOs(
                this@PermissionActivity,
                this@PermissionActivity,
                WearOsConstants.PHONE_APP_CAPABILITY
            )
        }

        // in case of empty intent
        findViewById<Button>(R.id.permission_button).apply {
            setOnClickListener {
                askForPermissions()
            }
        }

        permission = null
        intent.getStringExtra(PERMISSION_KEY)?.let {
            permission = it
        }
    }

    private fun askForPermissions() {

        val permissions = arrayListOf<String>()

        permission?.let {
            permissions.add(it)
        }

        if (permissions.isEmpty()) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (SensorTools.isHeartRatePermissionRequired(this)) {
                permissions.add(Manifest.permission.BODY_SENSORS)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissions.isEmpty()) {
                goHome()
                return
            }
        }

        val arrayOfPermissions = Array(size = permissions.size) { "" }
        permissions.forEachIndexed { i, permission -> arrayOfPermissions[i] = permission }

        requestPermissionLauncher.launch(permissions.toTypedArray())

    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if (wearOsStates is WearOsStates.PresenceResult) {
            present = wearOsStates.present
            permission = intent.getStringExtra(PERMISSION_KEY)
            if (permission != null && present) { // send message to phone about permission, if available
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission!!
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    wearOsHandler.sendMsg(
                        this,
                        WearOsConstants.PHONE_MESSAGE_PATH,
                        "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};1"
                    )
                    goHome()
                } else {
                    wearOsHandler.sendMsg(
                        this,
                        WearOsConstants.PHONE_MESSAGE_PATH,
                        "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};0"
                    )
                }
            }
        }
    }

    private fun onPermissionsResult(permissions: Map<String, @JvmSuppressWildcards Boolean>) {

        for (permission in permissions.entries) {
            if (permission.key == Manifest.permission.BODY_SENSORS) {
                val rational =
                    shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS)

                // send message back to phone for body sensors - it is needed
                if (permission.value) {
                    if (present) {
                        wearOsHandler.sendMsg(
                            this,
                            WearOsConstants.PHONE_MESSAGE_PATH,
                            "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};1"
                        )
                    }

                } else {
                    if (!rational) { // show settings if user clicks deny and do not ask together
                        showSettings(this)
                        return
                    }

                    if (present) {
                        wearOsHandler.sendMsg(
                            this,
                            WearOsConstants.PHONE_MESSAGE_PATH,
                            "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};0"
                        )
                    }
                }
            }

            // show settings if user clicks deny and do not ask together - for location
            if (permission.key == Manifest.permission.ACCESS_FINE_LOCATION) {
                val rational =
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                if (permission.value && !rational) {
                    showSettings(this)
                    return
                }
            }

            if (permission.key == Manifest.permission.POST_NOTIFICATIONS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                val rational =
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                if (permission.value && !rational) {
                    showSettings(this)
                    return
                }
            }
        }

    }

    private fun goHome() {
        finish()
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }


    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        job = null
    }

    companion object {
        const val PERMISSION_KEY = "PERMISSION"

        fun showSettings(activity: Activity) {
            activity.finish()
            Toasty.warning(activity, R.string.permission_toast, Toasty.LENGTH_LONG, true).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
    }
}