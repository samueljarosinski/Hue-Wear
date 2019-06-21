package io.github.samueljarosinski.huewear.hue

import android.graphics.Color
import androidx.annotation.ColorInt
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge
import com.philips.lighting.hue.sdk.wrapper.domain.HueError
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor
import timber.log.Timber

private const val TRANSITION_TIME = 1

class LightsController(
    private val bridge: Bridge
) {

    private val lightState: LightState = LightState().setTransitionTime(TRANSITION_TIME)

    fun setColor(@ColorInt color: Int) {
        Timber.v("Setting color.")

        val rgb = HueColor.RGB(Color.red(color), Color.green(color), Color.blue(color))

        bridge.bridgeState.lights
            .filter { it.lightState.isOn }
            .forEach {
                lightState.setXYWithColor(
                    HueColor(rgb, it.configuration.modelIdentifier, it.configuration.swVersion)
                )

                it.updateState(
                    lightState, BridgeConnectionType.LOCAL, LightStateUpdateResponseCallback(it, lightState)
                )
            }
    }

    fun setBrightness(brightness: Int) {
        Timber.d("Setting brightness $brightness.")

        bridge.bridgeState.lights
            .filter { it.lightState.isOn }
            .forEach {
                lightState.brightness = brightness
                it.updateState(
                    lightState, BridgeConnectionType.LOCAL, LightStateUpdateResponseCallback(it, lightState)
                )
            }
    }
}

internal class LightStateUpdateResponseCallback(
    private val light: LightPoint,
    private val lightState: LightState
) : BridgeResponseCallback() {

    override fun handleCallback(
        bridge: Bridge,
        returnCode: ReturnCode,
        clipResponses: List<ClipResponse>,
        errorList: List<HueError>
    ) {
        when {
            returnCode == ReturnCode.CANCELED -> {
                Timber.w("Changing light cancelled for %s, retrying.", light.identifier)

                light.updateState(lightState, BridgeConnectionType.LOCAL, this)
            }

            returnCode != ReturnCode.SUCCESS  -> {
                Timber.e("Error changing light %s: %s.", light.identifier, returnCode)

                errorList.forEach { error -> Timber.e(error.toString()) }
            }
        }
    }
}
