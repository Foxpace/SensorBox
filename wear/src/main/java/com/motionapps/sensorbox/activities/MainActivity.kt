package com.motionapps.sensorbox.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.presentation.dashboard.WearDashboardEffect
import com.motionapps.sensorbox.presentation.dashboard.WearDashboardIntent
import com.motionapps.sensorbox.presentation.dashboard.WearDashboardScreen
import com.motionapps.sensorbox.presentation.dashboard.WearDashboardViewModel
import com.motionapps.sensorbox.presentation.dashboard.WearRoute
import com.motionapps.sensorbox.presentation.menu.WearMenuDestination
import com.motionapps.sensorbox.ui.theme.WearSensorBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: WearDashboardViewModel by viewModels()
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.accept(WearDashboardIntent.PermissionsResolved(result.values.all { it }))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.state.value.route == WearRoute.MENU) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            } else {
                viewModel.accept(WearDashboardIntent.Back)
            }
        }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) { viewModel.effects.collect(::handleEffect) }
            LaunchedEffect(state.preferences.display.keepWearDisplayOn) {
                updateDisplayPolicy(state.preferences.display.keepWearDisplayOn)
            }
            WearSensorBoxTheme {
                WearDashboardScreen(
                    state = state,
                    chartModelProducer = viewModel.chartModelProducer,
                    accept = viewModel::accept,
                )
            }
        }
    }

    private fun handleEffect(effect: WearDashboardEffect) {
        when (effect) {
            is WearDashboardEffect.RequestPermissions -> appResult(
                AppErrorCode.PERMISSION,
                "Request Wear permissions",
            ) {
                permissions.launch(effect.permissions.toTypedArray())
            }

            WearDashboardEffect.OpenPhone -> appResult(
                AppErrorCode.EXTERNAL_ACTION,
                "Open phone launcher",
            ) { startActivity(Intent(this, MoveToMain::class.java)) }

            is WearDashboardEffect.OpenUrl -> openOnPhone(effect.destination)
        }
    }

    private fun openOnPhone(destination: WearMenuDestination) {
        val url = when (destination) {
            WearMenuDestination.PRIVACY -> getString(R.string.link_privacy_policy)
            else -> getString(R.string.link_terms)
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(url.toUri())
        appResult(AppErrorCode.EXTERNAL_ACTION, "Request phone browser") {
            RemoteActivityHelper(this).startRemoteActivity(intent)
        }.onSuccess { request ->
            request.addListener(
                {
                    appResult(AppErrorCode.EXTERNAL_ACTION, "Open phone browser") { request.get() }
                        .fold(
                            onSuccess = {
                                Toast.makeText(this, R.string.open_phone_browser, Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(this, R.string.open_phone_browser_failed, Toast.LENGTH_LONG).show()
                            },
                        )
                },
                ContextCompat.getMainExecutor(this),
            )
        }.onFailure {
            Toast.makeText(this, R.string.open_phone_browser_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateDisplayPolicy(keepDisplayOn: Boolean) {
        if (keepDisplayOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
