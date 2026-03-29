// ReactorCore.kt
package com.nerf.launcher.reactor.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Stable
class ReactorController internal constructor(
    initialMode: ReactorMode
) {
    var mode by mutableStateOf(initialMode)
        internal set

    var isExpanded by mutableStateOf(false)
        internal set

    fun setMode(new: ReactorMode) {
        mode = new
    }

    fun collapse() {
        isExpanded = false
    }

    fun expand() {
        isExpanded = true
    }
}

@Composable
fun rememberReactorController(
    initialMode: ReactorMode = ReactorMode.Idle
): ReactorController = remember { ReactorController(initialMode) }

@Composable
fun ReactorCore(
    modifier: Modifier = Modifier,
    controller: ReactorController = rememberReactorController(),
    config: ReactorConfig = ReactorConfig(),
    // Callbacks per zone
    onZoneTap: (ReactorZone) -> Unit = {},
    onZoneLongPress: (ReactorZone) -> Unit = {},
    onZoneDoubleTap: (ReactorZone) -> Unit = {},
    // Optional: hook external mode transitions
    onOverdriveStart: () -> Unit = {},
    onOverdriveEnd: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    // Animation “channels” – speeds depend on mode
    val transition = updateTransition(controller.mode, label = "reactorMode")

    val pulseScale by transition.animateFloat(
        transitionSpec = {
            when (targetState) {
                ReactorMode.Idle -> tween(900, easing = LinearEasing)
                ReactorMode.Active -> tween(700, easing = FastOutSlowInEasing)
                ReactorMode.Alert -> tween(480, easing = FastOutSlowInEasing)
                ReactorMode.Overdrive -> tween(260, easing = LinearEasing)
            }
        },
        label = "pulseScale"
    ) { mode ->
        when (mode) {
            ReactorMode.Idle -> 1.02f
            ReactorMode.Active -> 1.06f
            ReactorMode.Alert -> 1.08f
            ReactorMode.Overdrive -> 1.12f
        }
    }

    val coreGlowAlpha by transition.animateFloat(
        transitionSpec = { tween(450) },
        label = "coreGlow"
    ) { mode ->
        when (mode) {
            ReactorMode.Idle -> 0.65f
            ReactorMode.Active -> 0.9f
            ReactorMode.Alert -> 1.0f
            ReactorMode.Overdrive -> 1.0f
        }
    }

    val colorAlert = Color(0xFFFF5A2C)
    val colorOverdrive = Color(0xFF00F7FF)

    val ringColors = when (controller.mode) {
        ReactorMode.Idle -> listOf(
            Color(0xFF29FF9A),
            Color(0xFF00D4FF),
            Color(0xFFFFC800),
            Color(0xFFFF4FD8)
        )
        ReactorMode.Active -> listOf(
            Color(0xFF3CFFB4),
            Color(0xFF32E1FF),
            Color(0xFFFFE45A),
            Color(0xFFFF7CEC)
        )
        ReactorMode.Alert -> listOf(
            colorAlert,
            Color(0xFFFF9100),
            colorAlert,
            Color(0xFFFF9100)
        )
        ReactorMode.Overdrive -> listOf(
            colorOverdrive,
            Color(0xFF00FF8A),
            Color(0xFFFFFF5A),
            Color(0xFFFF53FF)
        )
    }

    val infinite = rememberInfiniteTransition(label = "reactorInfinite")

    val baseRotationOuter by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (9000 / config.idleOuterRotationSpeed).toInt()
                    .coerceAtLeast(1000),
                easing = LinearEasing
            )
        ),
        label = "outerRotation"
    )

    val baseRotationMid by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (11000 / kotlin.math.abs(config.idleMidRotationSpeed)).toInt()
                    .coerceAtLeast(1000),
                easing = LinearEasing
            )
        ),
        label = "midRotation"
    )

    val baseRotationCore by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (16000 / config.idleCoreRotationSpeed).toInt()
                    .coerceAtLeast(1000),
                easing = LinearEasing
            )
        ),
        label = "coreRotation"
    )

    val flowPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing)
        ),
        label = "flowPhase"
    )

    val alertPulseAlpha by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertPulse"
    )

    val overdriveFactor = if (controller.mode == ReactorMode.Overdrive) {
        config.overdriveMultiplier
    } else 1f

    val sizeDp = if (controller.isExpanded) config.baseSizeDp * 1.4f else config.baseSizeDp

    val textMeasurer = rememberTextMeasurer()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp.dp * pulseScale)
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .reactorPointerInput(
                    onZoneTap = { zone ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onZoneTap(zone)
                    },
                    onZoneLongPress = { zone ->
                        controller.expand()
                        onZoneLongPress(zone)
                    },
                    onZoneDoubleTap = { zone ->
                        if (controller.mode != ReactorMode.Overdrive) {
                            controller.mode = ReactorMode.Overdrive
                            onOverdriveStart()
                        } else {
                            controller.mode = ReactorMode.Active
                            onOverdriveEnd()
                        }
                        onZoneDoubleTap(zone)
                    },
                    longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis.toLong()
                )
        ) {
            drawReactorContent(
                controller = controller,
                config = config,
                ringColors = ringColors,
                baseRotationOuter = baseRotationOuter,
                baseRotationMid = baseRotationMid,
                baseRotationCore = baseRotationCore,
                flowPhase = flowPhase,
                alertPulseAlpha = alertPulseAlpha,
                coreGlowAlpha = coreGlowAlpha,
                overdriveFactor = overdriveFactor,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawReactorContent(
    controller: ReactorController,
    config: ReactorConfig,
    ringColors: List<Color>,
    baseRotationOuter: Float,
    baseRotationMid: Float,
    baseRotationCore: Float,
    flowPhase: Float,
    alertPulseAlpha: Float,
    coreGlowAlpha: Float,
    overdriveFactor: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val center = this.center
    val radius = size.minDimension / 2f

    // Outer mechanical frame
    drawOuterRing(
        center = center,
        radius = radius * 0.98f,
        phase = baseRotationOuter * overdriveFactor,
        colors = ringColors,
        mode = controller.mode,
        alertPulseAlpha = alertPulseAlpha
    )

    // Energy channels
    drawMidEnergyRing(
        center = center,
        radius = radius * 0.72f,
        phase = (baseRotationMid * -1f) * overdriveFactor,
        colors = ringColors,
        flowPhase = flowPhase
    )

    // Aura / plasma
    drawAura(
        center = center,
        radius = radius * 0.82f,
        mode = controller.mode,
        coreGlowAlpha = coreGlowAlpha,
        alertPulseAlpha = alertPulseAlpha
    )

    // Core node
    drawCore(
        center = center,
        radius = radius * 0.46f,
        rotation = baseRotationCore * overdriveFactor,
        glowAlpha = coreGlowAlpha,
        mode = controller.mode,
        textMeasurer = textMeasurer
    )

    // HUD labels around bottom/top
    drawHudLabels(
        center = center,
        radius = radius * 0.88f,
        mode = controller.mode,
        textMeasurer = textMeasurer
    )
}
