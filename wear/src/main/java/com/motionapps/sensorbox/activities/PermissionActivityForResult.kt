package com.motionapps.sensorbox.activities

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.motionapps.sensorbox.R

class PermissionActivityForResult : AppCompatActivity() {

    private lateinit var permissionPick: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        findViewById<TextView>(R.id.permission_text).apply {
            text = getString(R.string.permission_show_gps_only)
        }

        if(intent.extras == null){
            goBack()
        }else{
            permissionPick = intent.extras!!.getString(GPS_INTENT, GPS_SHOW)
        }

        findViewById<TextView>(R.id.permission_button).setOnClickListener{

            if(permissionPick == GPS_SHOW || Build.VERSION_CODES.Q > Build.VERSION.SDK_INT){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), GPS_REQUEST)
            }else{
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    GPS_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == GPS_REQUEST){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                goBack()
            }else{
                val rational = shouldShowRequestPermissionRationale(permissions[0])

                if(!rational){
                    PermissionActivity.showSettings(this)
                }
            }
        }
    }

    private fun goBack(){
        if(permissionPick == GPS_SHOW || Build.VERSION_CODES.Q > Build.VERSION.SDK_INT){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                setResult(Activity.RESULT_OK)
            }else{
                setResult(Activity.RESULT_CANCELED)
            }
        }else{
            if(checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED){
                setResult(Activity.RESULT_OK)
            }else{
                setResult(Activity.RESULT_CANCELED)
            }
        }
        finish()
    }

    companion object{

        private const val GPS_REQUEST = 126

        const val GPS_INTENT = "GPS_INTENT"
        const val GPS_SHOW = "GPS_SHOW"
        const val GPS_LOG = "GPS_LOG"
    }
}