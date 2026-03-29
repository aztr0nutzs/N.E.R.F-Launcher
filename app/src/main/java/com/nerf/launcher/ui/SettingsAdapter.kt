package com.nerf.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemSettingBinding
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.PreferencesManager
import com.nerf.launcher.util.ThemeRepository

/**
 * Adapter for the Settings RecyclerView.
 * Handles different setting types via view visibility toggles.
 */
class SettingsAdapter(
    private val items: List<SettingItem>,
    private val onSettingChanged: (SettingItem) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingItem) {
            binding.title.text = item.title
            when (item.type) {
                SettingsType.THEME -> {
                    binding.themeContainer.visibility = View.VISIBLE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val spinner = binding.themeSpinner
                    val themes = (item.payload as String).split(", ")
                    val adapter = ArrayAdapter<String>(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        themes
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    // Set current selection
                    val current = PreferencesManager.getSelectedTheme(binderContext) ?: ThemeRepository.CLASSIC_NERF.name
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as String
                            onSettingChanged(SettingItem(SettingsType.THEME, item.title, selected))
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
                SettingsType.ICON_PACK -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.VISIBLE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val spinner = binding.iconPackSpinner
                    val packs = (item.payload as String).split(", ")
                    val adapter = ArrayAdapter<String>(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        packs
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    // Set current selection
                    val current = IconPackManager.getCurrentPack(binderContext)
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as String
                            onSettingChanged(SettingItem(SettingsType.ICON_PACK, item.title, selected))
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
                SettingsType.GLOW_INTENSITY -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.VISIBLE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val seekBar = binding.glowSeekBar
                    val progress = (item.payload as Float * 100).toInt()
                    seekBar.progress = progress
                    seekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val value = progress / 100f
                                onSettingChanged(SettingItem(SettingsType.GLOW_INTENSITY, item.title, value))
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }
                SettingsType.ANIMATION_SPEED -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.VISIBLE
                    binding.spinnerContainer.visibility = View.GONE

                    val switchCompat = binding.animationSwitch
                    switchCompat.isChecked = item.payload as Boolean
                    switchCompat.setOnCheckedChangeListener { _, isChecked ->
                        onSettingChanged(SettingItem(SettingsType.ANIMATION_SPEED, item.title, isChecked))
                    }
                }
                SettingsType.GRID_SIZE -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.VISIBLE

                    val spinner = binding.gridSizeSpinner
                    val options = listOf(2, 3, 4, 5, 6)
                    val adapter = ArrayAdapter<Int>(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        options
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    // Set current selection
                    val current = PreferencesManager.getGridSize(binderContext)
                    spinner.setSelection(adapter.getPosition(current))
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as Int
                            onSettingChanged(SettingItem(SettingsType.GRID_SIZE, item.title, selected))
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            }
        }

        private val binderContext: android.content.Context
            get() = itemView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
