package com.nerf.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.databinding.ItemSettingBinding
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.SettingChange
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository

/**
 * Adapter for the Settings RecyclerView.
 * Handles different setting types via view visibility toggles.
 */
class SettingsAdapter(
    private val items: List<SettingItem>,
    private val onSettingChanged: (SettingChange) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {
    private val themePayload = Any()
    private var currentConfig: AppConfig? = null
    private val settingIndexByType: Map<SettingsType, Int> =
        items.mapIndexed { index, item -> item.type to index }.toMap()

    inner class ViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingItem) {
            val theme = ThemeManager.resolveActiveTheme(
                context = binding.root.context,
                themeName = currentConfig?.themeName,
                glowIntensity = currentConfig?.glowIntensity
            )
            binding.title.text = item.title
            binding.title.setTextColor(theme.hudInfoColor)
            binding.animationLabel.setTextColor(theme.hudSuccessColor)
            when (item) {
                is SettingItem.Theme -> {
                    binding.themeContainer.visibility = View.VISIBLE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val spinner = binding.themeSpinner
                    val adapter = ArrayAdapter(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        item.options
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    val current = currentConfig?.themeName ?: ThemeRepository.defaultThemeName
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = item.options.getOrNull(pos) ?: return
                            if (selected != currentConfig?.themeName) {
                                onSettingChanged(SettingChange.Theme(selected))
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
                }

                is SettingItem.IconPack -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.VISIBLE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val spinner = binding.iconPackSpinner
                    val adapter = ArrayAdapter(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        item.options
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    val current = currentConfig?.iconPack
                    val currentIndex = adapter.getPosition(current)
                    if (currentIndex >= 0) {
                        spinner.setSelection(currentIndex)
                    }
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = item.options.getOrNull(pos) ?: return
                            if (selected != currentConfig?.iconPack) {
                                onSettingChanged(SettingChange.IconPack(selected))
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                    }
                }

                is SettingItem.GlowIntensity -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.VISIBLE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.GONE

                    val seekBar = binding.glowSeekBar
                    seekBar.setOnSeekBarChangeListener(null)
                    val progress = ((currentConfig?.glowIntensity ?: item.initialValue) * 100).toInt()
                    seekBar.progress = progress
                    binding.glowValue.text = binding.root.context.getString(
                        com.nerf.launcher.R.string.settings_glow_percent,
                        progress
                    )
                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                binding.glowValue.text = binding.root.context.getString(
                                    com.nerf.launcher.R.string.settings_glow_percent,
                                    progress
                                )
                                val value = progress / 100f
                                if (value != currentConfig?.glowIntensity) {
                                    onSettingChanged(SettingChange.GlowIntensity(value))
                                }
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    })
                }

                is SettingItem.AnimationSpeed -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.VISIBLE
                    binding.spinnerContainer.visibility = View.GONE

                    val switchCompat = binding.animationSwitch
                    switchCompat.setOnCheckedChangeListener(null)
                    switchCompat.isChecked = currentConfig?.animationSpeedEnabled ?: item.initialValue
                    switchCompat.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != currentConfig?.animationSpeedEnabled) {
                            onSettingChanged(SettingChange.AnimationSpeed(isChecked))
                        }
                    }
                }

                is SettingItem.GridSize -> {
                    binding.themeContainer.visibility = View.GONE
                    binding.iconPackContainer.visibility = View.GONE
                    binding.sliderContainer.visibility = View.GONE
                    binding.switchContainer.visibility = View.GONE
                    binding.spinnerContainer.visibility = View.VISIBLE

                    val spinner = binding.gridSizeSpinner
                    val options = listOf(2, 3, 4, 5, 6)
                    val adapter = ArrayAdapter(
                        binderContext,
                        android.R.layout.simple_spinner_item,
                        options
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                    val current = currentConfig?.gridSize ?: item.initialValue
                    spinner.setSelection(adapter.getPosition(current))
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            val selected = options.getOrNull(pos) ?: return
                            if (selected != currentConfig?.gridSize) {
                                onSettingChanged(SettingChange.GridSize(selected))
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateFromConfig(config: AppConfig) {
        val previous = currentConfig
        currentConfig = config
        if (previous == null) {
            notifyItemRangeChanged(0, itemCount, themePayload)
            return
        }

        if (previous.themeName != config.themeName || previous.glowIntensity != config.glowIntensity) {
            notifyItemRangeChanged(0, itemCount, themePayload)
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
