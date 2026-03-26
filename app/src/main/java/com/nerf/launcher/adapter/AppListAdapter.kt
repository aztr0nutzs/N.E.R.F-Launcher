package com.nerf.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemAppBinding
import com.nerf.launcher.model.AppInfo

class AppListAdapter(
    private var items: List<AppInfo>,
    private val onAppClicked: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = items[position]
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appName.text = app.appName
        holder.binding.root.setOnClickListener {
            onAppClicked(app)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<AppInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}