package com.nerf.launcher.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.nerf.launcher.R
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.ThemeRepository

/**
 * Controller for the Nerf HUD overlay.
 * Fully reactive to configuration changes via ConfigRepository.
 * Uses lifecycle-scoped observers to avoid leaks.
 */
class HudController(
    private val activity: Activity,
    private val hudView: View,
    private val lifecycleOwner: LifecycleOwner
) {

    private val batteryMeter: ProgressBar = hudView.findViewById(R.id.battery_meter)
    private val timeDisplay: TextView = hudView.findViewById(R.id.time_display)
    private val widgetContainer: FrameLayout = hudView.findViewById(R.id.widget_container)
    private val addWidgetBtn: Button = hudView.findViewById(R.id.add_widget_btn)

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val level = intent?.getIntExtra(android.content.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent?.getIntExtra(android.content.BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            batteryMeter.progress = percent
        }
    }

    private val timeUpdater = Runnable {
        val now = java.util.Calendar.getInstance()
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        val timeFormatted = String.format("%02d:%02d", hour, minute)
        timeDisplay.text = timeFormatted
        handler.postDelayed(timeUpdater, 60_000L - (SystemClock.uptimeMillis() % 60_000L))
    }

    private val handler = Handler(Looper.getMainLooper())

    init {
        // Register for battery changes (sticky intent, no permission needed)
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        activity.registerReceiver(batteryReceiver, filter)

        // Initial battery reading
        val batIntent = activity.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        batIntent?.let {
            val level = it.getIntExtra(android.content.BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(android.content.BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryMeter.progress = level * 100 / scale
            }
        }

        // Start time updates
        handler.post(timeUpdater)

        // Tap animations on interactive elements
        setupTapAnimation(batteryMeter)
        setupTapAnimation(timeDisplay)
        setupTapAnimation(addWidgetBtn)

        // Observe configuration changes for theme and glow intensity
        setupConfigObservers()
    }

    private fun setupTapAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.hud_recoil))
                    playSoundEffect()
                    true
                }
                else -> false
            }
        }
    }

    private fun playSoundEffect() {
        // Hook for sound feedback – implement with SoundPool/MediaPlayer if desired.
        // No external assets required for this stub.
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(lifecycleOwner) { config ->
            val themeName = config.themeName
            val baseTheme = ThemeRepository.byName(themeName) 
                    ?: ThemeRepository.CLASSIC_NERF
            val finalTheme = baseTheme.copy(glowIntensity = config.glowIntensity)

            // Update HUD views with theme colors
            batteryMeter.progressTintList = android.content.res.ColorStateList.valueOf(finalTheme.primary)
            batteryMeter.backgroundTintList = android.content.res.ColorStateList.valueOf(finalTheme.secondary)
            timeDisplay.setTextColor(finalTheme.secondary)
            addWidgetBtn.setTextColor(finalTheme.primary)

            // Update glow overlay
            val glowAlpha = (finalTheme.glowIntensity * 0.2).coerceIn(0.0f, 0.4f)
            hudView.setBackgroundColor(Color.argb(
                (glowAlpha * 255).toInt(),
                Color.red(finalTheme.primary),
                Color.green(finalTheme.primary),
                Color.blue(finalTheme.primary)
            ))
        }
    }

    fun release() {
        handler.removeCallbacks(timeUpdater)
        try {
            activity.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered
        }
    }
}