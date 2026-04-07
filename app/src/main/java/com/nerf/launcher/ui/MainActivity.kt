package com.nerf.launcher.ui

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.view.inputmethod.EditorInfo
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
import android.util.Log
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nerf.launcher.adapter.AppAdapter
import com.nerf.launcher.BuildConfig
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
import com.nerf.launcher.util.NerfTheme
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Main launcher screen – app grid + HUD + taskbar.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val STATE_LOCK_SURFACE_VISIBLE = "state_lock_surface_visible"
        private const val MENU_ITEM_PIN_TOGGLE = 1001
        private const val DRAG_UPDATE_THROTTLE_MS = 120L
        private const val DRAG_PROGRESS_STEP = 2
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
    private var networkScanJob: Job? = null
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
        setupDefaultLauncherBanner()
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
        adapter = AppAdapter(
            iconProvider = iconProvider,
            onAppClicked = { app ->
                binding.drawerSearchInput.clearFocus()
                hideDrawerKeyboard()
                AppUtils.launchApp(this, app)
            },
            onAppLongPressed = { anchor, app ->
                showAppContextMenu(anchor, app)
            }
        )
        val initialTheme = ThemeManager.resolveActiveTheme(this)
        adapter.updateThemeResources(
            labelColor = initialTheme.hudAppLabelColor,
            socketBackground = ThemeManager.createAppIconSocketBackground(this, initialTheme)
        )

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

    private fun showAppContextMenu(anchor: View, app: AppInfo) {
        val popupMenu = PopupMenu(this, anchor)
        val isPinned = TaskbarController.isPinned(app.packageName)
        val pinActionTitle = if (isPinned) {
            getString(R.string.taskbar_unpin_action)
        } else {
            getString(R.string.taskbar_pin_action)
        }
        popupMenu.menu.add(0, MENU_ITEM_PIN_TOGGLE, 0, pinActionTitle)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ITEM_PIN_TOGGLE -> {
                    TaskbarController.togglePinnedApp(app.packageName)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun setupDrawerSearch() {
        binding.drawerSearchInput.doAfterTextChanged {
            applyDrawerFilter(it?.toString().orEmpty())
        }
        binding.drawerSearchInput.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                textView.clearFocus()
                hideDrawerKeyboard()
                true
            } else {
                false
            }
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
        val trimmedQuery = query.trim()
        val normalizedQuery = trimmedQuery.lowercase(Locale.getDefault())
        val filtered = if (normalizedQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.normalizedAppName.contains(normalizedQuery) ||
                    app.normalizedPackageName.contains(normalizedQuery)
            }
        }
        adapter.submitList(filtered)
        filteredAppCount = filtered.size
        binding.drawerResultCount.text = getString(com.nerf.launcher.R.string.drawer_result_count, filtered.size)

        val emptyStateMessageRes = if (trimmedQuery.isBlank()) {
            com.nerf.launcher.R.string.drawer_empty_state_no_apps
        } else {
            null
        }
        val emptyMessage = if (filtered.isEmpty()) {
            if (emptyStateMessageRes != null) {
                getString(emptyStateMessageRes)
            } else {
                getString(com.nerf.launcher.R.string.drawer_empty_state_no_results, trimmedQuery)
            }
        } else {
            null
        }
        binding.drawerEmptyState.text = emptyMessage
        binding.drawerEmptyState.visibility = if (emptyMessage == null) View.GONE else View.VISIBLE

        systemModuleController.setInputs(ConfigRepository.get().config.value, filteredAppCount, allApps.size)
    }

    private fun setupDefaultLauncherBanner() {
        binding.defaultLauncherAction.setOnClickListener {
            AppUtils.openDefaultHomeSettings(this)
        }
        updateDefaultLauncherBannerVisibility()
    }

    private fun updateDefaultLauncherBannerVisibility() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolvedHome = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val isDefaultLauncher = resolvedHome?.activityInfo?.packageName == packageName
        binding.defaultLauncherBanner.visibility = if (isDefaultLauncher) View.GONE else View.VISIBLE
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
                val activeTheme = ThemeManager.resolveConfigTheme(this, config)
                applyLauncherShellTheme(activeTheme)
                applyStatusBarTheme(config)
                adapter.updateThemeResources(
                    labelColor = activeTheme.hudAppLabelColor,
                    socketBackground = ThemeManager.createAppIconSocketBackground(this, activeTheme)
                )
                refreshVisibleAppTheme()
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

    private fun refreshVisibleAppTheme() {
        val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            adapter.refreshTheme()
            return
        }
        adapter.refreshThemeInRange(firstVisible, lastVisible)
    }

    private fun applyLauncherShellTheme(theme: NerfTheme) {
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(theme.windowBackground))
        binding.rootContainer.setBackgroundColor(theme.windowBackground)
        binding.lockSurfaceRoot.setBackgroundColor(theme.lockSurfaceScrim)
        binding.scanlineOverlay.background = ThemeManager.createScanlineOverlayDrawable(theme)

        binding.drawerSearchInput.background = ThemeManager.createDrawerSearchFieldBackground(this, theme)
        binding.drawerSearchInput.setTextColor(theme.hudPanelTextPrimary)
        binding.drawerSearchInput.setHintTextColor(theme.hudPanelTextSecondary)

        val quickOrbDrawable = ThemeManager.createQuickToggleOrbDrawable(this, theme)
        listOf(
            binding.quickThemeBtn,
            binding.quickIconPackBtn,
            binding.quickAnimationBtn,
            binding.quickTaskbarBtn
        ).forEach { button ->
            button.background = ThemeManager.cloneMutableDrawable(quickOrbDrawable)
        }

        val actionTileDrawable = ThemeManager.createHudActionTileDrawable(this, theme)
        listOf(
            binding.openSettingsTile,
            binding.reloadTile,
            binding.lockSurfaceTile,
            binding.lockSurfaceUnlockButton
        ).forEach { tile ->
            tile.background = ThemeManager.cloneMutableDrawable(actionTileDrawable)
        }

        mapOf(
            binding.quickThemeBtn to theme.hudInfoColor,
            binding.quickIconPackBtn to theme.hudSuccessColor,
            binding.quickAnimationBtn to theme.hudWarningColor,
            binding.quickTaskbarBtn to theme.hudAccentColor,
            binding.quickGlowValue to theme.hudInfoColor,
            binding.quickGridValue to theme.hudWarningColor,
            binding.openSettingsTile to theme.hudInfoColor,
            binding.reloadTile to theme.hudWarningColor,
            binding.lockSurfaceTile to theme.hudAccentColor,
            binding.lockSurfaceUnlockButton to theme.hudSuccessColor
        ).forEach { (view, color) ->
            view.setTextColor(color)
        }

        val seekbarBackground = androidx.core.graphics.ColorUtils.setAlphaComponent(
            theme.hudPanelTextSecondary,
            0x66
        )
        ThemeManager.applySeekBarTint(binding.quickGlowSeekbar, theme.hudInfoColor, seekbarBackground)
        ThemeManager.applySeekBarTint(binding.quickGridSeekbar, theme.hudWarningColor, seekbarBackground)
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
        assistantOverlayController.bind(this)
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
        binding.scanlineOverlay.alpha = fractionValue(R.fraction.nerf_alpha_scanline_base)
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
            }
            scanlineOpacityAnimator = ValueAnimator.ofFloat(
                fractionValue(R.fraction.nerf_alpha_scanline_min),
                fractionValue(R.fraction.nerf_alpha_scanline_max)
            ).apply {
                duration = scaledDuration(4_800L)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = FastOutLinearInInterpolator()
                addUpdateListener { animator ->
                    binding.scanlineOverlay.alpha = animator.animatedValue as Float
                }
            }
            updateScanlineAnimationState()
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
        downScale: Float = fractionValue(R.fraction.nerf_touch_scale_down_launcher_shell),
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
                        .alpha(fractionValue(R.fraction.nerf_touch_alpha_down_launcher_shell))
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

    private fun fractionValue(fractionRes: Int): Float = resources.getFraction(fractionRes, 1, 1)

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
        updateScanlineAnimationState()
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
                if (isNetworkScanRunning || networkScanJob?.isActive == true) {
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
                networkScanJob = lifecycleScope.launch {
                    try {
                        lastNetworkScanResult = localNetworkScanner.scanLocalSubnet()
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "Local network scan failed.")
                        }
                        lastNetworkScanResult = emptyList()
                    } finally {
                        isNetworkScanRunning = false
                    }
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
            private var lastCommittedProgress = binding.quickGlowSeekbar.progress
            private var lastCommittedAt = 0L

            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (!fromUser) return
                binding.quickGlowValue.text = getString(com.nerf.launcher.R.string.quick_glow_percent, progress)
                val value = progress / 100f
                if (shouldCommitDragUpdate(progress, lastCommittedProgress, lastCommittedAt)) {
                    ConfigRepository.get().updateGlowIntensity(value)
                    lastCommittedProgress = progress
                    lastCommittedAt = SystemClock.elapsedRealtime()
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                lastCommittedProgress = seekBar?.progress ?: lastCommittedProgress
                lastCommittedAt = 0L
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val finalProgress = seekBar?.progress ?: return
                val value = finalProgress / 100f
                ConfigRepository.get().updateGlowIntensity(value)
            }
        })

        binding.quickGridSeekbar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            private var lastCommittedProgress = binding.quickGridSeekbar.progress
            private var lastCommittedAt = 0L

            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (!fromUser) return
                val gridSize = progress + 2
                binding.quickGridValue.text = getString(com.nerf.launcher.R.string.quick_grid_state, gridSize)
                if (shouldCommitDragUpdate(progress, lastCommittedProgress, lastCommittedAt)) {
                    ConfigRepository.get().updateGridSize(gridSize)
                    lastCommittedProgress = progress
                    lastCommittedAt = SystemClock.elapsedRealtime()
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                lastCommittedProgress = seekBar?.progress ?: lastCommittedProgress
                lastCommittedAt = 0L
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val finalProgress = seekBar?.progress ?: return
                val gridSize = finalProgress + 2
                ConfigRepository.get().updateGridSize(gridSize)
            }
        })
    }

    private fun shouldCommitDragUpdate(
        progress: Int,
        lastCommittedProgress: Int,
        lastCommittedAt: Long
    ): Boolean {
        val progressDelta = kotlin.math.abs(progress - lastCommittedProgress)
        val elapsed = SystemClock.elapsedRealtime() - lastCommittedAt
        return lastCommittedAt == 0L || progressDelta >= DRAG_PROGRESS_STEP || elapsed >= DRAG_UPDATE_THROTTLE_MS
    }

    private fun bindQuickControls(config: AppConfig) {
        binding.quickThemeBtn.text = getString(com.nerf.launcher.R.string.quick_theme_state, config.themeName)
        val iconStateRes = if (iconPackNames.size <= 1) {
            com.nerf.launcher.R.string.quick_icon_state_locked
        } else {
            com.nerf.launcher.R.string.quick_icon_state
        }
        binding.quickIconPackBtn.text = getString(iconStateRes, config.iconPack)
        binding.quickIconPackBtn.isEnabled = iconPackNames.size > 1
        binding.quickIconPackBtn.alpha = if (iconPackNames.size > 1) 1f else 0.78f
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
        val primaryColor = ThemeManager.resolveConfigTheme(this, config).primary
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
        updateDefaultLauncherBannerVisibility()
        if (isLockSurfaceVisible) {
            lockSurfaceClockHandler.post(lockSurfaceClockTick)
        }
        updateScanlineAnimationState()
    }

    override fun onPause() {
        lockSurfaceClockHandler.removeCallbacks(lockSurfaceClockTick)
        pauseScanlineAnimation()
        super.onPause()
    }

    private fun updateScanlineAnimationState() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            pauseScanlineAnimation()
            return
        }
        if (systemModuleController.isPowerSaveModeEnabled()) {
            pauseScanlineAnimation()
            return
        }
        scanlineSweepAnimator?.start()
        scanlineOpacityAnimator?.start()
    }

    private fun pauseScanlineAnimation() {
        scanlineSweepAnimator?.pause()
        scanlineOpacityAnimator?.pause()
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
