package com.nerf.launcher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
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
import android.view.animation.LinearInterpolator
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
    private var filteredAppCount: Int = 0
    private var batteryPercent: Int? = null
    private var isCharging: Boolean = false
    private val themeNames by lazy { ThemeRepository.all.map { it.name } }
    private val iconPackNames by lazy { IconPackManager.getAvailablePacks() }
    private val moduleRefreshHandler = Handler(Looper.getMainLooper())
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private var scanlineSweepAnimator: ObjectAnimator? = null
    private var scanlineOpacityAnimator: ValueAnimator? = null

    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            batteryPercent = if (level >= 0 && scale > 0) {
                (level * 100 / scale).coerceIn(0, 100)
            } else {
                null
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            updateSystemModules()
        }
    }

    private val moduleRefreshTick = object : Runnable {
        override fun run() {
            updateSystemModules()
            moduleRefreshHandler.postDelayed(this, 60_000L)
        }
    }

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
        setupPressFeedbacks()

        setupQuickControls()
        setupConfigObservers()
        setupSystemModules()
        setupSurfaceTransitions()
        setupScanlineSweep()
        viewModel.loadApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(iconProvider, onAppClicked = { app ->
            binding.drawerSearchInput.clearFocus()
            hideDrawerKeyboard()
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
        filteredAppCount = filtered.size
        binding.drawerResultCount.text = getString(com.nerf.launcher.R.string.drawer_result_count, filtered.size)
        updateSystemModules()
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
            updateSystemModules(config)
        }
    }

    private fun setupSystemModules() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        moduleRefreshHandler.post(moduleRefreshTick)
        binding.moduleEnergyBar.segments = 18
        binding.moduleStorageBar.segments = 12

        binding.moduleEnergyCard.setOnClickListener { updateSystemModules() }
        binding.moduleStorageCard.setOnClickListener { updateSystemModules() }
        binding.moduleRuntimeCard.setOnClickListener { updateSystemModules() }
        binding.moduleStateCard.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.taskbarSettings.enabled
            val settings = current.taskbarSettings.copy(enabled = toggled)
            saveTaskbarSettings(settings)
            ConfigRepository.get().updateTaskbarSettings(settings)
            updateSystemModules(current.copy(taskbarSettings = settings))
        }
        binding.reactorCore.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val currentIndex = themeNames.indexOf(current.themeName).takeIf { it >= 0 } ?: 0
            val nextTheme = themeNames[(currentIndex + 1) % themeNames.size]
            PreferencesManager.saveSelectedTheme(this, nextTheme)
            ConfigRepository.get().updateTheme(nextTheme)
        }
    }

    private fun setupSurfaceTransitions() {
        listOf(binding.leftPanel, binding.corePanel, binding.rightPanel).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 16f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 56L) + 48L)
                .setDuration(310L)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }

        binding.drawerShell.alpha = 0f
        binding.drawerShell.translationY = 20f
        binding.drawerShell.translationX = 10f
        binding.drawerShell.animate()
            .alpha(1f)
            .translationY(0f)
            .translationX(0f)
            .setStartDelay(185L)
            .setDuration(330L)
            .setInterpolator(LinearOutSlowInInterpolator())
            .start()
    }

    private fun setupScanlineSweep() {
        binding.scanlineOverlay.alpha = 0.14f
        binding.scanlineOverlay.post {
            scanlineSweepAnimator?.cancel()
            scanlineOpacityAnimator?.cancel()
            scanlineSweepAnimator = ObjectAnimator.ofFloat(
                binding.scanlineOverlay,
                "translationY",
                -binding.scanlineOverlay.height.toFloat(),
                binding.scanlineOverlay.height.toFloat()
            ).apply {
                duration = 8_400L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) &&
                    powerManager?.isPowerSaveMode != true
                ) {
                    start()
                }
            }
            scanlineOpacityAnimator = ValueAnimator.ofFloat(0.11f, 0.17f).apply {
                duration = 4_800L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = FastOutLinearInInterpolator()
                addUpdateListener { animator ->
                    binding.scanlineOverlay.alpha = animator.animatedValue as Float
                }
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) &&
                    powerManager?.isPowerSaveMode != true
                ) {
                    start()
                }
            }
        }
    }

    private fun setupPressFeedbacks() {
        val pressableViews = listOf(
            binding.openSettingsTile,
            binding.reloadTile,
            binding.quickThemeBtn,
            binding.quickIconPackBtn,
            binding.quickAnimationBtn,
            binding.quickTaskbarBtn,
            binding.moduleEnergyCard,
            binding.moduleStorageCard,
            binding.moduleRuntimeCard,
            binding.moduleStateCard,
            binding.reactorCore
        )
        pressableViews.forEach { view ->
            view.applyPressFeedback()
        }
    }

    private fun View.applyPressFeedback(
        downScale: Float = 0.975f,
        downDuration: Long = 72L,
        upDuration: Long = 150L
    ) {
        setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchedView.animate().cancel()
                    touchedView.animate()
                        .scaleX(downScale)
                        .scaleY(downScale)
                        .alpha(0.94f)
                        .setDuration(downDuration)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touchedView.animate().cancel()
                    touchedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(upDuration)
                        .setInterpolator(LinearOutSlowInInterpolator())
                        .start()
                }
            }
            false
        }
    }

    private fun updateSystemModules(config: AppConfig? = ConfigRepository.get().config.value) {
        val energy = batteryPercent
        if (energy != null) {
            val batteryStateTextRes = if (isCharging) {
                com.nerf.launcher.R.string.modules_battery_charging
            } else {
                com.nerf.launcher.R.string.modules_battery_idle
            }
            binding.moduleEnergyValue.text = getString(
                com.nerf.launcher.R.string.modules_energy_percent,
                energy,
                getString(batteryStateTextRes)
            )
            binding.moduleEnergyBar.progress = energy
            binding.moduleEnergyBar.setActiveColor(
                if (energy < 20) getColor(com.nerf.launcher.R.color.nerf_hud_magenta)
                else getColor(com.nerf.launcher.R.color.nerf_hud_orange)
            )
        } else {
            binding.moduleEnergyValue.text = "--"
            binding.moduleEnergyBar.progress = 0
        }

        val storageStats = readStorageUsagePercent()
        if (storageStats != null) {
            binding.moduleStorageValue.text = getString(com.nerf.launcher.R.string.modules_storage_percent, storageStats)
            binding.moduleStorageBar.progress = storageStats
        } else {
            binding.moduleStorageValue.text = getString(com.nerf.launcher.R.string.modules_storage_unavailable)
            binding.moduleStorageBar.progress = 0
        }

        val uptimeDays = (SystemClock.elapsedRealtime() / (24 * 60 * 60 * 1000L)).toInt()
        val uptimeHours = ((SystemClock.elapsedRealtime() / (60 * 60 * 1000L)) % 24).toInt()
        binding.moduleRuntimeValue.text = getString(
            com.nerf.launcher.R.string.modules_runtime_value,
            uptimeDays,
            uptimeHours
        )

        val interactiveState = if (powerManager?.isInteractive == true) {
            getString(com.nerf.launcher.R.string.modules_state_active)
        } else {
            getString(com.nerf.launcher.R.string.modules_state_idle)
        }
        val powerMode = if (powerManager?.isPowerSaveMode == true) {
            getString(com.nerf.launcher.R.string.modules_state_eco)
        } else {
            getString(com.nerf.launcher.R.string.modules_state_nominal)
        }
        binding.moduleStateValue.text = getString(
            com.nerf.launcher.R.string.modules_state_value,
            interactiveState,
            powerMode
        )

        val appPopulation = if (allApps.isEmpty()) 0 else (filteredAppCount * 100 / allApps.size)
        val batteryScore = energy ?: 0
        val storageScore = storageStats?.let { 100 - it } ?: 0
        val taskbarScore = if (config?.taskbarSettings?.enabled == true) 100 else 70
        val reactorSync = ((appPopulation + batteryScore + storageScore + taskbarScore) / 4).coerceIn(0, 100)
        binding.moduleReactorValue.text = getString(com.nerf.launcher.R.string.modules_reactor_sync, reactorSync)
    }

    private fun readStorageUsagePercent(): Int? {
        val dataPath = Environment.getDataDirectory().absolutePath
        val stats = StatFs(dataPath)
        val totalBytes = stats.totalBytes
        val availableBytes = stats.availableBytes
        if (totalBytes <= 0L) return null
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
        return ((usedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
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
        binding.taskbarView.updateIcons(pinnedApps)
    }

    private fun applyStatusBarTheme(config: AppConfig) {
        val primaryColor = (ThemeRepository.byName(config.themeName)
            ?: ThemeRepository.CLASSIC_NERF).primary
        val isLightTheme = com.nerf.launcher.util.ColorUtils.isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }


    private fun hideDrawerKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(binding.drawerSearchInput.windowToken, 0)
    }

    override fun onBackPressed() {
        // Do nothing – stay on launcher.
    }

    override fun onResume() {
        super.onResume()
        if (powerManager?.isPowerSaveMode == true) return
        scanlineSweepAnimator?.start()
        scanlineOpacityAnimator?.start()
    }

    override fun onPause() {
        scanlineSweepAnimator?.pause()
        scanlineOpacityAnimator?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanlineSweepAnimator?.cancel()
        scanlineOpacityAnimator?.cancel()
        scanlineSweepAnimator = null
        scanlineOpacityAnimator = null
        moduleRefreshHandler.removeCallbacks(moduleRefreshTick)
        unregisterReceiver(batteryReceiver)
        hudController.release()
        StatusBarManager.resetStatusBar(this)
    }
}
