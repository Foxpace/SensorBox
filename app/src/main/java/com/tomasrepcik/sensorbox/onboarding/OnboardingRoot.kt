package com.tomasrepcik.sensorbox.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.platform.openWebPage
import com.tomasrepcik.sensorbox.platform.rememberRecordingArchivePicker
import com.tomasrepcik.sensorbox.platform.requestBatteryOptimizationExemption

@Composable
fun OnboardingRoot(onNavigate: (NavKey) -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val privacyPolicyUrl = stringResource(R.string.link_privacy_policy)
    val termsUrl = stringResource(R.string.link_terms)
    val pickRecordingArchive = rememberRecordingArchivePicker(
        onResult = viewModel::handleRecordingArchiveResult,
        onFailure = viewModel::reportFailure,
    )
    LaunchedEffect(viewModel, context, privacyPolicyUrl, termsUrl) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.PickRecordingArchive -> pickRecordingArchive()

                OnboardingEffect.OpenPrivacyPolicy -> {
                    openWebPage(context, privacyPolicyUrl).onFailure(viewModel::reportFailure)
                }

                OnboardingEffect.OpenTermsOfUse -> {
                    openWebPage(context, termsUrl).onFailure(viewModel::reportFailure)
                }

                OnboardingEffect.RequestBatteryOptimizationExemption -> {
                    requestBatteryOptimizationExemption(context).onFailure(viewModel::reportFailure)
                }

                is OnboardingEffect.Navigate -> onNavigate(effect.route)
            }
        }
    }
    OnboardingScreen(state, viewModel::accept)
}
