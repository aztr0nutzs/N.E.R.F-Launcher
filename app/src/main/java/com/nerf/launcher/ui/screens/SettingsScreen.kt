package com.nerf.launcher.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.launcher.theme.LauncherTheme
import com.nerf.launcher.util.AppConfig
import com.nerf.launcher.util.ConfigRepository
import com.nerf.launcher.util.IconPackManager
import com.nerf.launcher.util.ThemeRepository

// ─────────────────────────────────────────────────────────────────────────────
//  SettingsScreen
//
//  Compose-based settings surface. Reads AppConfig from ConfigRepository and
//  writes every change back through the same repository — ConfigRepository
//  remains the single source of truth.
//
//  All colors are sourced from LauncherTheme.colors so the screen
//  re-themes automatically when the user switches a theme without any
//  imperative applySettingsTheme() calls.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    config: AppConfig,
    onOpenTaskbarSettings: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val colors  = LauncherTheme.colors
    val context = LocalContext.current

    // Map LauncherColors to local semantic aliases — keep composable body readable.
    val bg      = colors.backgroundTop
    val accent  = colors.accentCyan
    val info    = colors.textPrimary
    val warn    = colors.textSecondary
    val surface = colors.panelOuter
    val border  = colors.frameLine.copy(alpha = 0.35f)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            SettingsHeader(
                accent  = accent,
                info    = info,
                warn    = warn,
                surface = surface,
                border  = border,
                onBack  = onNavigateBack
            )

            Spacer(Modifier.height(16.dp))

            // ── Theme ────────────────────────────────────────────────────────
            SettingsSection(
                label   = "THEME NODE",
                accent  = accent,
                surface = surface,
                border  = border
            ) {
                val options = ThemeRepository.allThemeNames
                SettingsSelectorRow(
                    title   = "Palette",
                    options = options,
                    current = config.themeName,
                    accent  = accent,
                    info    = info,
                    surface = surface,
                    border  = border
                ) { selected ->
                    ConfigRepository.get().updateTheme(selected)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Icon Pack ────────────────────────────────────────────────────
            val iconPacks = remember(context) { IconPackManager.getAvailablePacks(context) }
            val hasExtra  = iconPacks.size > 1
            SettingsSection(
                label   = "ICON NODE",
                accent  = accent,
                surface = surface,
                border  = border
            ) {
                if (hasExtra) {
                    SettingsSelectorRow(
                        title   = "Icon Pack",
                        options = iconPacks,
                        current = config.iconPack,
                        accent  = accent,
                        info    = info,
                        surface = surface,
                        border  = border
                    ) { selected ->
                        IconPackManager.setCurrentPack(context, selected)
                    }
                } else {
                    SettingsLockedRow(
                        label  = "Icon Pack",
                        note   = "SYSTEM ONLY",
                        accent = accent,
                        info   = info
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Glow Intensity ───────────────────────────────────────────────
            SettingsSection(
                label   = "GLOW FIELD",
                accent  = accent,
                surface = surface,
                border  = border
            ) {
                SettingsSliderRow(
                    title   = "Intensity",
                    value   = config.glowIntensity,
                    range   = 0f..1f,
                    display = { v -> "${(v * 100).toInt()}%" },
                    accent  = accent,
                    info    = info
                ) { v ->
                    ConfigRepository.get().updateGlowIntensity(v)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Animation Speed ──────────────────────────────────────────────
            SettingsSection(
                label   = "MOTION NODE",
                accent  = accent,
                surface = surface,
                border  = border
            ) {
                SettingsToggleRow(
                    title   = "Fast Animation",
                    checked = config.animationSpeedEnabled,
                    onLabel = "FAST",
                    offLabel = "NORMAL",
                    accent  = accent,
                    info    = info,
                    surface = surface,
                    border  = border
                ) { checked ->
                    ConfigRepository.get().updateAnimationSpeed(checked)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Grid Size ────────────────────────────────────────────────────
            SettingsSection(
                label   = "GRID DENSITY",
                accent  = accent,
                surface = surface,
                border  = border
            ) {
                val gridOptions = listOf(2, 3, 4, 5, 6)
                SettingsSelectorRow(
                    title   = "Columns",
                    options = gridOptions.map { it.toString() },
                    current = config.gridSize.toString(),
                    accent  = accent,
                    info    = info,
                    surface = surface,
                    border  = border
                ) { selected ->
                    selected.toIntOrNull()?.let { ConfigRepository.get().updateGridSize(it) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Taskbar CTA ──────────────────────────────────────────────────
            HudActionButton(
                label   = "OPEN TASKBAR TUNER",
                accent  = accent,
                surface = surface,
                border  = border,
                onClick = onOpenTaskbarSettings
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsHeader(
    accent: Color,
    info: Color,
    warn: Color,
    surface: Color,
    border: Color,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(surface)
            .border(0.5.dp, border, CutCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back chevron
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CutCornerShape(6.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(0.5.dp, accent.copy(alpha = 0.30f), CutCornerShape(6.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "←",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = accent
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text          = "CONTROL SETTINGS",
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 13.sp,
                letterSpacing = 1.8.sp,
                color         = info
            )
            Text(
                text          = "N.E.R.F.-OWNED LAUNCHER CALIBRATION",
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Normal,
                fontSize      = 8.sp,
                letterSpacing = 1.2.sp,
                color         = warn.copy(alpha = 0.75f)
            )
        }

        Spacer(Modifier.weight(1f))

        // Live accent dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )
    }
}

@Composable
private fun SettingsSection(
    label: String,
    accent: Color,
    surface: Color,
    border: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp))
            .background(surface)
            .border(0.5.dp, border, CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp))
    ) {
        // Section label strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text          = label,
                fontFamily    = FontFamily.Monospace,
                fontWeight    = FontWeight.Bold,
                fontSize      = 8.sp,
                letterSpacing = 1.4.sp,
                color         = accent
            )
        }

        // Content
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            content()
        }
    }
}

/** Single-row dropdown-style selector using pill chips. */
@Composable
private fun SettingsSelectorRow(
    title: String,
    options: List<String>,
    current: String,
    accent: Color,
    info: Color,
    surface: Color,
    border: Color,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text       = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize   = 9.sp,
            color      = info.copy(alpha = 0.70f)
        )
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment    = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == current
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) accent.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "chip_bg_$option"
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) accent.copy(alpha = 0.80f) else accent.copy(alpha = 0.25f),
                    animationSpec = tween(180),
                    label = "chip_border_$option"
                )
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(chipBg)
                        .border(0.5.dp, chipBorder, CutCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (!isSelected) onSelect(option) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = option.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 8.sp,
                        color      = if (isSelected) accent else info.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

/** Slider row for continuous float values. */
@Composable
private fun SettingsSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    accent: Color,
    info: Color,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize   = 9.sp,
                color      = info.copy(alpha = 0.70f)
            )
            Text(
                text       = display(value),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize   = 9.sp,
                color      = accent
            )
        }
        Slider(
            value       = value,
            onValueChange = onChange,
            valueRange  = range,
            modifier    = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor            = accent,
                activeTrackColor      = accent.copy(alpha = 0.80f),
                inactiveTrackColor    = accent.copy(alpha = 0.20f),
                activeTickColor       = Color.Transparent,
                inactiveTickColor     = Color.Transparent
            )
        )
    }
}

