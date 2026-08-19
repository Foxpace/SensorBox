package com.motionapps.sensorbox.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R

@Composable
fun OnboardingScreen(state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val pageIndex = state.page.coerceIn(ONBOARDING_PAGES.indices)
    val page = ONBOARDING_PAGES[pageIndex]
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isLandscape = maxWidth > maxHeight
        OnboardingLayout(pageIndex, page, state, isLandscape, onIntent)
    }
}

@Composable
private fun OnboardingLayout(
    pageIndex: Int,
    page: OnboardingPage,
    state: OnboardingState,
    isLandscape: Boolean,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = ONBOARDING_MAX_WIDTH)
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = if (isLandscape) 12.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingHeader(pageIndex, onIntent)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            OnboardingMessage(page, state, isLandscape, onIntent)
        }
        OnboardingControls(
            pageIndex = pageIndex,
            hasStorage = state.storagePath != null,
            isLandscape = isLandscape,
            onIntent = onIntent,
            modifier = Modifier.padding(bottom = if (isLandscape) 0.dp else 8.dp),
        )
    }
}

@Composable
private fun OnboardingHeader(pageIndex: Int, onIntent: (OnboardingIntent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(52.dp)) {
            if (pageIndex > 0) {
                SensorBoxBackButton(
                    label = stringResource(R.string.intro_back),
                    onClick = { onIntent(OnboardingIntent.RetreatOnboarding) },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            Text(
                stringResource(R.string.intro_page_progress, pageIndex + 1, ONBOARDING_PAGES.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.CenterEnd),
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
            val color = if (index <= pageIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
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
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingIllustration(page, containerSize = 104.dp, imageSize = 64.dp)
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
private fun OnboardingIllustration(page: OnboardingPage, containerSize: Dp = 124.dp, imageSize: Dp = 76.dp) {
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(page.image),
            contentDescription = stringResource(page.title),
            modifier = Modifier.size(imageSize),
        )
    }
}

@Composable
private fun OnboardingText(page: OnboardingPage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(page.body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        )
        SensorBoxSecondaryButton(
            label = stringResource(R.string.intro_terms_button),
            onClick = { onIntent(OnboardingIntent.OpenTermsOfUse) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BatteryAction(onIntent: (OnboardingIntent) -> Unit) {
    SensorBoxSecondaryButton(
        label = stringResource(R.string.intro_battery_action),
        onClick = { onIntent(OnboardingIntent.RequestBatteryOptimizationExemption) },
        modifier = Modifier.fillMaxWidth(),
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
        )
        if (path != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.intro_storage_selected, path),
                color = MaterialTheme.colorScheme.primary,
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
        SensorBoxPrimaryButton(
            label = stringResource(if (isLastPage) R.string.intro_finish else R.string.next),
            onClick = { onIntent(onboardingForwardIntent(isLastPage)) },
            modifier = Modifier.widthIn(max = ONBOARDING_MESSAGE_MAX_WIDTH).fillMaxWidth(),
            enabled = !isLastPage || hasStorage,
        )
    }
}

private fun onboardingForwardIntent(isLastPage: Boolean): OnboardingIntent =
    if (isLastPage) OnboardingIntent.CompleteOnboarding else OnboardingIntent.AdvanceOnboarding

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @DrawableRes val image: Int,
    val action: OnboardingAction = OnboardingAction.NONE,
)

private enum class OnboardingAction { NONE, POLICIES, BATTERY, STORAGE }

private val ONBOARDING_MAX_WIDTH = 960.dp
private val ONBOARDING_MESSAGE_MAX_WIDTH = 520.dp

private val ONBOARDING_PAGES = listOf(
    OnboardingPage(
        R.string.intro_welcome_title,
        R.string.intro_welcome_body,
        R.drawable.ic_launcher_historic_round,
    ),
    OnboardingPage(
        R.string.intro_incognito_title,
        R.string.intro_incognito_body,
        R.drawable.ic_incognito,
    ),
    OnboardingPage(
        R.string.intro_policy_title,
        R.string.intro_policy_body,
        R.drawable.ic_bug,
        OnboardingAction.POLICIES,
    ),
    OnboardingPage(
        R.string.intro_lifecycle_title,
        R.string.intro_lifecycle_body,
        R.drawable.ic_android_big,
    ),
    OnboardingPage(
        R.string.intro_battery_title,
        R.string.intro_battery_body,
        R.drawable.ic_battery,
        OnboardingAction.BATTERY,
    ),
    OnboardingPage(
        R.string.intro_storage_title,
        R.string.intro_storage_body,
        R.drawable.ic_folder,
        OnboardingAction.STORAGE,
    ),
)
