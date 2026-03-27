package com.nerf.launcher.util

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.Window
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.ui.SegmentedBarView

/**
 * Applies a NerfTheme to an Activity. Reads the current configuration
 * exclusively from ConfigRepository (single source of truth).
 */
object ThemeManager {

    fun applyTheme(activity: Activity) {
        val config = ConfigRepository.get().config.value ?: return
        val themeName = config.themeName
        val baseTheme = ThemeRepository.byName(themeName) ?: ThemeRepository.CLASSIC_NERF
        val finalTheme = baseTheme.copy(glowIntensity = config.glowIntensity)

        // Apply the base theme (styles.xml) – programmatic tweaks follow.
        activity.setTheme(R.style.Theme_NerfLauncher)
        updateWindowBackground(activity.window, finalTheme)
        updateHudViews(activity, finalTheme)
    }

    private fun updateWindowBackground(window: Window?, theme: NerfTheme) {
        // Use the theme‑defined window background color.
        window?.setBackgroundColor(theme.windowBackground)
    }

    private fun updateHudViews(activity: Activity, theme: NerfTheme) {
        val root = activity.findViewById<View>(R.id.hud_root) ?: return

        val batteryMeter = root.findViewById<SegmentedBarView>(R.id.battery_meter)
        batteryMeter?.setActiveColor(theme.primary)
        batteryMeter?.setInactiveColor(Color.argb(80, 255, 255, 255))

        val timeDisplay = root.findViewById<TextView>(R.id.time_display)
        timeDisplay?.setTextColor(theme.secondary)

        val addWidgetBtn = root.findViewById<MaterialButton>(R.id.add_widget_btn)
        addWidgetBtn?.setTextColor(theme.primary)

        val glowAlpha = (theme.glowIntensity * 0.2).coerceIn(0.0f, 0.4f)
        root.setBackgroundColor(
            Color.argb(
                (glowAlpha * 255).toInt(),
                Color.red(theme.primary),
                Color.green(theme.primary),
                Color.blue(theme.primary)
            )
        )
    }
}
