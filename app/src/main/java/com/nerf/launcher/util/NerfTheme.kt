package com.nerf.launcher.util

import android.graphics.Color

/**
 * Authoritative launcher theme model.
 *
 * Values here are fully-resolved ARGB colors consumed by launcher-owned surfaces.
 */
data class NerfTheme(
    val name: String,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val windowBackground: Int,
    val taskbarDarkBackground: Int,
    val taskbarLightBackground: Int,
    val lockSurfaceScrim: Int,
    val hudInactiveMeterColor: Int,
    val hudPanelTextPrimary: Int,
    val hudPanelTextSecondary: Int,
    val hudInfoColor: Int,
    val hudWarningColor: Int,
    val hudSuccessColor: Int,
    val hudAccentColor: Int,
    val hudEnergyHighColor: Int,
    val hudEnergyLowColor: Int,
    val reactorArmorDarkColor: Int,
    val reactorArmorMidColor: Int,
    val reactorInteriorDarkColor: Int,
    val reactorInteriorMidColor: Int,
    val reactorFrameShadowColor: Int,
    val assistantMutedColor: Int,
    val assistantErrorColor: Int,
    val glowIntensity: Float = 1.0f
) {
    fun withGlowIntensity(value: Float): NerfTheme = copy(glowIntensity = value.coerceIn(0f, 1f))

    companion object {
        val DEFAULT_HUD_INACTIVE_METER_COLOR: Int = Color.argb(80, 255, 255, 255)
    }
}
