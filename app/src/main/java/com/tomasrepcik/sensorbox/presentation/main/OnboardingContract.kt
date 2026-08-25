package com.tomasrepcik.sensorbox.presentation.main

import com.tomasrepcik.sensorbox.core.error.AppErrorCode

data class OnboardingState(val page: Int = 0, val storagePath: String? = null, val errorCode: AppErrorCode? = null)

sealed interface OnboardingIntent {
    data object AdvanceOnboarding : OnboardingIntent
    data object RetreatOnboarding : OnboardingIntent
    data object CompleteOnboarding : OnboardingIntent
    data object OpenPrivacyPolicy : OnboardingIntent
    data object OpenTermsOfUse : OnboardingIntent
    data object RequestBatteryOptimizationExemption : OnboardingIntent
    data object ChooseStorage : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object PickStorageDirectory : OnboardingEffect
    data object OpenPrivacyPolicy : OnboardingEffect
    data object OpenTermsOfUse : OnboardingEffect
    data object RequestBatteryOptimizationExemption : OnboardingEffect
    data class Navigate(val route: MainRoute) : OnboardingEffect
}
