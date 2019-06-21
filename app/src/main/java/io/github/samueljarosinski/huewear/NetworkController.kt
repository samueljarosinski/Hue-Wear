package io.github.samueljarosinski.huewear

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import timber.log.Timber

typealias OnNetworkAvailableListener = (Boolean) -> Unit

class NetworkController(
    private val connectivityManager: ConnectivityManager,
    private val onNetworkAvailable: OnNetworkAvailableListener
) : NetworkCallback() {

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

        with(connectivityManager) {
            bindProcessToNetwork(null)
            unregisterNetworkCallback(this@NetworkController)
        }
    }

    override fun onAvailable(network: Network) {
        if (connectivityManager.bindProcessToNetwork(network)) {
            Timber.d("Connected to WiFi.")

            onNetworkAvailable(true)
        } else {
            Timber.e("WiFi not available.")

            onNetworkAvailable(false)
        }
    }
}
