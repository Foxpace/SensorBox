package com.tomasrepcik.sensorbox.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.BuildConfig
import com.tomasrepcik.sensorbox.R

@Composable
internal fun AboutDialogBody(onPrivacy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.about_app_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onPrivacy, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.about_privacy_policy))
        }
    }
}
