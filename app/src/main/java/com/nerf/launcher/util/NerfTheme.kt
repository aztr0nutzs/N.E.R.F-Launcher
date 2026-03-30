package com.nerf.launcher.util

import android.graphics.Color

/**
 * Represents a Nerf launcher theme.
 */
data class NerfTheme(
    val name: String,
    val primary: Int,   // ARGB color
    val secondary: Int, // ARGB color
    val accent: Int,    // ARGB color
    val windowBackground: Int, // ARGB color for window background
    val glowIntensity: Float = 1.0f,
    val backgroundStyle: Int = android.R.color.background_dark,
    val hudInactiveMeterColor: Int = Color.argb(80, 255, 255, 255)
) {
    /** Returns a copy with the primary color changed. */
    fun withPrimary(color: Int) = copy(primary = color)

    /** Returns a copy with the secondary color changed. */
    fun withSecondary(color: Int) = copy(secondary = color)

    /** Returns a copy with the accent color changed. */
    fun withAccent(color: Int) = copy(accent = color)
}
