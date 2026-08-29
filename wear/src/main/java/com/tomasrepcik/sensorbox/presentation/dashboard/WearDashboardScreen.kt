package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.runtime.Composable
import com.tomasrepcik.sensorbox.presentation.menu.WearMenuScreen
import com.tomasrepcik.sensorbox.presentation.menu.WearMenuState

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
