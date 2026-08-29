package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingLayout(
    pageIndex: Int,
    state: OnboardingState,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = if (isLandscape) 12.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingHeader(
            pageIndex,
            onIntent,
            Modifier.widthIn(max = ONBOARDING_MAX_WIDTH).fillMaxWidth().padding(horizontal = 24.dp),
        )
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            AnimatedOnboardingMessage(pageIndex, state, isLandscape, onIntent)
        }
        OnboardingControls(
            pageIndex = pageIndex,
            archiveSelected = state.recordingArchivePath != null,
            isLandscape = isLandscape,
            onIntent = onIntent,
            modifier = Modifier
                .widthIn(max = ONBOARDING_MAX_WIDTH)
                .padding(start = 24.dp, end = 24.dp, bottom = if (isLandscape) 0.dp else 8.dp),
        )
    }
}

@Composable
private fun AnimatedOnboardingMessage(
    pageIndex: Int,
    state: OnboardingState,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
) {
    AnimatedContent(
        targetState = pageIndex,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { onboardingTransition(targetState > initialState) },
        contentAlignment = Alignment.Center,
        label = "onboarding page",
    ) { targetPageIndex ->
        OnboardingMessage(ONBOARDING_PAGES[targetPageIndex], state, isLandscape, onIntent)
    }
}

private fun onboardingTransition(forward: Boolean): ContentTransform {
    val enteringOffset: (Int) -> Int = { width -> if (forward) width else -width }
    val leavingOffset: (Int) -> Int = { width -> if (forward) -width else width }
    return slideInHorizontally(onboardingTween(), enteringOffset)
        .togetherWith(slideOutHorizontally(onboardingTween(), leavingOffset))
}

private fun onboardingTween() = tween<IntOffset>(ONBOARDING_TRANSITION_MILLIS, easing = FastOutSlowInEasing)

private const val ONBOARDING_TRANSITION_MILLIS = 320
