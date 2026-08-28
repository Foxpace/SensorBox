package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.content.Context
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.handlers.StorageHandler
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class ServiceControllerFactory @Inject constructor(
    private val storage: StorageHandler,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
) {
    fun create(context: Context, request: MeasurementLaunchRequest, scope: CoroutineScope) = ServiceController(
        context = context,
        request = request,
        scope = scope,
        storage = storage,
        diagnosticLogger = diagnosticLogger,
        clock = clock,
    )
}
