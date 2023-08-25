package com.motionapps.sensorbox.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.motionapps.sensorbox.R

class PermissionActivityForResult : ComponentActivity() {

    private lateinit var permissionPick: String

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    private fun onPermissionResult(granted: Boolean?) {
        if (granted != null && granted) {
            goBack()
        } else {
            val rational =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            if (!rational) {
                PermissionActivity.showSettings(this)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        if (intent.extras == null) {
            goBack()
        } else {
            permissionPick = intent.extras!!.getString(GPS_INTENT, GPS_SHOW)
        }


        findViewById<TextView>(R.id.permission_text).apply {
            text = getString(R.string.permission_gps)
        }


        findViewById<TextView>(R.id.permission_button).setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun goBack() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {

        const val GPS_INTENT = "GPS_INTENT"
        const val GPS_SHOW = "GPS_SHOW"
        const val GPS_LOG = "GPS_LOG"
    }
}