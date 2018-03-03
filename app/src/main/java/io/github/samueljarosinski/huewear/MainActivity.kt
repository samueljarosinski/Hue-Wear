package io.github.samueljarosinski.huewear

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.Toast
import io.github.samueljarosinski.huewear.hue.HueController
import io.github.samueljarosinski.huewear.hue.MIN_UPDATE_DELAY
import io.github.samueljarosinski.huewear.hue.OnHueConnectionListener
import timber.log.Timber

class MainActivity : WearableActivity() {

    private lateinit var handleView: View
    private lateinit var progressView: View
    private lateinit var networkController: NetworkController

    private val hueController: HueController = HueController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        handleView = findViewById(R.id.handle)
        progressView = findViewById(R.id.progress)

        networkController = NetworkController(this, onNetworkAvailable = ::startHue)
    }

    override fun onStart() {
        super.onStart()

        progressView.visibility = View.VISIBLE
        handleView.visibility = View.GONE

        networkController.connect()
    }

    override fun onStop() {
        Timber.d("Stopping session.")

        hueController.stop()
        networkController.disconnect()

        super.onStop()
    }

    private fun startHue() {
        hueController.start(object : OnHueConnectionListener {

            override fun onConnected() {
                startSession()
            }

            override fun onConnectionError() {
                Toast.makeText(this@MainActivity, R.string.connection_error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startSession() {
        Timber.d("Starting session.")

        runOnUiThread {
            progressView.visibility = View.GONE
            handleView.visibility = View.VISIBLE
        }

        val colorExtractor = ColorExtractor(findViewById(R.id.palette), MIN_UPDATE_DELAY, onColorExtracted = { color ->
            hueController.setColor(color)
            (handleView.background as GradientDrawable).setColor(color)
        })

        HandleController(handleView, onMove = colorExtractor::extract, onScroll = hueController::setBrightness)
    }
}
