package com.nerf.launcher.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.LifecycleOwner
import com.nerf.launcher.R
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.LifecycleOwnerAware
import com.nerf.launcher.util.NerfTheme
import com.nerf.launcher.util.TaskbarSettings
import com.nerf.launcher.util.ThemeManager
import kotlin.math.roundToInt

/**
 * Custom view representing the taskbar at the bottom of the screen.
 */
class TaskbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), LifecycleOwnerAware {

    private var iconProvider: IconProvider? = null
    private val iconViews = mutableListOf<ImageView>()
    private var lifecycleOwner: LifecycleOwner? = null
    private var currentIconTint: Int? = null
    private var currentIconSizePx: Int? = null
    private var lastObservedConfig: AppConfig? = null
    private var renderedPackages: List<String> = emptyList()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(8, 6, 8, 6)
        applyShellBackground(android.R.color.background_dark, 1f, null)
        setOnLongClickListener {
            openTaskbarCustomization()
            true
        }
    }

    override fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        setupConfigObservers()
    }

    fun setIconProvider(provider: IconProvider) {
        iconProvider = provider
    }

    fun updateIcons(packageNames: List<String>, forceRebind: Boolean = false) {
        val renderablePackages = packageNames
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        if (!forceRebind && renderedPackages == renderablePackages) {
            return
        }
        renderedPackages = renderablePackages
        syncIconSlots(renderablePackages.size)
        iconViews.forEachIndexed { index, view ->
            bindIconView(view, renderablePackages[index])
        }
    }

    fun setIconTint(color: Int) {
        currentIconTint = color
        iconViews.forEach { it.setColorFilter(color) }
    }

    fun setIconSize(sizePx: Int) {
        currentIconSizePx = sizePx
        iconViews.forEach(::applyIconSize)
    }

    fun setTaskbarHeight(heightPx: Int) {
        val params = layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.height = heightPx
        layoutParams = params
    }

    fun setTransparency(alpha: Float) {
        val config = lastObservedConfig
        val styleRes = config?.taskbarSettings?.backgroundStyle ?: android.R.color.background_dark
        val theme = config?.let {
            ThemeManager.resolveActiveTheme(
                context = context,
                themeName = it.themeName,
                glowIntensity = it.glowIntensity
            )
        }
        applyShellBackground(styleRes, alpha, theme)
    }

    private fun setupConfigObservers() {
        lifecycleOwner?.let { owner ->
            ConfigRepository.get().config.observe(owner) { config ->
                val previous = lastObservedConfig
                val settings = config.taskbarSettings
                applyTaskbarSettings(previous?.taskbarSettings, settings)

                val iconTintNeedsUpdate = previous == null ||
                        previous.themeName != config.themeName ||
                        previous.glowIntensity != config.glowIntensity
                if (iconTintNeedsUpdate) {
                    val theme = ThemeManager.resolveActiveTheme(
                        context = context,
                        themeName = config.themeName,
                        glowIntensity = config.glowIntensity
                    )
                    val iconTint = ThemeManager.resolveTaskbarIconTint(context, theme)
                    setIconTint(iconTint)
                    applyShellBackground(settings.backgroundStyle, settings.transparency, theme)
                }

                val pinnedAppsChanged = previous?.taskbarSettings?.pinnedApps != settings.pinnedApps
                val iconPackChanged = previous?.iconPack != config.iconPack
                if (previous == null || pinnedAppsChanged || iconPackChanged) {
                    updateIcons(settings.pinnedApps, forceRebind = iconPackChanged)
                }

                lastObservedConfig = config
            }
        }
    }

    private fun applyTaskbarSettings(previous: TaskbarSettings?, current: TaskbarSettings) {
        if (previous?.enabled != current.enabled || previous == null) {
            visibility = if (current.enabled) View.VISIBLE else View.GONE
        }
        if (previous?.height != current.height || previous == null) {
            val heightPx = (current.height * resources.displayMetrics.density).roundToInt()
            setTaskbarHeight(heightPx)
        }
        if (previous?.iconSize != current.iconSize || previous == null) {
            val iconSizePx = (current.iconSize * resources.displayMetrics.density).roundToInt()
            setIconSize(iconSizePx)
        }
        if (previous?.transparency != current.transparency ||
            previous?.backgroundStyle != current.backgroundStyle ||
            previous == null
        ) {
            val theme = ThemeManager.resolveActiveTheme(
                context = context,
                themeName = lastObservedConfig?.themeName,
                glowIntensity = lastObservedConfig?.glowIntensity
            )
            applyShellBackground(current.backgroundStyle, current.transparency, theme)
        }
    }

    private fun syncIconSlots(targetCount: Int) {
        while (iconViews.size > targetCount) {
            val removedView = iconViews.removeAt(iconViews.lastIndex)
            removeView(removedView)
        }

        while (iconViews.size < targetCount) {
            val iconView = createIconView()
            addView(
                iconView,
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(4, 0, 4, 0) }
            )
            applyIconSize(iconView)
            iconViews.add(iconView)
        }
    }

    private fun createIconView(): ImageView {
        return ImageView(context).apply {
            setImageResource(R.drawable.ic_module)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(10, 8, 10, 8)
            background = ContextCompat.getDrawable(context, R.drawable.dock_tile_background)
            contentDescription = context.getString(R.string.app_icon)
            currentIconTint?.let { setColorFilter(it) }
            setOnClickListener {
                val packageName = it.tag as? String ?: return@setOnClickListener
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }

            setOnLongClickListener {
                openTaskbarCustomization()
                true
            }

            setOnTouchListener { touchedView, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touchedView.animate().cancel()
                        touchedView.animate()
                            .scaleX(0.965f)
                            .scaleY(0.965f)
                            .alpha(0.92f)
                            .setDuration(70L)
                            .setInterpolator(FastOutSlowInInterpolator())
                            .start()
                    }

                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        touchedView.animate().cancel()
                        touchedView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150L)
                            .setInterpolator(LinearOutSlowInInterpolator())
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun bindIconView(view: ImageView, packageName: String) {
        view.tag = packageName
        iconProvider?.loadIconInto(packageName, view)
            ?: view.setImageResource(R.drawable.ic_module)
        currentIconTint?.let { view.setColorFilter(it) }
    }

    private fun applyIconSize(view: ImageView) {
        val sizePx = currentIconSizePx ?: return
        val params = view.layoutParams as? LayoutParams ?: return
        params.width = sizePx
        params.height = sizePx
        view.layoutParams = params
    }

    private fun applyShellBackground(backgroundStyle: Int, alpha: Float, theme: NerfTheme?) {
        val shellDrawable = ContextCompat.getDrawable(context, R.drawable.hud_frame_panel)?.mutate() ?: return
        val wrapped = DrawableCompat.wrap(shellDrawable)
        val resolvedTheme = theme ?: ThemeManager.resolveActiveTheme(context)
        val bgColor = ThemeManager.resolveTaskbarBackgroundColor(resolvedTheme, backgroundStyle)
        DrawableCompat.setTint(wrapped, bgColor)
        wrapped.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        background = wrapped
    }

    private fun openTaskbarCustomization() {
        val intent = TaskbarSettingsActivity.createIntent(context)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
