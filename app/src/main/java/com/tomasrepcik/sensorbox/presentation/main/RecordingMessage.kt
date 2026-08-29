package com.tomasrepcik.sensorbox.presentation.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
fun RecordingMessageText(message: RecordingMessage) {
    if (message == RecordingMessage.NONE) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(painterResource(R.drawable.ic_info), null, Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(messageTitleResource(message)), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(messageTextResource(message)), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@StringRes
private fun messageTitleResource(message: RecordingMessage): Int = when (message) {
    RecordingMessage.PICK_AT_LEAST_ONE_SOURCE -> R.string.message_pick_source_title
    RecordingMessage.RECORDING_ARCHIVE_REQUIRED -> R.string.message_recording_archive_required_title
    RecordingMessage.PERMISSION_REQUIRED -> R.string.message_permission_required_title
    RecordingMessage.WATCH_PERMISSION_REQUIRED -> R.string.message_wear_permission_required_title
    RecordingMessage.RECORDING_FAILED -> R.string.message_recording_failed_title
    RecordingMessage.NONE -> R.string.app_name
}

@StringRes
private fun messageTextResource(message: RecordingMessage): Int = when (message) {
    RecordingMessage.PICK_AT_LEAST_ONE_SOURCE -> R.string.message_pick_source
    RecordingMessage.RECORDING_ARCHIVE_REQUIRED -> R.string.message_recording_archive_required
    RecordingMessage.PERMISSION_REQUIRED -> R.string.message_permission_required
    RecordingMessage.WATCH_PERMISSION_REQUIRED -> R.string.message_wear_permission_required
    RecordingMessage.RECORDING_FAILED -> R.string.message_recording_failed
    RecordingMessage.NONE -> R.string.app_name
}
