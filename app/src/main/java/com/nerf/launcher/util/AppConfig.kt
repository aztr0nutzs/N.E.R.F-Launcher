package com.nerf.launcher.util

/**
 * Central data model holding all UI configuration for the launcher.
 */
data class AppConfig(
    val themeName: String,
    val iconPack: String,
    val gridSize: Int,
    val animationSpeedEnabled: Boolean,
    val glowIntensity: Float,
    val taskbarSettings: TaskbarSettings
)

data class TaskbarSettings(
    val height: Int,            // in dp
    val iconSize: Int,          // in dp
    val backgroundStyle: Int,   // reference to a color resource
    val transparency: Float,    // 0.0f to 1.0f (0 = fully transparent, 1 = opaque)
    val enabled: Boolean,
    val pinnedApps: List<String> = emptyList()
)