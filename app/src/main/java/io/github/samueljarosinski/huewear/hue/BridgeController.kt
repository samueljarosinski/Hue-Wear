package io.github.samueljarosinski.huewear.hue

import android.text.TextUtils
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder
import com.philips.lighting.hue.sdk.wrapper.domain.HueError
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges
import com.philips.lighting.hue.sdk.wrapper.uicallback.BridgeConnectionNonUICallback
import com.philips.lighting.hue.sdk.wrapper.uicallback.BridgeDiscoveryNonUICallback
import com.philips.lighting.hue.sdk.wrapper.uicallback.BridgeStateUpdatedNonUICallback
import timber.log.Timber
import java.util.*

class BridgeController {

    private var bridge: Bridge? = null
    private var bridgeDiscovery: BridgeDiscovery? = null
    private var bridgeConnectedListener: OnBridgeConnectedListener? = null

    private val lastConnectedBridge: KnownBridge?
        get() {
            val knownBridges = KnownBridges.getAll()
            Timber.v("Found ${knownBridges.size} known bridge(s).")

            return if (knownBridges.isEmpty()) {
                null
            } else Collections.max(knownBridges) { a, b ->
                a.lastConnected.compareTo(b.lastConnected)
            }
        }

    fun connect(onBridgeConnectedListener: OnBridgeConnectedListener) {
        bridgeConnectedListener = onBridgeConnectedListener

        val knownBridge = lastConnectedBridge

        if (knownBridge == null || TextUtils.isEmpty(knownBridge.ipAddress)) startBridgeDiscovery()
        else connectToBridge(knownBridge.ipAddress, knownBridge.uniqueId)
    }

    fun disconnect() {
        stopBridgeDiscovery()
        disconnectFromBridge()

        bridgeConnectedListener = null
    }

    private fun startBridgeDiscovery() {
        disconnectFromBridge()

        Timber.d("Starting bridge discovery.")

        bridgeDiscovery = BridgeDiscovery()
        bridgeDiscovery!!.search(BridgeDiscovery.BridgeDiscoveryOption.ALL, BridgeDiscoveryCallback(::connectToBridge))
    }

    private fun stopBridgeDiscovery() {
        bridgeDiscovery?.apply {
            Timber.d("Stopping bridge discovery.")
            stop()
            bridgeDiscovery = null
        }
    }

    private fun connectToBridge(ipAddress: String, bridgeId: String) {
        stopBridgeDiscovery()
        disconnectFromBridge()

        if (bridgeConnectedListener == null) {
            return
        }

        bridge = BridgeBuilder(APP_NAME, DEVICE_NAME)
            .setConnectionType(BridgeConnectionType.LOCAL)
            .setIpAddress(ipAddress)
            .setBridgeId(bridgeId)
            .addBridgeStateUpdatedCallback(BridgeStateUpdatedCallback(bridgeConnectedListener!!))
            .setBridgeConnectionCallback(BridgeConnectionCallback(bridgeConnectedListener!!))
            .build()

        if (bridge != null) {
            Timber.d("Connecting to bridge with IP $ipAddress.")

            bridge!!.connect()
        }
    }

    private fun disconnectFromBridge() {
        bridge?.apply {
            Timber.d("Disconnecting from bridge.")
            disconnect()
            bridge = null
        }
    }
}

private class BridgeDiscoveryCallback(
    private val onBridgeDiscoveredListener: (String, String) -> Unit
) : BridgeDiscoveryNonUICallback() {

    override fun onFinished(results: List<BridgeDiscoveryResult>, returnCode: ReturnCode) {
        when (returnCode) {
            ReturnCode.SUCCESS -> {
                Timber.d("Found ${results.size} bridge(s) in the network.")

                if (results.isNotEmpty()) {
                    onBridgeDiscoveredListener(results[0].ip, results[0].uniqueID)
                } else {
                    Timber.w("No bridges found!")
                }
            }

            ReturnCode.STOPPED -> Timber.d("Bridge discovery stopped.")

            else -> Timber.e("Error doing bridge discovery: $returnCode")
        }
    }
}

private class BridgeConnectionCallback(
    private val onBridgeConnectedListener: OnBridgeConnectedListener
) : BridgeConnectionNonUICallback() {

    override fun onConnectionEvent(bridgeConnection: BridgeConnection, connectionEvent: ConnectionEvent) {
        Timber.v("Bridge connection event: $connectionEvent")

        if (connectionEvent == ConnectionEvent.AUTHENTICATED) {
            Timber.d("Starting heartbeat.")

            val heartbeatManager = bridgeConnection.heartbeatManager
            heartbeatManager.startHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_INTERVAL)
        }
    }

    override fun onConnectionError(bridgeConnection: BridgeConnection, errors: List<HueError>) {
        errors.forEach { error -> Timber.e("Connection error: $error") }

        onBridgeConnectedListener.onConnectionError()
    }
}

private class BridgeStateUpdatedCallback(
    private val onBridgeConnectedListener: OnBridgeConnectedListener
) : BridgeStateUpdatedNonUICallback(null) {

    override fun onBridgeStateUpdated(bridge: Bridge, bridgeStateUpdatedEvent: BridgeStateUpdatedEvent) {
        if (bridgeStateUpdatedEvent == BridgeStateUpdatedEvent.INITIALIZED) {
            Timber.d("Connected to bridge ${bridge.name}.")

            onBridgeConnectedListener.onBridgeConnected(bridge)
        }
    }
}

interface OnBridgeConnectedListener {
    fun onBridgeConnected(bridge: Bridge)
    fun onConnectionError()
}
