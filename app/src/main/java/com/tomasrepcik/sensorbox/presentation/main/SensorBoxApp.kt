package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tomasrepcik.sensorbox.sensorservices.session.MeasurementSessionState

@Composable
fun SensorBoxApp(
    mainState: MainState,
    onboardingState: OnboardingState,
    recordingState: RecordingState,
    measurementBrowserState: MeasurementBrowserState,
    settingsState: SettingsState,
    onNavigate: (MainRoute) -> Unit,
    onOnboardingIntent: (OnboardingIntent) -> Unit,
    onRecordingIntent: (RecordingIntent) -> Unit,
    onMeasurementBrowserIntent: (MeasurementBrowserIntent) -> Unit,
    onSettingsIntent: (SettingsIntent) -> Unit,
) {
    if (!mainState.hasLoadedPreferences) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    if (mainState.keepScreenAwake && mainState.session is MeasurementSessionState.Running) KeepScreenAwake()
    SensorBoxNavHost(
        mainState = mainState,
        onboardingState = onboardingState,
        recordingState = recordingState,
        measurementBrowserState = measurementBrowserState,
        settingsState = settingsState,
        onNavigate = onNavigate,
        onOnboardingIntent = onOnboardingIntent,
        onRecordingIntent = onRecordingIntent,
        onMeasurementBrowserIntent = onMeasurementBrowserIntent,
        onSettingsIntent = onSettingsIntent,
    )
}

@Composable
private fun SensorBoxNavHost(
    mainState: MainState,
    onboardingState: OnboardingState,
    recordingState: RecordingState,
    measurementBrowserState: MeasurementBrowserState,
    settingsState: SettingsState,
    onNavigate: (MainRoute) -> Unit,
    onOnboardingIntent: (OnboardingIntent) -> Unit,
    onRecordingIntent: (RecordingIntent) -> Unit,
    onMeasurementBrowserIntent: (MeasurementBrowserIntent) -> Unit,
    onSettingsIntent: (SettingsIntent) -> Unit,
) {
    val backStack = rememberNavBackStack(mainState.route)
    val displayedRoute = mainState.displayedRoute()
    SynchronizeBackStack(backStack, displayedRoute)
    val navigateBack = { navigateBack(mainState, backStack, onNavigate) }

    NavDisplay(
        backStack = backStack,
        onBack = navigateBack,
        transitionSpec = { forwardScreenTransition() },
        popTransitionSpec = { backwardScreenTransition() },
        predictivePopTransitionSpec = { backwardScreenTransition() },
        entryProvider = entryProvider {
            entry<MainRoute> { route ->
                RouteContent(
                    route = route,
                    onboardingState = onboardingState,
                    recordingState = recordingState,
                    measurementBrowserState = measurementBrowserState,
                    settingsState = settingsState,
                    onOnboardingIntent = onOnboardingIntent,
                    onRecordingIntent = onRecordingIntent,
                    onMeasurementBrowserIntent = onMeasurementBrowserIntent,
                    onSettingsIntent = onSettingsIntent,
                    onBack = navigateBack,
                )
            }
        },
    )
}

private fun MainState.displayedRoute() = if (session is MeasurementSessionState.Running) {
    MainRoute.ACTIVE_MEASUREMENT
} else {
    route
}

@Composable
private fun SynchronizeBackStack(backStack: NavBackStack<NavKey>, displayedRoute: MainRoute) {
    LaunchedEffect(displayedRoute) {
        if (displayedRoute != MainRoute.ACTIVE_MEASUREMENT && backStack.lastOrNull() == MainRoute.ACTIVE_MEASUREMENT) {
            backStack.removeLastOrNull()
        }
        if (backStack.lastOrNull() != displayedRoute) {
            if (displayedRoute.isRootDestination()) backStack.clear()
            backStack.add(displayedRoute)
        }
    }
}

private fun navigateBack(state: MainState, backStack: NavBackStack<NavKey>, onNavigate: (MainRoute) -> Unit) {
    if (state.session is MeasurementSessionState.Running) return
    if (backStack.size > 1) backStack.removeLastOrNull()
    val destination = backStack.lastOrNull() as? MainRoute ?: MainRoute.RECORD
    if (destination != MainRoute.ACTIVE_MEASUREMENT) onNavigate(destination)
}

