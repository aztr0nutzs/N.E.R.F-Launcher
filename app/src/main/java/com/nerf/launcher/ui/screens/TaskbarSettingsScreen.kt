package com.nerf.launcher.ui.screens

import android.content.pm.PackageManager
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nerf.launcher.theme.LauncherTheme
import com.nerf.launcher.ui.TaskbarController
import com.nerf.launcher.util.TaskbarBackgroundStyle
import com.nerf.launcher.util.TaskbarSettings
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  TaskbarSettingsScreen
//
//  Compose-based replacement for the legacy ViewBinding TaskbarSettingsActivity
//  UI. Receives the live [TaskbarSettings] snapshot from the parent
//  (already collected from ConfigRepository.config StateFlow) and writes every
//  change back through [TaskbarController], which routes to
//  ConfigRepository.updateTaskbarSettings() — no parallel state, no
//  imperative applyTheme() calls, no manual tint setters.
//
//  All colors come from LauncherTheme.colors.* — theme changes recompose
//  automatically without any imperative update.
//
//  Settings preserved from legacy screen:
//    · Taskbar enabled toggle
//    · Height slider (40–96 dp)
//    · Icon size slider (24–72 dp)
//    · Transparency slider (0–100%)
//    · Background style selector (DARK / LIGHT / TRANSPARENT)
//    · Pinned apps list with move-up / move-down / remove per row
//    · Clear all pinned apps button
// ─────────────────────────────────────────────────────────────────────────────

private const val HEIGHT_MIN = 40
private const val HEIGHT_MAX = 96
private const val ICON_SIZE_MIN = 24
private const val ICON_SIZE_MAX = 72

