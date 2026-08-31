package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.platform.rememberPermissionRequest
import com.tomasrepcik.sensorbox.platform.rememberRecordingArchivePicker
import com.tomasrepcik.sensorbox.recording.RecordingDraft

@Composable
fun RecordingSetupRoot(
    draft: RecordingDraft,
    onBack: () -> Unit,
    viewModel: RecordingSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickRecordingArchive = rememberRecordingArchivePicker(
        onResult = viewModel::handleRecordingArchiveResult,
        onFailure = viewModel::reportFailure,
    )
    val requestPermissions = rememberPermissionRequest(
        onResult = viewModel::handlePermissionResult,
        onFailure = viewModel::reportFailure,
    )
    LaunchedEffect(viewModel, draft) {
        viewModel.accept(RecordingSetupIntent.LoadDraft(draft))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RecordingSetupEffect.PickRecordingArchive -> pickRecordingArchive()
                is RecordingSetupEffect.RequestPermissions -> requestPermissions(effect.permissions)
            }
        }
    }
    FullScreen { modifier ->
        RecordingSetupScreen(
            state = state,
            onIntent = viewModel::accept,
            modifier = modifier,
            onBack = onBack,
        )
    }
}
