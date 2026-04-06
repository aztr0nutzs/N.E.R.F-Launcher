package com.nerf.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.nerf.launcher.R
import com.nerf.launcher.databinding.ActivityTaskbarSettingsBinding
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.NerfTheme
import com.nerf.launcher.util.TaskbarBackgroundStyle
import com.nerf.launcher.util.TaskbarSettings
import com.nerf.launcher.util.ThemeManager

class TaskbarSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskbarSettingsBinding
    private lateinit var backgroundStyleAdapter: ThemedSpinnerAdapter
    private var isBindingState = false
    private val pinnedPrimaryLabels = mutableListOf<TextView>()
    private val pinnedSecondaryLabels = mutableListOf<TextView>()
    private val pinnedActionButtons = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTheme = ThemeManager.resolveActiveTheme(this)

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

        binding.taskbarHeightSeekbar.max = MAX_HEIGHT_DP - MIN_HEIGHT_DP
        binding.taskbarHeightSeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(height = progress + MIN_HEIGHT_DP) }
        })

        binding.taskbarIconSizeSeekbar.max = MAX_ICON_SIZE_DP - MIN_ICON_SIZE_DP
        binding.taskbarIconSizeSeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(iconSize = progress + MIN_ICON_SIZE_DP) }
        })

        binding.taskbarTransparencySeekbar.max = 100
        binding.taskbarTransparencySeekbar.setOnSeekBarChangeListener(SimpleSeekBarListener { progress, fromUser ->
            if (!fromUser || isBindingState) return@SimpleSeekBarListener
            updateTaskbarSettings { copy(transparency = progress / 100f) }
        })

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
        ConfigRepository.get().config.observe(this) { config ->
            val theme = ThemeManager.resolveActiveTheme(
                context = this,
                themeName = config.themeName,
                glowIntensity = config.glowIntensity
            )
            applyTheme(theme)
            backgroundStyleAdapter.updateTheme(theme)
            bindTaskbarSettings(config.taskbarSettings, theme)
        }
    }

    private fun applyTheme(theme: NerfTheme) {
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(theme.windowBackground))
        binding.root.setBackgroundColor(theme.windowBackground)
        binding.toolbar.setBackgroundColor(theme.primary)
        binding.toolbar.setTitleTextColor(theme.hudPanelTextPrimary)
        binding.toolbar.navigationIcon?.mutate()?.setTint(theme.hudPanelTextPrimary)
        binding.toolbar.overflowIcon?.mutate()?.setTint(theme.hudPanelTextPrimary)
        applyTextColorRecursively(binding.root, theme.hudPanelTextPrimary)

        binding.clearPinnedAppsButton.let { button ->
            val fill = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf()
                ),
                intArrayOf(
                    theme.primary,
                    ColorUtils.setAlphaComponent(theme.primary, 0x61)
                )
            )
            val text = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf()
                ),
                intArrayOf(
                    theme.hudPanelTextPrimary,
                    ColorUtils.setAlphaComponent(theme.hudPanelTextPrimary, 0x61)
                )
            )
            button.backgroundTintList = fill
            button.setTextColor(text)
        }

        binding.taskbarEnabledSwitch.thumbTintList = createSwitchThumbTint(theme)
        binding.taskbarEnabledSwitch.trackTintList = createSwitchTrackTint(theme)

        listOf(
            binding.taskbarHeightSeekbar,
            binding.taskbarIconSizeSeekbar,
            binding.taskbarTransparencySeekbar
        ).forEach { seekBar ->
            seekBar.thumbTintList = android.content.res.ColorStateList.valueOf(theme.primary)
            seekBar.progressTintList = android.content.res.ColorStateList.valueOf(theme.primary)
            seekBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(theme.hudPanelTextSecondary, 0x66)
            )
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

    private fun applyTextColorRecursively(root: View, color: Int) {
        when (root) {
            is TextView -> root.setTextColor(color)
            is ViewGroup -> {
                for (index in 0 until root.childCount) {
                    applyTextColorRecursively(root.getChildAt(index), color)
                }
            }
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

        val heightDp = settings.height.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
        binding.taskbarHeightSeekbar.progress = heightDp - MIN_HEIGHT_DP
        binding.taskbarHeightValue.text = getString(R.string.taskbar_settings_height_value, heightDp)

        val iconSizeDp = settings.iconSize.coerceIn(MIN_ICON_SIZE_DP, MAX_ICON_SIZE_DP)
        binding.taskbarIconSizeSeekbar.progress = iconSizeDp - MIN_ICON_SIZE_DP
        binding.taskbarIconSizeValue.text = getString(R.string.taskbar_settings_icon_size_value, iconSizeDp)

        val transparencyPercent = (settings.transparency.coerceIn(0f, 1f) * 100).toInt()
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
                textSize = 13f
                setPadding(0, 8.dp, 0, 0)
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
            setPadding(0, if (index == 0) 8.dp else 12.dp, 0, 0)

            val title = TextView(context).apply {
                text = resolveLabel(packageName)
                textSize = 14f
            }
            addView(title)
            pinnedPrimaryLabels.add(title)

            val packageText = TextView(context).apply {
                text = packageName
                textSize = 12f
                setPadding(0, 2.dp, 0, 0)
            }
            addView(packageText)
            pinnedSecondaryLabels.add(packageText)

            val actions = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8.dp, 0, 0)
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
            minimumHeight = 36.dp
            minHeight = 36.dp
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8.dp
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private class SimpleSeekBarListener(
        private val onProgressChanged: (progress: Int, fromUser: Boolean) -> Unit
    ) : android.widget.SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: android.widget.SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            onProgressChanged(progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

        override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
    }

    companion object {
        private const val MIN_HEIGHT_DP = 40
        private const val MAX_HEIGHT_DP = 96
        private const val MIN_ICON_SIZE_DP = 24
        private const val MAX_ICON_SIZE_DP = 72
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

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
