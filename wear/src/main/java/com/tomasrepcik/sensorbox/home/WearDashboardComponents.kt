package com.tomasrepcik.sensorbox.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.WearSecondaryButton

@Composable
internal fun TransformingLazyColumnItemScope.WearBackButton(
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    WearSecondaryButton(stringResource(R.string.back), transformation) {
        accept(WearDashboardIntent.Back)
    }
}

@Composable
internal fun wearMessageText(message: WearDashboardMessage): String = when (message) {
    WearDashboardMessage.PickSource -> stringResource(R.string.message_pick_source)
    WearDashboardMessage.PermissionRequired -> stringResource(R.string.message_permission_required)
    WearDashboardMessage.SensorUnavailable -> stringResource(R.string.message_sensor_unavailable)
}
