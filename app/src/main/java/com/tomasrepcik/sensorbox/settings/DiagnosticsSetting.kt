package com.tomasrepcik.sensorbox.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.navigation.MainRoute

@Composable
internal fun DiagnosticsSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    DiagnosticsContent(state, onIntent, onClear = { confirmClear = true })
    if (confirmClear) {
        ClearDiagnosticsDialog(
            onConfirm = {
                confirmClear = false
                onIntent(SettingsIntent.ClearDiagnostics)
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun DiagnosticsContent(state: SettingsState, onIntent: (SettingsIntent) -> Unit, onClear: () -> Unit) {
    when {
        !state.diagnosticsLoaded -> DiagnosticsStatusText(R.string.diagnostics_loading)

        state.diagnosticsText.isNullOrBlank() -> DiagnosticsStatusText(R.string.diagnostics_empty)

        else -> {
            SettingsControlRow(
                title = stringResource(R.string.diagnostics_view),
                description = stringResource(R.string.diagnostics_view_summary),
            ) { onIntent(SettingsIntent.Navigate(MainRoute.DIAGNOSTICS)) }
            SettingsControlRow(
                title = stringResource(R.string.diagnostics_share),
                description = stringResource(R.string.diagnostics_share_summary),
            ) { onIntent(SettingsIntent.ShareDiagnosticsFile) }
            SettingsControlRow(
                title = stringResource(R.string.diagnostics_clear),
                description = stringResource(R.string.diagnostics_clear_summary),
                showDivider = false,
                onClick = onClear,
            )
        }
    }
}

@Composable
private fun DiagnosticsStatusText(textResource: Int) {
    Text(
        stringResource(textResource),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ClearDiagnosticsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diagnostics_clear)) },
        text = { Text(stringResource(R.string.diagnostics_clear_confirmation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.diagnostics_clear)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
