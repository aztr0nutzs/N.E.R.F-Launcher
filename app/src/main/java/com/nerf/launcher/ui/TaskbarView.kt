package com.nerf.launcher.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.R as MaterialR
import com.nerf.launcher.R
import com.nerf.launcher.util.AppUtils
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.LifecycleOwnerAware
import com.nerf.launcher.util.ThemeRepository
import kotlin.math.roundToInt

/**
 * Custom view representing the taskbar at the bottom of the screen.
 * Displays a horizontal list of app shortcut icons.
 * Fully reactive to configuration changes via ConfigRepository.
 */
class TaskbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), LifecycleOwnerAware {

    private lateinit var iconProvider: IconProvider
    private val iconViews = mutableListOf<ImageView>()
    private var lifecycleOwner: LifecycleOwner? = null

    init {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        // Initial padding will be updated by observers
        setPadding(8, 4, 8, 4)
    }

    override fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        setupConfigObservers()
    }

    /** Set the icon provider to use for loading icons. */
    fun setIconProvider(provider: IconProvider) {
        iconProvider = provider
    }

    /** Update the taskbar icons based on a list of package names. */
    fun updateIcons(packageNames: List<String>) {
        syncIconViews(packageNames.size)
        iconViews.forEachIndexed { index, view ->
            val packageName = packageNames.getOrNull(index)?.trim().orEmpty()
            view.setImageResource(R.drawable.ic_launcher_foreground)
            view.setImageDrawable(null)
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)

            if (packageName.isNotEmpty()) {
                if (::iconProvider.isInitialized) {
                    view.setImageDrawable(iconProvider.getIcon(packageName))
                } else {
                    view.setImageResource(R.drawable.ic_launcher_foreground)
                }
                view.setOnClickListener {
                    AppUtils.launchApp(context, packageName)
                }
            } else {
                view.setImageResource(R.drawable.ic_launcher_foreground)
            }

            view.setOnLongClickListener {
                openSettings()
                true
            }
        }
    }

    /** Set the icon tint color. */
    fun setIconTint(color: Int) {
        iconViews.forEach { it.setColorFilter(color) }
    }

    /** Set the icon size for all slots (in pixels). */
    fun setIconSize(sizePx: Int) {
        iconViews.forEach { view ->
            val params = view.layoutParams as LinearLayout.LayoutParams
            params.width = sizePx
            params.height = sizePx
            view.layoutParams = params
        }
    }

    /** Set the height of the taskbar (in pixels). */
    fun setHeight(heightPx: Int) {
        val params = layoutParams as? ViewGroup.LayoutParams ?: return
        params.height = heightPx
        layoutParams = params
    }

    /** Set the background transparency (alpha) of the taskbar. */
    fun setTransparency(alpha: Float) {
        val currentColor = solidColorToInt(background)
        val newColor = Color.argb(
            (alpha * 255).toInt(),
            Color.red(currentColor),
            Color.green(currentColor),
            Color.blue(currentColor)
        )
        setBackgroundColor(newColor)
    }

    private fun syncIconViews(targetCount: Int) {
        while (iconViews.size < targetCount) {
            val iconView = createIconView()
            addView(iconView, createIconLayoutParams())
            iconViews.add(iconView)
        }
        while (iconViews.size > targetCount) {
            val lastIndex = iconViews.lastIndex
            removeView(iconViews.removeAt(lastIndex))
        }
    }

    private fun createIconView(): ImageView {
        return ImageView(context).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(4, 4, 4, 4)
        }
    }

    private fun createIconLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            LayoutParams.WRAP_CONTENT,
            1.0f
        ).apply {
            setMargins(4, 0, 4, 0)
        }
    }

    private fun openSettings() {
        val intent = Intent(context, SettingsActivity::class.java).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    private fun solidColorToInt(drawable: android.graphics.drawable.Drawable?): Int {
        if (drawable is android.graphics.drawable.ColorDrawable) {
            return drawable.color
        }
        return resolveThemeColor(MaterialR.attr.colorSurface, R.color.black)
    }

    private fun setupConfigObservers() {
        lifecycleOwner?.let { owner ->
            ConfigRepository.get().config.observe(owner) { config ->
                val settings = config.taskbarSettings

                // Update taskbar visibility
                visibility = if (settings.enabled) View.VISIBLE else View.GONE

                // Update taskbar height (convert dp to px)
                val heightPx = (settings.height * context.resources.displayMetrics.density).roundToInt()
                setHeight(heightPx)

                // Update icons from pinned apps in settings
                val pinnedApps = settings.pinnedApps.map { it.trim() }
                if (::iconProvider.isInitialized) {
                    iconProvider.evictCache()
                }
                updateIcons(pinnedApps)

                // Update taskbar background
                val backgroundColor = try {
                    ContextCompat.getColor(context, settings.backgroundStyle)
                } catch (e: Exception) {
                    resolveThemeColor(MaterialR.attr.colorSurface, R.color.black)
                }
                setBackgroundColor(backgroundColor)

                // Update icon size (convert dp to px)
                val iconSizePx = (settings.iconSize * context.resources.displayMetrics.density).roundToInt()
                setIconSize(iconSizePx)

                // Update background transparency
                setTransparency(settings.transparency)

                // Update icon tint based on theme
                val baseTheme = ThemeRepository.byName(config.themeName)
                    ?: ThemeRepository.CLASSIC_NERF
                val isLightTheme = isColorLight(baseTheme.primary)
                val iconTint = if (isLightTheme) {
                    ContextCompat.getColor(context, R.color.black)
                } else {
                    ContextCompat.getColor(context, R.color.nerf_on_background)
                }
                setIconTint(iconTint)
            }
        }
    }

    private fun resolveThemeColor(attrResId: Int, fallbackColorResId: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attrResId, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
        } else {
            ContextCompat.getColor(context, fallbackColorResId)
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }
}
