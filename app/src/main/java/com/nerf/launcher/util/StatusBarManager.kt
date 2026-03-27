package com.nerf.launcher.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat

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
        // Make the status bar transparent to draw behind it
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val controller: WindowInsetsControllerCompat = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            controller.isAppearanceLightStatusBars = isLightTheme
            // Set status bar color
            controller.setStatusBarColor(Color.argb(255, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)))
        } else {
            // For older APIs, we can only set the color (not the icon lightness)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = primaryColor
            // Note: Icon lightness cannot be changed on APIs < M without root or custom ROMs.
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