package com.nerf.launcher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.nerf.launcher.adapter.AppAdapter
import com.nerf.launcher.databinding.ActivityMainBinding
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.AppUtils
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconCache
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.PreferencesManager
import com.nerf.launcher.util.StatusBarManager
import com.nerf.launcher.util.TaskbarSettings
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository
import com.nerf.launcher.viewmodel.LauncherViewModel
import java.util.Locale

/**
 * Main launcher screen – app grid + HUD + taskbar.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var adapter: AppAdapter
    private lateinit var iconProvider: IconProvider
    private lateinit var hudController: HudController
    private var allApps: List<AppInfo> = emptyList()
    private val themeNames by lazy { ThemeRepository.all.map { it.name } }
    private val iconPackNames by lazy { IconPackManager.getAvailablePacks() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iconProvider = IconProvider(applicationContext, IconCache(50))

        setupRecyclerView()
        setupDrawerSearch()
        observeViewModel()

        hudController = HudController(this, binding.hudRoot, this)
        binding.taskbarView.setIconProvider(iconProvider)
        binding.taskbarView.setLifecycleOwner(this)

        binding.openSettingsTile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.reloadTile.setOnClickListener {
            viewModel.loadApps()
        }

        setupQuickControls()
        setupConfigObservers()
        viewModel.loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(iconProvider, onAppClicked = { app ->
            AppUtils.launchApp(this, app)
        }, lifecycleOwner = this)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity,
                ConfigRepository.get().config.value?.gridSize ?: 2
            )
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = false
        }
    }

    private fun setupDrawerSearch() {
        binding.drawerSearchInput.doAfterTextChanged {
            applyDrawerFilter(it?.toString().orEmpty())
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            allApps = apps
            applyDrawerFilter(binding.drawerSearchInput.text?.toString().orEmpty())
            binding.moduleAppCount.text = getString(com.nerf.launcher.R.string.hud_apps_count, apps.size)
            binding.appsLoadBar.progress = ((apps.size / 48f) * 100f).toInt().coerceIn(10, 100)
            updateTaskbarIcons()
        }
    }

    private fun applyDrawerFilter(query: String) {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val filtered = if (normalizedQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        app.packageName.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
        adapter.submitList(filtered)
        binding.drawerResultCount.text = getString(com.nerf.launcher.R.string.drawer_result_count, filtered.size)
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(this) { config ->
            ThemeManager.applyTheme(this)

            iconProvider.evictCache()
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            (binding.recyclerView.layoutManager as? GridLayoutManager)?.spanCount = config.gridSize.coerceAtLeast(2)

            binding.moduleGridValue.text = getString(com.nerf.launcher.R.string.hud_grid_value, config.gridSize)
            binding.modulePinnedValue.text = getString(
                com.nerf.launcher.R.string.hud_dock_value,
                config.taskbarSettings.pinnedApps.size
            )

            applyStatusBarTheme(config)
            updateTaskbarIcons()
            bindQuickControls(config)
        }
    }

    private fun setupQuickControls() {
        binding.quickThemeBtn.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val currentIndex = themeNames.indexOf(current.themeName).takeIf { it >= 0 } ?: 0
            val nextTheme = themeNames[(currentIndex + 1) % themeNames.size]
            PreferencesManager.saveSelectedTheme(this, nextTheme)
            ConfigRepository.get().updateTheme(nextTheme)
        }

        binding.quickIconPackBtn.setOnClickListener {
            if (iconPackNames.isEmpty()) return@setOnClickListener
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val currentIndex = iconPackNames.indexOf(current.iconPack).takeIf { it >= 0 } ?: 0
            val nextPack = iconPackNames[(currentIndex + 1) % iconPackNames.size]
            PreferencesManager.saveIconPack(this, nextPack)
            ConfigRepository.get().updateIconPack(nextPack)
        }

        binding.quickAnimationBtn.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.animationSpeedEnabled
            PreferencesManager.saveAnimationSpeed(this, toggled)
            ConfigRepository.get().updateAnimationSpeed(toggled)
        }

        binding.quickTaskbarBtn.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.taskbarSettings.enabled
            val settings = current.taskbarSettings.copy(enabled = toggled)
            saveTaskbarSettings(settings)
            ConfigRepository.get().updateTaskbarSettings(settings)
        }

        binding.quickGlowSeekbar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (!fromUser) return
                val value = progress / 100f
                PreferencesManager.saveGlowIntensity(this@MainActivity, value)
                ConfigRepository.get().updateGlowIntensity(value)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })

        binding.quickGridSeekbar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (!fromUser) return
                val gridSize = progress + 2
                PreferencesManager.saveGridSize(this@MainActivity, gridSize)
                ConfigRepository.get().updateGridSize(gridSize)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })
    }

    private fun bindQuickControls(config: AppConfig) {
        binding.quickThemeBtn.text = getString(com.nerf.launcher.R.string.quick_theme_state, config.themeName)
        binding.quickIconPackBtn.text = getString(com.nerf.launcher.R.string.quick_icon_state, config.iconPack)
        val animStateRes = if (config.animationSpeedEnabled) {
            com.nerf.launcher.R.string.quick_animation_on
        } else {
            com.nerf.launcher.R.string.quick_animation_off
        }
        binding.quickAnimationBtn.text = getString(
            com.nerf.launcher.R.string.quick_animation_state,
            getString(animStateRes)
        )
        val taskbarStateRes = if (config.taskbarSettings.enabled) {
            com.nerf.launcher.R.string.quick_taskbar_on
        } else {
            com.nerf.launcher.R.string.quick_taskbar_off
        }
        binding.quickTaskbarBtn.text = getString(
            com.nerf.launcher.R.string.quick_taskbar_state,
            getString(taskbarStateRes)
        )

        val glowPercent = (config.glowIntensity * 100).toInt()
        binding.quickGlowValue.text = getString(com.nerf.launcher.R.string.quick_glow_percent, glowPercent)
        if (binding.quickGlowSeekbar.progress != glowPercent) {
            binding.quickGlowSeekbar.progress = glowPercent
        }

        binding.quickGridValue.text = getString(com.nerf.launcher.R.string.quick_grid_state, config.gridSize)
        val sliderProgress = (config.gridSize - 2).coerceIn(0, 4)
        if (binding.quickGridSeekbar.progress != sliderProgress) {
            binding.quickGridSeekbar.progress = sliderProgress
        }
    }

    private fun saveTaskbarSettings(settings: TaskbarSettings) {
        PreferencesManager.saveTaskbarHeight(this, settings.height)
        PreferencesManager.saveTaskbarIconSize(this, settings.iconSize)
        PreferencesManager.saveTaskbarBackgroundStyle(this, settings.backgroundStyle)
        PreferencesManager.saveTaskbarTransparency(this, settings.transparency)
        PreferencesManager.saveTaskbarEnabled(this, settings.enabled)
        PreferencesManager.savePinnedApps(this, settings.pinnedApps)
    }

    private fun updateTaskbarIcons() {
        val pinnedApps = TaskbarController.getPinnedApps(this)
        val fallback = viewModel.apps.value?.take(4)?.map { it.packageName }.orEmpty()
        binding.taskbarView.updateIcons(if (pinnedApps.isEmpty()) fallback else pinnedApps)
    }

    private fun applyStatusBarTheme(config: AppConfig) {
        val primaryColor = (ThemeRepository.byName(config.themeName)
            ?: ThemeRepository.CLASSIC_NERF).primary
        val isLightTheme = com.nerf.launcher.util.ColorUtils.isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }

    override fun onBackPressed() {
        // Do nothing – stay on launcher.
    }

    override fun onDestroy() {
        super.onDestroy()
        hudController.release()
        StatusBarManager.resetStatusBar(this)
    }
}
