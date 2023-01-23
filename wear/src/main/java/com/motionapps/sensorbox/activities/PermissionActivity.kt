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
import androidx.appcompat.app.AppCompatActivity
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


class PermissionActivity : AppCompatActivity(), WearOsListener {

    private var permission: String? = null
    private val wearOsHandler = WearOsHandler()
    private var present = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        job = CoroutineScope(Dispatchers.Main).launch {
            wearOsHandler.searchForWearOs(this@PermissionActivity, this@PermissionActivity, WearOsConstants.PHONE_APP_CAPABILITY)
        }

        // in case of empty intent
        findViewById<Button>(R.id.permission_button).apply {
            setOnClickListener {
                askForPermissions()
            }
        }

        permission = intent.getStringExtra(PERMISSION_KEY)
        if(permission != null){
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }

    }

    private fun askForPermissions() {

        val permissions = arrayListOf<String>()

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.addAll(
                    arrayOf(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
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

        val arrayOfPermissions = Array(size = permissions.size) { "" }
        permissions.forEachIndexed { i, permission -> arrayOfPermissions[i] = permission }

        ActivityCompat.requestPermissions(
            this, arrayOfPermissions, PERMISSION_REQUEST_CODE
        )

    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if(wearOsStates is WearOsStates.PresenceResult){
            present = wearOsStates.present
            permission = intent.getStringExtra(PERMISSION_KEY)
            if(permission != null && present){ // send message to phone about permission, if available
                if(ActivityCompat.checkSelfPermission(this, permission!!) == PackageManager.PERMISSION_GRANTED){
                    wearOsHandler.sendMsg(
                        this,
                        WearOsConstants.PHONE_MESSAGE_PATH,
                        "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};1"
                    )
                    goHome()
                }else{
                    wearOsHandler.sendMsg(
                        this,
                        WearOsConstants.PHONE_MESSAGE_PATH,
                        "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};0"
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_CODE){

            for (position in permissions.indices){
                if(permissions[position] == Manifest.permission.BODY_SENSORS){
                    val rational = shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS)
                    // send message back to phone for body sensors - it is needed
                    if(grantResults[position] == PackageManager.PERMISSION_DENIED){
                        wearOsHandler.sendMsg(
                            this,
                            WearOsConstants.PHONE_MESSAGE_PATH,
                            "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};1"
                        )

                        if(!rational){ // show settings if user clicks deny and do not ask together
                            showSettings(this)
                            return
                        }
                    }else{
                        if(present){
                            wearOsHandler.sendMsg(
                                this,
                                WearOsConstants.PHONE_MESSAGE_PATH,
                                "${WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED};0"
                            )
                        }
                    }
                }

                // show settings if user clicks deny and do not ask together - for location
                if(permissions[position] == Manifest.permission.ACCESS_FINE_LOCATION || permissions[position] == Manifest.permission.ACCESS_BACKGROUND_LOCATION){
                    val rational = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }else{
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    if(grantResults[position] == PackageManager.PERMISSION_DENIED && !rational){
                        showSettings(this)
                        return
                    }
                }
            }
        }

        // if everything is ok, go to home screen
        goHome()
    }

    private fun goHome(){
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

    companion object{
        const val PERMISSION_KEY = "PERMISSION"
        private const val PERMISSION_REQUEST_CODE = 589

        fun showSettings(activity: Activity){
            activity.finish()
            Toasty.warning(activity, R.string.permission_toast, Toasty.LENGTH_LONG, true).show()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
    }
}