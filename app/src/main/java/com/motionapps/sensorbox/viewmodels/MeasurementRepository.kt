package com.motionapps.sensorbox.viewmodels

import com.motionapps.countdowndialog.CountDownMain
import com.motionapps.sensorbox.uiHandlers.GraphUpdater
import com.motionapps.wearoslib.WearOsHandler

/**
 * Used by MeasurementActivity's ViewModel
 * @property countDownMain - handles countdown
 * @property graphUpdater - updates chart
 * @property wearOsHandler - handles Wear Os responses
 */
class MeasurementRepository(
    val countDownMain: CountDownMain,
    val graphUpdater: GraphUpdater,
    val wearOsHandler: WearOsHandler
)