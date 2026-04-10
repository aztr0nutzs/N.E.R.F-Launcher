package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityTaskbarSettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.NerfTheme
import com.nerf.launcher.util.TaskbarBackgroundStyle
import com.nerf.launcher.util.TaskbarSettings
import com.nerf.launcher.util.ThemeManager
import kotlinx.coroutines.launch

class TaskbarSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskbarSettingsBinding
    private lateinit var backgroundStyleAdapter: ThemedSpinnerAdapter
    private var isBindingState = false
    private var lastThemeKey: Pair<String, Float>? = null
    private val pinnedPrimaryLabels = mutableListOf<TextView>()
    private val pinnedSecondaryLabels = mutableListOf<TextView>()
    private val pinnedActionButtons = mutableListOf<MaterialButton>()
    private val minHeightDp by lazy { integerValue(R.integer.nerf_taskbar_settings_min_height_dp) }
    private val maxHeightDp by lazy { integerValue(R.integer.nerf_taskbar_settings_max_height_dp) }
    private val minIconSizeDp by lazy { integerValue(R.integer.nerf_taskbar_settings_min_icon_size_dp) }
    private val maxIconSizeDp by lazy { integerValue(R.integer.nerf_taskbar_settings_max_icon_size_dp) }
    private val dragUpdateThrottleMs by lazy { longIntegerValue(R.integer.nerf_taskbar_settings_drag_update_throttle_ms) }
    private val dragDpStep by lazy { integerValue(R.integer.nerf_taskbar_settings_drag_dp_step) }
    private val dragPercentStep by lazy { integerValue(R.integer.nerf_taskbar_settings_drag_percent_step) }
    private val transparencyPercentScale by lazy {
        integerValue(R.integer.nerf_taskbar_settings_transparency_percent_scale)
    }
    private val transparencyPercentScaleFloat by lazy { transparencyPercentScale.toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialConfig = ConfigRepository.get().config.value
        val initialTheme = initialConfig
            ?.let { ThemeManager.resolveConfigTheme(this, it) }
            ?: ThemeManager.resolveActiveTheme(this)
        lastThemeKey = initialConfig?.let(ThemeManager::themeKey)

        binding = ActivityTaskbarSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.taskbar_settings_title)

        setupControls(initialTheme)
        applyTheme(initialTheme)
        observeConfig()
    }

    private fun setupControls(initialTheme: NerfTheme) {
        binding.taskbarEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingState) return@setOnCheckedChangeListener
            updateTaskbarSettings { copy(enabled = isChecked) }
        }

        binding.taskbarHeightSeekbar.max = maxHeightDp - minHeightDp
        binding.taskbarHeightSeekbar.setOnSeekBarChangeListener(
            createThrottledSeekBarListener(
                onProgress = { progress ->
                    binding.taskbarHeightValue.text = getString(
                        R.string.taskbar_settings_height_value,
                        progress + minHeightDp
                    )
                },
                commitStep = dragDpStep,
                commitValue = { progress ->
                    updateTaskbarSettings { copy(height = progress + minHeightDp) }
                }
            )
        )

        binding.taskbarIconSizeSeekbar.max = maxIconSizeDp - minIconSizeDp
        binding.taskbarIconSizeSeekbar.setOnSeekBarChangeListener(
            createThrottledSeekBarListener(
                onProgress = { progress ->
                    binding.taskbarIconSizeValue.text = getString(
                        R.string.taskbar_settings_icon_size_value,
                        progress + minIconSizeDp
                    )
                },
                commitStep = dragDpStep,
                commitValue = { progress ->
                    updateTaskbarSettings { copy(iconSize = progress + minIconSizeDp) }
                }
            )
        )

        binding.taskbarTransparencySeekbar.max = transparencyPercentScale
        binding.taskbarTransparencySeekbar.setOnSeekBarChangeListener(
            createThrottledSeekBarListener(
                onProgress = { progress ->
                    binding.taskbarTransparencyValue.text = getString(
                        R.string.taskbar_settings_transparency_value,
                        progress
                    )
                },
                commitStep = dragPercentStep,
                commitValue = { progress ->
                    updateTaskbarSettings {
                        copy(transparency = transparencyFromPercent(progress))
                    }
                }
            )
        )

        backgroundStyleAdapter = ThemedSpinnerAdapter(
            this,
            TASKBAR_BACKGROUND_OPTIONS.map { getString(it.labelRes) },
            initialTheme
        )
        binding.taskbarBackgroundStyleSpinner.adapter = backgroundStyleAdapter
        binding.taskbarBackgroundStyleSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (isBindingState) return
                    val selected = TASKBAR_BACKGROUND_OPTIONS[position]
                    updateTaskbarSettings { copy(backgroundStyle = selected.style) }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }

        binding.clearPinnedAppsButton.setOnClickListener { TaskbarController.clearPinnedApps() }
    }

    private fun observeConfig() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ConfigRepository.get().config.collect { config ->
                    val themeKey = ThemeManager.themeKey(config)
                    val theme = ThemeManager.resolveConfigTheme(this@TaskbarSettingsActivity, config)
                    if (themeKey != lastThemeKey) {
                        applyTheme(theme)
                        backgroundStyleAdapter.updateTheme(theme)
                        lastThemeKey = themeKey
                    }
                    bindTaskbarSettings(config.taskbarSettings, theme)
                }
            }
        }
    }

    private fun applyTheme(theme: NerfTheme) {
        ThemeManager.applyWindowAndToolbarTheme(
            activity = this,
            root = binding.root,
            toolbar = binding.toolbar,
            theme = theme
        )
        ThemeManager.applyTextColorRecursively(binding.root, theme.hudPanelTextPrimary)

        binding.clearPinnedAppsButton.let { button ->
            button.backgroundTintList = ThemeManager.createEnabledDisabledColorStateList(
                enabledColor = theme.primary,
                disabledColor = ColorUtils.setAlphaComponent(theme.primary, 0x61)
            )
            button.setTextColor(
                ThemeManager.createEnabledDisabledColorStateList(
                    enabledColor = theme.hudPanelTextPrimary,
                    disabledColor = ColorUtils.setAlphaComponent(theme.hudPanelTextPrimary, 0x61)
                )
            )
        }

        binding.taskbarEnabledSwitch.thumbTintList = createSwitchThumbTint(theme)
        binding.taskbarEnabledSwitch.trackTintList = createSwitchTrackTint(theme)

        val seekBarBackground = ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0x66)
        listOf(
            binding.taskbarHeightSeekbar,
            binding.taskbarIconSizeSeekbar,
            binding.taskbarTransparencySeekbar
        ).forEach { seekBar ->
            ThemeManager.applySeekBarTint(seekBar, theme.primary, seekBarBackground)
        }

        applyPinnedManagementTheme(theme)
    }

    private fun applyPinnedManagementTheme(theme: NerfTheme) {
        pinnedPrimaryLabels.forEach { it.setTextColor(theme.hudPanelTextPrimary) }
        pinnedSecondaryLabels.forEach {
            it.setTextColor(ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0xD0))
        }

        val buttonFill = android.content.res.ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(theme.primary, 0xD9)
        )
        val buttonText = android.content.res.ColorStateList.valueOf(theme.hudPanelTextPrimary)
        pinnedActionButtons.forEach { button ->
            button.backgroundTintList = buttonFill
            button.setTextColor(buttonText)
        }
    }

    private fun createSwitchThumbTint(theme: NerfTheme): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                theme.primary,
                ColorUtils.setAlphaComponent(theme.hudPanelTextPrimary, 0xB3)
            )
        )
    }

    private fun createSwitchTrackTint(theme: NerfTheme): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                ColorUtils.setAlphaComponent(theme.primary, 0x80),
                ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0x4D)
            )
        )
    }

    private fun bindTaskbarSettings(settings: TaskbarSettings, theme: NerfTheme) {
        isBindingState = true

        binding.taskbarEnabledSwitch.isChecked = settings.enabled

        val heightDp = settings.height.coerceIn(minHeightDp, maxHeightDp)
        binding.taskbarHeightSeekbar.progress = heightDp - minHeightDp
        binding.taskbarHeightValue.text = getString(R.string.taskbar_settings_height_value, heightDp)

        val iconSizeDp = settings.iconSize.coerceIn(minIconSizeDp, maxIconSizeDp)
        binding.taskbarIconSizeSeekbar.progress = iconSizeDp - minIconSizeDp
        binding.taskbarIconSizeValue.text = getString(R.string.taskbar_settings_icon_size_value, iconSizeDp)

        val transparencyPercent = transparencyToPercent(settings.transparency)
        binding.taskbarTransparencySeekbar.progress = transparencyPercent
        binding.taskbarTransparencyValue.text = getString(
            R.string.taskbar_settings_transparency_value,
            transparencyPercent
        )
        val selectedBackgroundIndex = TASKBAR_BACKGROUND_OPTIONS
            .indexOfFirst { it.style == settings.backgroundStyle }
            .takeIf { it >= 0 }
            ?: 0
        binding.taskbarBackgroundStyleSpinner.setSelection(selectedBackgroundIndex, false)

        val pinnedCount = settings.pinnedApps.size
        binding.pinnedAppsSummary.text = getString(R.string.taskbar_settings_pinned_summary, pinnedCount)
        binding.clearPinnedAppsButton.isEnabled = pinnedCount > 0
        renderPinnedApps(settings.pinnedApps, theme)

        isBindingState = false
    }

    private fun renderPinnedApps(packageNames: List<String>, theme: NerfTheme) {
        pinnedPrimaryLabels.clear()
        pinnedSecondaryLabels.clear()
        pinnedActionButtons.clear()

        binding.pinnedAppsListContainer.removeAllViews()
        if (packageNames.isEmpty()) {
            val emptyState = TextView(this).apply {
                text = getString(R.string.taskbar_settings_pinned_empty)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.nerf_taskbar_empty_state_text_size))
                setPadding(0, dimensionPx(R.dimen.nerf_space_8), 0, 0)
            }
            binding.pinnedAppsListContainer.addView(emptyState)
            pinnedSecondaryLabels.add(emptyState)
            applyPinnedManagementTheme(theme)
            return
        }

        packageNames.forEachIndexed { index, packageName ->
            val row = createPinnedAppRow(index, packageName, packageNames.size)
            binding.pinnedAppsListContainer.addView(row)
        }
        applyPinnedManagementTheme(theme)
    }

    private fun createPinnedAppRow(index: Int, packageName: String, totalCount: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0,
                if (index == 0) dimensionPx(R.dimen.nerf_space_8) else dimensionPx(R.dimen.nerf_space_12),
                0,
                0
            )

            val title = TextView(context).apply {
                text = resolveLabel(packageName)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.nerf_taskbar_row_title_text_size))
            }
            addView(title)
            pinnedPrimaryLabels.add(title)

            val packageText = TextView(context).apply {
                text = packageName
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.nerf_taskbar_row_package_text_size))
                setPadding(0, dimensionPx(R.dimen.nerf_space_2), 0, 0)
            }
            addView(packageText)
            pinnedSecondaryLabels.add(packageText)

            val actions = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dimensionPx(R.dimen.nerf_space_8), 0, 0)
            }
            actions.addView(createRowButton(R.string.taskbar_settings_move_up).apply {
                isEnabled = index > 0
                setOnClickListener { movePinnedApp(packageName, -1) }
            })
            actions.addView(createRowButton(R.string.taskbar_settings_move_down).apply {
                isEnabled = index < totalCount - 1
                setOnClickListener { movePinnedApp(packageName, 1) }
            })
            actions.addView(createRowButton(R.string.taskbar_settings_remove_pin).apply {
                setOnClickListener { TaskbarController.removePinnedApp(packageName) }
            })
            addView(actions)
        }
    }

    private fun createRowButton(labelRes: Int): MaterialButton {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = getString(labelRes)
            isAllCaps = false
            insetTop = 0
            insetBottom = 0
            minimumHeight = dimensionPx(R.dimen.nerf_taskbar_row_button_min_height)
            minHeight = dimensionPx(R.dimen.nerf_taskbar_row_button_min_height)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dimensionPx(R.dimen.nerf_space_8)
            }
            pinnedActionButtons.add(this)
        }
    }

    private fun movePinnedApp(packageName: String, direction: Int) {
        val currentPinnedApps = TaskbarController.getPinnedApps().toMutableList()
        val currentIndex = currentPinnedApps.indexOf(packageName)
        if (currentIndex < 0) return
        val targetIndex = currentIndex + direction
        if (targetIndex !in currentPinnedApps.indices) return
        val movedPackage = currentPinnedApps.removeAt(currentIndex)
        currentPinnedApps.add(targetIndex, movedPackage)
        TaskbarController.savePinnedApps(currentPinnedApps)
    }

    private fun resolveLabel(packageName: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private fun updateTaskbarSettings(transform: TaskbarSettings.() -> TaskbarSettings) {
        TaskbarController.updateSettings(transform)
    }

    private fun transparencyFromPercent(percent: Int): Float {
        return percent / transparencyPercentScaleFloat
    }

    private fun transparencyToPercent(transparency: Float): Int {
        return (
            transparency
                .coerceIn(TRANSPARENCY_MIN, TRANSPARENCY_MAX)
                * transparencyPercentScaleFloat
            ).toInt()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private class SimpleSeekBarListener(
        private val onProgressChanged: (progress: Int, fromUser: Boolean) -> Unit,
        private val onStartTrackingTouch: () -> Unit = {},
        private val onStopTrackingTouch: (progress: Int) -> Unit = {}
    ) : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: android.widget.SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            onProgressChanged(progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
            onStartTrackingTouch()
        }

        override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
            onStopTrackingTouch(seekBar?.progress ?: return)
        }
    }

    private fun createThrottledSeekBarListener(
        onProgress: (Int) -> Unit,
        commitStep: Int,
        commitValue: (Int) -> Unit
    ): SimpleSeekBarListener {
        var lastCommittedProgress = 0
        var lastCommittedAt = 0L
        return SimpleSeekBarListener(
            onProgressChanged = { progress, fromUser ->
                onProgress(progress)
                if (!fromUser || isBindingState) return@SimpleSeekBarListener
                if (shouldCommitDragUpdate(progress, lastCommittedProgress, lastCommittedAt, commitStep)) {
                    commitValue(progress)
                    lastCommittedProgress = progress
                    lastCommittedAt = SystemClock.elapsedRealtime()
                }
            },
            onStartTrackingTouch = {
                lastCommittedAt = 0L
            },
            onStopTrackingTouch = { progress ->
                if (isBindingState) return@SimpleSeekBarListener
                commitValue(progress)
            }
        )
    }

    private fun shouldCommitDragUpdate(
        progress: Int,
        lastCommittedProgress: Int,
        lastCommittedAt: Long,
        commitStep: Int
    ): Boolean {
        val progressDelta = kotlin.math.abs(progress - lastCommittedProgress)
        val elapsed = SystemClock.elapsedRealtime() - lastCommittedAt
        return lastCommittedAt == 0L || progressDelta >= commitStep || elapsed >= dragUpdateThrottleMs
    }

    companion object {
        private const val TRANSPARENCY_MIN = 0f
        private const val TRANSPARENCY_MAX = 1f
        private val BACKGROUND_LABELS = mapOf(
            TaskbarBackgroundStyle.DARK to R.string.taskbar_settings_background_dark,
            TaskbarBackgroundStyle.LIGHT to R.string.taskbar_settings_background_light,
            TaskbarBackgroundStyle.TRANSPARENT to R.string.taskbar_settings_background_transparent
        )
        private val TASKBAR_BACKGROUND_OPTIONS = listOf(
            TaskbarBackgroundStyle.DARK,
            TaskbarBackgroundStyle.LIGHT,
            TaskbarBackgroundStyle.TRANSPARENT
        )
            .filter { it in TaskbarSettings.supportedBackgroundStyles }
            .map { style ->
                TaskbarBackgroundOption(
                    style = style,
                    labelRes = BACKGROUND_LABELS.getValue(style)
                )
            }

        fun createIntent(context: Context): Intent = Intent(context, TaskbarSettingsActivity::class.java)
    }

    private class ThemedSpinnerAdapter(
        context: Context,
        items: List<String>,
        initialTheme: NerfTheme
    ) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
        private var theme: NerfTheme = initialTheme

        init {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        fun updateTheme(theme: NerfTheme) {
            this.theme = theme
            notifyDataSetChanged()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).also(::bindView)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent).also(::bindView)
        }

        private fun bindView(view: View) {
            val textView = view as? TextView ?: return
            textView.setTextColor(theme.hudPanelTextPrimary)
            textView.setBackgroundColor(theme.windowBackground)
        }
    }

    private data class TaskbarBackgroundOption(
        val style: TaskbarBackgroundStyle,
        val labelRes: Int
    )

    private fun dimensionPx(dimenRes: Int): Int = resources.getDimensionPixelSize(dimenRes)
    private fun integerValue(integerRes: Int): Int = resources.getInteger(integerRes)
    private fun longIntegerValue(integerRes: Int): Long = resources.getInteger(integerRes).toLong()
}
