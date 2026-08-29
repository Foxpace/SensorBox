package com.tomasrepcik.sensorbox.wearoslib.connection

internal object WearNodeSelector {
    fun select(nodes: Collection<WearNode>): WearNode? =
        nodes.firstOrNull(WearNode::isNearby) ?: nodes.minByOrNull(WearNode::id)
}
