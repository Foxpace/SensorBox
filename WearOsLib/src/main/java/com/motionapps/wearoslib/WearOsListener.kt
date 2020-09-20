package com.motionapps.wearoslib

interface WearOsListener {

    suspend fun onWearOsStates(wearOsStates: WearOsStates)

}