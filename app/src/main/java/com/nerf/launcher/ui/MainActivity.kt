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
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.StatusBarManager
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
        }
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
