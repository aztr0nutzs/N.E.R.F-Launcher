package com.nerf.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemSettingBinding
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.ThemeRepository

/**
 * Adapter for the Settings RecyclerView.
 * Handles different setting types via view visibility toggles.
 */
class SettingsAdapter(
    private val items: List<SettingItem>,
    private val onSettingChanged: (SettingItem) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {
    private var currentConfig: AppConfig? = ConfigRepository.get().config.value
    private val settingIndexByType: Map<SettingsType, Int> =
        items.mapIndexed { index, item -> item.type to index }.toMap()

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
                    val current = currentConfig?.themeName
                        ?: ThemeRepository.defaultThemeName
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as String
                            if (selected != currentConfig?.themeName) {
                                onSettingChanged(SettingItem(SettingsType.THEME, item.title, selected))
                            }
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
                    val current = currentConfig?.iconPack
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as String
                            if (selected != currentConfig?.iconPack) {
                                onSettingChanged(SettingItem(SettingsType.ICON_PACK, item.title, selected))
                            }
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
                    seekBar.setOnSeekBarChangeListener(null)
                    val progress = ((currentConfig?.glowIntensity ?: item.payload as Float) * 100).toInt()
                    seekBar.progress = progress
                    seekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val value = progress / 100f
                                if (value != currentConfig?.glowIntensity) {
                                    onSettingChanged(SettingItem(SettingsType.GLOW_INTENSITY, item.title, value))
                                }
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
                    switchCompat.setOnCheckedChangeListener(null)
                    switchCompat.isChecked = currentConfig?.animationSpeedEnabled ?: item.payload as Boolean
                    switchCompat.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != currentConfig?.animationSpeedEnabled) {
                            onSettingChanged(SettingItem(SettingsType.ANIMATION_SPEED, item.title, isChecked))
                        }
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
                    val current = currentConfig?.gridSize ?: 4
                    spinner.setSelection(adapter.getPosition(current))
                    spinner.onItemSelectedListener = object :
                        AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = parent?.getItemAtPosition(pos) as Int
                            if (selected != currentConfig?.gridSize) {
                                onSettingChanged(SettingItem(SettingsType.GRID_SIZE, item.title, selected))
                            }
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

    fun updateFromConfig(config: AppConfig) {
        val previous = currentConfig
        currentConfig = config
        if (previous == null) {
            notifyDataSetChanged()
            return
        }

        notifyIfChanged(previous.themeName != config.themeName, SettingsType.THEME)
        notifyIfChanged(previous.iconPack != config.iconPack, SettingsType.ICON_PACK)
        notifyIfChanged(previous.glowIntensity != config.glowIntensity, SettingsType.GLOW_INTENSITY)
        notifyIfChanged(
            previous.animationSpeedEnabled != config.animationSpeedEnabled,
            SettingsType.ANIMATION_SPEED
        )
        notifyIfChanged(previous.gridSize != config.gridSize, SettingsType.GRID_SIZE)
    }

    private fun notifyIfChanged(changed: Boolean, type: SettingsType) {
        if (!changed) return
        val index = settingIndexByType[type] ?: return
        notifyItemChanged(index)
    }
}
