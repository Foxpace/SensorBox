package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
internal fun OnboardingHeader(pageIndex: Int, onIntent: (OnboardingIntent) -> Unit, modifier: Modifier = Modifier) {
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
internal fun OnboardingMessage(
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
        OnboardingPageButtons(page.prompt, state.recordingArchivePath, onIntent)
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
            OnboardingPageButtons(page.prompt, state.recordingArchivePath, onIntent)
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
private fun OnboardingPageButtons(prompt: OnboardingPrompt, path: String?, onIntent: (OnboardingIntent) -> Unit) {
    when (prompt) {
        OnboardingPrompt.NONE -> Unit
        OnboardingPrompt.POLICIES -> PolicyButtons(onIntent)
        OnboardingPrompt.BATTERY -> BatteryButton(onIntent)
        OnboardingPrompt.ARCHIVE -> ArchiveButton(path, onIntent)
    }
}

@Composable
private fun PolicyButtons(onIntent: (OnboardingIntent) -> Unit) {
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
private fun BatteryButton(onIntent: (OnboardingIntent) -> Unit) {
    SensorBoxSecondaryButton(
        label = stringResource(R.string.intro_battery_action),
        onClick = { onIntent(OnboardingIntent.RequestBatteryOptimizationExemption) },
        modifier = Modifier.fillMaxWidth(),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun ArchiveButton(path: String?, onIntent: (OnboardingIntent) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SensorBoxSecondaryButton(
            label = stringResource(
                if (path == null) R.string.intro_archive_action else R.string.intro_archive_change_action,
            ),
            onClick = { onIntent(OnboardingIntent.ChooseRecordingArchive) },
            modifier = Modifier.fillMaxWidth(),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (path != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.intro_archive_selected, path),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun OnboardingControls(
    pageIndex: Int,
    archiveSelected: Boolean,
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
            enabled = !isLastPage || archiveSelected,
        )
    }
}

@Composable
private fun OnboardingProgress(pageIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
