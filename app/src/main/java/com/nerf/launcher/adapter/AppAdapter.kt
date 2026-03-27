package com.nerf.launcher.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
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

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.appName.text = app.appName
            iconProvider.loadIconInto(app.packageName, binding.appIcon)
            binding.root.setOnClickListener {
                onAppClicked(app)
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

    init {
        // Observe icon pack changes to refresh icons across all items.
        ConfigRepository.get().config.observe(lifecycleOwner) { config ->
            // Clear icon pack cache so new icons are loaded on next bind.
            iconProvider.evictCache()
            // Notify adapter to rebind all visible items (icon pack change affects all items).
            // Using notifyItemRangeChanged is more efficient than notifyDataSetChanged.
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