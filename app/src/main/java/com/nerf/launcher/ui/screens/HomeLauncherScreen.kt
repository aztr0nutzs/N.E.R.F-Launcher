package com.nerf.launcher.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nerf.launcher.state.*
import kotlin.math.*

// ─────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────
private object NToken {
    val bgBlack      = Color(0xFF030407)
    val shellDeep    = Color(0xFF070A0E)
    val shellDark    = Color(0xFF0C0F14)
    val gunmetal     = Color(0xFF12161C)
    val panelDark    = Color(0xFF0E1218)
    val panelMid     = Color(0xFF151C25)
    val panelEdge    = Color(0xFF1E2834)
    val bevelHigh    = Color(0xFF2C3A4A)
    val bevelLow     = Color(0xFF0A0D12)
    val screwRim     = Color(0xFF2A3545)
    val screwFace    = Color(0xFF1A2230)
    val seamLine     = Color(0xFF0F1620)
    val plateTop     = Color(0xFF1A2232)
    val cyan         = Color(0xFF00F0FF)
    val cyanMid      = Color(0xFF00AACC)
    val cyanDim      = Color(0xFF006E88)
    val cyanGlow     = Color(0x5500F0FF)
    val green        = Color(0xFF00FF88)
    val greenMid     = Color(0xFF00BB66)
    val greenDim     = Color(0xFF007744)
    val greenGlow    = Color(0x5500FF88)
    val magenta      = Color(0xFFFF00CC)
    val magentaMid   = Color(0xFFCC0099)
    val magentaDim   = Color(0xFF880066)
    val magentaGlow  = Color(0x55FF00CC)
    val amber        = Color(0xFFFFAA00)
    val amberMid     = Color(0xFFCC8800)
    val amberDim     = Color(0xFF886600)
    val amberGlow    = Color(0x55FFAA00)
    val blue         = Color(0xFF3366FF)
    val blueDim      = Color(0xFF112288)
    val blueGlow     = Color(0x553366FF)
    val white        = Color(0xFFDDEEFF)
    val dimText      = Color(0xFF4A5E72)
    val labelText    = Color(0xFF7A909F)

    fun sectorColor(s: ReactorSector) = when (s) {
        ReactorSector.SYS_NET_DIAG      -> cyan
        ReactorSector.STABILITY_MONITOR -> green
        ReactorSector.RECALIBRATION     -> amber
        ReactorSector.INTERFACE_CONFIG  -> magenta
        ReactorSector.NONE              -> dimText
    }
    fun sectorMid(s: ReactorSector) = when (s) {
        ReactorSector.SYS_NET_DIAG      -> cyanMid
        ReactorSector.STABILITY_MONITOR -> greenMid
        ReactorSector.RECALIBRATION     -> amberMid
        ReactorSector.INTERFACE_CONFIG  -> magentaMid
        ReactorSector.NONE              -> Color(0xFF1A2530)
    }
    fun sectorGlow(s: ReactorSector) = when (s) {
        ReactorSector.SYS_NET_DIAG      -> cyanGlow
        ReactorSector.STABILITY_MONITOR -> greenGlow
        ReactorSector.RECALIBRATION     -> amberGlow
        ReactorSector.INTERFACE_CONFIG  -> magentaGlow
        ReactorSector.NONE              -> Color.Transparent
    }
    fun dockColor(t: DockTile) = when (t) {
        DockTile.HUB     -> cyan
        DockTile.SCAN    -> green
        DockTile.MODULES -> amber
        DockTile.STORE   -> magenta
        DockTile.CONFIG  -> blue
    }
    fun dockGlow(t: DockTile) = when (t) {
        DockTile.HUB     -> cyanGlow
        DockTile.SCAN    -> greenGlow
        DockTile.MODULES -> amberGlow
        DockTile.STORE   -> magentaGlow
        DockTile.CONFIG  -> blueGlow
    }
}

// ─────────────────────────────────────────────────────────
// Polar hit-detection constants and helpers
// ─────────────────────────────────────────────────────────
private const val SECTOR_SWEEP = 90f
private const val SECTOR_GAP   = 5f

/** Maps each sector to its true angular start (same convention as Canvas drawArc: 0° = 3-o-clock). */
private val SECTOR_START_ANGLES = mapOf(
    ReactorSector.SYS_NET_DIAG      to -90f,
    ReactorSector.STABILITY_MONITOR to   0f,
    ReactorSector.RECALIBRATION     to  90f,
    ReactorSector.INTERFACE_CONFIG  to 180f
)

/**
 * Polar hit-test.  Returns the sector under [tapOffset] (canvas-local coords),
 * or null if the tap is inside the core, in a gap, or outside the ring.
 */
private fun hitTestSector(
    tapOffset: Offset,
    center: Offset,
    rInner: Float,
    rOuter: Float
): ReactorSector? {
    val dx   = tapOffset.x - center.x
    val dy   = tapOffset.y - center.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist < rInner || dist > rOuter) return null

    // atan2 → degrees in [0, 360)
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (deg < 0f) deg += 360f

    for ((sector, rawStart) in SECTOR_START_ANGLES) {
        val sa = ((rawStart + SECTOR_GAP / 2f) % 360f + 360f) % 360f
        val ea = sa + (SECTOR_SWEEP - SECTOR_GAP)
        val hit = if (ea <= 360f) {
            deg in sa..ea
        } else {
            deg >= sa || deg <= (ea - 360f)
        }
        if (hit) return sector
    }
    return null
}

// ─────────────────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────────────────
@Composable
fun HomeLauncherScreen(
    viewModel: LauncherViewModel = viewModel(),
    onLaunchApp: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(NToken.bgBlack)) {

        // Scanline + vignette background
        Canvas(Modifier.fillMaxSize()) {
            var y = 0f
            while (y < size.height) {
                drawLine(Color(0x08AABBCC), Offset(0f, y), Offset(size.width, y), 0.4f)
                y += 3f
            }
            var x = 0f
            while (x < size.width) {
                drawLine(Color(0x04AABBCC), Offset(x, 0f), Offset(x, size.height), 0.3f)
                x += 6f
            }
            drawRect(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, Color(0xCC000000)),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.maxDimension * 0.72f
                )
            )
            // Subtle top-edge ambient light leak
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(NToken.cyanDim.copy(0.04f), Color.Transparent),
                    startY = 0f, endY = size.height * 0.08f
                )
            )
        }

        OuterShellFrame {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))
                TopHeaderPlate(
                    title    = "NECORE 5  POWER",
                    subtitle = state.versionLabel,
                    pingMs   = state.pingMs
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    DiagnosticsPanel(Modifier.weight(1f), state.cpuLoad, state.thermalStatus, state.systemOnline)
                    NetworkSyncPanel(Modifier.weight(1f), state.signalStrength, state.neuralLinkPct, state.aiReady)
                }
                Spacer(Modifier.height(6.dp))
                SegmentedReactor(
                    modifier     = Modifier.fillMaxWidth().weight(1f),
                    activeSector = state.activeSector,
                    powerPct     = state.powerPct,
                    batteryPct   = state.batteryPct,
                    coreLabel    = state.coreLabel,
                    onSectorTap  = { viewModel.onEvent(LauncherEvent.SectorSelected(it)) },
                    onCoreTap    = { viewModel.onEvent(LauncherEvent.CoreTapped) }
                )
                Spacer(Modifier.height(5.dp))
                LowerStatusStrip(state = state)
                Spacer(Modifier.height(6.dp))
                LauncherDock(
                    activeTile = state.activeDockTile,
                    onTileTap  = {
                        viewModel.onEvent(LauncherEvent.DockTileSelected(it))
                        onLaunchApp(it.name)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        SideUtilityButtons(onSettings = { viewModel.onEvent(LauncherEvent.SettingsTapped) })
    }
}

