package com.tomasrepcik.sensorbox.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppFailureStore
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.recording.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.domain.recording.RecordingArchiveSelection
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
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        OnboardingState(recordingArchivePath = readRecordingArchivePath()),
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

    fun handleRecordingArchiveResult(selection: RecordingArchiveSelection) {
        if (selection is RecordingArchiveSelection.Cancelled) return

        val result = recordingArchive.select(selection as RecordingArchiveSelection.Selected)
        result.errorOrNull()?.let(appFailures::show)
        mutableState.value = state.value.copy(
            recordingArchivePath = readRecordingArchivePath(),
            errorCode = result.errorOrNull()?.code,
        )
    }

    fun reportFailure(error: com.tomasrepcik.sensorbox.core.error.AppError) {
        appFailures.show(error)
    }

    private fun completeOnboarding() {
        when (val selected = recordingArchive.isSelected()) {
            is AppResult.Failure -> {
                appFailures.show(selected.error)
                return
            }

            is AppResult.Success -> if (!selected.value) {
                mutableState.value = state.value.copy(errorCode = AppErrorCode.STORAGE)
                return
            }
        }
        viewModelScope.launch {
            val accepted = preferencesRepository.dispatch(AppPreferencesIntent.AcceptPolicy)
            if (accepted is AppResult.Failure) {
                appFailures.show(accepted.error)
                return@launch
            }
            when (val completed = preferencesRepository.dispatch(AppPreferencesIntent.CompleteIntro)) {
                is AppResult.Success -> mutableEffects.send(OnboardingEffect.Navigate(MainRoute.RECORD))
                is AppResult.Failure -> appFailures.show(completed.error)
            }
        }
    }

    private fun readRecordingArchivePath(): String? = recordingArchive.path().fold(
        onSuccess = { it },
        onFailure = {
            appFailures.show(it)
            null
        },
    )
}
