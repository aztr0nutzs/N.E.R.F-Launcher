package com.nerf.launcher.util.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress

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

    /**
     * Scans the local /24 subnet for active devices using parallel coroutines
     * and attempts to resolve hardware MAC addresses via the ARP table.
     */
    suspend fun scanLocalSubnet(): List<NetworkNode> = withContext(Dispatchers.IO) {
        val activeNodes = mutableListOf<NetworkNode>()
        val deviceIp = getLocalIpAddress() ?: return@withContext emptyList()
        
        val subnet = deviceIp.substringBeforeLast(".")
        
        // Phase 1: Parallel Ping Sweep to populate the ARP table
        val pingTasks = (1..254).map { i ->
            async {
                val ipToTest = "$subnet.$i"
                pingDevice(ipToTest)
            }
        }

        // Wait for all pings to complete
        val results = pingTasks.awaitAll().filterNotNull()
        
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

    private fun pingDevice(ipAddress: String): NetworkNode? {
        return try {
            val inetAddress = InetAddress.getByName(ipAddress)
            val startTime = System.currentTimeMillis()
            // 500ms timeout for better accuracy on heavily loaded local networks
            val isReachable = inetAddress.isReachable(500)
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
            e.printStackTrace()
            // Fallback: If ARP is completely blocked, return empty map.
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
