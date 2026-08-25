package com.motionapps.sensorbox.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R

@Composable
fun OnboardingScreen(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val pageIndex = state.page.coerceIn(ONBOARDING_PAGES.indices)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isLandscape = maxWidth > maxHeight
        OnboardingLayout(pageIndex, state, isLandscape, onIntent)
    }
}

@Composable
private fun OnboardingLayout(
    pageIndex: Int,
    state: OnboardingState,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = if (isLandscape) 12.dp else 32.dp),
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
            hasStorage = state.storagePath != null,
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

@Composable
private fun OnboardingHeader(pageIndex: Int, onIntent: (OnboardingIntent) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(52.dp)) {
        if (pageIndex > 0) {
            SensorBoxBackButton(
                label = stringResource(R.string.intro_back),
                onClick = { onIntent(OnboardingIntent.RetreatOnboarding) },
                modifier = Modifier.align(Alignment.CenterStart),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun OnboardingProgress(pageIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ONBOARDING_PAGES.indices.forEach { index ->
            val color = if (index == pageIndex) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.32f)
            }
            Box(
                Modifier
                    .size(if (index == pageIndex) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun OnboardingMessage(
    page: OnboardingPage,
    state: OnboardingState,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
) {
    if (isLandscape) {
        LandscapeOnboardingMessage(page, state, onIntent)
    } else {
        PortraitOnboardingMessage(page, state, onIntent)
    }
}

@Composable
private fun PortraitOnboardingMessage(
    page: OnboardingPage,
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = ONBOARDING_MESSAGE_MAX_WIDTH)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        OnboardingIllustration(page)
        Spacer(Modifier.height(24.dp))
        OnboardingText(page)
        Spacer(Modifier.height(18.dp))
        OnboardingPageActions(page.action, state.storagePath, onIntent)
    }
}

@Composable
private fun LandscapeOnboardingMessage(
    page: OnboardingPage,
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = ONBOARDING_MAX_WIDTH)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingIllustration(page, imageSize = 80.dp)
        Spacer(Modifier.width(32.dp))
        Column(
            modifier = Modifier.widthIn(max = ONBOARDING_MESSAGE_MAX_WIDTH).weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingText(page)
            Spacer(Modifier.height(12.dp))
            OnboardingPageActions(page.action, state.storagePath, onIntent)
        }
    }
}

@Composable
private fun OnboardingIllustration(page: OnboardingPage, imageSize: Dp = 112.dp) {
    Image(
        painter = painterResource(page.image),
        contentDescription = stringResource(page.title),
        modifier = Modifier.size(imageSize),
        colorFilter = if (page.tintIllustration) {
            ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            null
        },
    )
}

@Composable
private fun OnboardingText(page: OnboardingPage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(page.body),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun OnboardingPageActions(action: OnboardingAction, path: String?, onIntent: (OnboardingIntent) -> Unit) {
    when (action) {
        OnboardingAction.NONE -> Unit
        OnboardingAction.POLICIES -> PolicyActions(onIntent)
        OnboardingAction.BATTERY -> BatteryAction(onIntent)
        OnboardingAction.STORAGE -> StorageAction(path, onIntent)
    }
}

@Composable
private fun PolicyActions(onIntent: (OnboardingIntent) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SensorBoxSecondaryButton(
            label = stringResource(R.string.intro_policy_button),
            onClick = { onIntent(OnboardingIntent.OpenPrivacyPolicy) },
            modifier = Modifier.weight(1f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        SensorBoxSecondaryButton(
            label = stringResource(R.string.intro_terms_button),
            onClick = { onIntent(OnboardingIntent.OpenTermsOfUse) },
            modifier = Modifier.weight(1f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun BatteryAction(onIntent: (OnboardingIntent) -> Unit) {
    SensorBoxSecondaryButton(
        label = stringResource(R.string.intro_battery_action),
        onClick = { onIntent(OnboardingIntent.RequestBatteryOptimizationExemption) },
        modifier = Modifier.fillMaxWidth(),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun StorageAction(path: String?, onIntent: (OnboardingIntent) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SensorBoxSecondaryButton(
            label = stringResource(
                if (path == null) R.string.intro_storage_action else R.string.intro_storage_change_action,
            ),
            onClick = { onIntent(OnboardingIntent.ChooseStorage) },
            modifier = Modifier.fillMaxWidth(),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (path != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.intro_storage_selected, path),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OnboardingControls(
    pageIndex: Int,
    hasStorage: Boolean,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastPage = pageIndex == ONBOARDING_PAGES.lastIndex
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingProgress(pageIndex)
        Spacer(Modifier.height(if (isLandscape) 12.dp else 20.dp))
        OnboardingPrimaryButton(
            label = stringResource(if (isLastPage) R.string.intro_finish else R.string.next),
            onClick = { onIntent(onboardingForwardIntent(isLastPage)) },
            enabled = !isLastPage || hasStorage,
        )
    }
}

@Composable
private fun OnboardingPrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.widthIn(min = 176.dp, max = 240.dp).height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
            contentColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.62f),
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun onboardingForwardIntent(isLastPage: Boolean): OnboardingIntent =
    if (isLastPage) OnboardingIntent.CompleteOnboarding else OnboardingIntent.AdvanceOnboarding

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @DrawableRes val image: Int,
    val action: OnboardingAction = OnboardingAction.NONE,
    val tintIllustration: Boolean = true,
)

private enum class OnboardingAction { NONE, POLICIES, BATTERY, STORAGE }

private val ONBOARDING_MAX_WIDTH = 960.dp
private val ONBOARDING_MESSAGE_MAX_WIDTH = 520.dp
private const val ONBOARDING_TRANSITION_MILLIS = 320

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        R.string.intro_welcome_title,
        R.string.intro_welcome_body,
        R.drawable.ic_sensorbox_logo,
        tintIllustration = false,
    ),
    OnboardingPage(
        R.string.intro_incognito_title,
        R.string.intro_incognito_body,
        R.drawable.ic_onboarding_private,
    ),
    OnboardingPage(
        R.string.intro_policy_title,
        R.string.intro_policy_body,
        R.drawable.ic_onboarding_policy,
        OnboardingAction.POLICIES,
    ),
    OnboardingPage(
        R.string.intro_lifecycle_title,
        R.string.intro_lifecycle_body,
        R.drawable.ic_onboarding_paused,
    ),
    OnboardingPage(
        R.string.intro_battery_title,
        R.string.intro_battery_body,
        R.drawable.ic_onboarding_battery,
        OnboardingAction.BATTERY,
    ),
    OnboardingPage(
        R.string.intro_storage_title,
        R.string.intro_storage_body,
        R.drawable.ic_onboarding_storage,
        OnboardingAction.STORAGE,
    ),
)
