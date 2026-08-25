package com.tomasrepcik.sensorbox.wearoslib.connectivity

internal object WearNodeSelector {
    fun select(nodes: Collection<WearNode>): WearNode? =
        nodes.firstOrNull(WearNode::isNearby) ?: nodes.minByOrNull(WearNode::id)
}
