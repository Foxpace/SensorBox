package com.tomasrepcik.sensorbox.phonelaunch

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.WearListScreen
import com.tomasrepcik.sensorbox.design.WearPageTitle
import com.tomasrepcik.sensorbox.design.WearPrimaryButton
import com.tomasrepcik.sensorbox.design.WearSectionTitle
import com.tomasrepcik.sensorbox.diagnostics.WearAppErrorScreen

@Composable
fun PhoneLaunchScreen(state: PhoneLaunchState, onIntent: (PhoneLaunchIntent) -> Unit) {
    state.visibleFailureCode?.let { code ->
        WearAppErrorScreen(code) { onIntent(PhoneLaunchIntent.DismissFailure) }
        return
    }
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.activity_info_phone), transformation) }
        item { WearSectionTitle(stringResource(statusText(state.status)), transformation) }
        item {
            WearPrimaryButton(
                label = stringResource(R.string.open_phone),
                transformation = transformation,
                onClick = { onIntent(PhoneLaunchIntent.LaunchRequested) },
                enabled = state.isPhoneConnected && state.status != PhoneLaunchStatus.LAUNCHING,
            )
        }
    }
}
