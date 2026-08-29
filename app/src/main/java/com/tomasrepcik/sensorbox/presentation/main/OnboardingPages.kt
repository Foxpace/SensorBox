package com.tomasrepcik.sensorbox.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

internal data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @DrawableRes val image: Int,
    val prompt: OnboardingPrompt = OnboardingPrompt.NONE,
    val tintIllustration: Boolean = true,
)

internal enum class OnboardingPrompt { NONE, POLICIES, BATTERY, ARCHIVE }

internal val ONBOARDING_MAX_WIDTH = 960.dp
internal val ONBOARDING_MESSAGE_MAX_WIDTH = 520.dp

internal val ONBOARDING_PAGES = listOf(
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
        OnboardingPrompt.POLICIES,
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
        OnboardingPrompt.BATTERY,
    ),
    OnboardingPage(
        R.string.intro_archive_title,
        R.string.intro_archive_body,
        R.drawable.ic_onboarding_storage,
        OnboardingPrompt.ARCHIVE,
    ),
)
