package com.nerf.launcher.ui

import android.app.Activity
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository
import java.util.Calendar
import java.util.Locale

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

    private val batteryMeter: SegmentedBarView = hudView.findViewById(R.id.battery_meter)
    private val batteryPercent: TextView = hudView.findViewById(R.id.battery_percent)
    private val timeDisplay: TextView = hudView.findViewById(R.id.time_display)
    private val dateDisplay: TextView = hudView.findViewById(R.id.date_display)
    private val addWidgetBtn: MaterialButton = hudView.findViewById(R.id.add_widget_btn)

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            val level = intent?.getIntExtra(android.content.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent?.getIntExtra(android.content.BatteryManager.EXTRA_SCALE, -1)
            val percent = if (level != null && scale != null && level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                0
            }
            batteryMeter.progress = percent
            batteryPercent.text = "$percent%"
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var hudBreathingAnimator: ValueAnimator? = null

    private val timeUpdater = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            timeDisplay.text = String.format(Locale.US, "%02d:%02d", hour, minute)
            dateDisplay.text = String.format(
                Locale.US,
                "%s %02d %s",
                now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)?.uppercase(Locale.US)
                    ?: "DAY",
                now.get(Calendar.DAY_OF_MONTH),
                now.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)?.uppercase(Locale.US)
                    ?: "MON"
            )
            handler.postDelayed(this, 60_000L - (SystemClock.uptimeMillis() % 60_000L))
        }
    }

    init {
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        activity.registerReceiver(batteryReceiver, filter)

        val batIntent = activity.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        batIntent?.let {
            val level = it.getIntExtra(android.content.BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(android.content.BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                val percent = level * 100 / scale
                batteryMeter.progress = percent
                batteryPercent.text = "$percent%"
            }
        }

        handler.post(timeUpdater)
        startHudBreathing()

        setupTapAnimation(timeDisplay)
        setupTapAnimation(addWidgetBtn)

        addWidgetBtn.setOnClickListener {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
        }

        setupConfigObservers()
    }

    private fun setupTapAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.hud_recoil))
                    false
                }

                else -> false
            }
        }
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(lifecycleOwner) { config ->
            val baseTheme = ThemeRepository.byName(config.themeName)
                ?: ThemeRepository.CLASSIC_NERF
            val finalTheme = baseTheme.copy(glowIntensity = config.glowIntensity)

            batteryMeter.setActiveColor(finalTheme.primary)
            batteryMeter.setInactiveColor(Color.argb(80, 255, 255, 255))
            timeDisplay.setTextColor(finalTheme.secondary)
            addWidgetBtn.setTextColor(finalTheme.accent)
            ThemeManager.applyHudPanelGlow(hudView, finalTheme)
        }
    }

    private fun startHudBreathing() {
        hudBreathingAnimator?.cancel()
        hudBreathingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4_800L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val phase = animator.animatedValue as Float
                hudView.alpha = 0.97f + (phase * 0.03f)
            }
            start()
        }
    }

    fun release() {
        handler.removeCallbacks(timeUpdater)
        hudBreathingAnimator?.cancel()
        hudBreathingAnimator = null
        try {
            activity.unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
    }
}
