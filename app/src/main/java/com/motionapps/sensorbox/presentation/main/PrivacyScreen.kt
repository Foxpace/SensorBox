package com.motionapps.sensorbox.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SensorBoxTopAppBar(stringResource(R.string.privacy_health_data), onBack) }
        item { Text(stringResource(R.string.dialog_privacy_policy)) }
        item { Text(stringResource(R.string.privacy_access_title), style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                stringResource(R.string.privacy_permissions_body),
            )
        }
        item { Text(stringResource(R.string.privacy_storage_body)) }
        item {
            Text(stringResource(R.string.privacy_heart_rate_body))
        }
        item { Text(stringResource(R.string.privacy_no_upload_body)) }
    }
}
