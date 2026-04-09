package com.nerf.launcher.state

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.nerf.launcher.theme.IndustrialLauncherColors
import com.nerf.launcher.theme.LauncherColors
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.NerfTheme
import com.nerf.launcher.util.ThemeManager

/**
 * Converts an [AppConfig] into a [LauncherColors] palette for the Compose [LauncherTheme].
 *
 * This is the single point where [AppConfig.themeName] and [AppConfig.glowIntensity] cross
 * from the ViewBinding-era [NerfTheme] model (ARGB Int) into the Compose Color world.
 *
 * It reads from [ThemeManager] (which reads from [ThemeRepository]) so there
 * is exactly one config authority: [ConfigRepository] → [AppConfig] → here → Compose.
 *
 * All structural/spacing tokens ([LauncherTokens]) stay static — they are not
 * theme-driven. Only colors change per theme.
 */
internal object LauncherColorsMapper {

    /**
     * Resolve a [LauncherColors] from the given [config].
     * Falls back to [IndustrialLauncherColors] if the theme name is unknown.
     */
    fun fromConfig(context: Context, config: AppConfig): LauncherColors {
        return try {
            fromNerfTheme(ThemeManager.resolveConfigTheme(context, config))
        } catch (_: Exception) {
            IndustrialLauncherColors
        }
    }

    /**
     * Map an already-resolved [NerfTheme] (ARGB Int fields) into [LauncherColors]
     * (Compose [Color] fields).
     *
     * Mapping strategy:
     *  - Background pair     ← windowBackground (dark) + a slightly deeper shade
     *  - Panel surfaces      ← reactorInteriorDark / reactorInteriorMid / reactorArmorDark
     *  - Frame / dock tones  ← reactorArmorMid / reactorArmorDark
     *  - Text                ← hudPanelTextPrimary / hudPanelTextSecondary / hudInactiveMeter
     *  - Accent quartet      ← hudInfoColor (cyan) / hudSuccessColor (green) /
     *                          hudAccentColor (magenta) / hudWarningColor (yellow/amber)
     *  - Danger / disabled   ← assistantErrorColor / hudInactiveMeterColor
     *
     * The mapping is intentionally conservative: it preserves the Compose UI's
     * structural contrast ratios by anchoring background/panel tones to the
     * reactor interior palette rather than the raw window background.
     */
    private fun fromNerfTheme(theme: NerfTheme): LauncherColors {
        // Helper: ARGB Int → Compose Color (NerfTheme uses android.graphics.Color ints)
        fun Int.toComposeColor(): Color = Color(this.toLong() or 0xFF_00_00_00L)

        val bgTop       = theme.windowBackground.toComposeColor()
        // Slightly deeper variant for the gradient bottom: blend the window bg with pure black at 40%.
        val bgBottom    = Color(
            red   = bgTop.red   * 0.60f,
            green = bgTop.green * 0.60f,
            blue  = bgTop.blue  * 0.60f,
            alpha = 1f
        )
        val bgScrim     = bgBottom.copy(alpha = 0.85f)

        val metalDark   = theme.reactorInteriorDarkColor.toComposeColor()
        val metalMid    = theme.reactorInteriorMidColor.toComposeColor()
        val metalHigh   = theme.reactorArmorMidColor.toComposeColor()
        val frameBase   = theme.reactorArmorDarkColor.toComposeColor()
        val frameLine   = theme.reactorMidAColor.toComposeColor()

        val panelOuter  = metalDark
        val panelInner  = Color(
            red   = metalDark.red   * 0.75f,
            green = metalDark.green * 0.75f,
            blue  = metalDark.blue  * 0.75f,
            alpha = 1f
        )
        val panelInset  = Color(
            red   = metalDark.red   * 0.50f,
            green = metalDark.green * 0.50f,
            blue  = metalDark.blue  * 0.50f,
            alpha = 1f
        )

        val dockBase    = frameBase
        val dockInner   = Color(
            red   = frameBase.red   * 0.65f,
            green = frameBase.green * 0.65f,
            blue  = frameBase.blue  * 0.65f,
            alpha = 1f
        )

        val textPrimary    = theme.hudPanelTextPrimary.toComposeColor()
        val textSecondary  = theme.hudPanelTextSecondary.toComposeColor()
        val textMuted      = Color(
            red   = textSecondary.red   * 0.72f,
            green = textSecondary.green * 0.72f,
            blue  = textSecondary.blue  * 0.72f,
            alpha = 1f
        )

        // Apply glow intensity to the four accent colors so "glow intensity" actually
        // dims/brightens the accent tones across the Compose UI.
        val glowFactor  = 0.55f + (theme.glowIntensity * 0.45f)   // range: 0.55 – 1.00
        fun Int.accentColor(): Color {
            val base = toComposeColor()
            return Color(
                red   = (base.red   * glowFactor).coerceIn(0f, 1f),
                green = (base.green * glowFactor).coerceIn(0f, 1f),
                blue  = (base.blue  * glowFactor).coerceIn(0f, 1f),
                alpha = 1f
            )
        }

        return LauncherColors(
            backgroundTop      = bgTop,
            backgroundBottom   = bgBottom,
            backgroundScrim    = bgScrim,
            metalDark          = metalDark,
            metalMid           = metalMid,
            metalHighlight     = metalHigh,
            frameBase          = frameBase,
            frameLine          = frameLine,
            panelOuter         = panelOuter,
            panelInner         = panelInner,
            panelInset         = panelInset,
            dockBase           = dockBase,
            dockInner          = dockInner,
            textPrimary        = textPrimary,
            textSecondary      = textSecondary,
            textMuted          = textMuted,
            accentCyan         = theme.hudInfoColor.accentColor(),
            accentGreen        = theme.hudSuccessColor.accentColor(),
            accentMagenta      = theme.hudAccentColor.accentColor(),
            accentYellow       = theme.hudWarningColor.accentColor(),
            danger             = theme.assistantErrorColor.toComposeColor(),
            disabled           = theme.hudInactiveMeterColor.toComposeColor()
        )
    }
}
