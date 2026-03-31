package com.nerf.launcher.util

/**
 * Typed settings rows used by the settings surface RecyclerView.
 */
sealed class SettingItem {
    abstract val type: SettingsType
    abstract val title: String

    data class Theme(
        override val title: String,
        val options: List<String>
    ) : SettingItem() {
        override val type: SettingsType = SettingsType.THEME
    }

    data class IconPack(
        override val title: String,
        val options: List<String>
    ) : SettingItem() {
        override val type: SettingsType = SettingsType.ICON_PACK
    }

    data class GlowIntensity(
        override val title: String,
        val initialValue: Float
    ) : SettingItem() {
        override val type: SettingsType = SettingsType.GLOW_INTENSITY
    }

    data class AnimationSpeed(
        override val title: String,
        val initialValue: Boolean
    ) : SettingItem() {
        override val type: SettingsType = SettingsType.ANIMATION_SPEED
    }

    data class GridSize(
        override val title: String,
        val initialValue: Int
    ) : SettingItem() {
        override val type: SettingsType = SettingsType.GRID_SIZE
    }
}

/**
 * Typed setting change events emitted by the settings adapter.
 */
sealed class SettingChange {
    data class Theme(val themeName: String) : SettingChange()
    data class IconPack(val packName: String) : SettingChange()
    data class GlowIntensity(val intensity: Float) : SettingChange()
    data class AnimationSpeed(val enabled: Boolean) : SettingChange()
    data class GridSize(val size: Int) : SettingChange()
}
