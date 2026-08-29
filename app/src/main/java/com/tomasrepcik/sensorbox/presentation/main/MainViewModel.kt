package com.tomasrepcik.sensorbox.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.error.AppFailureStore
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val sessionStore: RecordingSessionStore,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MainState())
    private var hasChosenInitialRoute = false

    val state: StateFlow<MainState> = mutableState.asStateFlow()

    init {
        observePreferences()
        observeSession()
        observeFailures()
    }

    fun navigate(route: MainRoute) {
        hasChosenInitialRoute = true
        mutableState.value = state.value.copy(route = route)
    }

    fun showPrivacyRationale() {
        navigate(MainRoute.PRIVACY)
    }

    fun dismissFailure() {
        appFailures.dismiss()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                result.fold(
                    onSuccess = { preferences ->
                        val route = if (hasChosenInitialRoute) {
                            state.value.route
                        } else if (
                            preferences.onboarding.hasCompletedIntro &&
                            preferences.onboarding.hasAcceptedPolicy
                        ) {
                            MainRoute.RECORD
                        } else {
                            MainRoute.ONBOARDING
                        }
                        hasChosenInitialRoute = true
                        mutableState.value = state.value.copy(
                            route = route,
                            keepScreenAwake = preferences.display.keepPhoneDisplayOn,
                            hasLoadedPreferences = true,
                        )
                    },
                    onFailure = { error ->
                        mutableState.value = state.value.copy(hasLoadedPreferences = true)
                        appFailures.show(error)
                    },
                )
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                mutableState.value = state.value.copy(session = session)
            }
        }
    }

    private fun observeFailures() {
        viewModelScope.launch {
            appFailures.visibleFailure.collect { failure ->
                mutableState.value = state.value.copy(visibleFailureCode = failure?.code)
            }
        }
    }
}
