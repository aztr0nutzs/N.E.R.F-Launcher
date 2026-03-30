package com.nerf.launcher.adapter

import android.view.MotionEvent
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemAppBinding
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconProvider

/**
 * Adapter that binds a list of {@link AppInfo} to a RecyclerView using ViewBinding.
 * Uses ListAdapter with DiffUtil for efficient updates and retrieves icons via IconProvider.
 * Observes icon pack changes to refresh icons without full dataset reload.
 */
class AppAdapter(
    private val iconProvider: IconProvider,
    private val onAppClicked: (AppInfo) -> Unit,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(DIFF_CALLBACK) {
    private var observedIconPack: String? = null

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.root.scaleX = 1f
            binding.root.scaleY = 1f
            binding.root.alpha = 1f
            binding.appName.text = app.appName
            iconProvider.loadIconInto(app.packageName, binding.appIcon)
            binding.root.setOnClickListener {
                onAppClicked(app)
            }
            binding.root.setOnTouchListener { touchedView, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> animatePress(touchedView, pressed = true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animatePress(touchedView, pressed = false)
                }
                false
            }
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

    override fun onViewRecycled(holder: AppViewHolder) {
        holder.binding.root.animate().cancel()
        holder.binding.root.scaleX = 1f
        holder.binding.root.scaleY = 1f
        holder.binding.root.alpha = 1f
        super.onViewRecycled(holder)
    }

    init {
        // Observe configuration and react only when icon pack changes.
        ConfigRepository.get().config.observe(lifecycleOwner) {
            val previousPack = observedIconPack
            observedIconPack = it.iconPack
            if (previousPack == null || previousPack == it.iconPack) {
                return@observe
            }
            iconProvider.evictCache()
            notifyItemRangeChanged(0, itemCount)
        }
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
