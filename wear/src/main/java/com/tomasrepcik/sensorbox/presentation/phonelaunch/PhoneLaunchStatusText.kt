package com.tomasrepcik.sensorbox.presentation.phonelaunch

import com.tomasrepcik.sensorbox.R

internal fun statusText(status: PhoneLaunchStatus): Int = when (status) {
    PhoneLaunchStatus.IDLE -> R.string.phone_ready
    PhoneLaunchStatus.PHONE_UNAVAILABLE -> R.string.norespond
    PhoneLaunchStatus.LAUNCHING -> R.string.open_module
    PhoneLaunchStatus.SENT -> R.string.phone_opened
    PhoneLaunchStatus.FAILED -> R.string.error_toast
}
