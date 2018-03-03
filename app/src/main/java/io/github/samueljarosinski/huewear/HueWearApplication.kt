package io.github.samueljarosinski.huewear

import android.app.Application
import android.os.Build
import com.philips.lighting.hue.sdk.wrapper.HueLog
import com.philips.lighting.hue.sdk.wrapper.Persistence
import timber.log.Timber

class HueWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Persistence.setStorageLocation(filesDir.absolutePath, Build.MODEL)
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO, HueLog.LogComponent.ALL)
    }

    companion object {
        init {
            System.loadLibrary("huesdk")
        }
    }
}
