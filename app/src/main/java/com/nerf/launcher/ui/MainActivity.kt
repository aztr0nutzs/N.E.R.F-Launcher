package com.nerf.launcher.ui

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.nerf.launcher.util.SystemModuleController
import com.nerf.launcher.util.SystemModuleSnapshot
import com.nerf.launcher.util.ThemeManager
import com.nerf.launcher.util.ThemeRepository
import com.nerf.launcher.util.assistant.AssistantAction
import com.nerf.launcher.util.assistant.AssistantActionResult
import com.nerf.launcher.util.assistant.AiResponseRepository
import com.nerf.launcher.util.assistant.AssistantController
import com.nerf.launcher.util.assistant.AssistantSessionManager
import com.nerf.launcher.util.network.LocalNetworkScanner
import com.nerf.launcher.util.network.NetworkNode
import com.nerf.launcher.viewmodel.LauncherViewModel
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

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
    private lateinit var systemModuleController: SystemModuleController
    private var allApps: List<AppInfo> = emptyList()
    private var filteredAppCount: Int = 0
    private val themeNames by lazy { ThemeRepository.allThemeNames }
    private val iconPackNames by lazy { IconPackManager.getAvailablePacks(this) }
    private val lockSurfaceClockHandler = Handler(Looper.getMainLooper())
    private var scanlineSweepAnimator: ObjectAnimator? = null
    private var scanlineOpacityAnimator: ValueAnimator? = null
    private var isLockSurfaceVisible: Boolean = false
    private var lastObservedConfig: AppConfig? = null
    private var animationSpeedMultiplier: Float = 1f
    private var latestSystemSnapshot: SystemModuleSnapshot? = null
    private lateinit var localNetworkScanner: LocalNetworkScanner
    private var isNetworkScanRunning: Boolean = false
    private var lastNetworkScanResult: List<NetworkNode>? = null
    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        assistantOverlayController.onRecordAudioPermissionResult(granted)
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
        localNetworkScanner = LocalNetworkScanner(applicationContext)
        systemModuleController = SystemModuleController(applicationContext) { snapshot ->
            renderSystemModules(snapshot)
        }
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
        })

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
        systemModuleController.setInputs(ConfigRepository.get().config.value, filteredAppCount, allApps.size)
    }

    private fun setupConfigObservers() {
        ConfigRepository.get().config.observe(this) { config ->
            val previous = lastObservedConfig
            val themeChanged = previous?.themeName != config.themeName ||
                    previous?.glowIntensity != config.glowIntensity
            val gridChanged = previous?.gridSize != config.gridSize
            val animationSpeedChanged = previous?.animationSpeedEnabled != config.animationSpeedEnabled
            val iconPackChanged = previous?.iconPack != null && previous.iconPack != config.iconPack

            animationSpeedMultiplier = if (config.animationSpeedEnabled) 0.65f else 1f
            if (themeChanged || previous == null) {
                val activeTheme = ThemeManager.resolveActiveTheme(
                    context = this,
                    themeName = config.themeName,
                    glowIntensity = config.glowIntensity
                )
                ThemeManager.applyTheme(this, binding.rootContainer, activeTheme)
                applyStatusBarTheme(config)
                adapter.refreshTheme()
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
            if (animationSpeedChanged || previous == null) {
                setupScanlineSweep()
            }
            if (iconPackChanged) {
                iconProvider.evictCache(previous?.iconPack)
                refreshVisibleAppIcons()
            }
            systemModuleController.setInputs(config, filteredAppCount, allApps.size)
            lastObservedConfig = config
        }
    }

    private fun refreshVisibleAppIcons() {
        val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == androidx.recyclerview.widget.RecyclerView.NO_POSITION ||
            lastVisible == androidx.recyclerview.widget.RecyclerView.NO_POSITION
        ) {
            adapter.refreshIcons()
            return
        }
        adapter.refreshIconsInRange(firstVisible, lastVisible)
    }

    private fun setupSystemModules() {
        binding.moduleEnergyBar.segments = 18
        binding.moduleStorageBar.segments = 12

        binding.moduleEnergyCard.setOnClickListener { systemModuleController.refreshNow() }
        binding.moduleStorageCard.setOnClickListener { systemModuleController.refreshNow() }
        binding.moduleRuntimeCard.setOnClickListener { systemModuleController.refreshNow() }
        binding.moduleStateCard.setOnClickListener {
            val current = ConfigRepository.get().config.value ?: return@setOnClickListener
            val toggled = !current.taskbarSettings.enabled
            TaskbarController.updateSettings { copy(enabled = toggled) }
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
                systemModuleController.refreshNow()
            },
            onOpenAssistantOverlay = {
                assistantOverlayController.showWakeOverlay()
            }
        )
        reactorCoordinator.bind()
    }

    private fun setupAssistantOverlay() {
        assistantController = AssistantSessionManager.acquire(this)
        assistantController.setActiveSurface("main")
        assistantController.onLauncherAction = { command ->
            handleAssistantLauncherCommand(command)
        }
        assistantOverlayController = AssistantOverlayController(
            binding = binding.assistantOverlay,
            assistantController = assistantController,
            hasRecordAudioPermission = {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            },
            requestRecordAudioPermission = {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
        assistantOverlayController.bind()
    }

    private fun bindAssistantStateSync() {
        assistantController.onStateChanged = { snapshot ->
            assistantOverlayController.renderState(snapshot)
            if (::reactorCoordinator.isInitialized) {
                reactorCoordinator.renderAssistantState(snapshot.state)
            }
        }
    }

    private fun openNodeHunterModule(launchSource: String) {
        startActivity(NodeHunterModuleActivity.createIntent(this, launchSource))
    }

    private fun setupSurfaceTransitions() {
        val animatedPanels = listOf(binding.leftPanel, binding.corePanel, binding.rightPanel)
        animatedPanels.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 16f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 56L) + 48L)
                .setDuration(scaledDuration(310L))
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    view.alpha = 1f
                    view.translationY = 0f
                }
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
            .setDuration(scaledDuration(330L))
            .setInterpolator(LinearOutSlowInInterpolator())
            .withEndAction {
                binding.drawerShell.alpha = 1f
                binding.drawerShell.translationY = 0f
                binding.drawerShell.translationX = 0f
            }
            .start()

        val maxTransitionDuration = maxOf(
            scaledDuration(310L) + ((animatedPanels.size - 1) * 56L) + 48L,
            scaledDuration(330L) + 185L
        ) + 32L
        binding.root.postDelayed({
            animatedPanels.forEach { panel ->
                if (panel.alpha <= 0f) {
                    panel.alpha = 1f
                    panel.translationY = 0f
                }
            }
            if (binding.drawerShell.alpha <= 0f) {
                binding.drawerShell.alpha = 1f
                binding.drawerShell.translationY = 0f
                binding.drawerShell.translationX = 0f
            }
        }, maxTransitionDuration)
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
                duration = scaledDuration(8_400L)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) &&
                    !systemModuleController.isPowerSaveModeEnabled()
                ) {
                    start()
                }
            }
            scanlineOpacityAnimator = ValueAnimator.ofFloat(0.11f, 0.17f).apply {
                duration = scaledDuration(4_800L)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = FastOutLinearInInterpolator()
                addUpdateListener { animator ->
                    binding.scanlineOverlay.alpha = animator.animatedValue as Float
                }
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) &&
                    !systemModuleController.isPowerSaveModeEnabled()
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

    private fun renderSystemModules(snapshot: SystemModuleSnapshot) {
        latestSystemSnapshot = snapshot
        val activeTheme = ThemeManager.resolveActiveTheme(this)
        if (snapshot.batteryPercent != null) {
            val batteryStateTextRes = if (snapshot.isCharging) {
                com.nerf.launcher.R.string.modules_battery_charging
            } else {
                com.nerf.launcher.R.string.modules_battery_idle
            }
            binding.moduleEnergyValue.text = getString(
                com.nerf.launcher.R.string.modules_energy_percent,
                snapshot.batteryPercent,
                getString(batteryStateTextRes)
            )
            binding.moduleEnergyBar.progress = snapshot.batteryPercent
            binding.moduleEnergyBar.setActiveColor(
                if (snapshot.batteryPercent < 20) activeTheme.hudEnergyLowColor
                else activeTheme.hudEnergyHighColor
            )
        } else {
            binding.moduleEnergyValue.text = "--"
            binding.moduleEnergyBar.progress = 0
        }

        if (snapshot.storageUsagePercent != null) {
            binding.moduleStorageValue.text = getString(
                com.nerf.launcher.R.string.modules_storage_percent,
                snapshot.storageUsagePercent
            )
            binding.moduleStorageBar.progress = snapshot.storageUsagePercent
        } else {
            binding.moduleStorageValue.text = getString(com.nerf.launcher.R.string.modules_storage_unavailable)
            binding.moduleStorageBar.progress = 0
        }

        binding.moduleRuntimeValue.text = getString(
            com.nerf.launcher.R.string.modules_runtime_value,
            snapshot.uptimeDays,
            snapshot.uptimeHours
        )

        val interactiveState = if (snapshot.isInteractive) {
            getString(com.nerf.launcher.R.string.modules_state_active)
        } else {
            getString(com.nerf.launcher.R.string.modules_state_idle)
        }
        val powerMode = if (snapshot.isPowerSaveMode) {
            getString(com.nerf.launcher.R.string.modules_state_eco)
        } else {
            getString(com.nerf.launcher.R.string.modules_state_nominal)
        }
        binding.moduleStateValue.text = getString(
            com.nerf.launcher.R.string.modules_state_value,
            interactiveState,
            powerMode
        )
        binding.moduleReactorValue.text = getString(
            com.nerf.launcher.R.string.modules_reactor_sync,
            snapshot.reactorSync
        )
        reactorCoordinator.updateSyncPreview(snapshot.reactorSync)
    }

    private fun handleAssistantLauncherCommand(
        command: AssistantAction.LauncherCommand
    ): AssistantActionResult.LauncherCommandHandled {
        return when (command) {
            AssistantAction.LauncherCommand.OPEN_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = "Opening launcher settings now.",
                    outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                    responseCategory = AiResponseRepository.Category.COMMAND_RECEIVED,
                    details = AssistantActionResult.LauncherCommandDetails.OpenedDestination(
                        destination = "settings"
                    )
                )
            }

            AssistantAction.LauncherCommand.OPEN_DIAGNOSTICS -> {
                startActivity(Intent(this, ReactorDiagnosticsActivity::class.java))
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = "Opening reactor diagnostics.",
                    outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                    responseCategory = AiResponseRepository.Category.DIAGNOSTICS,
                    details = AssistantActionResult.LauncherCommandDetails.OpenedDestination(
                        destination = "diagnostics"
                    )
                )
            }

            AssistantAction.LauncherCommand.OPEN_NODE_HUNTER -> {
                openNodeHunterModule(NodeHunterModuleActivity.SOURCE_ASSISTANT)
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = "Opening Node Hunter module.",
                    outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                    responseCategory = AiResponseRepository.Category.APP_LAUNCH,
                    details = AssistantActionResult.LauncherCommandDetails.OpenedDestination(
                        destination = "node_hunter"
                    )
                )
            }

            AssistantAction.LauncherCommand.SHOW_LOCK_SURFACE -> {
                val wasAlreadyVisible = isLockSurfaceVisible
                showLockSurface()
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = if (wasAlreadyVisible) {
                        "Lock surface is already active."
                    } else {
                        "Lock surface is now active."
                    },
                    outcome = if (wasAlreadyVisible) {
                        AssistantActionResult.LauncherOutcome.ALREADY_ACTIVE
                    } else {
                        AssistantActionResult.LauncherOutcome.PERFORMED
                    },
                    responseCategory = AiResponseRepository.Category.COMMAND_RECEIVED,
                    details = AssistantActionResult.LauncherCommandDetails.OpenedDestination(
                        destination = "lock_surface"
                    )
                )
            }

            AssistantAction.LauncherCommand.CYCLE_THEME -> {
                val config = ConfigRepository.get().config.value
                if (config == null || themeNames.isEmpty()) {
                    AssistantActionResult.LauncherCommandHandled(
                        command = command,
                        spokenText = "Theme cycle is unavailable until launcher config is loaded.",
                        outcome = AssistantActionResult.LauncherOutcome.BLOCKED,
                        responseCategory = AiResponseRepository.Category.THEME_SWITCH,
                        details = AssistantActionResult.LauncherCommandDetails.CurrentTheme(
                            activeTheme = null
                        )
                    )
                } else {
                    val currentIndex = themeNames.indexOf(config.themeName).takeIf { it >= 0 } ?: 0
                    val currentTheme = themeNames[currentIndex]
                    val nextTheme = themeNames[(currentIndex + 1) % themeNames.size]
                    ConfigRepository.get().updateTheme(nextTheme)
                    AssistantActionResult.LauncherCommandHandled(
                        command = command,
                        spokenText = "Theme cycled to $nextTheme.",
                        outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                        responseCategory = AiResponseRepository.Category.THEME_SWITCH,
                        details = AssistantActionResult.LauncherCommandDetails.ThemeCycled(
                            previousTheme = currentTheme,
                            newTheme = nextTheme
                        )
                    )
                }
            }

            AssistantAction.LauncherCommand.REPORT_CURRENT_THEME -> {
                val activeTheme = ConfigRepository.get().config.value?.themeName
                val spokenText = if (activeTheme.isNullOrBlank()) {
                    "Current theme is unavailable because configuration is not ready."
                } else {
                    "Current launcher theme is $activeTheme."
                }
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = spokenText,
                    outcome = AssistantActionResult.LauncherOutcome.INFORMATIONAL,
                    responseCategory = AiResponseRepository.Category.STATUS_REPORT,
                    details = AssistantActionResult.LauncherCommandDetails.CurrentTheme(
                        activeTheme = activeTheme
                    )
                )
            }

            AssistantAction.LauncherCommand.REPORT_SYSTEM_STATE -> {
                val snapshot = latestSystemSnapshot
                val spokenText = if (snapshot == null) {
                    "System telemetry is still initializing. Try again in a moment."
                } else {
                    val battery = snapshot.batteryPercent?.let { "$it percent" } ?: "unavailable"
                    val storage = snapshot.storageUsagePercent?.let { "$it percent used" } ?: "unavailable"
                    val powerSave = if (snapshot.isPowerSaveMode) "enabled" else "disabled"
                    "Battery $battery, storage $storage, uptime ${snapshot.uptimeDays} days ${snapshot.uptimeHours} hours, power save $powerSave."
                }
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = spokenText,
                    outcome = AssistantActionResult.LauncherOutcome.INFORMATIONAL,
                    responseCategory = AiResponseRepository.Category.STATUS_REPORT,
                    details = snapshot?.let {
                        AssistantActionResult.LauncherCommandDetails.SystemState(
                            batteryPercent = it.batteryPercent,
                            storageUsagePercent = it.storageUsagePercent,
                            uptimeDays = it.uptimeDays,
                            uptimeHours = it.uptimeHours,
                            isPowerSaveMode = it.isPowerSaveMode
                        )
                    }
                )
            }

            AssistantAction.LauncherCommand.REPORT_APP_FILTER_STATE -> {
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = "App catalog has ${allApps.size} apps loaded with $filteredAppCount currently in filter scope.",
                    outcome = AssistantActionResult.LauncherOutcome.INFORMATIONAL,
                    responseCategory = AiResponseRepository.Category.STATUS_REPORT,
                    details = AssistantActionResult.LauncherCommandDetails.AppFilterState(
                        totalApps = allApps.size,
                        filteredApps = filteredAppCount
                    )
                )
            }

            AssistantAction.LauncherCommand.START_LOCAL_NETWORK_SCAN -> {
                if (!localNetworkScanner.canScanLocalSubnet()) {
                    return AssistantActionResult.LauncherCommandHandled(
                        command = command,
                        spokenText = "Local network scan is unavailable because Wi-Fi subnet data is not accessible.",
                        outcome = AssistantActionResult.LauncherOutcome.BLOCKED,
                        responseCategory = AiResponseRepository.Category.NETWORK_FAILURE,
                        details = AssistantActionResult.LauncherCommandDetails.NetworkScanStatus(
                            supported = false,
                            running = false,
                            nodeCount = null
                        )
                    )
                }
                if (isNetworkScanRunning) {
                    return AssistantActionResult.LauncherCommandHandled(
                        command = command,
                        spokenText = "A local network scan is already running.",
                        outcome = AssistantActionResult.LauncherOutcome.IN_PROGRESS,
                        responseCategory = AiResponseRepository.Category.SCANNING,
                        details = AssistantActionResult.LauncherCommandDetails.NetworkScanStatus(
                            supported = true,
                            running = true,
                            nodeCount = null
                        )
                    )
                }
                isNetworkScanRunning = true
                lastNetworkScanResult = null
                lifecycleScope.launch {
                    lastNetworkScanResult = localNetworkScanner.scanLocalSubnet()
                    isNetworkScanRunning = false
                }
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = "Starting local network scan now.",
                    outcome = AssistantActionResult.LauncherOutcome.PERFORMED,
                    responseCategory = AiResponseRepository.Category.SCANNING,
                    details = AssistantActionResult.LauncherCommandDetails.NetworkScanStatus(
                        supported = true,
                        running = true,
                        nodeCount = null
                    )
                )
            }

            AssistantAction.LauncherCommand.SUMMARIZE_LOCAL_NETWORK_SCAN -> {
                val nodes = lastNetworkScanResult.orEmpty()
                val supported = localNetworkScanner.canScanLocalSubnet()
                val spokenText = when {
                    isNetworkScanRunning -> "Local network scan is in progress. Ask again when it finishes."
                    lastNetworkScanResult == null -> "No completed local network scan is available yet."
                    else -> {
                        if (nodes.isEmpty()) {
                            "Local network scan completed with zero reachable nodes."
                        } else {
                            val averagePing = nodes.map { it.pingMs }.average().roundToLong()
                            "Local network scan found ${nodes.size} reachable nodes with average latency ${averagePing} milliseconds."
                        }
                    }
                }
                AssistantActionResult.LauncherCommandHandled(
                    command = command,
                    spokenText = spokenText,
                    outcome = when {
                        isNetworkScanRunning -> AssistantActionResult.LauncherOutcome.IN_PROGRESS
                        !supported -> AssistantActionResult.LauncherOutcome.BLOCKED
                        else -> AssistantActionResult.LauncherOutcome.INFORMATIONAL
                    },
                    responseCategory = when {
                        isNetworkScanRunning -> AiResponseRepository.Category.SCANNING
                        !supported -> AiResponseRepository.Category.NETWORK_FAILURE
                        lastNetworkScanResult == null -> AiResponseRepository.Category.NETWORK_SCAN
                        nodes.isEmpty() -> AiResponseRepository.Category.NETWORK_FAILURE
                        else -> AiResponseRepository.Category.NETWORK_SUCCESS
                    },
                    details = when {
                        isNetworkScanRunning -> AssistantActionResult.LauncherCommandDetails.NetworkScanStatus(
                            supported = supported,
                            running = true,
                            nodeCount = null
                        )

                        lastNetworkScanResult == null -> AssistantActionResult.LauncherCommandDetails.NetworkScanStatus(
                            supported = supported,
                            running = false,
                            nodeCount = null
                        )

                        nodes.isEmpty() -> AssistantActionResult.LauncherCommandDetails.NetworkScanSummary(
                            nodeCount = 0,
                            averagePingMs = null,
                            fastestPingMs = null,
                            slowestPingMs = null
                        )

                        else -> {
                            val pingValues = nodes.map { it.pingMs }
                            AssistantActionResult.LauncherCommandDetails.NetworkScanSummary(
                                nodeCount = nodes.size,
                                averagePingMs = pingValues.average().roundToLong(),
                                fastestPingMs = pingValues.minOrNull(),
                                slowestPingMs = pingValues.maxOrNull()
                            )
                        }
                    }
                )
            }
        }
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
            TaskbarController.updateSettings { copy(enabled = toggled) }
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
            context = this,
            themeName = config.themeName,
            glowIntensity = config.glowIntensity
        ).primary
        val isLightTheme = com.nerf.launcher.util.ColorUtils.isColorLight(primaryColor)
        StatusBarManager.applyStatusBarTheme(this, primaryColor, isLightTheme)
    }

    private fun scaledDuration(baseDurationMs: Long): Long {
        return (baseDurationMs * animationSpeedMultiplier).toLong().coerceAtLeast(1L)
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
        if (systemModuleController.isPowerSaveModeEnabled()) return
        scanlineSweepAnimator?.start()
        scanlineOpacityAnimator?.start()
    }

    override fun onPause() {
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        scanlineSweepAnimator?.pause()
        scanlineOpacityAnimator?.pause()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        hudController.start()
        systemModuleController.start()
    }

    override fun onStop() {
        hudController.stop()
        systemModuleController.stop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanlineSweepAnimator?.cancel()
        scanlineOpacityAnimator?.cancel()
        scanlineSweepAnimator = null
        scanlineOpacityAnimator = null
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        assistantController.onLauncherAction = null
        assistantController.onStateChanged = null
        assistantOverlayController.release()
        AssistantSessionManager.release(assistantController)
        hudController.release()
        iconProvider.release()
        StatusBarManager.resetStatusBar(this)
    }
}
