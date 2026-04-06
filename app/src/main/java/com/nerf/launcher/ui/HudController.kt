package com.nerf.launcher.ui

import android.app.Activity
import android.animation.ValueAnimator
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.ui.reactor.ReactorModuleView
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.HudStatusSnapshot
import com.nerf.launcher.util.HudStatusMonitor
import com.nerf.launcher.util.NerfTheme
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
    private val hudStatusMonitor = HudStatusMonitor(activity.applicationContext, ::renderStatus)
    private var hudBreathingAnimator: ValueAnimator? = null
    private var lastObservedConfig: AppConfig? = null

    init {
        setupTapAnimation(timeDisplay)
        setupTapAnimation(addWidgetBtn)

        addWidgetBtn.setOnClickListener {
            activity.startActivity(Intent(activity, SettingsActivity::class.java))
        }

        setupConfigObservers()
    }

    fun start() {
        hudStatusMonitor.start()
        startHudBreathing()
    }

    fun stop() {
        hudStatusMonitor.stop()
        stopHudBreathing()
    }

    private fun setupTapAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    v.animate()
                        .scaleX(fractionValue(R.fraction.nerf_touch_scale_down_hud))
                        .scaleY(fractionValue(R.fraction.nerf_touch_scale_down_hud))
                        .alpha(fractionValue(R.fraction.nerf_touch_alpha_down_hud))
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
            val finalTheme = ThemeManager.resolveConfigTheme(activity, config)
            applyHudTheme(finalTheme)
            lastObservedConfig = config
        }
    }

    private fun applyHudTheme(theme: NerfTheme) {
        batteryMeter.setActiveColor(theme.primary)
        batteryMeter.setInactiveColor(theme.hudInactiveMeterColor)
        batteryMeter.setGradientHighlightColor(theme.hudPanelTextPrimary)
        timeDisplay.setTextColor(theme.secondary)
        dateDisplay.setTextColor(theme.hudPanelTextSecondary)
        hudView.findViewById<TextView>(R.id.battery_label)?.setTextColor(theme.hudWarningColor)
        hudView.findViewById<TextView>(R.id.brand_signature)?.setTextColor(theme.hudPanelTextSecondary)
        addWidgetBtn.setTextColor(theme.hudSuccessColor)
        addWidgetBtn.background = ThemeManager.createHudActionTileDrawable(activity, theme)
        ThemeManager.applyHudPanelGlow(hudView, theme)
        hudView.findViewById<ReactorModuleView>(R.id.reactor_core)?.updateTheme(theme)
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
                hudView.alpha = fractionValue(R.fraction.nerf_hud_breath_alpha_min) +
                    (phase * fractionValue(R.fraction.nerf_hud_breath_alpha_range))
            }
            start()
        }
    }

    private fun stopHudBreathing() {
        hudBreathingAnimator?.cancel()
        hudBreathingAnimator = null
        hudView.alpha = 1f
    }

    private fun renderStatus(snapshot: HudStatusSnapshot) {
        batteryMeter.progress = snapshot.batteryPercent
        batteryPercent.text = "${snapshot.batteryPercent}%"

        val now = Calendar.getInstance().apply {
            timeInMillis = snapshot.timestampMillis
        }
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
    }

    fun release() {
        hudStatusMonitor.release()
        stopHudBreathing()
    }

    private fun fractionValue(fractionRes: Int): Float =
        activity.resources.getFraction(fractionRes, 1, 1)
}