/** On/off toggle row implemented as two-state selector chips. */
@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onLabel: String,
    offLabel: String,
    accent: Color,
    info: Color,
    surface: Color,
    border: Color,
    onChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text       = title,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize   = 9.sp,
            color      = info.copy(alpha = 0.70f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(true to onLabel, false to offLabel).forEach { (value, label) ->
                val isSelected = checked == value
                val chipBg by animateColorAsState(
                    targetValue   = if (isSelected) accent.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(180),
                    label         = "toggle_chip_$label"
                )
                val chipBorder by animateColorAsState(
                    targetValue   = if (isSelected) accent.copy(alpha = 0.80f) else accent.copy(alpha = 0.25f),
                    animationSpec = tween(180),
                    label         = "toggle_border_$label"
                )
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(4.dp))
                        .background(chipBg)
                        .border(0.5.dp, chipBorder, CutCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (!isSelected) onChange(value) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize   = 8.sp,
                        color      = if (isSelected) accent else info.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

/** Row shown when a setting is present but locked/unavailable. */
@Composable
private fun SettingsLockedRow(
    label: String,
    note: String,
    accent: Color,
    info: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize   = 9.sp,
            color      = info.copy(alpha = 0.40f)
        )
        Text(
            text       = note,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize   = 8.sp,
            letterSpacing = 1.2.sp,
            color      = accent.copy(alpha = 0.35f)
        )
    }
}

/** HUD-styled full-width action button. */
@Composable
private fun HudActionButton(
    label: String,
    accent: Color,
    surface: Color,
    border: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(0.5.dp, accent.copy(alpha = 0.45f), CutCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = label,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            fontSize      = 10.sp,
            letterSpacing = 1.6.sp,
            color         = accent
        )
    }
}
