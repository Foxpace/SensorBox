package com.motionapps.sensorbox.presentation.phonelaunch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.motionapps.sensorbox.R

@Composable
fun PhoneLaunchScreen(state: PhoneLaunchState, onLaunchPhone: () -> Unit) {
    AppScaffold {
        ScreenScaffold {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(statusText(state.status)))
                Button(
                    label = { Text(stringResource(R.string.open_phone)) },
                    onClick = onLaunchPhone,
                    enabled = state.isPhoneConnected && state.status != PhoneLaunchStatus.LAUNCHING,
                )
            }
        }
    }
}

private fun statusText(status: PhoneLaunchStatus): Int = when (status) {
    PhoneLaunchStatus.IDLE -> R.string.phone_ready
    PhoneLaunchStatus.PHONE_UNAVAILABLE -> R.string.norespond
    PhoneLaunchStatus.LAUNCHING -> R.string.open_module
    PhoneLaunchStatus.SENT -> R.string.phone_opened
    PhoneLaunchStatus.FAILED -> R.string.error_toast
}
