package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.error.AppErrorCode

@Composable
internal fun AppErrorScreen(errorCode: AppErrorCode, onDismiss: () -> Unit) {
    FullScreen { modifier ->
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_error_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(errorCode.messageResource()),
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            SensorBoxPrimaryButton(
                label = stringResource(R.string.close),
                onClick = onDismiss,
            )
        }
    }
}

private fun AppErrorCode.messageResource(): Int = when (this) {
    AppErrorCode.CONNECTIVITY -> R.string.app_error_connectivity

    AppErrorCode.PERMISSION -> R.string.app_error_permission

    AppErrorCode.PREFERENCES -> R.string.app_error_preferences

    AppErrorCode.RECORDING -> R.string.app_error_recording

    AppErrorCode.STORAGE -> R.string.app_error_storage

    AppErrorCode.EXTERNAL_ACTION -> R.string.app_error_external_action

    AppErrorCode.TIMEOUT -> R.string.app_error_timeout

    AppErrorCode.VALIDATION,
    AppErrorCode.CONFLICT,
    AppErrorCode.UNKNOWN,
    -> R.string.app_error_unknown
}
