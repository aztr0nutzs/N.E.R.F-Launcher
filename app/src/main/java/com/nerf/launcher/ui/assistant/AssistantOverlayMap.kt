package com.nerf.launcher.ui.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.atan2
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
//  AssistantOverlayMap
//
//  Single authoritative source for every normalized interactive region on
//  the assistant artwork backplate (source: 1177 × 2048 px).
//
//  ALL coordinates are normalized fractions of the backplate image rect
//  (0f–1f on each axis, where 0,0 = top-left of the rendered image).
//
//  Normalized rect encoding:
//    left   = x / imageWidth
//    top    = y / imageHeight
//    right  = (x + w) / imageWidth
//    bottom = (y + h) / imageHeight
//
//  Reactor physics:
//    Stored as pixel values relative to the 1177×2048 source image.
//    Use [AssistantOverlayMap.reactorPhysics] for ring/sector hit-testing.
//
//  Usage:
//    val bounds = AssistantOverlayMap.inputShell
//    val px = bounds.toPx(imageRect)   // → Rect in screen pixels
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A rect expressed as normalized fractions of the artwork image.
 * Prefer the named constants on [AssistantOverlayMap].
 */
@JvmInline
value class NormRect(val rect: Rect) {

    /** Converts this normalized rect into screen-pixel coordinates within [imageRect]. */
    fun toPx(imageRect: Rect): Rect = Rect(
        left   = imageRect.left + rect.left   * imageRect.width,
        top    = imageRect.top  + rect.top    * imageRect.height,
        right  = imageRect.left + rect.right  * imageRect.width,
        bottom = imageRect.top  + rect.bottom * imageRect.height
    )

    /** Center of this region in [imageRect] screen-pixel space. */
    fun centerPx(imageRect: Rect): Offset {
        val px = toPx(imageRect)
        return Offset((px.left + px.right) / 2f, (px.top + px.bottom) / 2f)
    }

    /**
     * Returns an expanded version of this [NormRect] by [dxNorm] horizontally
     * and [dyNorm] vertically (normalized units), clamped to [0, 1].
     *
     * Use this to widen hit-test targets for small touch regions without
     * moving the visual widget.
     */
    fun expandHitTarget(dxNorm: Float, dyNorm: Float = dxNorm): NormRect = NormRect(
        Rect(
            left   = (rect.left   - dxNorm).coerceAtLeast(0f),
            top    = (rect.top    - dyNorm).coerceAtLeast(0f),
            right  = (rect.right  + dxNorm).coerceAtMost(1f),
            bottom = (rect.bottom + dyNorm).coerceAtMost(1f)
        )
    )

    /** Width as a fraction of the image width. */
    val normWidth: Float  get() = rect.right  - rect.left

    /** Height as a fraction of the image height. */
    val normHeight: Float get() = rect.bottom - rect.top

    companion object {
        /** Build a NormRect from x, y, w, h fractions (matching the spec format). */
        fun of(x: Float, y: Float, w: Float, h: Float): NormRect =
            NormRect(Rect(left = x, top = y, right = x + w, bottom = y + h))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reactor physics
// ─────────────────────────────────────────────────────────────────────────────

/** Reactor segmented-ring hit-test model. */
data class ReactorPhysics(
    /** Normalized center of reactorOuter (image-relative, [0,1] × [0,1]). */
    val centerNorm: Offset,
    /**
     * Inner radius of the active tap band on the original 1177×2048 source image.
     * Sector taps must fall in [innerRadiusPx, outerRadiusPx].
     */
    val innerRadiusPx: Float = 66f,
    /** Outer radius of the sector tap band on the source image. */
    val outerRadiusPx: Float = 142f,
    /** Core tap radius on the source image. */
    val coreRadiusPx: Float  = 56f,
    /** Source image width — used to compute the uniform Fit scale. */
    val sourceWidthPx: Float  = 1177f,
    /** Source image height — used to compute the uniform Fit scale. */
    val sourceHeightPx: Float = 2048f
) {
    /**
     * Classify a tap at [tapOffset] (screen pixels) given the displayed [imageRect].
     *
     * Scale factor is `minOf(imageRect.width / sourceWidth, imageRect.height / sourceHeight)`,
     * which exactly mirrors ContentScale.Fit — no axis averaging.
     *
     * Returns null if the tap is outside all active zones.
     */
    fun classify(tapOffset: Offset, imageRect: Rect): ReactorZone? {
        val cxPx = imageRect.left + centerNorm.x * imageRect.width
        val cyPx = imageRect.top  + centerNorm.y * imageRect.height

        val dx = tapOffset.x - cxPx
        val dy = tapOffset.y - cyPx

        // ContentScale.Fit → uniform scale = min(scaleX, scaleY).
        // When imageRect is computed by computeImageRect(), scaleX == scaleY always.
        // Using minOf here makes the intent explicit and is safe for any imageRect source.
        val scale = minOf(
            imageRect.width  / sourceWidthPx,
            imageRect.height / sourceHeightPx
        )

        val dist     = sqrt(dx * dx + dy * dy)
        val corePx   = coreRadiusPx  * scale
        val innerPx  = innerRadiusPx * scale
        val outerPx  = outerRadiusPx * scale

        return when {
            dist <= corePx -> ReactorZone.Core
            dist in innerPx..outerPx -> {
                // atan2: 0° = east, positive = clockwise when y increases downward.
                val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    .let { if (it < 0f) it + 360f else it }
                // fromAngle returns null for gap zones — propagate as a no-op tap.
                val sector = ReactorSector.fromAngle(angleDeg) ?: return null
                ReactorZone.Sector(sector)
            }
            else -> null
        }
    }
}

/** Zones within the reactor that can receive tap events. */
sealed interface ReactorZone {
    data object Core : ReactorZone
    data class  Sector(val sector: ReactorSector) : ReactorZone
}

/**
 * The four reactor ring sectors as defined in the artwork spec.
 *
 * Angles: 0° = east, clockwise (matching Android Canvas / atan2 conventions).
 *
 *   315°–045° = STABILITY_MONITOR   (top arc)
 *   045°–135° = INTERFACE_CONFIG    (right arc)
 *   135°–225° = RECALIBRATION       (bottom arc)
 *   225°–315° = SYS_NET_DIAG        (left arc)
 *
 * These are also the fixed `drawArc` start angles used in the FX overlay.
 */
enum class ReactorSector(
    /** Android drawArc startAngle (0° = east, clockwise). */
    val arcStartAngle: Float,
    /** Angular sweep of the sector arc highlight (deg). */
    val arcSweepAngle: Float,
    val label: String
) {
    // -45° = same as 315°; Android drawArc handles negative start angles correctly.
    STABILITY_MONITOR(arcStartAngle = -45f, arcSweepAngle = 86f, label = "STABILITY MONITOR"),
    INTERFACE_CONFIG( arcStartAngle =  45f, arcSweepAngle = 86f, label = "INTERFACE CONFIG"),
    RECALIBRATION(    arcStartAngle = 135f, arcSweepAngle = 86f, label = "RECALIBRATION"),
    SYS_NET_DIAG(     arcStartAngle = 225f, arcSweepAngle = 86f, label = "SYS / NET / DIAG");

    // Legacy fields kept for enum compatibility — use arcStartAngle / arcSweepAngle in new code.
    val startAngle: Float get() = arcStartAngle
    val endAngle:   Float get() = arcStartAngle + arcSweepAngle

    companion object {
        /**
         * Maps [angleDeg] (0° = east, clockwise, normalized to [0, 360)) to the
         * sector that owns that angle, or **null** if the angle falls in one of the
         * four 4° visual gap zones between adjacent sectors.
         *
         * Sector arcs (each 86° wide, leaving 4° gaps at the boundaries):
         *
         *   STABILITY_MONITOR : [315°, 41°)   → wraps through 0°
         *   INTERFACE_CONFIG  : [ 45°, 131°)
         *   RECALIBRATION     : [135°, 221°)
         *   SYS_NET_DIAG      : [225°, 311°)
         *
         * Gap dead-zones (return null):
         *   Gap 1 (SM→IC)  : [41°,  45°)
         *   Gap 2 (IC→RC)  : [131°, 135°)
         *   Gap 3 (RC→SND) : [221°, 225°)
         *   Gap 4 (SND→SM) : [311°, 315°)
         */
        fun fromAngle(angleDeg: Float): ReactorSector? {
            val a = ((angleDeg % 360f) + 360f) % 360f
            return when {
                // ── Active sector arcs ─────────────────────────────────────
                // STABILITY_MONITOR wraps through 0°: [315°, 360°) ∪ [0°, 41°)
                a >= 315f || a < 41f  -> STABILITY_MONITOR
                // Gap 1: [41°, 45°) — no sector
                a < 45f              -> null
                // INTERFACE_CONFIG: [45°, 131°)
                a < 131f             -> INTERFACE_CONFIG
                // Gap 2: [131°, 135°) — no sector
                a < 135f             -> null
                // RECALIBRATION: [135°, 221°)
                a < 221f             -> RECALIBRATION
                // Gap 3: [221°, 225°) — no sector
                a < 225f             -> null
                // SYS_NET_DIAG: [225°, 311°)
                a < 311f             -> SYS_NET_DIAG
                // Gap 4: [311°, 315°) — no sector
                else                 -> null
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  The map — authoritative, single source of truth
// ─────────────────────────────────────────────────────────────────────────────

object AssistantOverlayMap {

    // ── TOP MODULES ──────────────────────────────────────────────────────────
    val energyModule   = NormRect.of(x = 0.0272f, y = 0.0161f, w = 0.3212f, h = 0.0815f)
    val statusModule   = NormRect.of(x = 0.0263f, y = 0.1382f, w = 0.3212f, h = 0.0815f)
    val logoTopRight   = NormRect.of(x = 0.7893f, y = 0.0146f, w = 0.1818f, h = 0.0713f)

    // ── ROBOT ────────────────────────────────────────────────────────────────
    val robotBody      = NormRect.of(x = 0.2642f, y = 0.1021f, w = 0.5344f, h = 0.4702f)
    val visorZone      = NormRect.of(x = 0.3339f, y = 0.1045f, w = 0.3730f, h = 0.2324f)
    val reactorOuter   = NormRect.of(x = 0.3653f, y = 0.3184f, w = 0.2744f, h = 0.1577f)
    val reactorCore    = NormRect.of(x = 0.4444f, y = 0.3638f, w = 0.1164f, h = 0.0669f)
    val handNode       = NormRect.of(x = 0.3076f, y = 0.4155f, w = 0.1190f, h = 0.0684f)

    // ── LEFT ACTION STACK ────────────────────────────────────────────────────
    val leftPower      = NormRect.of(x = 0.0238f, y = 0.2607f, w = 0.1020f, h = 0.0586f)
    val leftNetwork    = NormRect.of(x = 0.0238f, y = 0.3306f, w = 0.1020f, h = 0.0586f)
    val leftAlerts     = NormRect.of(x = 0.0238f, y = 0.4004f, w = 0.1020f, h = 0.0586f)
    val leftSettings   = NormRect.of(x = 0.0238f, y = 0.4702f, w = 0.1020f, h = 0.0586f)

    // ── CHAT + INPUT ─────────────────────────────────────────────────────────
    val panelOuter       = NormRect.of(x = 0.0501f, y = 0.5308f, w = 0.8904f, h = 0.3745f)
    val transcriptRegion = NormRect.of(x = 0.0875f, y = 0.5771f, w = 0.8156f, h = 0.1489f)
    val inputShell       = NormRect.of(x = 0.1538f, y = 0.7476f, w = 0.5599f, h = 0.0376f)
    val inputTextRegion  = NormRect.of(x = 0.1716f, y = 0.7534f, w = 0.4758f, h = 0.0244f)
    val inputMic         = NormRect.of(x = 0.6517f, y = 0.7534f, w = 0.0357f, h = 0.0244f)
    val inputEmoji       = NormRect.of(x = 0.6890f, y = 0.7534f, w = 0.0357f, h = 0.0244f)
    val sendButton       = NormRect.of(x = 0.7298f, y = 0.7466f, w = 0.1266f, h = 0.0376f)
    val dartCount        = NormRect.of(x = 0.2430f, y = 0.8091f, w = 0.2277f, h = 0.0605f)
    val toggleModule     = NormRect.of(x = 0.6185f, y = 0.8159f, w = 0.1453f, h = 0.0454f)

    // ── BOTTOM DOCK ──────────────────────────────────────────────────────────
    val dockHousing    = NormRect.of(x = 0.0340f, y = 0.8608f, w = 0.9252f, h = 0.1201f)
    val dockSettings   = NormRect.of(x = 0.0527f, y = 0.8740f, w = 0.0918f, h = 0.0527f)
    val dockMap        = NormRect.of(x = 0.1521f, y = 0.8740f, w = 0.0918f, h = 0.0527f)
    val dockModules    = NormRect.of(x = 0.2523f, y = 0.8740f, w = 0.0918f, h = 0.0527f)
    val dockCenterCore = NormRect.of(x = 0.3951f, y = 0.8438f, w = 0.2056f, h = 0.1182f)
    val dockMic        = NormRect.of(x = 0.6695f, y = 0.8740f, w = 0.0918f, h = 0.0527f)
    val dockProfile    = NormRect.of(x = 0.8309f, y = 0.8740f, w = 0.0918f, h = 0.0527f)

    // ── REACTOR PHYSICS ──────────────────────────────────────────────────────
    val reactorPhysics = ReactorPhysics(
        centerNorm = Offset(
            x = reactorOuter.rect.left + reactorOuter.normWidth  / 2f,
            y = reactorOuter.rect.top  + reactorOuter.normHeight / 2f
        ),
        innerRadiusPx  = 66f,
        outerRadiusPx  = 142f,
        coreRadiusPx   = 56f,
        sourceWidthPx  = 1177f,
        sourceHeightPx = 2048f
    )

    // ── EXPANDED HIT TARGETS ─────────────────────────────────────────────────
    // Visual regions stay at their exact spec positions.
    // Hit-test regions are expanded so small targets meet touch-target guidelines.
    // Expansion is in normalized units: 0.012 ≈ 14 px on a 1080-wide display.

    /** Expanded hit target for inputMic (visual: ~20dp; target: ~44dp). */
    val inputMicHit    = inputMic.expandHitTarget(dxNorm = 0.015f, dyNorm = 0.010f)

    /** Expanded hit target for inputEmoji (same). */
    val inputEmojiHit  = inputEmoji.expandHitTarget(dxNorm = 0.015f, dyNorm = 0.010f)

    /** Expanded hit target for left-stack buttons to fill the full stack column width. */
    val leftPowerHit   = leftPower.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.004f)
    val leftNetworkHit = leftNetwork.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.004f)
    val leftAlertsHit  = leftAlerts.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.004f)
    val leftSettingsHit= leftSettings.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.004f)

    /** Expanded hit target for dock side buttons (already adequate but slight expansion). */
    val dockSettingsHit = dockSettings.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.005f)
    val dockMapHit      = dockMap.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.005f)
    val dockModulesHit  = dockModules.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.005f)
    val dockMicHit      = dockMic.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.005f)
    val dockProfileHit  = dockProfile.expandHitTarget(dxNorm = 0.006f, dyNorm = 0.005f)

    // ── CONVENIENCE GROUPINGS ────────────────────────────────────────────────

    /** Visual regions for rendering. */
    val leftActionStack: List<Pair<LeftAction, NormRect>> = listOf(
        LeftAction.POWER    to leftPower,
        LeftAction.NETWORK  to leftNetwork,
        LeftAction.ALERTS   to leftAlerts,
        LeftAction.SETTINGS to leftSettings
    )

    /** Expanded hit-test regions for the left action stack (parallel order to [leftActionStack]). */
    val leftActionStackHit: List<Pair<LeftAction, NormRect>> = listOf(
        LeftAction.POWER    to leftPowerHit,
        LeftAction.NETWORK  to leftNetworkHit,
        LeftAction.ALERTS   to leftAlertsHit,
        LeftAction.SETTINGS to leftSettingsHit
    )

    /** Visual regions for rendering. */
    val dockButtons: List<Pair<DockAction, NormRect>> = listOf(
        DockAction.SETTINGS to dockSettings,
        DockAction.MAP      to dockMap,
        DockAction.MODULES  to dockModules,
        DockAction.MIC      to dockMic,
        DockAction.PROFILE  to dockProfile
    )

    /** Expanded hit-test regions for dock buttons (parallel order to [dockButtons]). */
    val dockButtonsHit: List<Pair<DockAction, NormRect>> = listOf(
        DockAction.SETTINGS to dockSettingsHit,
        DockAction.MAP      to dockMapHit,
        DockAction.MODULES  to dockModulesHit,
        DockAction.MIC      to dockMicHit,
        DockAction.PROFILE  to dockProfileHit
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Actions
// ─────────────────────────────────────────────────────────────────────────────

/** Actions on the left vertical action stack. */
enum class LeftAction(val label: String) {
    POWER("PWR"),
    NETWORK("NET"),
    ALERTS("ALT"),
    SETTINGS("CFG")
}

/** Actions on the bottom dock. */
enum class DockAction(val label: String) {
    SETTINGS("CFG"),
    MAP("MAP"),
    MODULES("MOD"),
    MIC("MIC"),
    PROFILE("PRF")
}

// ─────────────────────────────────────────────────────────────────────────────
//  Extensions
// ─────────────────────────────────────────────────────────────────────────────

/** Converts a normalized-fraction [Rect] to screen pixels within [imageRect]. */
fun Rect.toImagePixelRect(imageRect: Rect): Rect = Rect(
    left   = imageRect.left + left   * imageRect.width,
    top    = imageRect.top  + top    * imageRect.height,
    right  = imageRect.left + right  * imageRect.width,
    bottom = imageRect.top  + bottom * imageRect.height
)

/** Pixel center of this [NormRect] within [imageRect]. */
fun NormRect.centerScreen(imageRect: Rect): Offset = centerPx(imageRect)

/**
 * True if [offset] (screen pixels) falls inside this [NormRect] mapped to [imageRect].
 * Uses the visual rect bounds — for hit-testing, prefer [containsScreenExpanded]
 * with an explicit hit-target margin.
 */
fun NormRect.containsScreen(offset: Offset, imageRect: Rect): Boolean {
    val px = toPx(imageRect)
    return offset.x in px.left..px.right && offset.y in px.top..px.bottom
}
