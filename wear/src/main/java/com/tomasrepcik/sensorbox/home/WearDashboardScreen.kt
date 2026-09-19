package com.tomasrepcik.sensorbox.home

import androidx.compose.runtime.Composable
import com.tomasrepcik.sensorbox.diagnostics.WearAppErrorScreen
import com.tomasrepcik.sensorbox.menu.WearMenuScreen
import com.tomasrepcik.sensorbox.menu.WearMenuState
import com.tomasrepcik.sensorbox.recording.active.WearActiveScreen
import com.tomasrepcik.sensorbox.recording.live.WearLiveScreen
import com.tomasrepcik.sensorbox.recording.setup.WearRecordScreen
import com.tomasrepcik.sensorbox.settings.WearSettingsScreen

@Composable
fun WearDashboardScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    state.visibleFailureCode?.let { failureCode ->
        WearAppErrorScreen(failureCode) { accept(WearDashboardIntent.DismissFailure) }
        return
    }
    when (state.route) {
        WearRoute.MENU -> WearMenuScreen(WearMenuState()) { accept(WearDashboardIntent.Open(it)) }
        WearRoute.RECORD -> WearRecordScreen(state, accept)
        WearRoute.LIVE -> WearLiveScreen(state, accept)
        WearRoute.SETTINGS -> WearSettingsScreen(state, accept)
        WearRoute.ACTIVE -> WearActiveScreen(state, accept)
    }
}
