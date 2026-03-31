package com.nerf.launcher.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.ui.ReactorCoreView
import com.nerf.launcher.ui.reactor.ReactorModuleView
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

    fun resolveTaskbarBackgroundColor(theme: NerfTheme, backgroundStyle: TaskbarBackgroundStyle): Int {
        return when (backgroundStyle) {
            TaskbarBackgroundStyle.LIGHT -> theme.taskbarLightBackground
            TaskbarBackgroundStyle.TRANSPARENT -> Color.TRANSPARENT
            TaskbarBackgroundStyle.DARK -> theme.taskbarDarkBackground
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
        root.findViewById<View>(R.id.lock_surface_root)?.setBackgroundColor(theme.lockSurfaceScrim)
        root.findViewById<View>(R.id.scanline_overlay)?.background = createScanlineOverlayDrawable(theme)
        root.findViewById<EditText>(R.id.drawerSearchInput)?.background = createDrawerSearchFieldBackground(root.context, theme)
        applyButtonBackground(root, R.id.quickThemeBtn, createQuickToggleOrbDrawable(root.context, theme))
        applyButtonBackground(root, R.id.quickIconPackBtn, createQuickToggleOrbDrawable(root.context, theme))
        applyButtonBackground(root, R.id.quickAnimationBtn, createQuickToggleOrbDrawable(root.context, theme))
        applyButtonBackground(root, R.id.quickTaskbarBtn, createQuickToggleOrbDrawable(root.context, theme))
        applyButtonBackground(root, R.id.openSettingsTile, createHudActionTileDrawable(root.context, theme))
        applyButtonBackground(root, R.id.reloadTile, createHudActionTileDrawable(root.context, theme))
        applyButtonBackground(root, R.id.lockSurfaceTile, createHudActionTileDrawable(root.context, theme))
        applyButtonBackground(root, R.id.lockSurfaceUnlockButton, createHudActionTileDrawable(root.context, theme))
        applyButtonBackground(root, R.id.openTaskbarSettingsButton, createHudActionTileDrawable(root.context, theme))
        applyThemeToCustomViews(root, theme)
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

        root.findViewById<TextView>(R.id.battery_label)?.setTextColor(theme.hudWarningColor)
        root.findViewById<TextView>(R.id.brand_signature)?.setTextColor(theme.hudPanelTextSecondary)

        val addWidgetBtn = root.findViewById<MaterialButton>(R.id.add_widget_btn)
        addWidgetBtn?.setTextColor(theme.hudSuccessColor)
        addWidgetBtn?.background = createHudActionTileDrawable(activity, theme)

        applyHudPanelGlow(root, theme)
        root.findViewById<ReactorModuleView>(R.id.reactor_core)?.updateTheme(theme)
    }

    fun createScanlineOverlayDrawable(theme: NerfTheme): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(0x11, 0, 0, 0),
                theme.scanlineGlowColor,
                Color.argb(0x11, 0, 0, 0)
            )
        )
    }

    fun createDrawerSearchFieldBackground(context: Context, theme: NerfTheme): Drawable {
        val radius = context.resources.getDimension(R.dimen.nerf_tile_radius_medium)
        val stroke = px(context, 1f)
        val focused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ColorUtils.setAlphaComponent(theme.windowBackground, 0x22))
            setStroke(stroke, theme.hudInfoColor)
        }
        val idle = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ColorUtils.setAlphaComponent(theme.windowBackground, 0x12))
            setStroke(stroke, theme.drawerBorderColor)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), idle)
        }
    }

    fun createQuickToggleOrbDrawable(context: Context, theme: NerfTheme): Drawable {
        val orbSize = px(context, 44f)
        val stroke = px(context, 1f)
        val pressedInset = px(context, 8f)
        val middleInset = px(context, 3f)
        val innerInset = px(context, 11f)
        val pressedOuter = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setSize(orbSize, orbSize)
            setColor(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudInfoColor, 0.18f), 0x40))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0xBF))
        }
        val pressedInner = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(0x55, 0xFF, 0xFF, 0xFF))
            setStroke(stroke, Color.argb(0x99, 0xFF, 0xFF, 0xFF))
        }
        val defaultOuter = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudInfoColor, 0.18f), 0x2B),
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudPanelTextSecondary, 0.16f), 0x16)
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setSize(orbSize, orbSize)
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0x7A))
        }
        val defaultMiddle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudPanelTextPrimary, 0.08f), 0x1F))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0x6D))
        }
        val defaultInner = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.hudInfoColor, theme.hudPanelTextPrimary, 0.12f), 0x66),
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.hudInfoColor, theme.windowBackground, 0.22f), 0x15)
            )
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0xCC))
        }
        return StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                LayerDrawable(
                    arrayOf(
                        pressedOuter,
                        InsetDrawable(pressedInner, pressedInset)
                    )
                )
            )
            addState(
                intArrayOf(),
                LayerDrawable(
                    arrayOf(
                        defaultOuter,
                        InsetDrawable(defaultMiddle, middleInset),
                        InsetDrawable(defaultInner, innerInset)
                    )
                )
            )
        }
    }

    fun createHudActionTileDrawable(context: Context, theme: NerfTheme): Drawable {
        val outerRadius = pxF(context, 2f)
        val innerRadius = pxF(context, 1f)
        val stroke = px(context, 1f)
        val innerInset = px(context, 1f)
        val accentInset = px(context, 2f)
        val pressedAccentHeight = px(context, 2f)
        val defaultAccentHeight = px(context, 1f)
        val pressedOuter = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = outerRadius
            setColor(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, Color.BLACK, 0.45f), 0xE3))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0xFF))
        }
        val pressedInner = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudPanelTextSecondary, 0.35f), 0xD8),
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudPanelTextPrimary, 0.12f), 0xDA)
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = innerRadius
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0xAA))
        }
        val pressedAccent = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setSize(0, pressedAccentHeight)
            setColor(ColorUtils.setAlphaComponent(theme.hudInfoColor, 0xA0))
        }
        val defaultOuter = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = outerRadius
            setColor(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, Color.BLACK, 0.52f), 0xD7))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudWarningColor, 0x66))
        }
        val defaultInner = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = innerRadius
            setColor(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.windowBackground, theme.hudPanelTextSecondary, 0.14f), 0xD2))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0x66))
        }
        val defaultAccent = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setSize(0, defaultAccentHeight)
            setColor(ColorUtils.setAlphaComponent(theme.hudWarningColor, 0x80))
        }
        val pressedLayers = LayerDrawable(
            arrayOf(
                pressedOuter,
                pressedInner,
                pressedAccent
            )
        ).apply {
            setLayerInset(1, innerInset, innerInset, innerInset, innerInset)
            setLayerInset(2, accentInset, accentInset, accentInset, accentInset)
            setLayerGravity(2, Gravity.TOP or Gravity.START)
        }
        val defaultLayers = LayerDrawable(
            arrayOf(
                defaultOuter,
                defaultInner,
                defaultAccent
            )
        ).apply {
            setLayerInset(1, innerInset, innerInset, innerInset, innerInset)
            setLayerInset(2, accentInset, accentInset, accentInset, accentInset)
            setLayerGravity(2, Gravity.BOTTOM or Gravity.END)
        }
        return StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                pressedLayers
            )
            addState(
                intArrayOf(),
                defaultLayers
            )
        }
    }

    fun createAppIconSocketBackground(context: Context, theme: NerfTheme): Drawable {
        val radius = context.resources.getDimension(R.dimen.nerf_tile_radius_small)
        val stroke = px(context, 1f)
        val middleInset = px(context, 1f)
        val innerInset = px(context, 3f)
        val stripInset = px(context, 4f)
        val stripBottomInset = px(context, 4f)
        val stripHeight = px(context, 2f)
        val outer = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.reactorInteriorDarkColor, theme.reactorArmorDarkColor, 0.25f), 0xB9),
                ColorUtils.setAlphaComponent(theme.reactorInteriorDarkColor, 0xD0)
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.reactorArmorMidColor, 0x7F))
        }
        val middle = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.setAlphaComponent(ColorUtils.blendARGB(theme.reactorInteriorMidColor, theme.reactorArmorMidColor, 0.18f), 0x26),
                ColorUtils.setAlphaComponent(theme.reactorInteriorDarkColor, 0x12)
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.reactorArmorMidColor, 0x66))
        }
        val inner = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ColorUtils.setAlphaComponent(Color.BLACK, 0x17))
            setStroke(stroke, ColorUtils.setAlphaComponent(theme.hudInfoColor, 0x77))
        }
        val topStrip = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                ColorUtils.setAlphaComponent(theme.hudInfoColor, 0x7A),
                ColorUtils.setAlphaComponent(theme.hudInfoColor, 0x0C)
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            setSize(0, stripHeight)
        }
        return LayerDrawable(
            arrayOf(
                outer,
                middle,
                inner,
                topStrip
            )
        ).apply {
            setLayerInset(1, middleInset, middleInset, middleInset, middleInset)
            setLayerInset(2, innerInset, innerInset, innerInset, innerInset)
            setLayerInset(3, stripInset, stripInset, stripInset, stripBottomInset)
            setLayerGravity(3, Gravity.TOP)
        }
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

    private fun applyButtonBackground(root: View, viewId: Int, drawable: Drawable) {
        root.findViewById<MaterialButton>(viewId)?.background = drawable.constantState?.newDrawable()?.mutate() ?: drawable
    }

    private fun applyThemeToCustomViews(root: View, theme: NerfTheme) {
        when (root) {
            is SegmentedBarView -> {
                root.setActiveColor(theme.hudInfoColor)
                root.setInactiveColor(theme.hudInactiveMeterColor)
                root.setGradientHighlightColor(theme.hudPanelTextPrimary)
            }
            is ReactorModuleView -> root.updateTheme(theme)
            is ReactorCoreView -> root.updateTheme(theme)
        }
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                applyThemeToCustomViews(root.getChildAt(index), theme)
            }
        }
    }

    private fun px(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun pxF(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

}
