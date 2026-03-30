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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
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
        applyShellBackground(1f)
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

    fun updateIcons(packageNames: List<String>) {
        val renderablePackages = packageNames
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

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
        applyShellBackground(alpha)
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
            setImageResource(R.drawable.ic_module)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(10, 8, 10, 8)
            background = ContextCompat.getDrawable(context, R.drawable.dock_tile_background)
            contentDescription = context.getString(R.string.app_icon)
            currentIconTint?.let { setColorFilter(it) }
        }
    }

    private fun bindIconView(view: ImageView, packageName: String) {
        iconProvider?.loadIconInto(packageName, view)
            ?: view.setImageResource(R.drawable.ic_module)

        currentIconTint?.let { view.setColorFilter(it) }

        view.setOnClickListener {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }

        view.setOnLongClickListener {
            openTaskbarCustomization()
            true
        }

        view.setOnTouchListener { touchedView, event ->
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

    private fun applyIconSize(view: ImageView) {
        val sizePx = currentIconSizePx ?: return
        val params = view.layoutParams as? LayoutParams ?: return
        params.width = sizePx
        params.height = sizePx
        view.layoutParams = params
    }

    private fun applyShellBackground(alpha: Float) {
        background = ContextCompat.getDrawable(context, R.drawable.hud_frame_panel)?.mutate()?.apply {
            this.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        }
    }

    private fun openTaskbarCustomization() {
        val intent = TaskbarSettingsActivity.createIntent(context)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
