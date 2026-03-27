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
 * Main launcher screen – app grid + HUD + taskbar.
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

        iconProvider = IconProvider(applicationContext, IconCache(50))

        setupRecyclerView()
        observeViewModel()

        hudController = HudController(this, binding.hudRoot, this)
        binding.taskbarView.setIconProvider(iconProvider)
        binding.taskbarView.setLifecycleOwner(this)

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
                ConfigRepository.get().config.value?.gridSize ?: 4
            )
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(this) { apps ->
            adapter.submitList(apps)
            updateTaskbarIcons()
        }
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(this) { config ->
            ThemeManager.applyTheme(this)

            iconProvider.evictCache()
            adapter.notifyItemRangeChanged(0, adapter.itemCount)

            (binding.recyclerView.layoutManager as? GridLayoutManager)?.spanCount = config.gridSize

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
