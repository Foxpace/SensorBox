package com.tomasrepcik.sensorbox.navigation

import androidx.compose.runtime.Composable
import com.tomasrepcik.sensorbox.about.OpenSourceLicensesScreen
import com.tomasrepcik.sensorbox.about.PrivacyScreen
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsLogScreen
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserIntent
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserState
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementsScreen
import com.tomasrepcik.sensorbox.measurements.details.MeasurementDetailsScreen
import com.tomasrepcik.sensorbox.measurements.details.MeasurementFileScreen
import com.tomasrepcik.sensorbox.onboarding.OnboardingIntent
import com.tomasrepcik.sensorbox.onboarding.OnboardingScreen
import com.tomasrepcik.sensorbox.onboarding.OnboardingState
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.active.ActiveRecordingScreen
import com.tomasrepcik.sensorbox.recording.active.RecordScreen
import com.tomasrepcik.sensorbox.recording.details.SensorDetailsScreen
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewScreen
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetupScreen
import com.tomasrepcik.sensorbox.settings.SettingsIntent
import com.tomasrepcik.sensorbox.settings.SettingsScreen
import com.tomasrepcik.sensorbox.settings.SettingsState

@Composable
internal fun RouteContent(
    route: MainRoute,
    onboardingState: OnboardingState,
    recordingState: RecordingState,
    measurementBrowserState: MeasurementBrowserState,
    settingsState: SettingsState,
    onOnboardingIntent: (OnboardingIntent) -> Unit,
    onRecordingIntent: (RecordingIntent) -> Unit,
    onMeasurementBrowserIntent: (MeasurementBrowserIntent) -> Unit,
    onSettingsIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    when (route) {
        MainRoute.ONBOARDING -> OnboardingScreen(onboardingState, onOnboardingIntent)

        MainRoute.ACTIVE_RECORDING,
        MainRoute.SENSOR_DETAILS,
        MainRoute.SENSOR_PREVIEW,
        MainRoute.RECORDING_SETUP,
        MainRoute.RECORD,
        -> RecordingRouteContent(route, recordingState, onRecordingIntent, onBack)

        MainRoute.MEASUREMENTS,
        MainRoute.MEASUREMENT_DETAILS,
        MainRoute.MEASUREMENT_FILE,
        -> MeasurementBrowserRoute(route, measurementBrowserState, onMeasurementBrowserIntent, onBack)

        MainRoute.SETTINGS,
        MainRoute.DIAGNOSTICS,
        MainRoute.LICENSES,
        MainRoute.PRIVACY,
        -> SettingsRouteContent(route, settingsState, onSettingsIntent, onBack)
    }
}

@Composable
private fun RecordingRouteContent(
    route: MainRoute,
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    onBack: () -> Unit,
) {
    FullScreen { modifier ->
        when (route) {
            MainRoute.ACTIVE_RECORDING -> ActiveRecordingScreen(state, onIntent, modifier)

            MainRoute.SENSOR_DETAILS -> SensorDetailsScreen(
                state,
                onBack,
                { onIntent(RecordingIntent.Navigate(MainRoute.SENSOR_PREVIEW)) },
                onIntent,
                modifier,
            )

            MainRoute.SENSOR_PREVIEW -> SensorPreviewScreen(state, onIntent, onBack, modifier)

            MainRoute.RECORDING_SETUP -> RecordingSetupScreen(
                state = state,
                onIntent = onIntent,
                modifier = modifier,
                onBack = { onIntent(RecordingIntent.ReturnToSensorSelection) },
            )

            MainRoute.RECORD -> RecordScreen(state, onIntent, modifier)

            else -> error("Not a recording route")
        }
    }
}

@Composable
private fun MeasurementBrowserRoute(
    route: MainRoute,
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
) {
    FullScreen { modifier ->
        when (route) {
            MainRoute.MEASUREMENTS -> MeasurementsScreen(state, onIntent, onBack, modifier)
            MainRoute.MEASUREMENT_DETAILS -> MeasurementDetailsScreen(state, onIntent, onBack, modifier)
            MainRoute.MEASUREMENT_FILE -> MeasurementFileScreen(state, onIntent, onBack, modifier)
            else -> error("Not a measurement browser route")
        }
    }
}

@Composable
private fun SettingsRouteContent(
    route: MainRoute,
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    FullScreen { modifier ->
        when (route) {
            MainRoute.SETTINGS -> SettingsScreen(state, onIntent, modifier, onBack)

            MainRoute.DIAGNOSTICS -> DiagnosticsLogScreen(
                state = state,
                onBack = onBack,
                onRefresh = { onIntent(SettingsIntent.ViewDiagnostics) },
                modifier = modifier,
            )

            MainRoute.LICENSES -> OpenSourceLicensesScreen(state, onIntent, onBack, modifier)

            MainRoute.PRIVACY -> PrivacyScreen(onBack, modifier)

            else -> error("Not a settings route")
        }
    }
}