// ─────────────────────────────────────────────────────────
// Outer shell frame
// ─────────────────────────────────────────────────────────
@Composable
private fun OuterShellFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .drawBehind {
                val seamY1 = size.height * 0.22f
                val seamY2 = size.height * 0.78f

                // Primary horizontal seams
                listOf(seamY1, seamY2).forEach { sy ->
                    drawLine(NToken.seamLine, Offset(24.dp.toPx(), sy), Offset(size.width - 24.dp.toPx(), sy), 1f)
                    drawLine(NToken.bevelHigh.copy(0.14f), Offset(24.dp.toPx(), sy + 1), Offset(size.width - 24.dp.toPx(), sy + 1), 0.5f)
                    drawLine(NToken.bevelLow.copy(0.5f), Offset(24.dp.toPx(), sy - 0.5f), Offset(size.width - 24.dp.toPx(), sy - 0.5f), 0.5f)
                }

                // Mid-panel sub-seam (~50%)
                val midY = size.height * 0.495f
                drawLine(NToken.seamLine.copy(0.8f), Offset(32.dp.toPx(), midY), Offset(size.width - 32.dp.toPx(), midY), 0.5f)
                drawLine(NToken.bevelHigh.copy(0.08f), Offset(32.dp.toPx(), midY + 0.8f), Offset(size.width - 32.dp.toPx(), midY + 0.8f), 0.4f)

                val cx2 = size.width / 2
                drawLine(NToken.seamLine, Offset(cx2, 32.dp.toPx()), Offset(cx2, 56.dp.toPx()), 0.6f)
                drawLine(NToken.seamLine, Offset(cx2, size.height - 56.dp.toPx()), Offset(cx2, size.height - 32.dp.toPx()), 0.6f)

                // Asymmetric vertical sub-panel lines
                val vx1 = size.width * 0.28f; val vx2 = size.width * 0.72f
                listOf(seamY1 to seamY1 + 18.dp.toPx(), seamY2 - 18.dp.toPx() to seamY2).forEach { (top, bot) ->
                    drawLine(NToken.seamLine.copy(0.6f), Offset(vx1, top + 2), Offset(vx1, bot - 2), 0.5f)
                    drawLine(NToken.seamLine.copy(0.6f), Offset(vx2, top + 2), Offset(vx2, bot - 2), 0.5f)
                }

                // L-bracket corner accents at each seam endpoint
                listOf(seamY1, seamY2).forEach { sy ->
                    val bx1 = 24.dp.toPx(); val bx2 = size.width - 24.dp.toPx()
                    val bLen = 8.dp.toPx(); val bDir = if (sy == seamY1) 1f else -1f
                    listOf(bx1 to 1f, bx2 to -1f).forEach { (bx, hDir) ->
                        drawLine(NToken.bevelHigh.copy(0.22f), Offset(bx, sy), Offset(bx + bLen * hDir, sy), 1f)
                        drawLine(NToken.bevelHigh.copy(0.22f), Offset(bx, sy), Offset(bx, sy + bDir * bLen), 1f)
                    }
                }

                // Corner screws
                val cr = 18.dp.toPx()
                listOf(
                    Offset(cr + 6, cr + 6),
                    Offset(size.width - cr - 6, cr + 6),
                    Offset(cr + 6, size.height - cr - 6),
                    Offset(size.width - cr - 6, size.height - cr - 6)
                ).forEach { o ->
                    drawCircle(
                        Brush.radialGradient(listOf(NToken.screwRim, NToken.screwFace, NToken.bevelLow), center = o, radius = 9.dp.toPx()),
                        9.dp.toPx(), o
                    )
                    drawCircle(NToken.screwFace, 5.5.dp.toPx(), o)
                    drawLine(NToken.bevelHigh.copy(0.55f), Offset(o.x - 3.5f, o.y), Offset(o.x + 3.5f, o.y), 1.3f)
                    drawLine(NToken.bevelHigh.copy(0.55f), Offset(o.x, o.y - 3.5f), Offset(o.x, o.y + 3.5f), 1.3f)
                    drawCircle(NToken.cyanDim.copy(0.15f), 10.dp.toPx(), o, style = Stroke(1f))
                    drawCircle(NToken.bevelHigh.copy(0.10f), 13.dp.toPx(), o, style = Stroke(0.5f))
                }

                // Amber side indicator strips
                val stripH = 28.dp.toPx(); val stripW = 4.dp.toPx()
                val stripY = size.height * 0.48f - stripH / 2
                listOf(2f, size.width - stripW - 2).forEach { sx ->
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.Transparent, NToken.amber, Color.Transparent), startY = stripY, endY = stripY + stripH),
                        topLeft = Offset(sx, stripY), size = Size(stripW, stripH),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    drawLine(NToken.bevelLow.copy(0.8f), Offset(sx + stripW / 2, stripY + 4), Offset(sx + stripW / 2, stripY + stripH - 4), 0.5f)
                }

                // Micro dot indicators — top cyan, bottom amber
                listOf(12.dp.toPx(), size.height - 12.dp.toPx()).forEachIndexed { ei, dy ->
                    listOf(0.38f, 0.50f, 0.62f).forEach { fx ->
                        drawCircle(
                            if (ei == 0) NToken.cyanDim.copy(0.30f) else NToken.amberDim.copy(0.25f),
                            1.5f, Offset(size.width * fx, dy)
                        )
                    }
                }
            }
            .border(1.5.dp, Brush.linearGradient(listOf(NToken.cyanDim, NToken.bevelHigh.copy(0.9f), NToken.magentaDim.copy(0.9f), NToken.bevelHigh.copy(0.6f), NToken.cyanDim)), RoundedCornerShape(18.dp))
            .padding(2.dp)
            .border(1.dp, Brush.linearGradient(listOf(NToken.bevelLow, NToken.panelEdge, NToken.bevelLow)), RoundedCornerShape(16.dp))
            .padding(1.5.dp)
            .border(1.dp, Brush.linearGradient(listOf(NToken.panelEdge, NToken.bevelHigh.copy(0.22f), NToken.panelEdge)), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(NToken.gunmetal, NToken.shellDark, NToken.shellDeep, NToken.panelDark)))
    ) { content() }
}

