package com.tomasrepcik.sensorbox.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SensorBoxBackScreen(
        title = stringResource(R.string.privacy_health_data),
        onBack = onBack,
        modifier = modifier,
    ) {
        item { Text(stringResource(R.string.dialog_privacy_policy)) }
        item { Text(stringResource(R.string.privacy_access_title), style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                stringResource(R.string.privacy_permissions_body),
            )
        }
        item { Text(stringResource(R.string.privacy_storage_body)) }
        item { Text(stringResource(R.string.privacy_no_upload_body)) }
    }
}
