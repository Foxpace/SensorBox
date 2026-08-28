package com.tomasrepcik.sensorbox.presentation.phonelaunch

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.presentation.WearListScreen
import com.tomasrepcik.sensorbox.presentation.WearPageTitle
import com.tomasrepcik.sensorbox.presentation.WearPrimaryButton
import com.tomasrepcik.sensorbox.presentation.WearSectionTitle

@Composable
fun PhoneLaunchScreen(state: PhoneLaunchState, onLaunchPhone: () -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.activity_info_phone), transformation) }
        item { WearSectionTitle(stringResource(statusText(state.status)), transformation) }
        item {
            WearPrimaryButton(
                label = stringResource(R.string.open_phone),
                transformation = transformation,
                onClick = onLaunchPhone,
                enabled = state.isPhoneConnected && state.status != PhoneLaunchStatus.LAUNCHING,
            )
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
