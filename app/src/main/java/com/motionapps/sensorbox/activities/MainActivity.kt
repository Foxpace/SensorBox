package com.motionapps.sensorbox.activities

import android.app.Dialog
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.navigation.NavigationView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.settings.SettingsFragment
import com.motionapps.sensorbox.fragments.settings.SettingsFragment.Companion.POLICY_AGREED
import com.motionapps.sensorbox.intro.IntroActivity
import com.motionapps.sensorbox.viewmodels.MainViewModel
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.wearoslib.WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED
import com.motionapps.wearoslib.WearOsConstants.WEAR_HEART_RATE_PERMISSION_REQUIRED_BOOLEAN
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_SENSOR_INFO
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_SENSOR_INFO_EXTRA
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS
import com.motionapps.wearoslib.WearOsConstants.WEAR_STATUS_EXTRA
import com.motionapps.wearoslib.WearOsStates
import com.motionapps.wearoslib.WearOsSyncService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*


/**
 * Covers all the main functionality
 * Composes different fragments like Home, Advanced, Settings, About, ...
 * Connected to MainViewModel for other functions - covers functions for advanced options too
 * Uses hilt as dependency injection framework
 */

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // manages all the fragments
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val mainViewModel: MainViewModel by viewModels()

    private var wearOsJob: Job? = null // coroutine for Wear Os interaction
    private var wearOsMenuItemPresence: MenuItem? = null // items in AppBar for Wear Os interaction
    private var wearOsMenuItemSync: MenuItem? = null
    private var dialog: Dialog? = null
    private var materialDialog: MaterialDialog? = null

    private val connectionMeasurementService: ServiceConnection = object : ServiceConnection{
        /**
         * Checks if the measurement is ongoing, If yes -> switch to MeasurementActivity,
         * otherwise nothing
         *
         * @param componentName
         * @param binder
         */
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            (binder as MeasurementService.MeasurementBinder).also {
                val service: MeasurementService = it.getService()
                if(service.running){
                    switchToMeasurementActivity(service)
                }else{
                    unbindService(this)
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {}
    }

    /**
     * Starts MeasurementActivity to check on proceeding measurement
     * Adds all the necessary info to intent by intent itself or from the service
     * @param service : MeasurementService from binder
     */
    private fun switchToMeasurementActivity(service: MeasurementService){
        val intent = Intent(this@MainActivity, MeasurementActivity::class.java)
        if(service.intent != null){
            intent.putExtras(service.intent!!)
        }else{
            intent.putExtra(MeasurementService.ANDROID_SENSORS, service.paramSensorId)
            intent.putExtra(MeasurementService.TYPE, service.paramType)
        }
        finish()
        startActivity(intent)
    }

    private var wearOsReceiverB = false // receiverRegistered
    private val wearOsReceiver = object : BroadcastReceiver(){
        /**
         * Receiver for intents sent by MessageListener for Wear Os communication
         * Covers Status of the Wear Os device and sensor info
         * Data are passed to ViewModel
         * @param context
         * @param intent
         */
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.action?.let {
                when(it){
                    WEAR_SEND_SENSOR_INFO -> { // SensorInfo are divided by "\n" and specific parameters by "|"
                        mainViewModel.onWearOsProperties(
                            context, intent.getStringExtra(
                                WEAR_SEND_SENSOR_INFO_EXTRA
                            )
                        )
                        wearOsMenuItemPresence?.setIcon(R.drawable.ic_wear_os_on)
                    }
                    WEAR_STATUS -> {
                        // Wear status is one line, which consists number of
                        // measurements, files total size, number of files, and if the measurement
                        // is running on the wearable
                        mainViewModel.onWearOsStatus(intent.getStringExtra(WEAR_STATUS_EXTRA))
                    }
                    WEAR_HEART_RATE_PERMISSION_REQUIRED -> {
                        // hear rate sensor needs permission in order to get data - this
                        mainViewModel.onWearOsHearRatePermissionRequired(
                            intent.getBooleanExtra(
                                WEAR_HEART_RATE_PERMISSION_REQUIRED_BOOLEAN,
                                false
                            )
                        )
                    }

                    MeasurementService.RUNNING -> {
                        Toast.makeText(context, "Measuring", Toast.LENGTH_LONG).show()
                    }
                    else ->{
                        // empty
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkFirstUsage()

        // UI stuff
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // drawer set up
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_advanced,
                R.id.nav_settings,
                R.id.nav_about
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    /**
     * Check if the user is using app for the first time by sharedPreferences attribute
     */
    private fun checkFirstUsage(){

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if(!preferences.getBoolean(SettingsFragment.APP_FIRST_TIME, false)){
            finish()
            Intent(this@MainActivity, IntroActivity::class.java).apply {
                startActivity(this)
            }
            return
        }
    }

    override fun onResume() {
        super.onResume()
        setUp()
        checkPolicy()
    }

    /**
     * check, whether the user has agreed to privacy policy
     *
     */
    private fun checkPolicy() {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
        if (!sharedPreferences.getBoolean(POLICY_AGREED, false)) {
            materialDialog = MaterialDialog(this).show {
                title(R.string.intro_policy_button)
                message(R.string.dialog_privacy_policy)
                cornerRadius(16f)
                cancelable(false)
                cancelOnTouchOutside(false)
                positiveButton(R.string.agree) {
                    it.dismiss()
                    val editor = sharedPreferences.edit()
                    editor.putBoolean(POLICY_AGREED, true)
                    editor.apply()
                }
                negativeButton(R.string.intro_policy_button) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.link_privacy_policy))
                    )
                    startActivity(browserIntent)
                }
            }
        }
    }

    /**
     * Binds to service to checks, if the measurement is present, permissions check and sets up
     * Wear Os receiver
     */
    private fun setUp(){

        Intent(this, MeasurementService::class.java).also { intent ->
            bindService(intent, connectionMeasurementService, Context.BIND_AUTO_CREATE)
        }

        registerWearOsReceiver()

    }

    /**
     * Registers WearOs intents from MsgListener
     *
     */
    private fun registerWearOsReceiver(){
        if(!wearOsReceiverB){
            val intentFilter = IntentFilter()
            intentFilter.addAction(WEAR_SEND_SENSOR_INFO)
            intentFilter.addAction(WEAR_STATUS)
            intentFilter.addAction(WEAR_HEART_RATE_PERMISSION_REQUIRED)
            registerReceiver(wearOsReceiver, intentFilter)
            wearOsReceiverB = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if(wearOsReceiverB){
            unregisterReceiver(wearOsReceiver)
            wearOsReceiverB = false
        }

        mainViewModel.onDestroy()

        dialog?.dismiss()
        dialog = null

        materialDialog?.dismiss()
        materialDialog = null

        wearOsJob?.cancel()

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Inflation of Wear Os buttons to AppBar
     * @param menu
     * @return
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        wearOsMenuItemPresence = menu.findItem(R.id.wear_os_action_present)
        wearOsMenuItemSync = menu.findItem(R.id.wear_os_action_sync)
        checkWearOs()
        return true
    }

    /**
     * Set up of observers of LiveData in MainViewModel
     *
     */
    private fun checkWearOs() {

        // show Wear Os button
        mainViewModel.wearOsPresence.observe(this, { wearOsState ->
            when (wearOsState) {
                is WearOsStates.PresenceResult -> {
                    wearOsMenuItemPresence?.isEnabled = wearOsState.present
                    wearOsMenuItemPresence?.isVisible = wearOsState.present
                }
                else -> {
                }
            }
        })

        // Wear Os sensors synced
        mainViewModel.wearOsContacted.observe(this, { data ->
            if (data.isNullOrEmpty()) {
                wearOsMenuItemPresence?.setIcon(R.drawable.ic_wear_os_off)
                wearOsMenuItemPresence?.isEnabled = true


            } else {
                wearOsMenuItemPresence?.setIcon(R.drawable.ic_wear_os_on)
                wearOsMenuItemPresence?.isEnabled = true
            }
        })

        // status of Wear Os and reaction to it
        mainViewModel.wearOsStatus.observe(this, { status ->
            when (status) {
                is WearOsStates.AwaitResult -> {
                    wearOsMenuItemPresence?.isEnabled = false
                }

                is WearOsStates.Offline -> {
                    wearOsMenuItemSync?.isEnabled = false
                    wearOsMenuItemSync?.isVisible = false
                }

                is WearOsStates.Status -> {
                    if (!status.running && status.totalNumberOfFiles > 0) {
                        wearOsMenuItemSync?.isEnabled = true
                        wearOsMenuItemSync?.isVisible = true
                    } else {
                        wearOsMenuItemSync?.isEnabled = false
                        wearOsMenuItemSync?.isVisible = false
                    }
                }
                else -> {
                }
            }
        })

        // launches search for the Wear Os device
        wearOsJob = CoroutineScope(Dispatchers.IO).launch {
            mainViewModel.getWearPresenceAndStatus(this@MainActivity)
        }
    }

    /**
     * Manages Wear Os buttons
     *
     * @param item
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.wear_os_action_present -> {
            mainViewModel.onWearPresentClick(this)
            true
        }
        R.id.wear_os_action_sync -> {
            Intent(this, WearOsSyncService::class.java).also { intent ->
                bindService(intent, object : ServiceConnection { // checks if sync is not running
                    override fun onServiceConnected(
                        componentName: ComponentName?,
                        binder: IBinder?
                    ) {
                        if (!(binder as WearOsSyncService.WearOsSyncServiceBinder).getService().running) {
                            mainViewModel.onWearSyncClick(this@MainActivity)?.let {
                                dialog = it
                            }
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.wear_os_sync_in_progress),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        unbindService(this)
                    }

                    override fun onServiceDisconnected(componentName: ComponentName?) {}
                }, Context.BIND_AUTO_CREATE)
            }
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}