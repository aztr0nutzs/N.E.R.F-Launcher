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
//    left  = x / imageWidth
//    top   = y / imageHeight
//    right = (x + w) / imageWidth
//    bottom= (y + h) / imageHeight
//
//  Reactor physics:
//    Stored as pixel values relative to a 1177×2048 source image.
//    Use [AssistantOverlayMap.reactorPhysics] for ring/sector hit-testing;
//    normalise to pixel space before calling.
//
//  Usage:
//    val bounds = AssistantOverlayMap.inputShell
//    val px = bounds.toPx(imageRect)   // → Rect in screen pixels
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A rect expressed as normalized fractions of the artwork image.
 * Prefer using the named constants on [AssistantOverlayMap] over constructing
 * these directly; only build custom rects when testing or extending regions.
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

    /** Width as a fraction of the image width. */
    val normWidth: Float get() = rect.right - rect.left

    /** Height as a fraction of the image height. */
    val normHeight: Float get() = rect.bottom - rect.top

    companion object {
        /** Build a NormRect from x,y,w,h fractions (matching the spec format). */
        fun of(x: Float, y: Float, w: Float, h: Float): NormRect =
            NormRect(Rect(left = x, top = y, right = x + w, bottom = y + h))
    }
}

/** Reactor segmented-ring hit-test model. */
data class ReactorPhysics(
    /** Normalized center of reactorOuter (image-relative). */
    val centerNorm: Offset,
    /**
     * Inner radius of the active tap band on the original 1177×2048 source image.
     * Sector taps must fall inside [innerRadiusPx, outerRadiusPx].
     */
    val innerRadiusPx: Float = 66f,
    /** Outer radius of the active tap band. */
    val outerRadiusPx: Float = 142f,
    /** Radius for core tap (circle at center). */
    val coreRadiusPx: Float = 56f,
    /** Source image width in px — used to normalise radii to the displayed image. */
    val sourceWidthPx: Float = 1177f,
    /** Source image height in px — used to normalise radii to the displayed image. */
    val sourceHeightPx: Float = 2048f
) {
    /**
     * Classify a tap at [tapOffset] (screen pixels) given the displayed [imageRect].
     *
     * Returns:
     *   - null if the tap is outside all active zones
     *   - [ReactorZone.Core] for a core tap
     *   - A [ReactorZone.Sector] for a ring sector tap
     */
    fun classify(tapOffset: Offset, imageRect: Rect): ReactorZone? {
        val cxPx = imageRect.left + centerNorm.x * imageRect.width
        val cyPx = imageRect.top  + centerNorm.y * imageRect.height

        val dx = tapOffset.x - cxPx
        val dy = tapOffset.y - cyPx

        // Scale radii to the displayed image size
        val scaleX = imageRect.width  / sourceWidthPx
        val scaleY = imageRect.height / sourceHeightPx
        val scale  = (scaleX + scaleY) / 2f

        val dist = sqrt(dx * dx + dy * dy)

        val corePx   = coreRadiusPx   * scale
        val innerPx  = innerRadiusPx  * scale
        val outerPx  = outerRadiusPx  * scale

        return when {
            dist <= corePx -> ReactorZone.Core
            dist in innerPx..outerPx -> {
                // Angle: atan2 returns radians in -π..π; convert to 0°–360° clockwise
                val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    .let { if (it < 0f) it + 360f else it }
                ReactorZone.Sector(ReactorSector.fromAngle(angleDeg))
            }
            else -> null
        }
    }
}

/** Zones within the reactor that can receive tap events. */
sealed interface ReactorZone {
    data object Core : ReactorZone
    data class Sector(val sector: ReactorSector) : ReactorZone
}

/**
 * The four reactor ring sectors as defined in the artwork spec.
 *
 * Sector angles use Cartesian convention: 0° = east (right), clockwise.
 * Mapping:
 *   315°–045° = STABILITY_MONITOR  (top arc)
 *   045°–135° = INTERFACE_CONFIG   (right arc)
 *   135°–225° = RECALIBRATION      (bottom arc)
 *   225°–315° = SYS_NET_DIAG       (left arc)
 */
