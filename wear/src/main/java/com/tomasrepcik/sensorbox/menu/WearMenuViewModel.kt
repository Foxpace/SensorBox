package com.tomasrepcik.sensorbox.menu

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class WearMenuViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(WearMenuState())
    private val mutableEffects = Channel<WearMenuEffect>(Channel.BUFFERED)

    val state: StateFlow<WearMenuState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun accept(intent: WearMenuIntent) {
        val next = WearMenuReducer.reduce(mutableState.value, intent)
        mutableState.value = next.state
        next.effect?.let(mutableEffects::trySend)
    }
}
