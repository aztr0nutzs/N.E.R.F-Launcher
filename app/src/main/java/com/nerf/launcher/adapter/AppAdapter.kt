package com.nerf.launcher.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemAppBinding
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.ThemeManager

/**
 * Adapter that binds a list of {@link AppInfo} to a RecyclerView using ViewBinding.
 * Uses ListAdapter with DiffUtil for efficient updates and retrieves icons via IconProvider.
 * Observes icon pack changes to refresh icons without full dataset reload.
 */
class AppAdapter(
    private val iconProvider: IconProvider,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongPressed: (View, AppInfo) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(DIFF_CALLBACK) {
    private val iconOnlyPayload = Any()

    init {
        setHasStableIds(true)
    }

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.root.scaleX = 1f
            binding.root.scaleY = 1f
            binding.root.alpha = 1f
            val theme = ThemeManager.resolveActiveTheme(binding.root.context)
            binding.appName.text = app.appName
            binding.appName.setTextColor(theme.hudAppLabelColor)
            binding.iconSocket.background = ThemeManager.createAppIconSocketBackground(binding.root.context, theme)
            iconProvider.loadIconInto(app.packageName, binding.appIcon)
            binding.root.setOnClickListener {
                onAppClicked(app)
            }
            binding.root.setOnLongClickListener {
                onAppLongPressed(binding.root, app)
                true
            }
            binding.root.setOnTouchListener { touchedView, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> animatePress(touchedView, pressed = true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePress(touchedView, pressed = false)
                }
                false
            }
        }

        fun bindIconOnly(app: AppInfo) {
            iconProvider.loadIconInto(app.packageName, binding.appIcon)
        }

        private fun animatePress(view: View, pressed: Boolean) {
            view.animate().cancel()
            if (pressed) {
                view.animate()
                    .scaleX(0.972f)
                    .scaleY(0.972f)
                    .alpha(0.95f)
                    .setDuration(70L)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            } else {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(150L)
                    .setInterpolator(LinearOutSlowInInterpolator())
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = getItem(position) ?: return
        holder.bind(app)
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return "${item.packageName}/${item.className}".hashCode().toLong()
    }

    override fun onBindViewHolder(
        holder: AppViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val app = getItem(position) ?: return
        if (payloads.contains(iconOnlyPayload)) {
            holder.bindIconOnly(app)
            return
        }
        holder.bind(app)
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        holder.binding.root.animate().cancel()
        holder.binding.root.scaleX = 1f
        holder.binding.root.scaleY = 1f
        holder.binding.root.alpha = 1f
        super.onViewRecycled(holder)
    }

    fun refreshIcons() {
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, iconOnlyPayload)
        }
    }

    fun refreshIconsInRange(start: Int, endInclusive: Int) {
        if (itemCount == 0) return
        val safeStart = start.coerceAtLeast(0)
        val safeEnd = endInclusive.coerceAtMost(itemCount - 1)
        if (safeStart > safeEnd) return
        notifyItemRangeChanged(safeStart, safeEnd - safeStart + 1, iconOnlyPayload)
    }

    fun refreshTheme() {
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun refreshThemeInRange(start: Int, endInclusive: Int) {
        if (itemCount == 0) return
        val safeStart = start.coerceAtLeast(0)
        val safeEnd = endInclusive.coerceAtMost(itemCount - 1)
        if (safeStart > safeEnd) return
        notifyItemRangeChanged(safeStart, safeEnd - safeStart + 1)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return oldItem.packageName == newItem.packageName &&
                        oldItem.className == newItem.className
            }

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
