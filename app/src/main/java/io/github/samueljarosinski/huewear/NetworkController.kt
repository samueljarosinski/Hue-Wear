package io.github.samueljarosinski.huewear

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.content.systemService
import timber.log.Timber

typealias OnNetworkAvailableListener = () -> Unit

class NetworkController(
    private val context: Context,
    private val onNetworkAvailable: OnNetworkAvailableListener
) : NetworkCallback() {

    private val connectivityManager: ConnectivityManager get() = context.systemService()

    fun connect() {
        Timber.d("Connecting to WiFi.")

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.requestNetwork(request, this)
    }

    fun disconnect() {
        Timber.d("Disconnecting from WiFi.")

        connectivityManager.apply {
            bindProcessToNetwork(null)
            unregisterNetworkCallback(this@NetworkController)
        }
    }

    override fun onAvailable(network: Network) {
        if (connectivityManager.bindProcessToNetwork(network)) {
            Timber.d("Connected to WiFi.")

            onNetworkAvailable()
        } else {
            Timber.e("WiFi not available.")
        }
    }
}
