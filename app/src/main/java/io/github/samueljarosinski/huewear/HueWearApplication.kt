package io.github.samueljarosinski.huewear

import android.app.Application
import com.philips.lighting.hue.sdk.wrapper.HueLog
import com.philips.lighting.hue.sdk.wrapper.Persistence
import io.github.samueljarosinski.huewear.hue.DEVICE_NAME
import timber.log.Timber

@Suppress("unused")
class HueWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Persistence.setStorageLocation(filesDir.absolutePath, DEVICE_NAME)
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO, HueLog.LogComponent.ALL)
        // HueLog.setFileLogLevel(HueLog.LogLevel.INFO, HueLog.LogComponent.ALL)
    }

    companion object {
        init {
            System.loadLibrary("huesdk")
        }
    }
}
