package com.nerf.launcher.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

data class HudStatusSnapshot(
    val batteryPercent: Int,
    val timestampMillis: Long
)

/**
 * Owns HUD battery monitoring and minute-aligned status ticking.
 */
class HudStatusMonitor(
    context: Context,
    private val onStatusUpdated: (HudStatusSnapshot) -> Unit
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var batteryPercent: Int = 0
    private var started: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            batteryPercent = extractBatteryPercent(intent)
            publishSnapshot()
        }
    }

    private val timeTick = object : Runnable {
        override fun run() {
            publishSnapshot()
            handler.postDelayed(this, 60_000L - (SystemClock.uptimeMillis() % 60_000L))
        }
    }

    fun start() {
        if (started) return
        started = true
        val stickyIntent = appContext.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        batteryPercent = extractBatteryPercent(stickyIntent)
        publishSnapshot()
        handler.post(timeTick)
    }

    fun stop() {
        if (!started) return
        started = false
        handler.removeCallbacks(timeTick)
        try {
            appContext.unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
    }

    fun release() {
        stop()
    }

    private fun publishSnapshot() {
        onStatusUpdated(
            HudStatusSnapshot(
                batteryPercent = batteryPercent,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    private fun extractBatteryPercent(intent: Intent?): Int {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else {
            0
        }
    }
}
