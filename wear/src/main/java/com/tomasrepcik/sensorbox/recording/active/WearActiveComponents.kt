package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.tomasrepcik.sensorbox.R

@Composable
internal fun WearRecordingStatus(selectedCount: Int, keepsDisplayOn: Boolean) {
    Text(
        text = stringResource(R.string.recording),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = pluralStringResource(R.plurals.source_count, selectedCount, selectedCount),
        style = MaterialTheme.typography.displaySmall,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(if (keepsDisplayOn) R.string.display_stays_on else R.string.display_may_sleep),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun WearStopRecordingButton(onStop: () -> Unit) {
    Button(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Text(stringResource(R.string.stop_and_save))
    }
}
