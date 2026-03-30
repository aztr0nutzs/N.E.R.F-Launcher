package com.nerf.launcher.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.Window
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.ui.SegmentedBarView

/**
 * Applies launcher visuals from the authoritative NerfTheme produced by ThemeRepository.
 */
object ThemeManager {

    fun resolveActiveTheme(context: Context): NerfTheme {
        val config = ConfigRepository.get().config.value
        return resolveActiveTheme(context, config?.themeName, config?.glowIntensity)
    }

    fun resolveActiveTheme(context: Context, themeName: String?, glowIntensity: Float?): NerfTheme {
        val baseTheme = ThemeRepository.resolve(context, themeName)
        val resolvedGlow = glowIntensity ?: baseTheme.glowIntensity
        return baseTheme.withGlowIntensity(resolvedGlow)
    }

    fun resolveTaskbarIconTint(context: Context, theme: NerfTheme): Int {
        return if (ColorUtils.isColorLight(theme.primary)) {
            context.getColor(R.color.nerf_on_secondary)
        } else {
            context.getColor(R.color.nerf_on_background)
        }
    }

    fun resolveTaskbarBackgroundColor(theme: NerfTheme, backgroundStyle: Int): Int {
        return when (backgroundStyle) {
            android.R.color.background_light -> theme.taskbarLightBackground
            android.R.color.transparent -> Color.TRANSPARENT
            else -> theme.taskbarDarkBackground
        }
    }

    fun applyTheme(activity: Activity, root: View? = null, theme: NerfTheme = resolveActiveTheme(activity)) {
        activity.setTheme(R.style.Theme_NerfLauncher)
        applyWindowTheme(activity, theme)
        applyLauncherShellTheme(root ?: activity.findViewById(R.id.root_container), theme)
        applyHudTheme(activity, theme)
    }

    fun applyWindowTheme(activity: Activity, theme: NerfTheme) {
        updateWindowBackground(activity.window, theme)
    }

    private fun updateWindowBackground(window: Window?, theme: NerfTheme) {
        window?.setBackgroundDrawable(ColorDrawable(theme.windowBackground))
    }

    fun applyLauncherShellTheme(root: View?, theme: NerfTheme) {
        root ?: return
        root.setBackgroundColor(theme.windowBackground)
        root.findViewById<View>(R.id.lock_surface_root)?.setBackgroundColor(
            root.context.getColor(R.color.nerf_lock_surface_scrim)
        )
    }

    fun applyHudTheme(activity: Activity, theme: NerfTheme) {
        val root = activity.findViewById<View>(R.id.hud_root) ?: return

        val batteryMeter = root.findViewById<SegmentedBarView>(R.id.battery_meter)
        batteryMeter?.setActiveColor(theme.primary)
        batteryMeter?.setInactiveColor(theme.hudInactiveMeterColor)
        batteryMeter?.setGradientHighlightColor(theme.hudPanelTextPrimary)

        val timeDisplay = root.findViewById<TextView>(R.id.time_display)
        timeDisplay?.setTextColor(theme.secondary)

        val dateDisplay = root.findViewById<TextView>(R.id.date_display)
        dateDisplay?.setTextColor(theme.hudPanelTextSecondary)

        val addWidgetBtn = root.findViewById<MaterialButton>(R.id.add_widget_btn)
        addWidgetBtn?.setTextColor(theme.accent)

        applyHudPanelGlow(root, theme)
    }

    fun applyHudPanelGlow(root: View, theme: NerfTheme) {
        val baseBackground = resolveHudBaseBackground(root) ?: return
        val radius = root.resources.getDimension(R.dimen.nerf_tile_radius_small)
        val inset = root.resources.displayMetrics.density.toInt().coerceAtLeast(1)
        val glowAlpha = (theme.glowIntensity * 0.15f).coerceIn(0.0f, 0.35f)
        val primaryGlow = Color.argb(
            (glowAlpha * 255).toInt(),
            Color.red(theme.primary),
            Color.green(theme.primary),
            Color.blue(theme.primary)
        )
        val secondaryGlow = Color.argb(
            (glowAlpha * 160).toInt(),
            Color.red(theme.secondary),
            Color.green(theme.secondary),
            Color.blue(theme.secondary)
        )
        val glowOverlay = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(primaryGlow, secondaryGlow)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (radius - inset).coerceAtLeast(0f)
        }

        root.background = LayerDrawable(
            arrayOf(
                baseBackground,
                InsetDrawable(glowOverlay, inset)
            )
        )
    }

    private fun resolveHudBaseBackground(root: View): Drawable? {
        when (val cached = root.getTag(R.id.hud_root)) {
            is Drawable.ConstantState -> return cached.newDrawable(root.resources).mutate()
            is Drawable -> {
                val recreated = cached.constantState?.newDrawable(root.resources) ?: cached
                return recreated.mutate()
            }
        }

        val currentBackground = root.background ?: return null
        val constantState = currentBackground.constantState
        if (constantState != null) {
            root.setTag(R.id.hud_root, constantState)
            return constantState.newDrawable(root.resources).mutate()
        }

        val mutated = currentBackground.mutate()
        root.setTag(R.id.hud_root, mutated)
        return mutated
    }
}