// ─────────────────────────────────────────────────────────
// Top header plate
// ─────────────────────────────────────────────────────────
@Composable
private fun TopHeaderPlate(title: String, subtitle: String, pingMs: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(52.dp)
            .drawBehind {
                drawRoundRect(
                    Brush.verticalGradient(listOf(NToken.plateTop, NToken.panelDark, NToken.shellDeep, NToken.panelMid)),
                    cornerRadius = CornerRadius(9.dp.toPx())
                )
                drawRoundRect(NToken.bevelHigh.copy(0.45f), topLeft = Offset.Zero, size = Size(size.width, 1.5f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1.5f), size = Size(size.width, 1.5f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawRoundRect(NToken.cyanMid.copy(0.7f), topLeft = Offset.Zero, size = Size(size.width, 2f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawLine(NToken.magentaDim.copy(0.6f), Offset(20.dp.toPx(), size.height - 0.5f), Offset(size.width - 20.dp.toPx(), size.height - 0.5f), 1f)
                drawLine(NToken.bevelLow, Offset(0f, 2.5f), Offset(size.width, 2.5f), 0.5f)
                drawLine(NToken.bevelHigh.copy(0.1f), Offset(0f, 3f), Offset(size.width, 3f), 0.5f)
                val innerSeamY = size.height * 0.56f
                drawLine(NToken.seamLine.copy(0.7f), Offset(14.dp.toPx(), innerSeamY), Offset(size.width - 14.dp.toPx(), innerSeamY), 0.5f)
                drawLine(NToken.bevelHigh.copy(0.06f), Offset(14.dp.toPx(), innerSeamY + 0.8f), Offset(size.width - 14.dp.toPx(), innerSeamY + 0.8f), 0.4f)
                val tH = 18.dp.toPx(); val tY = (size.height - tH) / 2
                drawLine(NToken.cyanMid.copy(0.6f), Offset(10.dp.toPx(), tY), Offset(10.dp.toPx(), tY + tH), 1.8f)
                drawLine(NToken.cyanDim.copy(0.25f), Offset(12.5f, tY + 3), Offset(12.5f, tY + tH - 3), 0.7f)
                drawLine(NToken.magentaDim.copy(0.6f), Offset(size.width - 10.dp.toPx(), tY), Offset(size.width - 10.dp.toPx(), tY + tH), 1.8f)
                drawLine(NToken.magentaDim.copy(0.2f), Offset(size.width - 12.5f, tY + 3), Offset(size.width - 12.5f, tY + tH - 3), 0.7f)
            },
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = NToken.white, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.5.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text(subtitle, color = NToken.dimText, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(6.dp))
            PingBadge(pingMs)
        }
    }
}

@Composable
private fun PingBadge(pingMs: Int) {
    Box(
        modifier = Modifier
            .drawBehind {
                drawRoundRect(NToken.magentaDim.copy(0.18f), cornerRadius = CornerRadius(5.dp.toPx()))
                drawRoundRect(NToken.bevelHigh.copy(0.25f), topLeft = Offset.Zero, size = Size(size.width, 1.2f), cornerRadius = CornerRadius(5.dp.toPx()))
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f), cornerRadius = CornerRadius(5.dp.toPx()))
                drawLine(NToken.magentaDim.copy(0.5f), Offset(1.5f, 4f), Offset(1.5f, size.height - 4f), 1.0f)
                drawCircle(NToken.magentaDim.copy(0.40f), 1.5f, Offset(4f, size.height - 4f))
                drawCircle(NToken.magentaDim.copy(0.40f), 1.5f, Offset(size.width - 4f, size.height - 4f))
            }
            .border(1.dp, NToken.magenta.copy(0.75f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PING", color = NToken.magentaDim, fontSize = 6.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Text("$pingMs ms", color = NToken.magenta, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────
// Shared mechanical panel shell
// ─────────────────────────────────────────────────────────
@Composable
private fun MechanicalPanel(
    modifier: Modifier,
    accentColor: Color,
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    Brush.verticalGradient(listOf(NToken.panelMid, NToken.panelDark, NToken.shellDeep)),
                    cornerRadius = CornerRadius(9.dp.toPx())
                )
                drawRoundRect(NToken.bevelHigh.copy(0.40f), topLeft = Offset.Zero, size = Size(size.width, 1.5f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawRoundRect(accentColor.copy(0.80f), topLeft = Offset.Zero, size = Size(size.width, 2.5f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawLine(NToken.bevelLow, Offset(4.dp.toPx(), 3.5f), Offset(size.width - 4.dp.toPx(), 3.5f), 0.7f)
                drawLine(NToken.bevelHigh.copy(0.10f), Offset(4.dp.toPx(), 4.2f), Offset(size.width - 4.dp.toPx(), 4.2f), 0.5f)
                val divY = 16.dp.toPx()
                drawLine(NToken.seamLine, Offset(0f, divY), Offset(size.width, divY), 0.6f)
                drawLine(NToken.bevelHigh.copy(0.07f), Offset(0f, divY + 0.8f), Offset(size.width, divY + 0.8f), 0.4f)
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f), cornerRadius = CornerRadius(9.dp.toPx()))
                drawRoundRect(
                    Brush.verticalGradient(listOf(accentColor.copy(0.55f), accentColor.copy(0.22f), Color.Transparent), startY = 0f, endY = size.height * 0.6f),
                    topLeft = Offset(0f, 2f), size = Size(2.5f, size.height * 0.55f), cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawCircle(accentColor.copy(0.45f), 2.5f, Offset(size.width - 8.dp.toPx(), 8.dp.toPx()))
                drawCircle(NToken.bevelLow, 1.2f, Offset(size.width - 8.dp.toPx(), 8.dp.toPx()))
                drawLine(NToken.panelEdge.copy(0.5f), Offset(size.width - 12.dp.toPx(), size.height - 1f), Offset(size.width, size.height - 1f), 0.5f)
                drawLine(NToken.panelEdge.copy(0.5f), Offset(size.width - 1f, size.height - 12.dp.toPx()), Offset(size.width - 1f, size.height), 0.5f)
            }
            .border(1.dp, Brush.linearGradient(listOf(accentColor.copy(0.5f), NToken.panelEdge, accentColor.copy(0.15f))), RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .padding(start = 8.dp, end = 8.dp, top = 9.dp, bottom = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.dp).background(accentColor, CircleShape))
            Spacer(Modifier.width(2.dp))
            Box(Modifier.size(2.dp).background(accentColor.copy(0.4f), CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(label, color = accentColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("[ ]", color = accentColor.copy(0.28f), fontSize = 6.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(2.dp))
            Text("//", color = accentColor.copy(0.38f), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
        Canvas(Modifier.fillMaxWidth().height(6.dp).padding(vertical = 2.dp)) {
            drawLine(accentColor.copy(0.20f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 0.7f)
            listOf(0.25f, 0.50f, 0.75f).forEach { fx ->
                drawLine(accentColor.copy(0.25f), Offset(size.width * fx, 0f), Offset(size.width * fx, size.height), 0.7f)
            }
        }
        content()
    }
}

@Composable
private fun DiagnosticsPanel(modifier: Modifier, cpuLoad: Float, thermalStatus: String, systemOnline: Boolean) {
    MechanicalPanel(modifier, NToken.cyan, "SYS DIAGNOSTICS") {
        PanelBarRow("CPU LOAD", cpuLoad, NToken.cyan)
        Spacer(Modifier.height(4.dp))
        PanelBarRow("THERMAL", 0.38f, NToken.green)
        Spacer(Modifier.height(4.dp))
        PanelBarRow("MEMORY", 0.71f, NToken.amber)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(on = systemOnline, color = NToken.green)
            Spacer(Modifier.width(4.dp))
            Text(if (systemOnline) "SYS:ONLINE" else "SYS:OFFLINE",
                color = if (systemOnline) NToken.green else NToken.amber,
                fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("THERM:$thermalStatus", color = NToken.dimText, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun NetworkSyncPanel(modifier: Modifier, signalPct: Float, neuralLinkPct: Int, aiReady: Boolean) {
    MechanicalPanel(modifier, NToken.magenta, "NETWORK SYNC") {
        PanelBarRow("SIGNAL", signalPct, NToken.magenta)
        Spacer(Modifier.height(4.dp))
        PanelBarRow("NEURAL LNK", neuralLinkPct / 100f, NToken.cyan)
        Spacer(Modifier.height(4.dp))
        PanelBarRow("SYNC RATE", 0.92f, NToken.green)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(on = aiReady, color = NToken.magenta)
            Spacer(Modifier.width(4.dp))
            Text("AI:${if (aiReady) "READY" else "INIT"}",
                color = if (aiReady) NToken.magenta else NToken.dimText,
                fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("LINK:$neuralLinkPct%", color = NToken.dimText, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PanelBarRow(label: String, value: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = NToken.labelText, fontSize = 6.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(52.dp))
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f).height(5.dp).drawBehind {
            drawRoundRect(NToken.shellDeep, cornerRadius = CornerRadius(2.5.dp.toPx()))
            drawRoundRect(NToken.panelEdge, topLeft = Offset.Zero, size = Size(size.width, 1f), cornerRadius = CornerRadius(2.5.dp.toPx()))
            // Segment markers at 25% intervals
            repeat(3) { s ->
                val mx = size.width * (s + 1) / 4f
                drawLine(NToken.panelEdge.copy(0.8f), Offset(mx, 1f), Offset(mx, size.height - 1f), 0.6f)
            }
        }) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).drawBehind {
                drawRoundRect(Brush.horizontalGradient(listOf(color.copy(0.45f), color, color.copy(0.95f))), cornerRadius = CornerRadius(2.5.dp.toPx()))
                drawRoundRect(Color.White.copy(0.12f), topLeft = Offset.Zero, size = Size(size.width, 1.2f), cornerRadius = CornerRadius(2.5.dp.toPx()))
                drawRoundRect(Color.Black.copy(0.18f), topLeft = Offset(0f, size.height - 1.2f), size = Size(size.width, 1.2f), cornerRadius = CornerRadius(2.5.dp.toPx()))
            })
        }
        Spacer(Modifier.width(4.dp))
        Text("${(value * 100).toInt()}%", color = color, fontSize = 6.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(22.dp))
    }
}

@Composable
private fun PulsingDot(on: Boolean, color: Color) {
    val inf = rememberInfiniteTransition(label = "dot")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), "dotA")
    Box(Modifier.size(8.dp).drawBehind {
        if (on) drawCircle(color.copy(a * 0.30f), size.minDimension / 2)
        drawCircle(if (on) color.copy(a) else NToken.dimText, size.minDimension / 2 * 0.65f)
    })
}

// ─────────────────────────────────────────────────────────
// Segmented reactor — full polar hit detection + press states
// ─────────────────────────────────────────────────────────
@Composable
private fun SegmentedReactor(
    modifier: Modifier,
    activeSector: ReactorSector,
    powerPct: Int,
    batteryPct: Int,
    coreLabel: String,
    onSectorTap: (ReactorSector) -> Unit,
    onCoreTap: () -> Unit
) {
    // ── Continuous animations ──
    val inf = rememberInfiniteTransition(label = "reactor")
    val rot1 by inf.animateFloat(  0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), "r1")
    val rot2 by inf.animateFloat(360f,   0f, infiniteRepeatable(tween(22000, easing = LinearEasing)), "r2")
    val rot3 by inf.animateFloat(  0f, 360f, infiniteRepeatable(tween(8000,  easing = LinearEasing)), "r3")
    // Slower, wider pulse (3 s) for housing ambient
    val pulse     by inf.animateFloat(0.50f, 1.00f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), "pulse")
    val corePulse by inf.animateFloat(0.40f, 0.95f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), "cPulse")
    // Slow sweep shimmer arc on outer rim
    val sweep by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), "sweep")

    // ── Touch / press state ──
    var pressedSector by remember { mutableStateOf<ReactorSector?>(null) }
    var corePressed   by remember { mutableStateOf(false) }

    // ── Per-sector breathing glow alpha ──
    val allSectors = listOf(
        ReactorSector.SYS_NET_DIAG,
        ReactorSector.STABILITY_MONITOR,
        ReactorSector.RECALIBRATION,
        ReactorSector.INTERFACE_CONFIG
    )
    val startAngles = listOf(-90f, 0f, 90f, 180f)

    // Build per-sector animated alpha — separate InfiniteTransition per sector avoids shared recompose
    val sectorGlowAlpha: Map<ReactorSector, Float> = allSectors.associate { sector ->
        val isActive = activeSector == sector
        val si = rememberInfiniteTransition(label = "sg_${sector.name}")
        val alpha by si.animateFloat(
            initialValue = if (isActive) 0.55f else 0.08f,
            targetValue  = if (isActive) 1.00f else 0.22f,
            animationSpec = infiniteRepeatable(
                tween(1800 + sector.ordinal * 180, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "sga_${sector.name}"
        )
        sector to alpha
    }

    // ── Core press scale spring ──
    val coreScale by animateFloatAsState(
        targetValue   = if (corePressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "coreScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // ── Reactor drawing canvas (no input) ──
        Canvas(modifier = Modifier
            .fillMaxWidth(0.90f)
            .aspectRatio(1f)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val R  = size.minDimension / 2f
            val c  = Offset(cx, cy)

            val rHousing     = R * 1.00f
            val rRim1        = R * 0.955f
            val rTickOuter   = R * 0.935f
            val rTickInner   = R * 0.900f
            val rSectorOuter = R * 0.880f
            val rSectorInner = R * 0.610f
            val rGear1Outer  = R * 0.600f
            val rGear1Inner  = R * 0.555f
            val rPowerArc    = R * 0.540f
            val rGear2Outer  = R * 0.505f
            val rGear2Inner  = R * 0.460f
            val rInnerPlate  = R * 0.445f
            val rCorePlate   = R * 0.310f
            val rCoreInner   = R * 0.230f
            val rCoreHub     = R * 0.150f

            // ── Housing body ──
            drawCircle(
                Brush.radialGradient(listOf(NToken.panelMid, NToken.gunmetal, NToken.shellDeep, NToken.shellDeep, NToken.bgBlack), center = c, radius = rHousing),
                rHousing, c
            )
            drawCircle(
                Brush.radialGradient(listOf(NToken.bevelHigh.copy(0.85f), NToken.panelEdge.copy(0.9f), NToken.panelDark, NToken.shellDeep), center = c, radius = rHousing),
                rHousing, c, style = Stroke(rHousing - rRim1)
            )
            drawCircle(NToken.bevelHigh.copy(0.30f), rRim1 - (rHousing - rRim1) * 0.45f, c, style = Stroke(0.7f))
            drawArc(NToken.bevelHigh.copy(0.75f), 195f, 110f, false, Offset(cx - rHousing, cy - rHousing), Size(rHousing * 2, rHousing * 2), style = Stroke((rHousing - rRim1) * 0.45f))
            drawArc(NToken.bevelLow.copy(0.6f),   15f,  100f, false, Offset(cx - rHousing, cy - rHousing), Size(rHousing * 2, rHousing * 2), style = Stroke((rHousing - rRim1) * 0.35f))
            drawCircle(NToken.bevelHigh.copy(0.85f), rHousing, c, style = Stroke(1.2.dp.toPx()))
            drawCircle(NToken.panelEdge, rRim1, c, style = Stroke(1.0f))
            drawCircle(NToken.bevelLow.copy(0.7f), rRim1 * 0.990f, c, style = Stroke(0.5f))
            // Pulsing ambient glow
            drawCircle(
                Brush.radialGradient(listOf(Color.Transparent, NToken.cyanGlow.copy(pulse * 0.28f), Color.Transparent), center = c, radius = rHousing),
                rHousing, c
            )
            // Slow energy sweep shimmer on outer rim
            drawArc(NToken.cyanMid.copy(0.10f * pulse), sweep, 60f, false,
                Offset(cx - rHousing + 2, cy - rHousing + 2), Size((rHousing - 2) * 2, (rHousing - 2) * 2),
                style = Stroke(3.dp.toPx()))

            // ── Stationary outer tick reference ring ──
            drawCircle(NToken.panelEdge.copy(0.55f), rTickOuter + 3f, c, style = Stroke(0.8f))
            drawCircle(NToken.bevelLow.copy(0.7f),  rTickOuter + 4.5f, c, style = Stroke(0.4f))

            // ── Rotating tick ring (60 ticks, 3-tier) ──
            withTransform({ rotate(rot2, c) }) {
                val count = 60
                repeat(count) { i ->
                    val major = i % 5 == 0; val mid = i % 5 == 2 || i % 5 == 3
                    val ang = Math.toRadians((i * 360.0 / count))
                    val r1  = rTickOuter
                    val r2  = if (major) rTickInner - 5 else if (mid) rTickInner - 1 else rTickInner + 2
                    val tc  = when { major -> NToken.cyanMid.copy(0.92f); mid -> NToken.panelEdge.copy(0.75f); else -> NToken.panelEdge.copy(0.40f) }
                    drawLine(tc,
                        Offset(cx + (cos(ang) * r1).toFloat(), cy + (sin(ang) * r1).toFloat()),
                        Offset(cx + (cos(ang) * r2).toFloat(), cy + (sin(ang) * r2).toFloat()),
                        if (major) 2.0f else if (mid) 1.0f else 0.7f)
                }
                drawCircle(NToken.bevelLow.copy(0.8f), rTickInner - 2f, c, style = Stroke(0.5f))
            }

            // ── Sector arcs ──
            allSectors.forEachIndexed { idx, sector ->
                val sa      = startAngles[idx] + SECTOR_GAP / 2f
                val sw      = SECTOR_SWEEP - SECTOR_GAP
                val isAct   = activeSector == sector
                val isPress = pressedSector == sector
                val col     = NToken.sectorColor(sector)
                val mid     = NToken.sectorMid(sector)
                val glow    = NToken.sectorGlow(sector)
                val gAlpha  = sectorGlowAlpha[sector] ?: 0.1f

                // Press-down flash (before confirm)
                if (isPress && !isAct) {
                    drawArc(glow.copy(0.50f), sa - 2f, sw + 4f, false,
                        Offset(cx - rSectorOuter - 6, cy - rSectorOuter - 6),
                        Size((rSectorOuter + 6) * 2, (rSectorOuter + 6) * 2),
                        style = Stroke(16f))
                    drawArc(col.copy(0.65f), sa, sw, false,
                        Offset(cx - rSectorOuter, cy - rSectorOuter),
                        Size(rSectorOuter * 2, rSectorOuter * 2),
                        style = Stroke(3.dp.toPx()))
                }

                // Active breathing glow layers
                if (isAct) {
                    repeat(3) { g ->
                        val expand = (g + 1) * 4f
                        drawArc(glow.copy(gAlpha * (0.38f - g * 0.10f)), sa - expand, sw + expand * 2, false,
                            Offset(cx - rSectorOuter - expand, cy - rSectorOuter - expand),
                            Size((rSectorOuter + expand) * 2, (rSectorOuter + expand) * 2),
                            style = Stroke(12f + g * 6f))
                    }
                    // Inner radial bloom
                    drawArc(glow.copy(gAlpha * 0.18f), sa, sw, true,
                        Offset(cx - rSectorOuter, cy - rSectorOuter),
                        Size(rSectorOuter * 2, rSectorOuter * 2))
                }

                // Wedge fill
                drawArc(
                    Brush.sweepGradient(listOf(
                        col.copy(if (isAct) 0.22f else 0.06f),
                        mid.copy(if (isAct) 0.32f else 0.10f),
                        col.copy(if (isAct) 0.22f else 0.06f)
                    ), center = c),
                    sa, sw, useCenter = true,
                    topLeft = Offset(cx - rSectorOuter, cy - rSectorOuter),
                    size = Size(rSectorOuter * 2, rSectorOuter * 2)
                )
                drawCircle(NToken.shellDeep, rSectorInner, c)

                // Outer arc stroke — scaled by state
                val strokeW = when { isAct -> 2.8.dp.toPx(); isPress -> 2.2.dp.toPx(); else -> 1.4.dp.toPx() }
                drawArc(col.copy(if (isAct || isPress) gAlpha else 0.40f), sa, sw, false,
                    Offset(cx - rSectorOuter, cy - rSectorOuter),
                    Size(rSectorOuter * 2, rSectorOuter * 2),
                    style = Stroke(strokeW))

                // Mid-ring sub-arc
                val rSectorMid = (rSectorOuter + rSectorInner) * 0.5f + rSectorOuter * 0.04f
                drawArc(col.copy(if (isAct) 0.40f else 0.14f), sa + 2f, sw - 4f, false,
                    Offset(cx - rSectorMid, cy - rSectorMid), Size(rSectorMid * 2, rSectorMid * 2), style = Stroke(0.7f))
                drawArc(col.copy(if (isAct) 0.22f else 0.10f), sa + 1f, sw - 2f, false,
                    Offset(cx - rSectorInner, cy - rSectorInner), Size(rSectorInner * 2, rSectorInner * 2), style = Stroke(1.0f))

                // Gap divider line
                val divAng = Math.toRadians((sa - SECTOR_GAP / 4f).toDouble())
                drawLine(NToken.panelEdge,
                    Offset(cx + (cos(divAng) * rSectorInner).toFloat(), cy + (sin(divAng) * rSectorInner).toFloat()),
                    Offset(cx + (cos(divAng) * rSectorOuter).toFloat(), cy + (sin(divAng) * rSectorOuter).toFloat()), 1f)

                // Radial ticks + mid-ring secondary ticks
                repeat(7) { t ->
                    val tAng = Math.toRadians((sa + sw / 8f * (t + 1)).toDouble())
                    val r1   = rSectorOuter * 0.975f
                    val r2   = rSectorOuter * (if (t == 3) 0.87f else 0.92f)
                    drawLine(col.copy(if (isAct) gAlpha else 0.28f),
                        Offset(cx + (cos(tAng) * r1).toFloat(), cy + (sin(tAng) * r1).toFloat()),
                        Offset(cx + (cos(tAng) * r2).toFloat(), cy + (sin(tAng) * r2).toFloat()),
                        if (t == 3) 1.8f else 0.9f)
                    val rm1 = rSectorMid * 1.02f; val rm2 = rSectorMid * 0.97f
                    drawLine(col.copy(if (isAct) 0.30f else 0.08f),
                        Offset(cx + (cos(tAng) * rm1).toFloat(), cy + (sin(tAng) * rm1).toFloat()),
                        Offset(cx + (cos(tAng) * rm2).toFloat(), cy + (sin(tAng) * rm2).toFloat()), 0.6f)
                }
            }

            // ── Outer gear ring (rot1) ──
            withTransform({ rotate(rot1, c) }) {
                val teeth = 32
                drawCircle(Brush.radialGradient(listOf(NToken.panelEdge, NToken.panelDark, NToken.shellDeep), center = c, radius = rGear1Outer), rGear1Outer, c)
                drawCircle(NToken.bevelLow.copy(0.9f),  rGear1Outer * 0.985f, c, style = Stroke(1.2f))
                drawCircle(NToken.bevelHigh.copy(0.18f), rGear1Outer * 0.96f, c, style = Stroke(0.5f))
                drawCircle(NToken.panelEdge, rGear1Outer, c, style = Stroke(1f))
                drawCircle(NToken.bevelHigh.copy(0.35f), rGear1Outer * 0.97f, c, style = Stroke(0.5f))
                repeat(teeth) { i ->
                    val ang = Math.toRadians((i * 360.0 / teeth)); val major = i % 4 == 0
                    drawLine(if (major) NToken.cyanDim.copy(0.7f) else NToken.panelEdge,
                        Offset(cx + (cos(ang) * rGear1Outer).toFloat(), cy + (sin(ang) * rGear1Outer).toFloat()),
                        Offset(cx + (cos(ang) * rGear1Inner).toFloat(), cy + (sin(ang) * rGear1Inner).toFloat()),
                        if (major) 2.8f else 1.8f)
                }
                drawCircle(NToken.bevelLow, rGear1Inner, c, style = Stroke(1.2f))
                drawCircle(NToken.panelEdge.copy(0.5f), rGear1Inner * 1.015f, c, style = Stroke(0.5f))
            }

            // ── Power arc (carved channel) ──
            drawCircle(NToken.bevelLow.copy(0.9f), rPowerArc, c, style = Stroke(8.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(NToken.shellDeep, NToken.panelDark), center = c, radius = rPowerArc),
                rPowerArc, c, style = Stroke(6.dp.toPx()))
            drawCircle(NToken.panelEdge.copy(0.4f), rPowerArc - 3.dp.toPx(), c, style = Stroke(0.6f))
            drawCircle(NToken.panelEdge.copy(0.4f), rPowerArc + 3.dp.toPx(), c, style = Stroke(0.6f))
            drawArc(Brush.sweepGradient(listOf(NToken.cyanDim.copy(0.7f), NToken.cyan, NToken.cyanMid), center = c),
                -90f, 360f * (powerPct / 100f), false,
                Offset(cx - rPowerArc, cy - rPowerArc), Size(rPowerArc * 2, rPowerArc * 2),
                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            val headAng = Math.toRadians((-90f + 360f * powerPct / 100f).toDouble())
            val headPt  = Offset(cx + (cos(headAng) * rPowerArc).toFloat(), cy + (sin(headAng) * rPowerArc).toFloat())
            drawCircle(NToken.cyanGlow.copy(0.5f), 7f, headPt)
            drawCircle(NToken.cyan, 4f, headPt)
            drawCircle(Color.White.copy(0.6f), 1.5f, headPt)
            val startAngRad = Math.toRadians(-90.0)
            drawCircle(NToken.cyanDim.copy(0.35f), 2.5f,
                Offset(cx + (cos(startAngRad) * rPowerArc).toFloat(), cy + (sin(startAngRad) * rPowerArc).toFloat()))

            // ── Inner gear ring (rot2 × 1.5) ──
            withTransform({ rotate(rot2 * 1.5f, c) }) {
                val teeth = 24
                drawCircle(Brush.radialGradient(listOf(NToken.gunmetal, NToken.shellDeep), center = c, radius = rGear2Outer), rGear2Outer, c)
                drawCircle(NToken.bevelHigh.copy(0.15f), rGear2Outer * 0.98f,  c, style = Stroke(0.5f))
                drawCircle(NToken.bevelLow.copy(0.8f),  rGear2Outer * 0.975f, c, style = Stroke(0.8f))
                repeat(teeth) { i ->
                    val ang = Math.toRadians((i * 360.0 / teeth)); val major = i % 6 == 0
                    drawLine(if (major) NToken.cyanDim.copy(0.5f) else NToken.panelEdge,
                        Offset(cx + (cos(ang) * rGear2Outer).toFloat(), cy + (sin(ang) * rGear2Outer).toFloat()),
                        Offset(cx + (cos(ang) * rGear2Inner).toFloat(), cy + (sin(ang) * rGear2Inner).toFloat()),
                        if (major) 2f else 1.6f)
                }
                drawCircle(NToken.cyanDim.copy(0.30f), rGear2Outer, c, style = Stroke(1.2f))
                drawCircle(NToken.bevelHigh.copy(0.25f), rGear2Inner,  c, style = Stroke(0.8f))
                drawCircle(NToken.bevelLow.copy(0.7f),   rGear2Inner * 0.985f, c, style = Stroke(0.5f))
            }

            // ── Inner housing plate ──
            drawCircle(Brush.radialGradient(listOf(NToken.panelMid, NToken.panelMid, NToken.panelDark, NToken.shellDeep), center = c, radius = rInnerPlate), rInnerPlate, c)
            drawCircle(NToken.bevelHigh.copy(0.55f), rInnerPlate, c, style = Stroke(2.0f))
            drawCircle(NToken.bevelLow.copy(0.9f),   rInnerPlate * 0.978f, c, style = Stroke(0.6f))
            drawCircle(NToken.panelEdge.copy(0.6f),  rInnerPlate * 0.955f, c, style = Stroke(0.4f))
            repeat(8) { i ->
                val ang = Math.toRadians((i * 45.0)); val dr = rInnerPlate * 0.965f
                drawCircle(NToken.cyanDim.copy(if (i % 2 == 0) 0.35f else 0.12f),
                    if (i % 2 == 0) 1.8f else 1.0f,
                    Offset(cx + (cos(ang) * dr).toFloat(), cy + (sin(ang) * dr).toFloat()))
            }

            // ── Orbit ring — dual track ──
            withTransform({ rotate(rot3, c) }) {
                val orbitR = (rCorePlate + rInnerPlate) / 2f
                drawCircle(NToken.cyanDim.copy(0.10f), orbitR + 4f, c, style = Stroke(0.5f))
                drawCircle(NToken.cyanDim.copy(0.15f), orbitR,      c, style = Stroke(1.2f))
                drawCircle(NToken.cyanDim.copy(0.08f), orbitR - 4f, c, style = Stroke(0.5f))
                repeat(6) { i ->
                    val ang   = Math.toRadians((i * 60.0))
                    val r     = when (i) { 0 -> orbitR; 1 -> orbitR - 1f; 5 -> orbitR + 1f; else -> orbitR }
                    val dotR  = when (i) { 0 -> 4.0f; 1 -> 2.8f; 5 -> 2.2f; else -> 1.8f }
                    val alpha = when (i) { 0 -> 1.0f; 1 -> 0.65f; 2 -> 0.40f; 3 -> 0.25f; 4 -> 0.35f; else -> 0.55f }
                    val pt    = Offset(cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat())
                    if (i == 0) drawCircle(NToken.cyanGlow.copy(0.45f), dotR + 3f, pt)
                    drawCircle(NToken.cyan.copy(alpha), dotR, pt)
                }
            }

            // ── Core plate ──
            drawCircle(Brush.radialGradient(
                listOf(Color(0xFF0F2848), Color(0xFF071428), Color(0xFF040C1A), NToken.bgBlack),
                center = c, radius = rCorePlate), rCorePlate, c)
            drawCircle(NToken.cyan.copy(0.70f * pulse), rCorePlate, c, style = Stroke(2.5.dp.toPx()))
            drawCircle(NToken.cyanDim.copy(0.45f), rCorePlate * 0.90f, c, style = Stroke(1.0f))
            drawCircle(NToken.bevelLow.copy(0.9f), rCorePlate * 0.86f, c, style = Stroke(0.5f))
            repeat(4) { i ->
                val ang = Math.toRadians((i * 90.0 + 45.0))
                drawLine(NToken.cyanDim.copy(0.18f),
                    Offset(cx + (cos(ang) * rCorePlate * 0.82f).toFloat(), cy + (sin(ang) * rCorePlate * 0.82f).toFloat()),
                    Offset(cx + (cos(ang) * rCorePlate * 0.52f).toFloat(), cy + (sin(ang) * rCorePlate * 0.52f).toFloat()), 0.8f)
            }
            drawCircle(Brush.radialGradient(listOf(NToken.cyan.copy(corePulse * 0.55f), Color.Transparent), center = c, radius = rCorePlate), rCorePlate, c)

            // ── Core inner ring ──
            drawCircle(Brush.radialGradient(
                listOf(Color(0xFF1E4870), Color(0xFF0C2040), Color(0xFF061020), NToken.bgBlack),
                center = c, radius = rCoreInner), rCoreInner, c)
            drawCircle(NToken.cyanMid.copy(0.55f), rCoreInner, c, style = Stroke(2.0f))
            drawCircle(NToken.bevelLow.copy(0.8f), rCoreInner * 0.975f, c, style = Stroke(0.6f))
            withTransform({ rotate(-rot3 * 0.5f, c) }) {
                repeat(12) { i ->
                    val ang = Math.toRadians((i * 30.0)); val major = i % 3 == 0
                    val r1  = rCoreInner * 0.98f; val r2 = rCoreInner * (if (major) 0.80f else 0.89f)
                    drawLine(if (major) NToken.cyan.copy(0.80f) else NToken.cyanDim.copy(0.35f),
                        Offset(cx + (cos(ang) * r1).toFloat(), cy + (sin(ang) * r1).toFloat()),
                        Offset(cx + (cos(ang) * r2).toFloat(), cy + (sin(ang) * r2).toFloat()),
                        if (major) 1.8f else 0.8f)
                }
            }

            // ── Center hub — press-brightness boost ──
            val hubB = if (corePressed) 1.0f else corePulse
            drawCircle(Brush.radialGradient(
                listOf(NToken.cyan.copy(hubB * 0.75f), Color(0xFF0C2848), Color(0xFF060E1C), NToken.bgBlack),
                center = c, radius = rCoreHub * 1.8f), rCoreHub, c)
            drawCircle(NToken.cyan.copy(0.85f * hubB), rCoreHub, c, style = Stroke(2.0f))
            drawCircle(NToken.cyanMid.copy(0.40f * hubB), rCoreHub * 0.72f, c, style = Stroke(1.2f))
            drawCircle(NToken.cyan.copy(hubB * 0.9f), rCoreHub * 0.22f, c)
            // Press-ring flash rings
            if (corePressed) {
                drawCircle(NToken.cyanGlow.copy(0.55f), rCoreHub * 2.2f, c, style = Stroke(2.dp.toPx()))
                drawCircle(NToken.cyanGlow.copy(0.25f), rCoreHub * 3.0f, c, style = Stroke(3.dp.toPx()))
            }
        }

        // ── Touch overlay — polar hit detection ──
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1f)
                .pointerInput(activeSector) {
                    detectTapGestures(
                        onPress = { tapOffset ->
                            val cxP = size.width / 2f; val cyP = size.height / 2f
                            val RP  = size.minDimension / 2f
                            val centerP = Offset(cxP, cyP)
                            val rSO = RP * 0.880f; val rSI = RP * 0.610f; val rHub = RP * 0.150f * 1.8f
                            val dx = tapOffset.x - cxP; val dy = tapOffset.y - cyP
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= rHub) corePressed = true
                            else pressedSector = hitTestSector(tapOffset, centerP, rSI, rSO)
                            tryAwaitRelease()
                            corePressed = false; pressedSector = null
                        },
                        onTap = { tapOffset ->
                            val cxT = size.width / 2f; val cyT = size.height / 2f
                            val RT  = size.minDimension / 2f
                            val centerT = Offset(cxT, cyT)
                            val rSO = RT * 0.880f; val rSI = RT * 0.610f; val rHub = RT * 0.150f * 1.8f
                            val dx = tapOffset.x - cxT; val dy = tapOffset.y - cyT
                            val dist = sqrt(dx * dx + dy * dy)
                            when {
                                dist <= rHub -> onCoreTap()
                                else -> hitTestSector(tapOffset, centerT, rSI, rSO)?.let { onSectorTap(it) }
                            }
                        }
                    )
                }
        )

        // ── Label overlays — display only, no input ──
        Box(modifier = Modifier.fillMaxWidth(0.90f).aspectRatio(1f)) {
            SectorLabel(Modifier.align(Alignment.TopCenter).fillMaxWidth(0.62f).fillMaxHeight(0.40f),
                ReactorSector.SYS_NET_DIAG, "SYS / NET / DIAG", activeSector == ReactorSector.SYS_NET_DIAG)
            SectorLabel(Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.40f).fillMaxHeight(0.52f),
                ReactorSector.STABILITY_MONITOR, "STABILITY\nMONITOR", activeSector == ReactorSector.STABILITY_MONITOR)
            SectorLabel(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.62f).fillMaxHeight(0.40f),
                ReactorSector.RECALIBRATION, "RE-CALIBRATION", activeSector == ReactorSector.RECALIBRATION)
            SectorLabel(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.40f).fillMaxHeight(0.52f),
                ReactorSector.INTERFACE_CONFIG, "INTERFACE\nCONFIG", activeSector == ReactorSector.INTERFACE_CONFIG)

            // Core tap label — scales with press spring
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .scale(coreScale),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("N", color = NToken.cyan, fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Text("CORE", color = NToken.cyanMid, fontSize = 5.5.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 2.5.sp)
                    Spacer(Modifier.height(1.dp))
                    Text("v3.1", color = NToken.cyanDim.copy(0.55f), fontSize = 4.5.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
            }
        }
    }
}

/**
 * Pure display label for a reactor sector.
 * No input handling — touch is managed by the polar pointerInput overlay above.
 */
@Composable
private fun SectorLabel(
    modifier: Modifier,
    sector: ReactorSector,
    label: String,
    isActive: Boolean
) {
    val col = NToken.sectorColor(sector)
    val inf = rememberInfiniteTransition(label = "sl_${sector.name}")
    val labelPulse by inf.animateFloat(
        0.72f, 1.0f,
        infiniteRepeatable(tween(1800 + sector.ordinal * 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "la_${sector.name}"
    )
    Box(
        modifier = modifier.drawBehind {
            if (isActive) {
                drawRect(Brush.radialGradient(
                    listOf(col.copy(0.06f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.maxDimension * 0.8f
                ))
                val bLen = 6.dp.toPx()
                // Active corner brackets
                drawLine(col.copy(0.40f), Offset(0f, 0f), Offset(bLen, 0f), 1.0f)
                drawLine(col.copy(0.40f), Offset(0f, 0f), Offset(0f, bLen), 1.0f)
                drawLine(col.copy(0.40f), Offset(size.width, size.height), Offset(size.width - bLen, size.height), 1.0f)
                drawLine(col.copy(0.40f), Offset(size.width, size.height), Offset(size.width, size.height - bLen), 1.0f)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color     = if (isActive) col.copy(labelPulse) else NToken.dimText.copy(0.50f),
            fontSize  = 7.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────
// Lower status strip
// ─────────────────────────────────────────────────────────
@Composable
private fun LowerStatusStrip(state: LauncherUiState) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        MiniStatusTile(Modifier.weight(1f), "POWER",   "${state.powerPct}%",                  NToken.cyan)
        MiniStatusTile(Modifier.weight(1f), "BATTERY", "${state.batteryPct}%",                NToken.green)
        MiniStatusTile(Modifier.weight(1f), "AI",      if (state.aiReady) "ACTIVE" else "INIT", NToken.magenta)
        MiniStatusTile(Modifier.weight(1f), "SECTOR",
            when (state.activeSector) {
                ReactorSector.NONE              -> "IDLE"
                ReactorSector.SYS_NET_DIAG      -> "SYS"
                ReactorSector.STABILITY_MONITOR -> "STAB"
                ReactorSector.RECALIBRATION     -> "RECAL"
                ReactorSector.INTERFACE_CONFIG  -> "IFACE"
            },
            NToken.sectorColor(state.activeSector)
        )
    }
}

@Composable
private fun MiniStatusTile(modifier: Modifier, label: String, value: String, color: Color) {
    Column(
        modifier = modifier.drawBehind {
            drawRoundRect(Brush.verticalGradient(listOf(NToken.panelMid, NToken.panelDark)), cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(color.copy(0.75f), topLeft = Offset.Zero, size = Size(size.width, 2f), cornerRadius = CornerRadius(6.dp.toPx()))
            drawLine(NToken.bevelHigh.copy(0.22f), Offset(2f, 2.5f), Offset(size.width - 2f, 2.5f), 0.5f)
            drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f), cornerRadius = CornerRadius(6.dp.toPx()))
            drawRoundRect(color.copy(0.08f), topLeft = Offset(2f, size.height * 0.4f), size = Size(size.width - 4f, size.height * 0.55f), cornerRadius = CornerRadius(4.dp.toPx()))
            drawLine(color.copy(0.30f), Offset(2f, 4f), Offset(2f, size.height - 4f), 0.8f)
            drawLine(color.copy(0.20f), Offset(size.width / 2 - 3f, size.height - 2f), Offset(size.width / 2 + 3f, size.height - 2f), 0.7f)
        }
        .border(1.dp, Brush.linearGradient(listOf(color.copy(0.45f), NToken.panelEdge, color.copy(0.2f))), RoundedCornerShape(6.dp))
        .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = NToken.dimText, fontSize = 6.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
        Text(value, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────
// Launcher dock
// ─────────────────────────────────────────────────────────
private data class DockTileData(val tile: DockTile, val symbol: String, val label: String)
private val DOCK_TILES = listOf(
    DockTileData(DockTile.HUB,     "⬡", "HUB"),
    DockTileData(DockTile.SCAN,    "◎", "SCAN"),
    DockTileData(DockTile.MODULES, "⊞", "MODULES"),
    DockTileData(DockTile.STORE,   "★", "STORE"),
    DockTileData(DockTile.CONFIG,  "⚙", "CONFIG")
)

@Composable
private fun LauncherDock(activeTile: DockTile, onTileTap: (DockTile) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            .drawBehind {
                drawRoundRect(Brush.verticalGradient(listOf(NToken.panelMid, NToken.panelDark, NToken.gunmetal, NToken.shellDeep, NToken.bgBlack)), cornerRadius = CornerRadius(14.dp.toPx()))
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, 1.5f), size = Size(size.width, 5f), cornerRadius = CornerRadius(14.dp.toPx()))
                drawRoundRect(NToken.bevelHigh.copy(0.65f), topLeft = Offset.Zero, size = Size(size.width, 2f), cornerRadius = CornerRadius(14.dp.toPx()))
                drawLine(Brush.horizontalGradient(listOf(Color.Transparent, NToken.cyanMid.copy(0.9f), NToken.magentaMid.copy(0.9f), Color.Transparent)),
                    start = Offset(20.dp.toPx(), 1f), end = Offset(size.width - 20.dp.toPx(), 1f), strokeWidth = 1.8f)
                drawRoundRect(NToken.panelEdge.copy(0.6f), topLeft = Offset(6.dp.toPx(), 5.dp.toPx()),
                    size = Size(size.width - 12.dp.toPx(), size.height - 10.dp.toPx()), cornerRadius = CornerRadius(10.dp.toPx()), style = Stroke(1f))
                drawRoundRect(NToken.bevelLow.copy(0.7f), topLeft = Offset(8.dp.toPx(), 7.dp.toPx()),
                    size = Size(size.width - 16.dp.toPx(), size.height - 14.dp.toPx()), cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(0.5f))
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1.5f), size = Size(size.width, 1.5f), cornerRadius = CornerRadius(14.dp.toPx()))
                drawLine(NToken.bevelHigh.copy(0.12f), Offset(16.dp.toPx(), size.height - 4.dp.toPx()), Offset(size.width - 16.dp.toPx(), size.height - 4.dp.toPx()), 0.5f)
                val screwY = size.height / 2
                listOf(10.dp.toPx(), size.width - 10.dp.toPx()).forEach { sx ->
                    drawCircle(NToken.bevelLow.copy(0.8f), 5.5.dp.toPx(), Offset(sx, screwY))
                    drawCircle(NToken.screwRim, 4.dp.toPx(), Offset(sx, screwY))
                    drawCircle(NToken.screwFace, 2.5.dp.toPx(), Offset(sx, screwY))
                    drawLine(NToken.bevelHigh.copy(0.5f), Offset(sx - 1.5f, screwY), Offset(sx + 1.5f, screwY), 0.9f)
                    drawLine(NToken.bevelHigh.copy(0.5f), Offset(sx, screwY - 1.5f), Offset(sx, screwY + 1.5f), 0.9f)
                }
                val tileW = (size.width - 28.dp.toPx()) / 5f
                val divTop = 10.dp.toPx(); val divBottom = size.height - 10.dp.toPx()
                repeat(4) { i ->
                    val dx = 14.dp.toPx() + tileW * (i + 1)
                    drawLine(NToken.seamLine, Offset(dx, divTop), Offset(dx, divBottom), 0.8f)
                    drawLine(NToken.bevelHigh.copy(0.08f), Offset(dx + 0.8f, divTop), Offset(dx + 0.8f, divBottom), 0.5f)
                }
            }
            .border(1.5.dp, Brush.linearGradient(listOf(NToken.bevelHigh.copy(0.6f), NToken.cyanDim.copy(0.40f), NToken.bevelHigh.copy(0.25f), NToken.magentaDim.copy(0.40f), NToken.bevelHigh.copy(0.6f))), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DOCK_TILES.forEach { data -> DockTileButton(data, data.tile == activeTile) { onTileTap(data.tile) } }
        }
    }
}

@Composable
private fun DockTileButton(data: DockTileData, isActive: Boolean, onTap: () -> Unit) {
    val col  = NToken.dockColor(data.tile)
    val glow = NToken.dockGlow(data.tile)
    val inf  = rememberInfiniteTransition(label = "dock_${data.tile.name}")
    val pAlpha by inf.animateFloat(
        if (isActive) 0.55f else 0.12f,
        if (isActive) 1.00f else 0.28f,
        infiniteRepeatable(tween(1500 + data.tile.ordinal * 200), RepeatMode.Reverse),
        "dA_${data.tile.name}"
    )

    var isPressed by remember { mutableStateOf(false) }
    val tileScale by animateFloatAsState(
        targetValue   = if (isPressed) 0.91f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label         = "tileScale_${data.tile.name}"
    )

    Column(
        modifier = Modifier
            .width(56.dp)
            .scale(tileScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap   = { onTap() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(52.dp).drawBehind {
                val fillTop = when {
                    isPressed -> col.copy(0.35f)
                    isActive  -> col.copy(0.22f)
                    else      -> NToken.panelMid
                }
                drawRoundRect(Brush.verticalGradient(listOf(fillTop, if (isActive || isPressed) NToken.panelDark else NToken.gunmetal, NToken.shellDeep)),
                    cornerRadius = CornerRadius(11.dp.toPx()))
                drawRoundRect(NToken.bevelHigh.copy(if (isActive) 0.6f else 0.30f), topLeft = Offset.Zero, size = Size(size.width, 2f), cornerRadius = CornerRadius(11.dp.toPx()))
                drawRoundRect(NToken.bevelLow.copy(0.9f), topLeft = Offset(2.5f, 2.5f), size = Size(size.width - 5f, size.height - 5f), cornerRadius = CornerRadius(9.dp.toPx()), style = Stroke(0.7f))
                if (isActive || isPressed) {
                    val ga = if (isPressed) (pAlpha * 1.3f).coerceAtMost(1f) else pAlpha
                    repeat(if (isPressed) 3 else 2) { g ->
                        val exp = (g + 1) * 3.5f
                        drawRoundRect(glow.copy((ga * (0.45f - g * 0.12f)).coerceAtMost(1f)),
                            topLeft = Offset(-exp, -exp),
                            size    = Size(size.width + exp * 2, size.height + exp * 2),
                            cornerRadius = CornerRadius((11 + g * 2).dp.toPx()))
                    }
                    drawRoundRect(col.copy(pAlpha * if (isPressed) 0.30f else 0.18f),
                        topLeft = Offset(3f, size.height * 0.45f),
                        size    = Size(size.width - 6f, size.height * 0.50f),
                        cornerRadius = CornerRadius(7.dp.toPx()))
                }
                drawRoundRect(NToken.bevelLow, topLeft = Offset(0f, size.height - 1.5f), size = Size(size.width, 1.5f), cornerRadius = CornerRadius(11.dp.toPx()))
                drawRoundRect(col.copy(if (isActive) pAlpha * 0.35f else 0.07f), topLeft = Offset(2f, 2f),
                    size = Size(size.width - 4f, size.height - 4f), cornerRadius = CornerRadius(9.dp.toPx()), style = Stroke(1f))
                drawCircle(col.copy(if (isActive) pAlpha else 0.22f), 2.2f, Offset(7.5f, 7.5f))
                drawCircle(col.copy(if (isActive) pAlpha * 0.4f else 0.08f), 1.5f, Offset(size.width - 7.5f, size.height - 7.5f))
            }
            .border(
                if (isActive) 1.5.dp else 1.dp,
                Brush.linearGradient(listOf(col.copy(pAlpha), col.copy(pAlpha * 0.40f), col.copy(pAlpha * 0.15f))),
                RoundedCornerShape(11.dp)
            )
            .clip(RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(data.symbol, color = col.copy(pAlpha), fontSize = 24.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(data.label,
            color      = if (isActive) col else NToken.dimText,
            fontSize   = 7.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.8.sp,
            textAlign  = TextAlign.Center,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        if (isActive) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.width(24.dp).height(2.dp).background(col.copy(pAlpha), RoundedCornerShape(1.dp)))
            Spacer(Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { Box(Modifier.size(2.dp).background(col.copy(pAlpha * 0.55f), CircleShape)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Side utility buttons
// ─────────────────────────────────────────────────────────
@Composable
private fun SideUtilityButtons(onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        SideUtilButton(Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 68.dp), "⚙", NToken.greenMid, onSettings)
        SideUtilButton(Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 68.dp), "AI", NToken.amberMid, {})
    }
}

@Composable
private fun SideUtilButton(modifier: Modifier, label: String, color: Color, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "sideBtnScale"
    )
    Box(
        modifier = modifier
            .size(28.dp)
            .scale(scale)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(color.copy(if (isPressed) 0.35f else 0.22f), NToken.gunmetal, NToken.shellDeep)))
                drawCircle(NToken.bevelLow.copy(0.9f), radius = size.minDimension / 2 * 0.88f, style = Stroke(1.0f))
                drawCircle(NToken.bevelHigh.copy(0.35f), radius = size.minDimension / 2, style = Stroke(1.8f))
                drawArc(NToken.bevelHigh.copy(0.40f), 200f, 100f, false,
                    Offset(size.minDimension * 0.05f, size.minDimension * 0.05f),
                    Size(size.minDimension * 0.90f, size.minDimension * 0.90f), style = Stroke(1.5f))
                if (isPressed) drawCircle(color.copy(0.25f), radius = size.minDimension / 2)
            }
            .border(1.dp, color.copy(if (isPressed) 0.90f else 0.65f), CircleShape)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap   = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = if (label.length > 1) 7.sp else 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
