package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.error.AppErrorCode

@Composable
internal fun WearAppErrorScreen(errorCode: AppErrorCode, onDismiss: () -> Unit) {
    AppScaffold {
        ScreenScaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_error_title),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(errorCode.messageResource()),
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

private fun AppErrorCode.messageResource(): Int = when (this) {
    AppErrorCode.CONNECTIVITY -> R.string.app_error_connectivity

    AppErrorCode.PERMISSION -> R.string.app_error_permission

    AppErrorCode.RECORDING -> R.string.app_error_recording

    AppErrorCode.STORAGE -> R.string.app_error_storage

    AppErrorCode.PREFERENCES,
    AppErrorCode.EXTERNAL_ACTION,
    AppErrorCode.VALIDATION,
    AppErrorCode.TIMEOUT,
    AppErrorCode.CONFLICT,
    AppErrorCode.UNKNOWN,
    -> R.string.app_error_unknown
}
