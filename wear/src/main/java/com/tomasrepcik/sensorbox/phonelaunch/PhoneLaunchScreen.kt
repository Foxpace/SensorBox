package com.tomasrepcik.sensorbox.phonelaunch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.diagnostics.WearAppErrorScreen

@Composable
fun PhoneLaunchScreen(state: PhoneLaunchState, onIntent: (PhoneLaunchIntent) -> Unit) {
    state.visibleFailureCode?.let { code ->
        WearAppErrorScreen(code) { onIntent(PhoneLaunchIntent.DismissFailure) }
        return
    }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(0.7f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.activity_info_phone), textAlign = TextAlign.Center)
            Text(stringResource(statusText(state.status)), textAlign = TextAlign.Center)
            Button(
                onClick = { onIntent(PhoneLaunchIntent.LaunchRequested) },
                enabled = state.isPhoneConnected && state.status != PhoneLaunchStatus.LAUNCHING,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.open_phone),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
