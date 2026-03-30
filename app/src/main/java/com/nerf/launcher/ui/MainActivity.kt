package com.nerf.launcher.ui

import android.annotation.SuppressLint
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
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityMainBinding
import com.nerf.launcher.model.AppInfo
import com.nerf.launcher.ui.assistant.AssistantOverlayController
import com.nerf.launcher.ui.nodehunter.NodeHunterModuleActivity
import com.nerf.launcher.ui.reactor.ReactorDiagnosticsActivity
import com.nerf.launcher.ui.reactor.ReactorCoordinator
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.AppUtils
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconCache
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.IconProvider
import com.nerf.launcher.util.StatusBarManager
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.viewmodel.LauncherViewModel
import android.view.animation.LinearInterpolator
import java.util.Calendar
import java.util.Locale

/**
 * Main launcher screen – app grid + HUD + taskbar.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val STATE_LOCK_SURFACE_VISIBLE = "state_lock_surface_visible"
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var adapter: AppAdapter
    private lateinit var iconProvider: IconProvider
    private lateinit var hudController: HudController
    private lateinit var reactorCoordinator: ReactorCoordinator
    private lateinit var assistantController: AssistantController
    private lateinit var assistantOverlayController: AssistantOverlayController
    private var allApps: List<AppInfo> = emptyList()
    private var filteredAppCount: Int = 0
    private var batteryPercent: Int? = null
    private var isCharging: Boolean = false
    private val themeNames by lazy { ThemeRepository.all.map { it.name } }
    private val iconPackNames by lazy { IconPackManager.getAvailablePacks(this) }
    private val moduleRefreshHandler = Handler(Looper.getMainLooper())
    private val lockSurfaceClockHandler = Handler(Looper.getMainLooper())
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private var scanlineSweepAnimator: ObjectAnimator? = null
    private var scanlineOpacityAnimator: ValueAnimator? = null
    private var isLockSurfaceVisible: Boolean = false
    private var lastObservedConfig: AppConfig? = null

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

    private val lockSurfaceClockTick = object : Runnable {
        override fun run() {
            updateLockSurfaceTimestamp()
            lockSurfaceClockHandler.postDelayed(this, 60_000L - (SystemClock.uptimeMillis() % 60_000L))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        iconProvider = IconProvider(applicationContext, IconCache(50))
        setupAssistantOverlay()
        setupReactorCoordinator()
        bindAssistantStateSync()

        setupRecyclerView()
        setupDrawerSearch()
        observeViewModel()

        hudController = HudController(this, binding.hudRoot.root, this)
        binding.taskbarView.setIconProvider(iconProvider)
        binding.taskbarView.setLifecycleOwner(this)

        binding.lockSurfaceTile.setOnClickListener {
            showLockSurface()
        }
        setupPressFeedbacks()
        setupLockSurface()

        setupQuickControls()
        setupConfigObservers()
        setupSystemModules()
        setupSurfaceTransitions()
        setupScanlineSweep()
        if (savedInstanceState?.getBoolean(STATE_LOCK_SURFACE_VISIBLE) == true) {
            showLockSurface()
        }
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
            val previous = lastObservedConfig
            val themeChanged = previous?.themeName != config.themeName ||
                    previous?.glowIntensity != config.glowIntensity
            val gridChanged = previous?.gridSize != config.gridSize
            val taskbarSettingsChanged = previous?.taskbarSettings != config.taskbarSettings

            if (themeChanged || previous == null) {
                ThemeManager.applyTheme(this)
                applyStatusBarTheme(config)
            }
            if (gridChanged || previous == null) {
                (binding.recyclerView.layoutManager as? GridLayoutManager)?.spanCount =
                    config.gridSize.coerceAtLeast(2)
            }

            binding.moduleGridValue.text = getString(com.nerf.launcher.R.string.hud_grid_value, config.gridSize)
            binding.modulePinnedValue.text = getString(
                com.nerf.launcher.R.string.hud_dock_value,
                config.taskbarSettings.pinnedApps.size
            )

            bindQuickControls(config)
            if (taskbarSettingsChanged || previous == null) {
                updateSystemModules(config)
            }
            lastObservedConfig = config
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
            ConfigRepository.get().updateTaskbarSettings(settings)
        }
    }

    private fun setupReactorCoordinator() {
        reactorCoordinator = ReactorCoordinator(
            activity = this,
            binding = binding,
            themeNames = themeNames,
            getCurrentThemeName = {
                ConfigRepository.get().config.value?.themeName ?: themeNames.firstOrNull().orEmpty()
            },
            onThemeSelected = { nextTheme ->
                ConfigRepository.get().updateTheme(nextTheme)
            },
            onWakeAssistant = {
                assistantOverlayController.wakeForCoreAction()
            },
            onOpenNodeHunter = {
                openNodeHunterModule(NodeHunterModuleActivity.SOURCE_REACTOR)
            },
            onRefreshDiagnostics = {
                viewModel.loadApps()
                updateSystemModules()
            },
            onOpenAssistantOverlay = {
                assistantOverlayController.showWakeOverlay()
            }
        )
        reactorCoordinator.bind()
    }

    private fun setupAssistantOverlay() {
        assistantController = AssistantController(this)
        assistantOverlayController = AssistantOverlayController(
            binding = binding.assistantOverlay,
            assistantController = assistantController,
            onOpenSettings = {
                startActivity(Intent(this, SettingsActivity::class.java))
            },
            onOpenDiagnostics = {
                startActivity(Intent(this, ReactorDiagnosticsActivity::class.java))
            },
            onOpenNodeHunter = {
                openNodeHunterModule(NodeHunterModuleActivity.SOURCE_ASSISTANT)
            },
            onShowLockSurface = {
                showLockSurface()
            }
        )
        assistantOverlayController.bind()
    }

    private fun bindAssistantStateSync() {
        assistantController.onStateChanged = { snapshot ->
            assistantOverlayController.renderState(snapshot)
            reactorCoordinator.renderAssistantState(snapshot.state)
        }
    }

    private fun openNodeHunterModule(launchSource: String) {
        startActivity(NodeHunterModuleActivity.createIntent(this, launchSource))
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
            binding.reactorCore,
            binding.lockSurfaceTile,
            binding.lockSurfaceUnlockButton
        )
        pressableViews.forEach { view ->
            view.applyPressFeedback()
        }
    }

    private fun setupLockSurface() {
        binding.lockSurfaceUnlockButton.setOnClickListener {
            hideLockSurface()
        }
        binding.lockSurfaceRoot.setOnClickListener {
            // Keep touches inside lock surface until user explicitly unlocks.
        }
    }

    private fun showLockSurface() {
        if (isLockSurfaceVisible) return
        if (assistantOverlayController.isShowing()) {
            assistantOverlayController.hide()
        }
        isLockSurfaceVisible = true
        binding.lockSurfaceRoot.visibility = View.VISIBLE
        binding.lockSurfaceRoot.alpha = 0f
        binding.lockSurfaceRoot.animate()
            .alpha(1f)
            .setDuration(180L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
        hideDrawerKeyboard()
        binding.drawerSearchInput.clearFocus()
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        lockSurfaceClockHandler.post(lockSurfaceClockTick)
    }

    private fun hideLockSurface() {
        if (!isLockSurfaceVisible) return
        isLockSurfaceVisible = false
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        binding.lockSurfaceRoot.animate()
            .alpha(0f)
            .setDuration(150L)
            .setInterpolator(LinearOutSlowInInterpolator())
            .withEndAction {
                binding.lockSurfaceRoot.visibility = View.GONE
            }
            .start()
    }

    private fun updateLockSurfaceTimestamp() {
        val now = Calendar.getInstance()
        binding.lockSurfaceTime.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
        val locale = Locale.getDefault()
        val day = now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
            ?.uppercase(locale) ?: "DAY"
        val month = now.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale)
            ?.uppercase(locale) ?: "MON"
        binding.lockSurfaceDate.text = String.format(
            locale,
            "%s %02d %s",
            day,
            now.get(Calendar.DAY_OF_MONTH),
            month
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LOCK_SURFACE_VISIBLE, isLockSurfaceVisible)
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
        reactorCoordinator.updateSyncPreview(reactorSync)
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
            ConfigRepository.get().updateTheme(nextTheme)
        }

        binding.quickIconPackBtn.setOnClickListener {
            if (iconPackNames.isEmpty()) return@setOnClickListener
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val currentIndex = iconPackNames.indexOf(current.iconPack).takeIf { it >= 0 } ?: 0
            val nextPack = iconPackNames[(currentIndex + 1) % iconPackNames.size]
            ConfigRepository.get().updateIconPack(nextPack)
        }

        binding.quickAnimationBtn.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.animationSpeedEnabled
            ConfigRepository.get().updateAnimationSpeed(toggled)
        }

        binding.quickTaskbarBtn.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.taskbarSettings.enabled
            val settings = current.taskbarSettings.copy(enabled = toggled)
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

    private fun applyStatusBarTheme(config: AppConfig) {
        val primaryColor = ThemeManager.resolveActiveTheme(
            themeName = config.themeName,
            glowIntensity = config.glowIntensity
        ).primary
        val isLightTheme = com.nerf.launcher.util.ColorUtils.isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }


    private fun hideDrawerKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(binding.drawerSearchInput.windowToken, 0)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        if (assistantOverlayController.isShowing()) {
            assistantOverlayController.hide()
            return
        }
        if (isLockSurfaceVisible) {
            hideLockSurface()
            return
        }
        // Do nothing – stay on launcher.
    }

    override fun onResume() {
        super.onResume()
        if (isLockSurfaceVisible) {
            lockSurfaceClockHandler.post(lockSurfaceClockTick)
        }
        if (powerManager?.isPowerSaveMode == true) return
        scanlineSweepAnimator?.start()
        scanlineOpacityAnimator?.start()
    }

    override fun onPause() {
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
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
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        unregisterReceiver(batteryReceiver)
        assistantOverlayController.release()
        hudController.release()
        StatusBarManager.resetStatusBar(this)
    }
}