enum class ReactorSector(
    val startAngle: Float,
    val endAngle: Float,
    val label: String
) {
    STABILITY_MONITOR(315f, 45f,  "STABILITY MONITOR"),
    INTERFACE_CONFIG( 45f,  135f, "INTERFACE CONFIG"),
    RECALIBRATION(   135f, 225f,  "RECALIBRATION"),
    SYS_NET_DIAG(    225f, 315f,  "SYS / NET / DIAG");

    companion object {
        fun fromAngle(angleDeg: Float): ReactorSector {
            // Normalise to 0–360
            val a = ((angleDeg % 360f) + 360f) % 360f
            return when {
                a >= 315f || a < 45f  -> STABILITY_MONITOR
                a >= 45f  && a < 135f -> INTERFACE_CONFIG
                a >= 135f && a < 225f -> RECALIBRATION
                else                  -> SYS_NET_DIAG
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  The map of all regions — authoritative, single source of truth
// ─────────────────────────────────────────────────────────────────────────────

object AssistantOverlayMap {

    // ── TOP MODULES ──────────────────────────────────────────────────────────
    val energyModule   = NormRect.of(x=0.0272f, y=0.0161f, w=0.3212f, h=0.0815f)
    val statusModule   = NormRect.of(x=0.0263f, y=0.1382f, w=0.3212f, h=0.0815f)
    val logoTopRight   = NormRect.of(x=0.7893f, y=0.0146f, w=0.1818f, h=0.0713f)

    // ── ROBOT ────────────────────────────────────────────────────────────────
    val robotBody      = NormRect.of(x=0.2642f, y=0.1021f, w=0.5344f, h=0.4702f)
    val visorZone      = NormRect.of(x=0.3339f, y=0.1045f, w=0.3730f, h=0.2324f)
    val reactorOuter   = NormRect.of(x=0.3653f, y=0.3184f, w=0.2744f, h=0.1577f)
    val reactorCore    = NormRect.of(x=0.4444f, y=0.3638f, w=0.1164f, h=0.0669f)
    val handNode       = NormRect.of(x=0.3076f, y=0.4155f, w=0.1190f, h=0.0684f)

    // ── LEFT ACTION STACK ────────────────────────────────────────────────────
    val leftPower      = NormRect.of(x=0.0238f, y=0.2607f, w=0.1020f, h=0.0586f)
    val leftNetwork    = NormRect.of(x=0.0238f, y=0.3306f, w=0.1020f, h=0.0586f)
    val leftAlerts     = NormRect.of(x=0.0238f, y=0.4004f, w=0.1020f, h=0.0586f)
    val leftSettings   = NormRect.of(x=0.0238f, y=0.4702f, w=0.1020f, h=0.0586f)

    // ── CHAT + INPUT ─────────────────────────────────────────────────────────
    val panelOuter         = NormRect.of(x=0.0501f, y=0.5308f, w=0.8904f, h=0.3745f)
    val transcriptRegion   = NormRect.of(x=0.0875f, y=0.5771f, w=0.8156f, h=0.1489f)
    val inputShell         = NormRect.of(x=0.1538f, y=0.7476f, w=0.5599f, h=0.0376f)
    val inputTextRegion    = NormRect.of(x=0.1716f, y=0.7534f, w=0.4758f, h=0.0244f)
    val inputMic           = NormRect.of(x=0.6517f, y=0.7534f, w=0.0357f, h=0.0244f)
    val inputEmoji         = NormRect.of(x=0.6890f, y=0.7534f, w=0.0357f, h=0.0244f)
    val sendButton         = NormRect.of(x=0.7298f, y=0.7466f, w=0.1266f, h=0.0376f)
    val dartCount          = NormRect.of(x=0.2430f, y=0.8091f, w=0.2277f, h=0.0605f)
    val toggleModule       = NormRect.of(x=0.6185f, y=0.8159f, w=0.1453f, h=0.0454f)

    // ── BOTTOM DOCK ──────────────────────────────────────────────────────────
    val dockHousing    = NormRect.of(x=0.0340f, y=0.8608f, w=0.9252f, h=0.1201f)
    val dockSettings   = NormRect.of(x=0.0527f, y=0.8740f, w=0.0918f, h=0.0527f)
    val dockMap        = NormRect.of(x=0.1521f, y=0.8740f, w=0.0918f, h=0.0527f)
    val dockModules    = NormRect.of(x=0.2523f, y=0.8740f, w=0.0918f, h=0.0527f)
    val dockCenterCore = NormRect.of(x=0.3951f, y=0.8438f, w=0.2056f, h=0.1182f)
    val dockMic        = NormRect.of(x=0.6695f, y=0.8740f, w=0.0918f, h=0.0527f)
    val dockProfile    = NormRect.of(x=0.8309f, y=0.8740f, w=0.0918f, h=0.0527f)

    // ── REACTOR PHYSICS ──────────────────────────────────────────────────────
    /**
     * Physics model for the reactor ring.
     * Center is the normalized center of [reactorOuter].
     */
    val reactorPhysics = ReactorPhysics(
        centerNorm = Offset(
            x = reactorOuter.rect.left + reactorOuter.normWidth  / 2f,
            y = reactorOuter.rect.top  + reactorOuter.normHeight / 2f
        ),
        innerRadiusPx = 66f,
        outerRadiusPx = 142f,
        coreRadiusPx  = 56f,
        sourceWidthPx  = 1177f,
        sourceHeightPx = 2048f
    )

    // ── CONVENIENCE GROUPINGS ────────────────────────────────────────────────

    val leftActionStack: List<Pair<LeftAction, NormRect>> = listOf(
        LeftAction.POWER    to leftPower,
        LeftAction.NETWORK  to leftNetwork,
        LeftAction.ALERTS   to leftAlerts,
        LeftAction.SETTINGS to leftSettings
    )

    val dockButtons: List<Pair<DockAction, NormRect>> = listOf(
        DockAction.SETTINGS to dockSettings,
        DockAction.MAP      to dockMap,
        DockAction.MODULES  to dockModules,
        DockAction.MIC      to dockMic,
        DockAction.PROFILE  to dockProfile
    )
}

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
//  Extension: convert NormRect to pixel Rect relative to the image rect
// ─────────────────────────────────────────────────────────────────────────────

/** Converts a [Rect] expressed in normalized image fractions to screen pixels within [imageRect]. */
fun Rect.toImagePixelRect(imageRect: Rect): Rect = Rect(
    left   = imageRect.left + left   * imageRect.width,
    top    = imageRect.top  + top    * imageRect.height,
    right  = imageRect.left + right  * imageRect.width,
    bottom = imageRect.top  + bottom * imageRect.height
)

/** Returns the pixel center of this [NormRect] within [imageRect]. */
fun NormRect.centerScreen(imageRect: Rect): Offset = centerPx(imageRect)

/** Returns whether [offset] (screen pixels) falls inside this [NormRect] mapped to [imageRect]. */
fun NormRect.containsScreen(offset: Offset, imageRect: Rect): Boolean {
    val px = toPx(imageRect)
    return offset.x in px.left..px.right && offset.y in px.top..px.bottom
}
