package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun OnboardingScreen(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val pageIndex = state.page.coerceIn(ONBOARDING_PAGES.indices)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.TopCenter,
    ) {
        OnboardingLayout(pageIndex, state, isLandscape = maxWidth > maxHeight, onIntent)
    }
}
