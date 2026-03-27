package com.nerf.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivitySettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.ThemeRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Build settings list
        val settings = buildSettingsList()
        settingsAdapter = SettingsAdapter(settings) { setting ->
            handleSettingChange(setting)
        }
        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = settingsAdapter
        }
    }

    /**
     * Build the list of setting items to display.
     */
    private fun buildSettingsList(): List<SettingItem> {
        val config = ConfigRepository.get().config.value
        val list = mutableListOf<SettingItem>()
        // Theme selector
        list.add(SettingItem(
            SettingsType.THEME,
            getString(R.string.settings_theme),
            ThemeRepository.all.joinToString { it.name }
        ))
        // Icon pack selector
        list.add(SettingItem(
            SettingsType.ICON_PACK,
            getString(R.string.settings_icon_pack),
            IconPackManager.getAvailablePacks().joinToString { it }
        ))
        // Glow intensity slider
        list.add(SettingItem(
            SettingsType.GLOW_INTENSITY,
            getString(R.string.settings_glow_intensity),
            config?.glowIntensity ?: 0.0f
        ))
        // Animation speed toggle
        list.add(SettingItem(
            SettingsType.ANIMATION_SPEED,
            getString(R.string.settings_animation_speed),
            config?.animationSpeedEnabled ?: false
        ))
        // Grid size selector
        list.add(SettingItem(
            SettingsType.GRID_SIZE,
            getString(R.string.settings_grid_size),
            config?.gridSize ?: 4
        ))
        return list
    }

    /** React to a setting change from the adapter. */
    private fun handleSettingChange(setting: SettingItem) {
        val repo = ConfigRepository.get()
        when (setting.type) {
            SettingsType.THEME -> {
                val themeName = setting.payload as String
                repo.updateTheme(themeName)
            }
            SettingsType.ICON_PACK -> {
                val packName = setting.payload as String
                repo.updateIconPack(packName)
            }
            SettingsType.GLOW_INTENSITY -> {
                val intensity = setting.payload as Float
                repo.updateGlowIntensity(intensity)
            }
            SettingsType.ANIMATION_SPEED -> {
                val enabled = setting.payload as Boolean
                repo.updateAnimationSpeed(enabled)
            }
            SettingsType.GRID_SIZE -> {
                val size = setting.payload as Int
                repo.updateGridSize(size)
            }
        }
    }

    // Support for up navigation
    override fun onSupportNavigateUp(): Boolean {
        @Suppress("OVERRIDE_DEPRECATION")
        onBackPressed()
        return true
    }
}
