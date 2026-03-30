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
 * Applies a NerfTheme to an Activity. Reads the current configuration
 * exclusively from ConfigRepository (single source of truth).
 */
object ThemeManager {

    fun resolveActiveTheme(): NerfTheme {
        val config = ConfigRepository.get().config.value
        return resolveActiveTheme(config?.themeName, config?.glowIntensity)
    }

    fun resolveActiveTheme(themeName: String?, glowIntensity: Float?): NerfTheme {
        val baseTheme = ThemeRepository.byName(themeName.orEmpty()) ?: ThemeRepository.CLASSIC_NERF
        val resolvedGlow = glowIntensity ?: baseTheme.glowIntensity
        return baseTheme.copy(glowIntensity = resolvedGlow)
    }

    fun resolveTaskbarIconTint(context: Context, theme: NerfTheme): Int {
        return if (ColorUtils.isColorLight(theme.primary)) {
            context.getColor(R.color.nerf_on_secondary)
        } else {
            context.getColor(R.color.nerf_on_background)
        }
    }

    fun applyTheme(activity: Activity) {
        val finalTheme = resolveActiveTheme()

        // Apply the base theme (styles.xml) – programmatic tweaks follow.
        activity.setTheme(R.style.Theme_NerfLauncher)
        updateWindowBackground(activity.window, finalTheme)
        applyHudTheme(activity, finalTheme)
    }

    private fun updateWindowBackground(window: Window?, theme: NerfTheme) {
        // Use the theme‑defined window background color.
        window?.setBackgroundDrawable(ColorDrawable(theme.windowBackground))
    }

    fun applyHudTheme(activity: Activity, theme: NerfTheme) {
        val root = activity.findViewById<View>(R.id.hud_root) ?: return

        val batteryMeter = root.findViewById<SegmentedBarView>(R.id.battery_meter)
        batteryMeter?.setActiveColor(theme.primary)
        batteryMeter?.setInactiveColor(theme.hudInactiveMeterColor)

        val timeDisplay = root.findViewById<TextView>(R.id.time_display)
        timeDisplay?.setTextColor(theme.secondary)

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
