package com.nerf.launcher.state

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.nerf.launcher.util.SystemModuleController
import com.nerf.launcher.util.SystemModuleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton telemetry source for the Compose launcher.
 *
 * Owns a single [SystemModuleController] instance and re-exposes its snapshots
 * as a [StateFlow] so the [LauncherViewModel] can collect them without holding
 * a lifecycle reference or registering any BroadcastReceivers itself.
 *
 * Lifecycle:
 *   - [start]/[stop] are called from [LauncherViewModel.init] / [onCleared].
 *   - The controller's BroadcastReceivers are registered against
 *     [Application.applicationContext], so there is no Activity leak.
 *
 * Network state (Wi-Fi / transport type) is read on-demand from
 * [ConnectivityManager]; it does not require a persistent receiver.
 */
internal class SystemTelemetryRepository(application: Application) {

    private val appContext: Context = application.applicationContext

    private val _snapshot = MutableStateFlow<SystemModuleSnapshot?>(null)
    val snapshot: StateFlow<SystemModuleSnapshot?> = _snapshot.asStateFlow()

    private val controller = SystemModuleController(appContext) { snap ->
        _snapshot.value = snap
    }

    fun start() {
        controller.start()
    }

    fun stop() {
        controller.stop()
    }

    // ── Network helpers (read synchronously; no persistent listener needed) ──

    /**
     * Returns a human-readable description of the active network transport.
     * Examples: "WI-FI", "MOBILE DATA", "ETHERNET", "OFFLINE"
     */
    fun activeTransportLabel(): String {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "OFFLINE"
        val caps = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE DATA"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)      -> "VPN LINK"
            else                                                       -> "ONLINE"
        }
    }

    /**
     * Returns the Wi-Fi signal strength as an RSSI label, or null if not on Wi-Fi.
     * Reads [WifiManager.connectionInfo] which is synchronous and permission-free.
     */
    fun wifiSignalLabel(): String? {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        @Suppress("DEPRECATION")   // WifiManager.connectionInfo is deprecated API 31 but still works and requires no extra perms
        val wm = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val rssi = wm.connectionInfo?.rssi ?: return null
        return when {
            rssi >= -50 -> "SIGNAL STRONG"
            rssi >= -70 -> "SIGNAL GOOD"
            rssi >= -80 -> "SIGNAL FAIR"
            else        -> "SIGNAL WEAK"
        }
    }
}
