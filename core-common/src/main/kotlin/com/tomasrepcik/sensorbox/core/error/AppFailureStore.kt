package com.tomasrepcik.sensorbox.core.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppFailureStore(private val diagnosticLogger: DiagnosticLogger) {
    private val mutableVisibleFailure = MutableStateFlow<AppError?>(null)

    val visibleFailure: StateFlow<AppError?> = mutableVisibleFailure.asStateFlow()

    fun show(error: AppError) {
        recordOnly(error)
        mutableVisibleFailure.compareAndSet(expect = null, update = error)
    }

    fun recordOnly(error: AppError) {
        diagnosticLogger.record(error.toDiagnosticEvent())
    }

    fun dismiss() {
        mutableVisibleFailure.value = null
    }
}
