package com.nerf.launcher.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.nerf.launcher.adapter.AppAdapter
import com.nerf.launcher.databinding.ActivityMainBinding
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.AppUtils
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconCache
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.StatusBarManager
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository
import com.nerf.launcher.viewmodel.LauncherViewModel

/**
 * Main launcher screen - shows a scrollable grid of apps and launches the selected one.
 * Includes RecyclerView performance optimizations: fixed size, view caching, and null-safe handling.
 * Also includes a taskbar at the bottom and status bar customization.
 * UI updates instantly via LiveData observers on ConfigRepository.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var adapter: AppAdapter
    private lateinit var iconProvider: IconProvider
    private lateinit var hudController: HudController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize icon cache and provider
        val iconCache = IconCache(50)
        iconProvider = IconProvider(applicationContext, iconCache)

        // Set up RecyclerView
        setupRecyclerView()

        // Observe apps from ViewModel
        observeViewModel()

        // Initialize HUD controller (passing this activity as lifecycle owner)
        hudController = HudController(this, binding.hudRoot, this)

        // Set lifecycle owner and icon provider for TaskbarView
        binding.taskbarView.setIconProvider(iconProvider)
        binding.taskbarView.setLifecycleOwner(this)

        // Load apps
        viewModel.loadApps()

        // Observe configuration changes (single source of truth)
        setupConfigObservers()

        // Safe navigation trigger to open SettingsActivity (since no XML edits are allowed)
        val openSettings = { _: android.view.View ->
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            true
        }
        binding.taskbarView.setOnLongClickListener(openSettings)
        binding.root.setOnLongClickListener(openSettings)
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            iconProvider = iconProvider,
            onAppClicked = { app -> AppUtils.launchApp(this, app) }
        )
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity,
                ConfigRepository.get().config.value?.gridSize ?: 4
            )
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            setDrawingCacheEnabled(false)
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            adapter.submitList(apps)
        }
    }

    private fun setupConfigObservers() {
        var initialThemeName: String? = null
        var initialIconPack: String? = null

        ConfigRepository.get().config.observe(this) { config ->
            if (initialThemeName == null) {
                initialThemeName = config.themeName
                initialIconPack = config.iconPack
            } else if (initialThemeName != config.themeName || initialIconPack != config.iconPack) {
                recreate()
                return@observe
            }

            // Apply theme (including glow) - ThemeManager reads from ConfigRepository internally
            ThemeManager.applyTheme(this)

            // Update grid span
            val layoutManager = binding.recyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = config.gridSize

            // Update status bar
            applyStatusBarTheme(config)
        }
    }

    /** Apply status bar theme based on the current theme from ConfigRepository. */
    private fun applyStatusBarTheme(config: AppConfig) {
        val primaryColor = ThemeRepository.byName(config.themeName)?.primary
            ?: ThemeRepository.CLASSIC_NERF.primary
        val isLightTheme = com.nerf.launcher.util.ColorUtils.isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }



    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Do nothing - stay on launcher.
    }

    override fun onDestroy() {
        super.onDestroy()
        hudController.release()
        // Reset status bar to avoid leaking the customization
        StatusBarManager.resetStatusBar(this)
    }
}
