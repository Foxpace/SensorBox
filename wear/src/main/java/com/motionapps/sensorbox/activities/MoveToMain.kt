package com.motionapps.sensorbox.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motionapps.sensorbox.presentation.phonelaunch.PhoneLaunchIntent
import com.motionapps.sensorbox.presentation.phonelaunch.PhoneLaunchScreen
import com.motionapps.sensorbox.presentation.phonelaunch.PhoneLaunchViewModel
import com.motionapps.sensorbox.ui.theme.WearSensorBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoveToMain : ComponentActivity() {
    private val viewModel: PhoneLaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            WearSensorBoxTheme {
                PhoneLaunchScreen(
                    state = state,
                    onLaunchPhone = { viewModel.accept(PhoneLaunchIntent.LaunchRequested) },
                )
            }
        }
    }
}