@Composable
fun TaskbarSettingsScreen(
    settings: TaskbarSettings,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors  = LauncherTheme.colors
    val accent  = colors.accentCyan
    val surface = colors.panelOuter
    val bg      = colors.backgroundTop

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, colors.backgroundBottom)))
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(surface, surface.copy(alpha = 0.80f))))
                .border(0.5.dp, colors.frameLine.copy(alpha = 0.35f), RoundedCornerShape(0.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CutCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .border(0.5.dp, accent.copy(alpha = 0.35f), CutCornerShape(6.dp))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = accent)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text("TASKBAR CONFIG", fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    letterSpacing = 1.8.sp, color = colors.textPrimary)
                Text("HARDWARE RAIL  ·  N.E.R.F. SHELL",
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                    fontSize = 8.sp, letterSpacing = 1.2.sp,
                    color = colors.textSecondary.copy(alpha = 0.70f))
            }

            StatusBadge(
                text   = if (settings.enabled) "ON-LINE" else "OFF-LINE",
                active = settings.enabled,
                accent = accent
            )
        }

        // ── Scrollable body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Enabled toggle ────────────────────────────────────────────────
            SettingsPanel(label = "TASKBAR POWER", accent = accent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Enable taskbar", fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal, fontSize = 10.sp,
                            color = colors.textPrimary)
                        Text("Show or hide the bottom hardware rail",
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                            fontSize = 8.sp, color = colors.textSecondary.copy(alpha = 0.70f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked         = settings.enabled,
                        onCheckedChange = { checked ->
                            TaskbarController.updateSettings { copy(enabled = checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor       = accent,
                            checkedTrackColor       = accent.copy(alpha = 0.35f),
                            uncheckedThumbColor     = colors.textSecondary,
                            uncheckedTrackColor     = colors.panelInset,
                            checkedBorderColor      = accent.copy(alpha = 0.60f),
                            uncheckedBorderColor    = colors.frameLine.copy(alpha = 0.40f)
                        )
                    )
                }
            }

            // ── Height ────────────────────────────────────────────────────────
            SettingsPanel(label = "HEIGHT", accent = accent) {
                val heightFraction = (settings.height - HEIGHT_MIN).toFloat() /
                    (HEIGHT_MAX - HEIGHT_MIN).toFloat()
                SliderRow(
                    label         = "Rail height",
                    valueLabel    = "${settings.height} dp",
                    fraction      = heightFraction,
                    accent        = accent,
                    onValueChange = { f ->
                        val dp = (HEIGHT_MIN + f * (HEIGHT_MAX - HEIGHT_MIN)).roundToInt()
                            .coerceIn(HEIGHT_MIN, HEIGHT_MAX)
                        TaskbarController.updateSettings { copy(height = dp) }
                    }
                )
            }

            // ── Icon size ─────────────────────────────────────────────────────
            SettingsPanel(label = "ICON SIZE", accent = accent) {
                val iconFraction = (settings.iconSize - ICON_SIZE_MIN).toFloat() /
                    (ICON_SIZE_MAX - ICON_SIZE_MIN).toFloat()
                SliderRow(
                    label         = "Icon size",
                    valueLabel    = "${settings.iconSize} dp",
                    fraction      = iconFraction,
                    accent        = accent,
                    onValueChange = { f ->
                        val dp = (ICON_SIZE_MIN + f * (ICON_SIZE_MAX - ICON_SIZE_MIN)).roundToInt()
                            .coerceIn(ICON_SIZE_MIN, ICON_SIZE_MAX)
                        TaskbarController.updateSettings { copy(iconSize = dp) }
                    }
                )
            }

            // ── Transparency ──────────────────────────────────────────────────
            SettingsPanel(label = "TRANSPARENCY", accent = accent) {
                SliderRow(
                    label         = "Rail transparency",
                    valueLabel    = "${(settings.transparency * 100).roundToInt()}%",
                    fraction      = settings.transparency,
                    accent        = accent,
                    onValueChange = { f ->
                        TaskbarController.updateSettings { copy(transparency = f.coerceIn(0f, 1f)) }
                    }
                )
            }

            // ── Background style ──────────────────────────────────────────────
            SettingsPanel(label = "BACKGROUND STYLE", accent = accent) {
                val options = TaskbarBackgroundStyle.supportedStyles.toList()
                    .sortedBy { it.persistedValue }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { style ->
                        val selected = style == settings.backgroundStyle
                        StyleChip(
                            label    = style.name,
                            selected = selected,
                            accent   = accent,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                TaskbarController.updateSettings { copy(backgroundStyle = style) }
                            }
                        )
                    }
                }
            }

            // ── Pinned apps ───────────────────────────────────────────────────
            PinnedAppsPanel(
                pinnedApps = settings.pinnedApps,
                accent     = accent
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pinned Apps Panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedAppsPanel(
    pinnedApps: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    val colors  = LauncherTheme.colors
    val context = LocalContext.current

    SettingsPanel(label = "PINNED APPS  (${pinnedApps.size})", accent = accent) {
        if (pinnedApps.isEmpty()) {
            Text("No apps pinned to the taskbar rail.",
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                fontSize = 9.sp, color = colors.textSecondary.copy(alpha = 0.60f))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pinnedApps.forEachIndexed { index, packageName ->
                    val label = remember(packageName) {
                        runCatching {
                            val info = context.packageManager.getApplicationInfo(packageName, 0)
                            context.packageManager.getApplicationLabel(info).toString()
                        }.getOrDefault(packageName)
                    }

                    PinnedAppRow(
                        label       = label,
                        packageName = packageName,
                        canMoveUp   = index > 0,
                        canMoveDown = index < pinnedApps.lastIndex,
                        accent      = accent,
                        onMoveUp    = {
                            val list = pinnedApps.toMutableList()
                            list.add(index - 1, list.removeAt(index))
                            TaskbarController.savePinnedApps(list)
                        },
                        onMoveDown  = {
                            val list = pinnedApps.toMutableList()
                            list.add(index + 1, list.removeAt(index))
                            TaskbarController.savePinnedApps(list)
                        },
                        onRemove    = { TaskbarController.removePinnedApp(packageName) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Clear all button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CutCornerShape(4.dp))
                .background(
                    if (pinnedApps.isNotEmpty())
                        colors.danger.copy(alpha = 0.12f)
                    else
                        colors.panelInset
                )
                .border(
                    0.5.dp,
                    if (pinnedApps.isNotEmpty())
                        colors.danger.copy(alpha = 0.45f)
                    else
                        colors.frameLine.copy(alpha = 0.20f),
                    CutCornerShape(4.dp)
                )
                .then(
                    if (pinnedApps.isNotEmpty())
                        Modifier.clickable(remember { MutableInteractionSource() }, null) {
                            TaskbarController.clearPinnedApps()
                        }
                    else Modifier
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("CLEAR ALL PINS",
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                fontSize = 9.sp, letterSpacing = 1.sp,
                color = if (pinnedApps.isNotEmpty())
                    colors.danger.copy(alpha = 0.85f)
                else
                    colors.textSecondary.copy(alpha = 0.30f))
        }
    }
}

@Composable
private fun PinnedAppRow(
    label: String,
    packageName: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LauncherTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(4.dp))
            .background(colors.panelInset)
            .border(0.5.dp, colors.frameLine.copy(alpha = 0.22f), CutCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                fontSize = 9.sp, color = colors.textPrimary, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(packageName, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                fontSize = 7.sp, color = colors.textSecondary.copy(alpha = 0.60f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        // Action buttons
        listOf(
            Triple("↑", canMoveUp, onMoveUp),
            Triple("↓", canMoveDown, onMoveDown),
            Triple("✕", true, onRemove)
        ).forEach { (icon, enabled, action) ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CutCornerShape(4.dp))
                    .background(
                        if (enabled) accent.copy(alpha = 0.08f)
                        else colors.panelOuter
                    )
                    .border(
                        0.5.dp,
                        if (enabled) accent.copy(alpha = 0.30f)
                        else colors.frameLine.copy(alpha = 0.12f),
                        CutCornerShape(4.dp)
                    )
                    .then(
                        if (enabled) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = action)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (enabled) accent.copy(alpha = 0.80f)
                    else colors.textSecondary.copy(alpha = 0.25f))
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared building blocks
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsPanel(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    val colors = LauncherTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp))
            .background(colors.panelOuter)
            .border(
                0.5.dp, colors.frameLine.copy(alpha = 0.28f),
                CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
            )
    ) {
        // Panel header strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent.copy(alpha = 0.07f))
                .border(
                    width = 0.dp,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                fontSize = 8.sp, letterSpacing = 1.4.sp,
                color = accent.copy(alpha = 0.75f))
        }

        // Panel body
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    fraction: Float,
    accent: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit
) {
    val colors = LauncherTheme.colors
    Column {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal,
                fontSize = 9.sp, color = colors.textPrimary)
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.10f))
                    .border(0.5.dp, accent.copy(alpha = 0.30f), CutCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(valueLabel, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 8.sp, color = accent)
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value         = fraction.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            colors        = SliderDefaults.colors(
                thumbColor            = accent,
                activeTrackColor      = accent,
                inactiveTrackColor    = colors.frameLine.copy(alpha = 0.35f),
                activeTickColor       = accent.copy(alpha = 0f),
                inactiveTickColor     = accent.copy(alpha = 0f)
            ),
            modifier      = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StyleChip(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LauncherTheme.colors
    Box(
        modifier = modifier
            .clip(CutCornerShape(4.dp))
            .background(if (selected) accent.copy(alpha = 0.15f) else colors.panelInset)
            .border(
                0.5.dp,
                if (selected) accent.copy(alpha = 0.55f) else colors.frameLine.copy(alpha = 0.25f),
                CutCornerShape(4.dp)
            )
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 8.sp, letterSpacing = 0.8.sp,
            color = if (selected) accent else colors.textSecondary)
    }
}

@Composable
private fun StatusBadge(
    text: String,
    active: Boolean,
    accent: androidx.compose.ui.graphics.Color
) {
    val colors = LauncherTheme.colors
    val badgeColor = if (active) accent else colors.textSecondary
    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.10f))
            .border(0.5.dp, badgeColor.copy(alpha = 0.40f), CutCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            fontSize = 8.sp, color = badgeColor)
    }
}
