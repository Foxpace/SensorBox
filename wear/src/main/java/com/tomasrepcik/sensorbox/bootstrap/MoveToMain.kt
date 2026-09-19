package com.tomasrepcik.sensorbox.bootstrap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme
import com.tomasrepcik.sensorbox.phonelaunch.PhoneLaunchScreen
import com.tomasrepcik.sensorbox.phonelaunch.PhoneLaunchViewModel
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
                    onIntent = viewModel::accept,
                )
            }
        }
    }
}
