package com.motionapps.sensorbox.presentation.main

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
import com.motionapps.sensorservices.session.MeasurementSessionState

@Composable
fun SensorBoxApp(state: MainState, onIntent: (MainIntent) -> Unit) {
    if (!state.hasLoadedPreferences) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    if (state.preferences.keepPhoneDisplayOn && state.session is MeasurementSessionState.Running) {
        KeepScreenAwake()
    }
    SensorBoxNavHost(state, onIntent)
}

@Composable
private fun SensorBoxNavHost(state: MainState, onIntent: (MainIntent) -> Unit) {
    val backStack = rememberNavBackStack(state.route)
    val displayedRoute = state.displayedRoute()
    SynchronizeBackStack(backStack, displayedRoute)
    val navigateBack = { navigateBack(state, backStack, onIntent) }

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
                    state = state,
                    onIntent = onIntent,
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
        if (
            displayedRoute != MainRoute.ACTIVE_MEASUREMENT &&
            backStack.lastOrNull() == MainRoute.ACTIVE_MEASUREMENT
        ) {
            backStack.removeLastOrNull()
        }
        if (backStack.lastOrNull() != displayedRoute) {
            if (displayedRoute.isRootDestination()) backStack.clear()
            backStack.add(displayedRoute)
        }
    }
}

private fun navigateBack(state: MainState, backStack: NavBackStack<NavKey>, onIntent: (MainIntent) -> Unit) {
    if (state.session is MeasurementSessionState.Running) return
    if (backStack.size > 1) backStack.removeLastOrNull()
    val destination = backStack.lastOrNull() as? MainRoute ?: MainRoute.RECORD
    if (destination != MainRoute.ACTIVE_MEASUREMENT) onIntent(MainIntent.Navigate(destination))
}

private fun MainRoute.isRootDestination() = when (this) {
    MainRoute.ONBOARDING,
    MainRoute.RECORD,
    -> true

    MainRoute.ACTIVE_MEASUREMENT,
    MainRoute.SENSOR_DETAILS,
    MainRoute.SENSOR_PREVIEW,
    MainRoute.SETUP,
    MainRoute.SETTINGS,
    MainRoute.LICENSES,
    MainRoute.PRIVACY,
    -> false
}

@Composable
private fun RouteContent(route: MainRoute, state: MainState, onIntent: (MainIntent) -> Unit, onBack: () -> Unit) {
    when (route) {
        MainRoute.ONBOARDING -> OnboardingScreen(state, onIntent)

        MainRoute.ACTIVE_MEASUREMENT -> FullScreen { modifier ->
            ActiveMeasurementScreen(state, onIntent, modifier)
        }

        MainRoute.SENSOR_DETAILS -> FullScreen { modifier ->
            SensorDetailsScreen(state, onBack, { onIntent(MainIntent.Navigate(MainRoute.SENSOR_PREVIEW)) }, modifier)
        }

        MainRoute.SENSOR_PREVIEW -> FullScreen { modifier ->
            SensorPreviewScreen(state = state, onBack = onBack, modifier = modifier)
        }

        MainRoute.SETUP -> FullScreen { modifier ->
            MeasurementSetupScreen(
                state = state,
                onIntent = onIntent,
                modifier = modifier,
                onBack = {
                    onIntent(MainIntent.ReturnToSensorSelection)
                    onBack()
                },
            )
        }

        MainRoute.LICENSES -> FullScreen { modifier ->
            OpenSourceLicensesScreen(onBack = onBack, modifier = modifier)
        }

        MainRoute.RECORD -> FullScreen { modifier -> RecordScreen(state, onIntent, modifier) }

        MainRoute.SETTINGS -> FullScreen { modifier ->
            SettingsScreen(state = state, onIntent = onIntent, modifier = modifier, onBack = onBack)
        }

        MainRoute.PRIVACY -> FullScreen { modifier -> PrivacyScreen(onBack, modifier) }
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

private const val SCREEN_TRANSITION_MILLIS = 300
private const val SCREEN_FADE_IN_MILLIS = 240
private const val SCREEN_FADE_OUT_MILLIS = 180
private const val SCREEN_SLIDE_DIVISOR = 5

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
                Modifier
                    .widthIn(max = ADAPTIVE_CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )
        }
    }
}

private val ADAPTIVE_CONTENT_MAX_WIDTH = 840.dp
