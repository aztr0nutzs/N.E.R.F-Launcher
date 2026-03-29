package com.nerf.launcher.util

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat

/**
 * Manages the status bar appearance, including color and icon lightness.
 */
object StatusBarManager {

    /**
     * Apply the status bar customization based on the provided theme.
     * @param activity The activity to apply the changes to.
     * @param primaryColor The primary color of the theme (for background).
     * @param isLightTheme Whether the theme is light (for icon tint).
     */
    fun applyStatusBarTheme(
        activity: Activity,
        primaryColor: Int,
        isLightTheme: Boolean
    ) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = primaryColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val controller = ViewCompat.getWindowInsetsController(window.decorView)
            controller?.isAppearanceLightStatusBars = isLightTheme
        }
    }

    /** Reset the status bar to default (useful when leaving the app). */
    fun resetStatusBar(activity: Activity) {
        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(activity, android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val controller = ViewCompat.getWindowInsetsController(window.decorView)
            controller?.isAppearanceLightStatusBars = false
        }
    }
}
