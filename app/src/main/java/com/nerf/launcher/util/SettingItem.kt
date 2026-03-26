package com.nerf.launcher.util

/**
 * Simple data class representing a setting row.
 */
data class SettingItem(
    val type: SettingsType,
    val title: String,
    val payload: Any   // meaning depends on type
)