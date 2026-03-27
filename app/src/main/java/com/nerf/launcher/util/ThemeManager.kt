package com.nerf.launcher.util

import android.app.Activity
import android.view.Window
import com.nerf.launcher.R

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
    }

    private fun updateWindowBackground(window: Window?, theme: NerfTheme) {
        // Use the theme-defined window background color.
        window?.setBackgroundColor(theme.windowBackground)
    }
}
