package com.nerf.launcher.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
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
 * Main launcher screen – shows a scrollable grid of apps and launches the selected one.
 * Includes RecyclerView performance optimizations: fixed size, view caching, and null‑safe handling.
 * Also includes a taskbar at the bottom and status bar customization.
 * UI updates instantly via LiveData observers on ConfigRepository.
 */
class MainActivity : AppCompatActivity(), LifecycleOwner {

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

        // Set lifecycle owner for TaskbarView
        binding.taskbarView.setLifecycleOwner(this)

        // Load apps
        viewModel.loadApps()

        // Observe configuration changes (single source of truth)
        setupConfigObservers()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            iconProvider = iconProvider,
            onAppClicked = { app -> AppUtils.launchApp(this, app) },
            lifecycleOwner = this
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
        ConfigRepository.get().config.observe(this) { config ->
            // Apply theme (including glow) – ThemeManager reads from ConfigRepository internally
            ThemeManager.applyTheme(this)

            // Update icon pack – clear cache so new icons are loaded
            iconProvider.evictCache()
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            // Update grid span
            val layoutManager = binding.recyclerView.layoutManager as GridLayoutManager
            layoutManager.spanCount = config.gridSize

            // Update status bar
            applyStatusBarTheme(config)

            // Update taskbar icons (may have changed due to pinned apps or theme)
            updateTaskbarIcons()
        }
    }

    /** Update the taskbar icons based on the pinned apps list from ConfigRepository. */
    private fun updateTaskbarIcons() {
        val pinnedApps = TaskbarController.getPinnedApps(this)
        // If no pinned apps are set, use the first 4 apps from the list as default
        val appsToShow = if (pinnedApps.isEmpty()) {
            viewModel.apps.value?.take(4)?.map { it.packageName } ?: emptyList()
        } else {
            pinnedApps
        }
        binding.taskbarView.updateIcons(appsToShow)
    }

    /** Apply status bar theme based on the current theme from ConfigRepository. */
    private fun applyStatusBarTheme(config: AppConfig) {
        val primaryColor = ThemeRepository.byName(config.themeName)?.primary
            ?: ThemeRepository.CLASSIC_NERF.primary
        val isLightTheme = isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }

    /** Calculate whether a color is light (for determining icon tint). */
    private fun isColorLight(color: Int): Boolean {
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Do nothing – stay on launcher.
    }

    override fun onDestroy() {
        super.onDestroy()
        hudController.release()
        // Reset status bar to avoid leaking the customization
        StatusBarManager.resetStatusBar(this)
    }
}