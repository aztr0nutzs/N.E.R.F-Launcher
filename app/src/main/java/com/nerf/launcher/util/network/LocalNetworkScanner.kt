package com.nerf.launcher.util.network

import android.content.Context
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.nerf.launcher.BuildConfig
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

// Expanded Data Model
data class NetworkNode(
    val ipAddress: String,
    val hostname: String,
    val macAddress: String,
    val deviceType: String, // e.g., "Mobile", "IoT", "Unknown"
    val isReachable: Boolean,
    val pingMs: Long
)

class LocalNetworkScanner(private val context: Context) {
    companion object {
        private const val TAG = "LocalNetworkScanner"
        private const val HOSTS_PER_SUBNET = 254
        private const val MAX_PARALLEL_PROBES = 24
        private const val PING_TIMEOUT_MS = 500
    }

    private val scanMutex = Mutex()
    private var inFlightScan: Deferred<List<NetworkNode>>? = null

    fun canScanLocalSubnet(): Boolean = getLocalIpAddress() != null

    /**
     * Scans the local /24 subnet for active devices using bounded parallel coroutines
     * and attempts to resolve hardware MAC addresses via the ARP table.
     */
    suspend fun scanLocalSubnet(): List<NetworkNode> = coroutineScope {
        val scanTask = scanMutex.withLock {
            inFlightScan?.takeIf { it.isActive } ?: async(Dispatchers.IO) {
                performSubnetScan()
            }.also { inFlightScan = it }
        }

        try {
            scanTask.await()
        } finally {
            scanMutex.withLock {
                if (inFlightScan === scanTask && !scanTask.isActive) {
                    inFlightScan = null
                }
            }
        }
    }

    private suspend fun performSubnetScan(): List<NetworkNode> = withContext(Dispatchers.IO) {
        val activeNodes = mutableListOf<NetworkNode>()
        val deviceIp = getLocalIpAddress() ?: return@withContext emptyList()
        val failedProbeCount = AtomicInteger(0)
        val firstProbeFailureType = AtomicReference<String?>(null)

        val subnet = deviceIp.substringBeforeLast(".")

        // Phase 1: Bounded ping sweep to populate the ARP table without high resource spikes
        val probeSemaphore = Semaphore(MAX_PARALLEL_PROBES)
        val pingTasks = (1..HOSTS_PER_SUBNET).map { i ->
            async {
                probeSemaphore.withPermit {
                    val ipToTest = "$subnet.$i"
                    pingDevice(ipToTest) { failureType ->
                        failedProbeCount.incrementAndGet()
                        firstProbeFailureType.compareAndSet(null, failureType)
                    }
                }
            }
        }

        // Wait for all pings to complete
        val results = pingTasks.awaitAll().filterNotNull()
        val failedCount = failedProbeCount.get()
        if (failedCount > 0 && BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "Subnet probe completed with $failedCount probe misses. " +
                    "First failure type: ${firstProbeFailureType.get() ?: "unavailable"}"
            )
        }

        // Phase 2: Read ARP table to match MAC addresses to the discovered IPs
        val arpTable = readArpTable()

        // Phase 3: Construct the final enriched node data
        for (basicNode in results) {
            val mac = arpTable[basicNode.ipAddress] ?: "UNKNOWN:MAC"
            val type = guessDeviceType(mac)
            
            activeNodes.add(
                NetworkNode(
                    ipAddress = basicNode.ipAddress,
                    hostname = basicNode.hostname,
                    macAddress = mac,
                    deviceType = type,
                    isReachable = basicNode.isReachable,
                    pingMs = basicNode.pingMs
                )
            )
        }

        return@withContext activeNodes
    }

    private fun pingDevice(
        ipAddress: String,
        onProbeFailure: (String) -> Unit
    ): NetworkNode? {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val startTime = System.currentTimeMillis()
            val isReachable = inetAddress.isReachable(PING_TIMEOUT_MS)
            val pingTime = System.currentTimeMillis() - startTime

            if (isReachable) {
                // Return a partial node; MAC and Type will be filled in Phase 3
                NetworkNode(
                    ipAddress = ipAddress,
                    hostname = inetAddress.hostName,
                    macAddress = "",
                    deviceType = "",
                    isReachable = true,
                    pingMs = pingTime
                )
            } else {
                null
            }
        } catch (e: Exception) {
            onProbeFailure(e.javaClass.simpleName)
            null
        }
    }

    /**
     * Reads the Linux ARP cache to map IP addresses to physical MAC addresses.
     * Note: Requires specific permissions or system privileges on Android 10+.
     */
    private fun readArpTable(): Map<String, String> {
        val arpMap = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line?.split(Regex(" +"))
                    if (parts != null && parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        
                        // Ignore header lines and blank MACs
                        if (mac.matches(Regex("..:..:..:..:..:..")) && mac != "00:00:00:00:00:00") {
                            arpMap[ip] = mac.uppercase()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "ARP table unavailable; skipping MAC enrichment (${e.javaClass.simpleName}).")
            }
            // Fallback: If ARP is blocked, return empty map.
        }
        return arpMap
    }

    /**
     * A lightweight framework for guessing device types based on MAC Organizationally Unique Identifiers (OUI).
     * You can expand this logic as you build out your reporting framework.
     */
    private fun guessDeviceType(macAddress: String): String {
        if (macAddress == "UNKNOWN:MAC") return "UNKNOWN NODE"
        
        // Example OUI prefixes (First 3 octets)
        val prefix = macAddress.take(8)
        
        return when (prefix) {
            "B8:27:EB", "DC:A6:32", "E4:5F:01" -> "RASPBERRY PI"
            "00:1A:11", "00:1E:8C" -> "GOOGLE HARDWARE"
            "00:14:22", "00:25:00" -> "APPLE DEVICE"
            "00:01:5D", "00:04:F2" -> "IOT SENSOR"
            else -> "GENERIC NODE" // If you want to make it feel tactical, use "UNIDENTIFIED NODE"
        }
    }

    private fun getLocalIpAddress(): String? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        }
        return null
    }
}
