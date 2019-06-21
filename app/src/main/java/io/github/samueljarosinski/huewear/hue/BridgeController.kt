package io.github.samueljarosinski.huewear.hue

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
import io.github.samueljarosinski.huewear.hue.BridgeDiscoveryCallback.OnBridgeDiscoveryListener
import timber.log.Timber

internal class BridgeController {

    private var bridge: Bridge? = null
    private var bridgeDiscovery: BridgeDiscovery? = null
    private var bridgeConnectedListener: OnBridgeConnectedListener? = null

    fun connect(onBridgeConnectedListener: OnBridgeConnectedListener) {
        bridgeConnectedListener = onBridgeConnectedListener

        val knownBridge = getLastConnectedBridge()

        if (knownBridge == null || knownBridge.ipAddress.isNullOrEmpty()) {
            startBridgeDiscovery()
        } else {
            connectToBridge(knownBridge.ipAddress, knownBridge.uniqueId)
        }
    }

    fun disconnect() {
        stopBridgeDiscovery()
        disconnectFromBridge()

        bridgeConnectedListener = null
    }

    private fun getLastConnectedBridge(): KnownBridge? {
        val knownBridges = KnownBridges.getAll()
        Timber.v("Found ${knownBridges.size} known bridge(s).")

        return if (knownBridges.isEmpty()) null else knownBridges.maxBy { it.lastConnected }
    }

    private fun startBridgeDiscovery() {
        disconnectFromBridge()

        Timber.d("Starting bridge discovery.")

        bridgeDiscovery = BridgeDiscovery().apply {
            search(
                BridgeDiscovery.BridgeDiscoveryOption.ALL,
                BridgeDiscoveryCallback(createOnBridgeDiscoveryListener())
            )
        }
    }

    private fun createOnBridgeDiscoveryListener(): OnBridgeDiscoveryListener =
        object : OnBridgeDiscoveryListener {

            override fun onNoBridgesFound() {
                stopBridgeDiscovery()
                bridgeConnectedListener?.onNoBridgesFound()
            }

            override fun onBridgeDiscovered(ipAddress: String, bridgeId: String) =
                connectToBridge(ipAddress, bridgeId)
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

        if (bridgeConnectedListener == null) return

        bridge = BridgeBuilder(APP_NAME, DEVICE_NAME)
            .setConnectionType(BridgeConnectionType.LOCAL)
            .setIpAddress(ipAddress)
            .setBridgeId(bridgeId)
            .addBridgeStateUpdatedCallback(BridgeStateUpdatedCallback(bridgeConnectedListener!!))
            .setBridgeConnectionCallback(BridgeConnectionCallback(bridgeConnectedListener!!))
            .build()

        bridge?.let {
            Timber.d("Connecting to bridge with IP $ipAddress.")

            it.connect()
        }
    }

    private fun disconnectFromBridge() {
        bridge?.let {
            Timber.d("Disconnecting from bridge.")

            it.disconnect()
            bridge = null
        }
    }
}

private class BridgeDiscoveryCallback(
    private val onBridgeDiscoveryListener: OnBridgeDiscoveryListener
) : BridgeDiscoveryNonUICallback() {

    override fun onFinished(results: List<BridgeDiscoveryResult>, returnCode: ReturnCode) {
        when (returnCode) {
            ReturnCode.SUCCESS -> {
                if (results.isNotEmpty()) {
                    Timber.d("Found ${results.size} bridge(s) in the network.")
                    onBridgeDiscoveryListener.onBridgeDiscovered(results[0].ip, results[0].uniqueID)
                } else {
                    Timber.d("No bridges found!")
                    onBridgeDiscoveryListener.onNoBridgesFound()
                }
            }

            ReturnCode.STOPPED -> Timber.d("Bridge discovery stopped.")

            else               -> Timber.e("Error doing bridge discovery: $returnCode.")
        }
    }

    interface OnBridgeDiscoveryListener {
        fun onNoBridgesFound()
        fun onBridgeDiscovered(ipAddress: String, bridgeId: String)
    }
}

private class BridgeConnectionCallback(
    private val onBridgeConnectedListener: OnBridgeConnectedListener
) : BridgeConnectionNonUICallback() {

    override fun onConnectionEvent(bridgeConnection: BridgeConnection, connectionEvent: ConnectionEvent) {
        Timber.v("Bridge connection event: $connectionEvent.")

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (connectionEvent) {
            ConnectionEvent.NOT_AUTHENTICATED,
            ConnectionEvent.LINK_BUTTON_NOT_PRESSED -> {
                Timber.d("Not authenticated.")

                onBridgeConnectedListener.onNotAuthenticated()
            }

            ConnectionEvent.AUTHENTICATED           -> {
                Timber.d("Starting heartbeat.")

                bridgeConnection.heartbeatManager.startHeartbeat(
                    BridgeStateCacheType.LIGHTS_AND_GROUPS, HEARTBEAT_INTERVAL
                )
            }
        }
    }

    override fun onConnectionError(bridgeConnection: BridgeConnection, errors: List<HueError>) {
        errors.forEach { error -> Timber.e("Connection error: $error.") }

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

internal interface OnBridgeConnectedListener {
    fun onNoBridgesFound()
    fun onNotAuthenticated()
    fun onConnectionError()
    fun onBridgeConnected(bridge: Bridge)
}
