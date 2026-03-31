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
        @ColorRes val lockSurfaceScrimRes: Int,
        @ColorRes val hudInactiveMeterRes: Int,
        @ColorRes val hudPanelTextPrimaryRes: Int,
        @ColorRes val hudPanelTextSecondaryRes: Int,
        @ColorRes val hudInfoRes: Int,
        @ColorRes val hudWarningRes: Int,
        @ColorRes val hudSuccessRes: Int,
        @ColorRes val hudAccentRes: Int,
        @ColorRes val hudAppLabelRes: Int,
        @ColorRes val drawerBorderRes: Int,
        @ColorRes val scanlineGlowRes: Int,
        @ColorRes val hudEnergyHighRes: Int,
        @ColorRes val hudEnergyLowRes: Int,
        @ColorRes val reactorMidARes: Int,
        @ColorRes val reactorMidBRes: Int,
        @ColorRes val reactorAccentRingRes: Int,
        @ColorRes val reactorCoreGlowRes: Int,
        @ColorRes val reactorArmorDarkRes: Int,
        @ColorRes val reactorArmorMidRes: Int,
        @ColorRes val reactorInteriorDarkRes: Int,
        @ColorRes val reactorInteriorMidRes: Int,
        @ColorRes val reactorFrameShadowRes: Int,
        @ColorRes val assistantMutedRes: Int,
        @ColorRes val assistantErrorRes: Int
    )

    private val classicNerf = ThemeSpec(
        name = "Classic Nerf",
        primaryRes = R.color.theme_classic_primary,
        secondaryRes = R.color.theme_classic_secondary,
        accentRes = R.color.theme_classic_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        lockSurfaceScrimRes = R.color.theme_shared_lock_surface_scrim,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudInfoRes = R.color.theme_shared_hud_cyan,
        hudWarningRes = R.color.theme_shared_hud_orange,
        hudSuccessRes = R.color.theme_shared_hud_lime,
        hudAccentRes = R.color.theme_shared_hud_magenta,
        hudAppLabelRes = R.color.theme_shared_hud_app_label,
        drawerBorderRes = R.color.theme_shared_drawer_border,
        scanlineGlowRes = R.color.theme_shared_scanline_glow,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low,
        reactorMidARes = R.color.theme_shared_reactor_mid_a,
        reactorMidBRes = R.color.theme_shared_reactor_mid_b,
        reactorAccentRingRes = R.color.theme_shared_reactor_accent_ring,
        reactorCoreGlowRes = R.color.theme_shared_reactor_core_glow,
        reactorArmorDarkRes = R.color.theme_shared_reactor_armor_dark,
        reactorArmorMidRes = R.color.theme_shared_reactor_armor_mid,
        reactorInteriorDarkRes = R.color.theme_shared_reactor_interior_dark,
        reactorInteriorMidRes = R.color.theme_shared_reactor_interior_mid,
        reactorFrameShadowRes = R.color.theme_shared_reactor_frame_shadow,
        assistantMutedRes = R.color.theme_shared_assistant_muted,
        assistantErrorRes = R.color.theme_shared_assistant_error
    )

    private val stealthOps = ThemeSpec(
        name = "Stealth Ops",
        primaryRes = R.color.theme_stealth_primary,
        secondaryRes = R.color.theme_stealth_secondary,
        accentRes = R.color.theme_stealth_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        lockSurfaceScrimRes = R.color.theme_shared_lock_surface_scrim,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudInfoRes = R.color.theme_shared_hud_cyan,
        hudWarningRes = R.color.theme_shared_hud_orange,
        hudSuccessRes = R.color.theme_shared_hud_lime,
        hudAccentRes = R.color.theme_shared_hud_magenta,
        hudAppLabelRes = R.color.theme_shared_hud_app_label,
        drawerBorderRes = R.color.theme_shared_drawer_border,
        scanlineGlowRes = R.color.theme_shared_scanline_glow,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low,
        reactorMidARes = R.color.theme_shared_reactor_mid_a,
        reactorMidBRes = R.color.theme_shared_reactor_mid_b,
        reactorAccentRingRes = R.color.theme_shared_reactor_accent_ring,
        reactorCoreGlowRes = R.color.theme_shared_reactor_core_glow,
        reactorArmorDarkRes = R.color.theme_shared_reactor_armor_dark,
        reactorArmorMidRes = R.color.theme_shared_reactor_armor_mid,
        reactorInteriorDarkRes = R.color.theme_shared_reactor_interior_dark,
        reactorInteriorMidRes = R.color.theme_shared_reactor_interior_mid,
        reactorFrameShadowRes = R.color.theme_shared_reactor_frame_shadow,
        assistantMutedRes = R.color.theme_shared_assistant_muted,
        assistantErrorRes = R.color.theme_shared_assistant_error
    )

    private val eliteBlue = ThemeSpec(
        name = "Elite Blue",
        primaryRes = R.color.theme_elite_primary,
        secondaryRes = R.color.theme_elite_secondary,
        accentRes = R.color.theme_elite_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        lockSurfaceScrimRes = R.color.theme_shared_lock_surface_scrim,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudInfoRes = R.color.theme_shared_hud_cyan,
        hudWarningRes = R.color.theme_shared_hud_orange,
        hudSuccessRes = R.color.theme_shared_hud_lime,
        hudAccentRes = R.color.theme_shared_hud_magenta,
        hudAppLabelRes = R.color.theme_shared_hud_app_label,
        drawerBorderRes = R.color.theme_shared_drawer_border,
        scanlineGlowRes = R.color.theme_shared_scanline_glow,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low,
        reactorMidARes = R.color.theme_shared_reactor_mid_a,
        reactorMidBRes = R.color.theme_shared_reactor_mid_b,
        reactorAccentRingRes = R.color.theme_shared_reactor_accent_ring,
        reactorCoreGlowRes = R.color.theme_shared_reactor_core_glow,
        reactorArmorDarkRes = R.color.theme_shared_reactor_armor_dark,
        reactorArmorMidRes = R.color.theme_shared_reactor_armor_mid,
        reactorInteriorDarkRes = R.color.theme_shared_reactor_interior_dark,
        reactorInteriorMidRes = R.color.theme_shared_reactor_interior_mid,
        reactorFrameShadowRes = R.color.theme_shared_reactor_frame_shadow,
        assistantMutedRes = R.color.theme_shared_assistant_muted,
        assistantErrorRes = R.color.theme_shared_assistant_error
    )

    private val zombieStrike = ThemeSpec(
        name = "Zombie Strike",
        primaryRes = R.color.theme_zombie_primary,
        secondaryRes = R.color.theme_zombie_secondary,
        accentRes = R.color.theme_zombie_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        lockSurfaceScrimRes = R.color.theme_shared_lock_surface_scrim,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudInfoRes = R.color.theme_shared_hud_cyan,
        hudWarningRes = R.color.theme_shared_hud_orange,
        hudSuccessRes = R.color.theme_shared_hud_lime,
        hudAccentRes = R.color.theme_shared_hud_magenta,
        hudAppLabelRes = R.color.theme_shared_hud_app_label,
        drawerBorderRes = R.color.theme_shared_drawer_border,
        scanlineGlowRes = R.color.theme_shared_scanline_glow,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low,
        reactorMidARes = R.color.theme_shared_reactor_mid_a,
        reactorMidBRes = R.color.theme_shared_reactor_mid_b,
        reactorAccentRingRes = R.color.theme_shared_reactor_accent_ring,
        reactorCoreGlowRes = R.color.theme_shared_reactor_core_glow,
        reactorArmorDarkRes = R.color.theme_shared_reactor_armor_dark,
        reactorArmorMidRes = R.color.theme_shared_reactor_armor_mid,
        reactorInteriorDarkRes = R.color.theme_shared_reactor_interior_dark,
        reactorInteriorMidRes = R.color.theme_shared_reactor_interior_mid,
        reactorFrameShadowRes = R.color.theme_shared_reactor_frame_shadow,
        assistantMutedRes = R.color.theme_shared_assistant_muted,
        assistantErrorRes = R.color.theme_shared_assistant_error
    )

    private val hyperNeon = ThemeSpec(
        name = "Hyper Neon",
        primaryRes = R.color.theme_hyper_primary,
        secondaryRes = R.color.theme_hyper_secondary,
        accentRes = R.color.theme_hyper_accent,
        windowBackgroundRes = R.color.theme_shared_window_background,
        taskbarDarkBackgroundRes = R.color.theme_shared_taskbar_dark_background,
        taskbarLightBackgroundRes = R.color.theme_shared_taskbar_light_background,
        lockSurfaceScrimRes = R.color.theme_shared_lock_surface_scrim,
        hudInactiveMeterRes = R.color.theme_shared_hud_inactive_meter,
        hudPanelTextPrimaryRes = R.color.theme_shared_hud_panel_text_primary,
        hudPanelTextSecondaryRes = R.color.theme_shared_hud_panel_text_secondary,
        hudInfoRes = R.color.theme_shared_hud_cyan,
        hudWarningRes = R.color.theme_shared_hud_orange,
        hudSuccessRes = R.color.theme_shared_hud_lime,
        hudAccentRes = R.color.theme_shared_hud_magenta,
        hudAppLabelRes = R.color.theme_shared_hud_app_label,
        drawerBorderRes = R.color.theme_shared_drawer_border,
        scanlineGlowRes = R.color.theme_shared_scanline_glow,
        hudEnergyHighRes = R.color.theme_shared_hud_energy_high,
        hudEnergyLowRes = R.color.theme_shared_hud_energy_low,
        reactorMidARes = R.color.theme_shared_reactor_mid_a,
        reactorMidBRes = R.color.theme_shared_reactor_mid_b,
        reactorAccentRingRes = R.color.theme_shared_reactor_accent_ring,
        reactorCoreGlowRes = R.color.theme_shared_reactor_core_glow,
        reactorArmorDarkRes = R.color.theme_shared_reactor_armor_dark,
        reactorArmorMidRes = R.color.theme_shared_reactor_armor_mid,
        reactorInteriorDarkRes = R.color.theme_shared_reactor_interior_dark,
        reactorInteriorMidRes = R.color.theme_shared_reactor_interior_mid,
        reactorFrameShadowRes = R.color.theme_shared_reactor_frame_shadow,
        assistantMutedRes = R.color.theme_shared_assistant_muted,
        assistantErrorRes = R.color.theme_shared_assistant_error
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
            lockSurfaceScrim = context.color(spec.lockSurfaceScrimRes),
            hudInactiveMeterColor = context.color(spec.hudInactiveMeterRes),
            hudPanelTextPrimary = context.color(spec.hudPanelTextPrimaryRes),
            hudPanelTextSecondary = context.color(spec.hudPanelTextSecondaryRes),
            hudInfoColor = context.color(spec.hudInfoRes),
            hudWarningColor = context.color(spec.hudWarningRes),
            hudSuccessColor = context.color(spec.hudSuccessRes),
            hudAccentColor = context.color(spec.hudAccentRes),
            hudAppLabelColor = context.color(spec.hudAppLabelRes),
            drawerBorderColor = context.color(spec.drawerBorderRes),
            scanlineGlowColor = context.color(spec.scanlineGlowRes),
            hudEnergyHighColor = context.color(spec.hudEnergyHighRes),
            hudEnergyLowColor = context.color(spec.hudEnergyLowRes),
            reactorMidAColor = context.color(spec.reactorMidARes),
            reactorMidBColor = context.color(spec.reactorMidBRes),
            reactorAccentRingColor = context.color(spec.reactorAccentRingRes),
            reactorCoreGlowColor = context.color(spec.reactorCoreGlowRes),
            reactorArmorDarkColor = context.color(spec.reactorArmorDarkRes),
            reactorArmorMidColor = context.color(spec.reactorArmorMidRes),
            reactorInteriorDarkColor = context.color(spec.reactorInteriorDarkRes),
            reactorInteriorMidColor = context.color(spec.reactorInteriorMidRes),
            reactorFrameShadowColor = context.color(spec.reactorFrameShadowRes),
            assistantMutedColor = context.color(spec.assistantMutedRes),
            assistantErrorColor = context.color(spec.assistantErrorRes)
        )
    }

    private fun Context.color(@ColorRes resId: Int): Int = ContextCompat.getColor(this, resId)
}
