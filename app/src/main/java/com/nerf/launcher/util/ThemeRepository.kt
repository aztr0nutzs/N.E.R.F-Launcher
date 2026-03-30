package com.nerf.launcher.util

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.nerf.launcher.R

/**
 * Authoritative source for launcher themes.
 *
 * Theme definitions are resource-based so launcher visuals stay centralized in XML palette tokens.
 */
object ThemeRepository {

    private data class ThemeSpec(
        val name: String,
        @ColorRes val primaryRes: Int,
        @ColorRes val secondaryRes: Int,
        @ColorRes val accentRes: Int,
        @ColorRes val windowBackgroundRes: Int,
        @ColorRes val taskbarDarkBackgroundRes: Int,
        @ColorRes val taskbarLightBackgroundRes: Int,
        @ColorRes val hudInactiveMeterRes: Int,
        @ColorRes val hudPanelTextPrimaryRes: Int,
        @ColorRes val hudPanelTextSecondaryRes: Int,
        @ColorRes val hudEnergyHighRes: Int,
        @ColorRes val hudEnergyLowRes: Int
    )

    private val classicNerf = ThemeSpec(
        name = "Classic Nerf",
        primaryRes = R.color.theme_classic_primary,
        secondaryRes = R.color.theme_classic_secondary,
        accentRes = R.color.theme_classic_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low
    )

    private val stealthOps = ThemeSpec(
        name = "Stealth Ops",
        primaryRes = R.color.theme_stealth_primary,
        secondaryRes = R.color.theme_stealth_secondary,
        accentRes = R.color.theme_stealth_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low
    )

    private val eliteBlue = ThemeSpec(
        name = "Elite Blue",
        primaryRes = R.color.theme_elite_primary,
        secondaryRes = R.color.theme_elite_secondary,
        accentRes = R.color.theme_elite_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low
    )

    private val zombieStrike = ThemeSpec(
        name = "Zombie Strike",
        primaryRes = R.color.theme_zombie_primary,
        secondaryRes = R.color.theme_zombie_secondary,
        accentRes = R.color.theme_zombie_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low
    )

    private val hyperNeon = ThemeSpec(
        name = "Hyper Neon",
        primaryRes = R.color.theme_hyper_primary,
        secondaryRes = R.color.theme_hyper_secondary,
        accentRes = R.color.theme_hyper_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low
    )

    private val allSpecs: List<ThemeSpec> = listOf(
        classicNerf,
        stealthOps,
        eliteBlue,
        zombieStrike,
        hyperNeon
    )

    val allThemeNames: List<String>
        get() = allSpecs.map { it.name }

    val defaultThemeName: String
        get() = classicNerf.name

    fun containsTheme(name: String): Boolean =
        allSpecs.any { it.name.equals(name, ignoreCase = true) }

    fun resolve(context: Context, name: String?): NerfTheme {
        val spec = allSpecs.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: classicNerf
        return NerfTheme(
            name = spec.name,
            primary = context.color(spec.primaryRes),
            secondary = context.color(spec.secondaryRes),
            accent = context.color(spec.accentRes),
            windowBackground = context.color(spec.windowBackgroundRes),
            taskbarDarkBackground = context.color(spec.taskbarDarkBackgroundRes),
            taskbarLightBackground = context.color(spec.taskbarLightBackgroundRes),
            hudInactiveMeterColor = context.color(spec.hudInactiveMeterRes),
            hudPanelTextPrimary = context.color(spec.hudPanelTextPrimaryRes),
            hudPanelTextSecondary = context.color(spec.hudPanelTextSecondaryRes),
            hudEnergyHighColor = context.color(spec.hudEnergyHighRes),
            hudEnergyLowColor = context.color(spec.hudEnergyLowRes)
        )
    }

    private fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)
}
