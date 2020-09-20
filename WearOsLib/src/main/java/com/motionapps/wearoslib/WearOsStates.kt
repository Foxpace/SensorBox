package com.motionapps.wearoslib

sealed class WearOsStates {
    class PresenceResult(val present: Boolean): WearOsStates() // if the Wear Os is present
    // Memory status
    class Status(val running: Boolean, val measurementsToSync: Int, val sizeOfData: Double, val totalNumberOfFiles: Int ): WearOsStates()
    // to turn off Wear Os
    object Offline: WearOsStates()
    // waiting for the result
    object AwaitResult: WearOsStates()
}