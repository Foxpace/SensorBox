package com.tomasrepcik.sensorbox.measurements.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.R

@Composable
fun WatchSyncRoot(isWatchConnected: Boolean, viewModel: WatchSyncViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(isWatchConnected) {
        if (isWatchConnected && !state.busy && state.status != WatchSyncStatus.FAILED) {
            viewModel.accept(WatchSyncIntent.CHECK)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isWatchConnected && !state.busy && state.status != WatchSyncStatus.FAILED) {
            viewModel.accept(WatchSyncIntent.CHECK)
        }
    }
    WatchSyncContent(state, isWatchConnected, viewModel::accept)
}

@Composable
internal fun WatchSyncContent(state: WatchSyncState, connected: Boolean, onIntent: (WatchSyncIntent) -> Unit) {
    var showDetails by rememberSaveable { mutableStateOf(false) }
    WatchSyncButton(state, connected) { showDetails = true }
    if (showDetails) {
        WatchSyncDialog(
            state = state,
            onIntent = onIntent,
            onDismiss = { showDetails = false },
        )
    }
}

@Composable
internal fun WatchSyncButton(state: WatchSyncState, connected: Boolean, onClick: () -> Unit) {
    if (!connected && state.status != WatchSyncStatus.COPYING && state.status != WatchSyncStatus.FAILED) return
    IconButton(onClick = onClick) {
        BadgedBox(badge = { WatchSyncBadge(state) }) {
            Icon(
                painter = painterResource(R.drawable.ic_sync_watch),
                contentDescription = watchSyncDescription(state),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun watchSyncDescription(state: WatchSyncState): String = when {
    state.status == WatchSyncStatus.FAILED -> stringResource(R.string.watch_sync_failed)

    state.status == WatchSyncStatus.COPYING -> stringResource(R.string.watch_sync_show_progress)

    state.busy || state.status == WatchSyncStatus.UNKNOWN -> stringResource(R.string.watch_sync_checking)

    state.measurementsToFetch == 0 -> stringResource(R.string.watch_sync_up_to_date)

    else -> pluralStringResource(
        R.plurals.watch_measurements_to_fetch,
        state.measurementsToFetch,
        state.measurementsToFetch,
    )
}

@Composable
private fun WatchSyncBadge(state: WatchSyncState) {
    val failed = state.status == WatchSyncStatus.FAILED
    val checking = state.busy || state.status == WatchSyncStatus.UNKNOWN
    if (!failed && !checking && state.measurementsToFetch > 0) {
        Badge { Text(state.measurementsToFetch.toString()) }
        return
    }
    val drawable = when {
        failed -> R.drawable.ic_watch_sync_error
        checking -> R.drawable.ic_watch_syncing
        else -> R.drawable.ic_watch_sync_complete
    }
    val color = when {
        failed -> Color(0xFFD32F2F)
        checking -> Color(0xFF2196F3)
        else -> Color(0xFF2E7D32)
    }
    Icon(painterResource(drawable), contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
}

@Composable
private fun syncLabel(state: WatchSyncState): String = when (state.status) {
    WatchSyncStatus.UNKNOWN -> stringResource(R.string.watch_sync_title)

    WatchSyncStatus.CHECKING -> stringResource(R.string.watch_sync_checking)

    WatchSyncStatus.COPYING -> stringResource(R.string.watch_sync_progress, state.receivedFiles.size)

    WatchSyncStatus.COMPLETE -> stringResource(R.string.watch_sync_complete, state.receivedFiles.size)

    WatchSyncStatus.FAILED -> stringResource(R.string.watch_sync_failed)

    WatchSyncStatus.AVAILABLE -> if (state.measurementsToFetch == 0) {
        stringResource(R.string.watch_sync_up_to_date)
    } else {
        stringResource(R.string.watch_sync_available, state.measurementsToFetch)
    }
}

@Composable
private fun WatchSyncDialog(state: WatchSyncState, onIntent: (WatchSyncIntent) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.watch_sync_title), modifier = Modifier.weight(1f))
                IconButton(onClick = { onIntent(WatchSyncIntent.CHECK) }, enabled = !state.busy) {
                    Icon(painterResource(R.drawable.ic_watch_syncing), stringResource(R.string.watch_sync_refresh))
                }
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(syncLabel(state))
                WatchSyncProgress(state)
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(stringResource(R.string.watch_sync_destination))
            }
        },
        confirmButton = { WatchSyncConfirmButton(state, onIntent) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_watch_sync_error), contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.watch_sync_close))
            }
        },
    )
}

@Composable
private fun WatchSyncProgress(state: WatchSyncState) {
    if (state.status != WatchSyncStatus.COPYING) return
    val total = state.fileCount ?: 0
    if (total > 0) {
        LinearProgressIndicator(progress = { (state.receivedFiles.size.toFloat() / total).coerceIn(0f, 1f) })
        Text(stringResource(R.string.watch_sync_file_progress, state.receivedFiles.size, total))
    } else {
        LinearProgressIndicator()
    }
}

@Composable
private fun WatchSyncConfirmButton(state: WatchSyncState, onIntent: (WatchSyncIntent) -> Unit) {
    val failed = state.status == WatchSyncStatus.FAILED
    TextButton(
        onClick = { onIntent(if (failed) WatchSyncIntent.RETRY else WatchSyncIntent.COPY) },
        enabled = !state.busy && (failed || state.measurementsToFetch > 0),
    ) {
        Icon(painterResource(R.drawable.ic_watch_syncing), contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(if (failed) R.string.watch_sync_retry else R.string.watch_sync_copy))
    }
}