private fun MainRoute.isRootDestination() = when (this) {
    MainRoute.ONBOARDING,
    MainRoute.RECORD,
    -> true

    MainRoute.ACTIVE_MEASUREMENT,
    MainRoute.SENSOR_DETAILS,
    MainRoute.SENSOR_PREVIEW,
    MainRoute.SETUP,
    MainRoute.MEASUREMENTS,
    MainRoute.MEASUREMENT_DETAILS,
    MainRoute.MEASUREMENT_FILE,
    MainRoute.SETTINGS,
    MainRoute.DIAGNOSTICS,
    MainRoute.LICENSES,
    MainRoute.PRIVACY,
    -> false
}

@Composable
private fun RouteContent(
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

        MainRoute.ACTIVE_MEASUREMENT -> FullScreen { modifier ->
            ActiveMeasurementScreen(recordingState, onRecordingIntent, modifier)
        }

        MainRoute.SENSOR_DETAILS -> FullScreen { modifier ->
            SensorDetailsScreen(
                recordingState,
                onBack,
                { onRecordingIntent(RecordingIntent.Navigate(MainRoute.SENSOR_PREVIEW)) },
                modifier,
            )
        }

        MainRoute.SENSOR_PREVIEW -> FullScreen { modifier ->
            SensorPreviewScreen(state = recordingState, onBack = onBack, modifier = modifier)
        }

        MainRoute.SETUP -> FullScreen { modifier ->
            MeasurementSetupScreen(
                state = recordingState,
                onIntent = onRecordingIntent,
                modifier = modifier,
                onBack = { onRecordingIntent(RecordingIntent.ReturnToSensorSelection) },
            )
        }

        MainRoute.MEASUREMENTS,
        MainRoute.MEASUREMENT_DETAILS,
        MainRoute.MEASUREMENT_FILE,
        -> MeasurementBrowserRoute(route, measurementBrowserState, onMeasurementBrowserIntent, onBack)

        MainRoute.LICENSES -> FullScreen { modifier ->
            OpenSourceLicensesScreen(onBack = onBack, modifier = modifier)
        }

        MainRoute.RECORD -> FullScreen { modifier ->
            RecordScreen(recordingState, onRecordingIntent, modifier)
        }

        MainRoute.SETTINGS -> FullScreen { modifier ->
            SettingsScreen(
                state = settingsState,
                onIntent = onSettingsIntent,
                modifier = modifier,
                onBack = onBack,
            )
        }

        MainRoute.DIAGNOSTICS -> FullScreen { modifier ->
            DiagnosticsLogScreen(
                state = settingsState,
                onBack = onBack,
                onRefresh = { onSettingsIntent(SettingsIntent.ViewDiagnostics) },
                modifier = modifier,
            )
        }

        MainRoute.PRIVACY -> FullScreen { modifier -> PrivacyScreen(onBack, modifier) }
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
            else -> error("Not a measurement archive route")
        }
    }
}

private fun forwardScreenTransition(): ContentTransform =
    (slideInHorizontally(screenTween()) { width -> width / SCREEN_SLIDE_DIVISOR } + fadeIn(screenFadeInTween()))
        .togetherWith(
            slideOutHorizontally(screenTween()) { width -> -width / SCREEN_SLIDE_DIVISOR } +
                fadeOut(screenFadeOutTween()),
        )

private fun backwardScreenTransition(): ContentTransform =
    (slideInHorizontally(screenTween()) { width -> -width / SCREEN_SLIDE_DIVISOR } + fadeIn(screenFadeInTween()))
        .togetherWith(
            slideOutHorizontally(screenTween()) { width -> width / SCREEN_SLIDE_DIVISOR } +
                fadeOut(screenFadeOutTween()),
        )

private fun screenTween(): FiniteAnimationSpec<IntOffset> =
    tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)

private fun screenFadeInTween() = tween<Float>(SCREEN_FADE_IN_MILLIS, easing = FastOutSlowInEasing)

private fun screenFadeOutTween() = tween<Float>(SCREEN_FADE_OUT_MILLIS, easing = FastOutSlowInEasing)

@Composable
private fun KeepScreenAwake() {
    val view = LocalView.current
    DisposableEffect(view) {
        val wasKeptOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = wasKeptOn }
    }
}

@Composable
private fun FullScreen(content: @Composable (Modifier) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            content(
                Modifier.widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH).fillMaxWidth().fillMaxHeight(),
            )
        }
    }
}

private const val SCREEN_TRANSITION_MILLIS = 300
private const val SCREEN_FADE_IN_MILLIS = 240
private const val SCREEN_FADE_OUT_MILLIS = 180
private const val SCREEN_SLIDE_DIVISOR = 5
private val ADAPTIVE_CONTENT_MAX_WIDTH = 840.dp
