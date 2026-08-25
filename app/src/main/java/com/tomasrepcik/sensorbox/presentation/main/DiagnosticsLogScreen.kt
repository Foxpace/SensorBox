package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.tomasrepcik.sensorbox.R

@Composable
fun DiagnosticsLogScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onRefresh() }
    SensorBoxBackScreen(
        title = stringResource(R.string.diagnostics_logs_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SelectionContainer {
                Text(
                    text = when {
                        !state.diagnosticsLoaded -> stringResource(R.string.diagnostics_loading)
                        state.diagnosticsText.isNullOrBlank() -> stringResource(R.string.diagnostics_empty)
                        else -> state.diagnosticsText
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
