package com.tomasrepcik.sensorbox.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.measurements.details.MeasurementDetailsRoot
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsRoot
import com.tomasrepcik.sensorbox.measurements.loading.MeasurementLoadingRoot
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewRoot
import com.tomasrepcik.sensorbox.onboarding.OnboardingRoot
import com.tomasrepcik.sensorbox.recording.active.ActiveRecordingRoot
import com.tomasrepcik.sensorbox.recording.active.RecordRoot
import com.tomasrepcik.sensorbox.recording.details.SensorDetailsRoot
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewRoot
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetupRoot
import com.tomasrepcik.sensorbox.settings.SettingsRoot

@Composable
internal fun RouteContent(
    route: MainRoute,
    onNavigate: (NavKey) -> Unit,
    onReplaceRoute: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    when (route) {
        MainRoute.ONBOARDING -> OnboardingRoot(onNavigate)

        MainRoute.ACTIVE_RECORDING -> ActiveRecordingRoot(onNavigate)

        MainRoute.RECORD -> RecordRoot(onNavigate)

        MainRoute.MEASUREMENTS -> MeasurementRouteContent(route, onNavigate, onReplaceRoute, onBack)

        MainRoute.SETTINGS,
        MainRoute.DIAGNOSTICS,
        MainRoute.LICENSES,
        MainRoute.PRIVACY,
        -> SettingsRoot(route, onNavigate, onBack)
    }
}

@Composable
internal fun RecordingDestinationContent(route: NavKey, onNavigate: (NavKey) -> Unit, onBack: () -> Unit) {
    when (route) {
        is SensorDetailsRoute -> SensorDetailsRoot(
            sensorType = route.sensorType,
            device = route.device,
            onNavigate = onNavigate,
            onBack = onBack,
        )

        is SensorPreviewRoute -> SensorPreviewRoot(
            sensorType = route.sensorType,
            device = route.device,
            onBack = onBack,
        )

        is RecordingSetupRoute -> RecordingSetupRoot(
            draft = route.draft,
            onBack = onBack,
        )

        else -> error("Not a recording route")
    }
}

@Composable
private fun MeasurementRouteContent(
    route: NavKey,
    onNavigate: (NavKey) -> Unit,
    onReplaceRoute: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    FullScreen { modifier ->
        when (route) {
            MainRoute.MEASUREMENTS -> MeasurementsRoot(
                onOpenDetails = { measurementId -> onNavigate(MeasurementDetailsRoute(measurementId)) },
                onBack = onBack,
                modifier = modifier,
            )

            is MeasurementDetailsRoute -> MeasurementDetailsRoot(
                measurementId = route.measurementId,
                onOpenFile = { measurementId, fileId ->
                    onNavigate(MeasurementLoadingRoute(measurementId, fileId))
                },
                onBack = onBack,
                modifier = modifier,
            )

            is MeasurementLoadingRoute -> MeasurementLoadingRoot(
                measurementId = route.measurementId,
                fileId = route.fileId,
                onOpenPreview = { measurementId, fileId ->
                    onReplaceRoute(MeasurementPreviewRoute(measurementId, fileId))
                },
                onBack = onBack,
                modifier = modifier,
            )

            is MeasurementPreviewRoute -> MeasurementPreviewRoot(
                measurementId = route.measurementId,
                fileId = route.fileId,
                onBack = onBack,
                modifier = modifier,
            )

            else -> error("Not a measurement route")
        }
    }
}

@Composable
internal fun MeasurementDestinationContent(
    route: NavKey,
    onNavigate: (NavKey) -> Unit,
    onReplaceRoute: (NavKey) -> Unit,
    onBack: () -> Unit,
) {
    MeasurementRouteContent(route, onNavigate, onReplaceRoute, onBack)
}
