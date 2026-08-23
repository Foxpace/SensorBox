package com.motionapps.wearoslib.connectivity

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.suspendAppResult
import com.motionapps.sensorbox.core.error.suspendFlatMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayWearConnectionRepository @Inject constructor(@ApplicationContext context: Context) :
    WearConnectionRepository {
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    override fun observeCapability(capability: String): Flow<WearConnection> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener { info ->
            trySend(info.toConnection())
        }
        capabilityClient.addListener(listener, capability).await()
        trySend(loadConnection(capability))
        awaitClose { capabilityClient.removeListener(listener) }
    }.catch { error ->
        AppError.from(AppErrorCode.CONNECTIVITY, "Observe Wear connection", error)
        emit(WearConnection.Disconnected)
    }

    override suspend fun findNode(capability: String): WearNode? {
        val info = capabilityClient
            .getCapability(capability, CapabilityClient.FILTER_REACHABLE)
            .await()
        return WearNodeSelector.select(info.nodes.map { it.toWearNode() })
    }

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> =
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Find Wear node") { findNode(capability) }
            .suspendFlatMap { node ->
                if (node == null) {
                    AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Find reachable Wear node for $capability"))
                } else {
                    suspendAppResult(AppErrorCode.CONNECTIVITY, "Send Wear message") {
                        messageClient.sendMessage(node.id, path, payload).await()
                        Unit
                    }
                }
            }

    private suspend fun loadConnection(capability: String): WearConnection = findNode(capability)
        ?.let(WearConnection::Connected)
        ?: WearConnection.Disconnected

    private fun CapabilityInfo.toConnection(): WearConnection = WearNodeSelector
        .select(nodes.map { it.toWearNode() })
        ?.let(WearConnection::Connected)
        ?: WearConnection.Disconnected

    private fun Node.toWearNode() = WearNode(
        id = id,
        displayName = displayName,
        isNearby = isNearby,
    )
}
