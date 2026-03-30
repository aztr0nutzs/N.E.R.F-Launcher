package com.nerf.launcher.ui

import android.app.Activity
import android.animation.ValueAnimator
import android.content.Intent
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ThemeManager
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
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
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
    private var lastObservedConfig: AppConfig? = null

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
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
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
                MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(0.972f)
                        .scaleY(0.972f)
                        .alpha(0.93f)
                        .setDuration(70L)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                    false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(140L)
                        .setInterpolator(LinearOutSlowInInterpolator())
                        .start()
                    false
                }

                else -> false
            }
        }
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(lifecycleOwner) { config ->
            val previous = lastObservedConfig
            val themeChanged = previous == null ||
                previous.themeName != config.themeName ||
                previous.glowIntensity != config.glowIntensity
            if (!themeChanged) {
                lastObservedConfig = config
                return@observe
            }
            val finalTheme = ThemeManager.resolveActiveTheme(
                context = activity,
                themeName = config.themeName,
                glowIntensity = config.glowIntensity
            )
            ThemeManager.applyHudTheme(activity, finalTheme)
            lastObservedConfig = config
        }
    }

    private fun startHudBreathing() {
        hudBreathingAnimator?.cancel()
        hudBreathingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6_400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                val phase = animator.animatedValue as Float
                hudView.alpha = 0.982f + (phase * 0.018f)
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
