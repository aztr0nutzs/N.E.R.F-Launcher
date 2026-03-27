package com.nerf.launcher.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
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
    private var currentIconTint: Int? = null
    private var currentIconSizePx: Int? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(8, 6, 8, 6)
        background = ContextCompat.getDrawable(context, R.drawable.hud_frame_panel)
    }

    override fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        setupConfigObservers()
    }

    fun setIconProvider(provider: IconProvider) {
        iconProvider = provider
    }

    fun updateIcons(packageNames: List<String>) {
        syncIconSlots(packageNames.size)
        iconViews.forEachIndexed { index, view ->
            bindIconView(view, packageNames[index])
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
            setImageResource(android.R.drawable.sym_def_app_icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(10, 8, 10, 8)
            background = ContextCompat.getDrawable(context, R.drawable.dock_tile_background)
            contentDescription = context.getString(R.string.app_icon)
            currentIconTint?.let { setColorFilter(it) }
        }
    }

    private fun bindIconView(view: ImageView, packageName: String) {
        iconProvider?.loadIconInto(packageName, view)
            ?: view.setImageResource(android.R.drawable.sym_def_app_icon)

        currentIconTint?.let { view.setColorFilter(it) }

        view.setOnClickListener {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }

        view.setOnLongClickListener {
            val intent = TaskbarSettingsActivity.createIntent(context)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }

        view.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchedView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(80L)
                        .start()
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    touchedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120L)
                        .start()
                }
            }
            false
        }
    }

    private fun applyIconSize(view: ImageView) {
        val sizePx = currentIconSizePx ?: return
        val params = view.layoutParams as? LayoutParams ?: return
        params.width = sizePx
        params.height = sizePx
        view.layoutParams = params
    }
}
