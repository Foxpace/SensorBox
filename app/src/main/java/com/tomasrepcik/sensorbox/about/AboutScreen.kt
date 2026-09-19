package com.tomasrepcik.sensorbox.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
fun AboutDialog(onDismiss: () -> Unit, onPrivacy: () -> Unit, onLicenses: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_sensorbox_logo),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        },
        title = { Text(stringResource(R.string.about_title)) },
        text = { AboutDialogBody(onPrivacy) },
        confirmButton = {
            TextButton(onClick = onLicenses) {
                Text(stringResource(R.string.about_licenses))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
