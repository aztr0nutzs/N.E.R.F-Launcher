package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nerf.launcher.R
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.LifecycleOwnerAware
import com.nerf.launcher.util.ThemeRepository
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

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(8, 6, 8, 6)
        background = ContextCompat.getDrawable(context, R.drawable.hud_frame_panel)

        repeat(4) {
            val iconView = ImageView(context).apply {
                setImageResource(android.R.drawable.sym_def_app_icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(10, 8, 10, 8)
                background = ContextCompat.getDrawable(context, R.drawable.dock_tile_background)
                contentDescription = context.getString(R.string.app_icon)
            }
            addView(
                iconView,
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply { setMargins(4, 0, 4, 0) }
            )
            iconViews.add(iconView)
        }
    }

    override fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        setupConfigObservers()
    }

    fun setIconProvider(provider: IconProvider) {
        iconProvider = provider
    }

    fun updateIcons(packageNames: List<String>) {
        iconViews.forEachIndexed { index, view ->
            val pkg = packageNames.getOrNull(index)
            if (pkg == null) {
                view.setImageResource(android.R.drawable.sym_def_app_icon)
                view.setOnClickListener(null)
                view.setOnLongClickListener(null)
            } else {
                iconProvider?.loadIconInto(pkg, view)
                    ?: view.setImageResource(android.R.drawable.sym_def_app_icon)

                view.setOnClickListener {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }

                view.setOnLongClickListener {
                    context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    true
                }
            }
        }
    }

    fun setIconTint(color: Int) {
        iconViews.forEach { it.setColorFilter(color) }
    }

    fun setIconSize(sizePx: Int) {
        iconViews.forEach { view ->
            val params = view.layoutParams as LayoutParams
            params.width = sizePx
            params.height = sizePx
            view.layoutParams = params
        }
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
        val currentColor = (background as? ColorDrawable)?.color ?: Color.BLACK
        val newColor = Color.argb(
            (alpha * 255).toInt(),
            Color.red(currentColor),
            Color.green(currentColor),
            Color.blue(currentColor)
        )
        super.setBackgroundColor(newColor)
    }

    private fun setupConfigObservers() {
        lifecycleOwner?.let { owner ->
            ConfigRepository.get().config.observe(owner) { config ->
                val settings = config.taskbarSettings
                visibility = if (settings.enabled) View.VISIBLE else View.GONE

                val heightPx = (settings.height * resources.displayMetrics.density).roundToInt()
                setTaskbarHeight(heightPx)

                val iconSizePx = (settings.iconSize * resources.displayMetrics.density).roundToInt()
                setIconSize(iconSizePx)

                val backgroundColor = ContextCompat.getColor(context, settings.backgroundStyle)
                super.setBackgroundColor(backgroundColor)
                setTransparency(settings.transparency)

                val baseTheme = ThemeRepository.byName(config.themeName)
                    ?: ThemeRepository.CLASSIC_NERF
                val iconTint = if (com.nerf.launcher.util.ColorUtils.isColorLight(baseTheme.primary)) {
                    ContextCompat.getColor(context, R.color.nerf_on_secondary)
                } else {
                    ContextCompat.getColor(context, R.color.nerf_on_background)
                }
                setIconTint(iconTint)

                updateIcons(settings.pinnedApps)
            }
        }
    }
}
