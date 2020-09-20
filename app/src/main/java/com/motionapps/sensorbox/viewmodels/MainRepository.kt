package com.motionapps.sensorbox.viewmodels

import com.motionapps.countdowndialog.CountDownMain
import com.motionapps.sensorbox.fragments.advanced.extrahandlers.AlarmHandler
import com.motionapps.sensorbox.fragments.advanced.extrahandlers.NoteHandler
import com.motionapps.sensorbox.uiHandlers.SensorViewHandler
import com.motionapps.wearoslib.WearOsHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * Main object, which are used in MainActivity
 *
 * @property sensorViewHandler - handles sensors to measure
 * @property countDownMain     - handles counting of the SHORT type  - used as part of AdvancedMeasurement
 * @property noteHandler       - saves notes to JSON - used as part of AdvancedMeasurement
 * @property alarmHandler      - saves time intervals for alarms - used as part of AdvancedMeasurement
 * @property wearOsHandler     - manages Wear Os, if available
 */
class MainRepository(
    val sensorViewHandler: SensorViewHandler,
    val countDownMain: CountDownMain,
    val noteHandler: NoteHandler,
    val alarmHandler: AlarmHandler,
    val wearOsHandler: WearOsHandler
)