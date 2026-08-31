package com.tomasrepcik.sensorbox.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.about.OpenSourceLicensesScreen
import com.tomasrepcik.sensorbox.about.PrivacyScreen
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsLogScreen
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.navigation.MainRoute
import com.tomasrepcik.sensorbox.platform.copyDiagnostics
import com.tomasrepcik.sensorbox.platform.requestBatteryOptimizationExemption
import com.tomasrepcik.sensorbox.platform.shareDiagnosticsFile
import com.tomasrepcik.sensorbox.platform.shareDiagnosticsText
import com.tomasrepcik.sensorbox.platform.showDiagnosticsCleared

@Composable
fun SettingsRoot(
    route: MainRoute,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    if (route == MainRoute.SETTINGS) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.accept(SettingsIntent.RefreshBatteryOptimization)
        }
    }
    LoadSettingsRoute(route, viewModel)
    ObserveSettingsEffects(viewModel, context, onNavigate)
    FullScreen { modifier ->
        RenderSettingsScreen(route, state, viewModel::accept, onBack, modifier)
    }
}

@Composable
private fun LoadSettingsRoute(route: MainRoute, viewModel: SettingsViewModel) {
    LaunchedEffect(viewModel, route) {
        if (route == MainRoute.SETTINGS || route == MainRoute.DIAGNOSTICS) {
            viewModel.accept(SettingsIntent.ViewDiagnostics)
        }
    }
}

@Composable
private fun ObserveSettingsEffects(viewModel: SettingsViewModel, context: Context, onNavigate: (NavKey) -> Unit) {
    LaunchedEffect(viewModel, context) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestBatteryOptimizationExemption -> {
                    requestBatteryOptimizationExemption(context).onFailure(viewModel::reportFailure)
                }

                is SettingsEffect.ShareDiagnosticsText -> {
                    shareDiagnosticsText(context, effect.text).onFailure(viewModel::reportFailure)
                }

                is SettingsEffect.ShareDiagnosticsFile -> {
                    shareDiagnosticsFile(
                        context,
                        effect.contentUri,
                        effect.displayName,
                        effect.mimeType,
                    ).onFailure(viewModel::reportFailure)
                }

                is SettingsEffect.CopyDiagnosticsText -> {
                    copyDiagnostics(context, effect.text).onFailure(viewModel::reportFailure)
                }

                SettingsEffect.DiagnosticsCleared -> showDiagnosticsCleared(context)

                is SettingsEffect.Navigate -> onNavigate(effect.route)
            }
        }
    }
}

@Composable
private fun RenderSettingsScreen(
    route: MainRoute,
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    when (route) {
        MainRoute.SETTINGS -> SettingsScreen(state, onIntent, modifier, onBack)
        MainRoute.DIAGNOSTICS -> DiagnosticsLogScreen(state, onBack, modifier)
        MainRoute.LICENSES -> OpenSourceLicensesScreen(state, onIntent, onBack, modifier)
        MainRoute.PRIVACY -> PrivacyScreen(onBack, modifier)
        else -> error("Not a settings route")
    }
}
