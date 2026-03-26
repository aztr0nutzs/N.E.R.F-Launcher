package com.nerf.launcher.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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
        // Create 4 icon slots by default (background and tint will be set via observers)
        for (i in 0 until 4) {
            val iconView = ImageView(context).apply {
                setImageResource(R.drawable.ic_launcher_foreground) // placeholder
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(4, 4, 4, 4)
            }
            addView(iconView, LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1.0f
            ).apply { setMargins(4, 0, 4, 0) })
            iconViews.add(iconView)
        }
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
        iconViews.forEach { it.setImageResource(R.drawable.ic_launcher_foreground) }
        iconViews.forEachIndexed { index, view ->
            if (index < packageNames.size) {
                val pkg = packageNames[index]
                if (::iconProvider.isInitialized) {
                    val icon = iconProvider.getIcon(pkg)
                    view.setImageDrawable(icon)
                }
                // Set click listener to launch the app
                view.setOnClickListener {
                    AppUtils.launchApp(context, pkg)
                }
                view.setOnLongClickListener {
                    android.widget.Toast.makeText(
                        context,
                        "Long press to customize (not implemented)",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    true
                }
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

    private fun solidColorToInt(drawable: android.graphics.drawable.Drawable?): Int {
        if (drawable is android.graphics.drawable.ColorDrawable) {
            return drawable.color
        }
        return Color.BLACK
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
                val pinnedApps = settings.pinnedApps
                val appsToShow = if (pinnedApps.isEmpty()) emptyList() else pinnedApps
                updateIcons(appsToShow)

                // Update taskbar background
                val backgroundColor = try {
                    ContextCompat.getColor(context, settings.backgroundStyle)
                } catch (e: Exception) {
                    Color.BLACK
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
                    ContextCompat.getColor(context, R.color.nerf_on_background)
                } else {
                    ContextCompat.getColor(context, R.color.nerf_on_background)
                }
                setIconTint(iconTint)
            }
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