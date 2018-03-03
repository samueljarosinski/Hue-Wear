package io.github.samueljarosinski.huewear.hue

import android.os.Build
import android.support.annotation.ColorInt
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import timber.log.Timber

const val APP_NAME = "Hue Wear"
val DEVICE_NAME: String = Build.MODEL
const val HEARTBEAT_INTERVAL = 10000
const val MIN_UPDATE_DELAY = 100

class HueController {

    private val bridgeController: BridgeController = BridgeController()
    private var lightsController: LightsController? = null
    private var hueConnectionListener: OnHueConnectionListener? = null

    fun start(onHueConnectionListener: OnHueConnectionListener?) {
        Timber.d("Starting Hue.")

        hueConnectionListener = onHueConnectionListener
        bridgeController.connect(object : OnBridgeConnectedListener {

            override fun onBridgeConnected(bridge: Bridge) {
                lightsController = LightsController(bridge)
                hueConnectionListener?.onConnected()
            }

            override fun onConnectionError() {
                hueConnectionListener?.onConnectionError()
            }
        })
    }

    fun stop() {
        Timber.d("Stopping Hue.")

        hueConnectionListener = null
        lightsController = null
        bridgeController.disconnect()
    }

    fun setColor(@ColorInt color: Int) {
        lightsController?.setColor(color)
    }

    fun setBrightness(brightness: Int) {
        lightsController?.setBrightness(brightness)
    }

}

interface OnHueConnectionListener {
    fun onConnected()
    fun onConnectionError()
}
