package com.nerf.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivitySettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.PreferencesManager
import com.nerf.launcher.util.SettingItem
import com.nerf.launcher.util.SettingsType
import com.nerf.launcher.util.ThemeRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: SettingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_surface_title)

        binding.openTaskbarSettingsButton.setOnClickListener {
            startActivity(TaskbarSettingsActivity.createIntent(this))
        }

        // Build settings list
        val settings = buildSettingsList()
        adapter = SettingsAdapter(settings) { setting ->
            handleSettingChange(setting)
        }
        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * Build the list of setting items to display.
     */
    private fun buildSettingsList(): List<SettingItem> {
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
            PreferencesManager.getGlowIntensity(this)
        ))
        // Animation speed toggle
        list.add(SettingItem(
            SettingsType.ANIMATION_SPEED,
            getString(R.string.settings_animation_speed),
            PreferencesManager.isAnimationSpeedEnabled(this)
        ))
        // Grid size selector
        list.add(SettingItem(
            SettingsType.GRID_SIZE,
            getString(R.string.settings_grid_size),
            PreferencesManager.getGridSize(this)
        ))
        return list
    }

    /** React to a setting change from the adapter. */
    private fun handleSettingChange(setting: SettingItem) {
        val repo = ConfigRepository.get()
        when (setting.type) {
            SettingsType.THEME -> {
                val themeName = setting.payload as String
                PreferencesManager.saveSelectedTheme(this, themeName)
                repo.updateTheme(themeName)
            }
            SettingsType.ICON_PACK -> {
                val packName = setting.payload as String
                PreferencesManager.saveIconPack(this, packName)
                repo.updateIconPack(packName)
            }
            SettingsType.GLOW_INTENSITY -> {
                val intensity = setting.payload as Float
                PreferencesManager.saveGlowIntensity(this, intensity)
                repo.updateGlowIntensity(intensity)
            }
            SettingsType.ANIMATION_SPEED -> {
                val enabled = setting.payload as Boolean
                PreferencesManager.saveAnimationSpeed(this, enabled)
                repo.updateAnimationSpeed(enabled)
            }
            SettingsType.GRID_SIZE -> {
                val size = setting.payload as Int
                PreferencesManager.saveGridSize(this, size)
                repo.updateGridSize(size)
            }
        }
    }

    // Support for up navigation
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
