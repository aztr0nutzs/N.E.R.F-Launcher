package com.nerf.launcher.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock

data class SystemModuleSnapshot(
    val batteryPercent: Int?,
    val isCharging: Boolean,
    val storageUsagePercent: Int?,
    val uptimeDays: Int,
    val uptimeHours: Int,
    val isInteractive: Boolean,
    val isPowerSaveMode: Boolean,
    val reactorSync: Int
)

/**
 * Owns launcher system-module telemetry collection and periodic refresh scheduling.
 */
class SystemModuleController(
    context: Context,
    private val onSnapshotUpdated: (SystemModuleSnapshot) -> Unit
) {
    companion object {
        private const val REFRESH_INTERVAL_MS = 60_000L
        private const val STORAGE_READ_MIN_INTERVAL_MS = 30_000L
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val HOUR_MS = 60 * 60 * 1000L
    }

    private val appContext = context.applicationContext
    private val powerManager by lazy { appContext.getSystemService(PowerManager::class.java) }
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var batteryPercent: Int? = null
    private var isCharging: Boolean = false
    private var filteredAppCount: Int = 0
    private var totalAppCount: Int = 0
    private var config: AppConfig? = null
    private var started: Boolean = false
    private var latestSnapshot: SystemModuleSnapshot? = null
    private var cachedStorageUsagePercent: Int? = null
    private var lastStorageReadElapsedRealtime: Long = Long.MIN_VALUE

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryState(intent)
            publishSnapshot()
        }
    }

    private val refreshTick = object : Runnable {
        override fun run() {
            publishSnapshot()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    fun start() {
        if (started) return
        started = true
        val stickyIntent = appContext.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        updateBatteryState(stickyIntent)
        refreshHandler.post(refreshTick)
    }

    fun stop() {
        if (!started) return
        started = false
        refreshHandler.removeCallbacks(refreshTick)
        appContext.unregisterReceiver(batteryReceiver)
    }

    fun setInputs(config: AppConfig?, filteredAppCount: Int, totalAppCount: Int) {
        this.config = config
        this.filteredAppCount = filteredAppCount
        this.totalAppCount = totalAppCount
        publishSnapshot()
    }

    fun refreshNow() {
        publishSnapshot()
    }

    fun isPowerSaveModeEnabled(): Boolean {
        return latestSnapshot?.isPowerSaveMode ?: (powerManager?.isPowerSaveMode == true)
    }

    private fun updateBatteryState(intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        batteryPercent = if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else {
            null
        }
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun publishSnapshot() {
        val snapshot = buildSnapshot()
        latestSnapshot = snapshot
        onSnapshotUpdated(snapshot)
    }

    private fun buildSnapshot(): SystemModuleSnapshot {
        val elapsedRealtime = SystemClock.elapsedRealtime()
        val storageUsagePercent = readStorageUsagePercent(elapsedRealtime)
        val isInteractive = powerManager?.isInteractive == true
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true
        return SystemModuleSnapshot(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            storageUsagePercent = storageUsagePercent,
            uptimeDays = (elapsedRealtime / DAY_MS).toInt(),
            uptimeHours = ((elapsedRealtime / HOUR_MS) % 24).toInt(),
            isInteractive = isInteractive,
            isPowerSaveMode = isPowerSaveMode,
            reactorSync = calculateReactorSync(storageUsagePercent)
        )
    }

    private fun calculateReactorSync(storageUsagePercent: Int?): Int {
        val appPopulation = if (totalAppCount == 0) 0 else (filteredAppCount * 100 / totalAppCount)
        val batteryScore = batteryPercent ?: 0
        val storageScore = storageUsagePercent?.let { 100 - it } ?: 0
        val taskbarScore = if (config?.taskbarSettings?.enabled == true) 100 else 70
        return ((appPopulation + batteryScore + storageScore + taskbarScore) / 4).coerceIn(0, 100)
    }

    private fun readStorageUsagePercent(currentElapsedRealtime: Long): Int? {
        val cached = cachedStorageUsagePercent
        if (cached != null &&
            currentElapsedRealtime - lastStorageReadElapsedRealtime < STORAGE_READ_MIN_INTERVAL_MS
        ) {
            return cached
        }

        val computed = runCatching {
            val stats = StatFs(Environment.getDataDirectory().absolutePath)
            val totalBytes = stats.totalBytes
            val availableBytes = stats.availableBytes
            if (totalBytes <= 0L) {
                null
            } else {
                val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
                ((usedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
            }
        }.getOrNull()

        lastStorageReadElapsedRealtime = currentElapsedRealtime
        cachedStorageUsagePercent = computed
        return computed
    }
}
