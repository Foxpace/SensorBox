package com.tomasrepcik.sensorbox.wearoslib.connection

data class WearNode(val id: String, val displayName: String, val isNearby: Boolean)

sealed interface WearConnection {
    data object Disconnected : WearConnection

    data class Connected(val node: WearNode) : WearConnection
}
