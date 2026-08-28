package com.tomasrepcik.sensorbox.presentation.main

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.recording.RecordingArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val recordingArchive: RecordingArchiveRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        OnboardingState(recordingArchivePath = recordingArchive.path().getOrNull()),
    )
    private val mutableEffects = Channel<OnboardingEffect>(Channel.BUFFERED)

    val state: StateFlow<OnboardingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun accept(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.AdvanceOnboarding -> mutableState.value = state.value.copy(
                page = state.value.page + 1,
                errorCode = null,
            )

            OnboardingIntent.RetreatOnboarding -> mutableState.value = state.value.copy(
                page = (state.value.page - 1).coerceAtLeast(0),
                errorCode = null,
            )

            OnboardingIntent.CompleteOnboarding -> completeOnboarding()

            OnboardingIntent.ChooseRecordingArchive -> mutableEffects.trySend(OnboardingEffect.PickRecordingArchive)

            OnboardingIntent.OpenPrivacyPolicy -> mutableEffects.trySend(OnboardingEffect.OpenPrivacyPolicy)

            OnboardingIntent.OpenTermsOfUse -> mutableEffects.trySend(OnboardingEffect.OpenTermsOfUse)

            OnboardingIntent.RequestBatteryOptimizationExemption ->
                mutableEffects.trySend(OnboardingEffect.RequestBatteryOptimizationExemption)
        }
    }

    fun handleRecordingArchiveResult(resultIntent: Intent?) {
        val result = resultIntent?.let(recordingArchive::select) ?: AppResult.failure(
            AppError(AppErrorCode.STORAGE, "Select onboarding recording archive"),
        )
        mutableState.value = state.value.copy(
            recordingArchivePath = recordingArchive.path().getOrNull(),
            errorCode = result.errorOrNull()?.code,
        )
    }

    private fun completeOnboarding() {
        if (recordingArchive.isSelected().getOrNull() != true) {
            mutableState.value = state.value.copy(errorCode = AppErrorCode.STORAGE)
            return
        }
        viewModelScope.launch {
            val accepted = preferencesRepository.dispatch(AppPreferencesIntent.AcceptPolicy)
            if (accepted is AppResult.Failure) {
                mutableState.value = state.value.copy(errorCode = accepted.error.code)
                return@launch
            }
            when (val completed = preferencesRepository.dispatch(AppPreferencesIntent.CompleteIntro)) {
                is AppResult.Success -> mutableEffects.send(OnboardingEffect.Navigate(MainRoute.RECORD))
                is AppResult.Failure -> mutableState.value = state.value.copy(errorCode = completed.error.code)
            }
        }
    }
}
