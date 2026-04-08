package com.nerf.launcher.ui.reactor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

@Composable
fun NanoCoreReactor(
    reactor: ReactorModel,
    modifier: Modifier = Modifier,
    palette: ReactorPalette = rememberIndustrialReactorPalette(),
    motionSpec: ReactorMotionSpec = ReactorMotionSpec(),
    interactionState: ReactorInteractionState = ReactorInteractionState(),
    onInteractionStateChange: (ReactorInteractionState) -> Unit = {},
    onCoreTap: () -> Unit = {},
    onSegmentTap: (String) -> Unit = {},
    onSegmentLongPress: (String) -> Unit = {},
    onHapticEvent: (ReactorHapticEvent) -> Unit = {}
) {
    val motionState = rememberReactorMotionState(motionSpec)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val reactorSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val coreDiameter = reactorSize * (reactor.coreRadiusFraction * 2f)
        val density = LocalDensity.current
        val boxSizePx = with(density) { reactorSize.toPx() }
        val outerRadiusPx = boxSizePx * (0.5f - reactor.outerPaddingFraction.coerceIn(0.02f, 0.18f))
        val ringThicknessPx = boxSizePx * reactor.outerRingThicknessFraction.coerceIn(0.08f, 0.22f)
        val center = remember(boxSizePx) { Offset(boxSizePx / 2f, boxSizePx / 2f) }
        val layoutMetrics = remember(
            center,
            outerRadiusPx,
            ringThicknessPx,
            boxSizePx,
            motionState.outerRotationDegrees
        ) {
            ReactorLayoutMetrics(
                center = center,
                outerRadiusPx = outerRadiusPx,
                ringThicknessPx = ringThicknessPx,
                coreRadiusPx = boxSizePx * reactor.coreRadiusFraction.coerceIn(0.08f, 0.4f),
                rotationDegrees = motionState.outerRotationDegrees
            )
        }

        ReactorRing(
            reactor = reactor,
            palette = palette,
            glowBreathScale = motionState.glowBreathScale,
            rotationDegrees = motionState.outerRotationDegrees,
            modifier = Modifier.fillMaxSize(),
            interactionState = interactionState
        )

        ReactorCore(
            model = reactor.core,
            palette = palette,
            glowBreathScale = motionState.glowBreathScale,
            pulseScale = motionState.corePulseScale,
            modifier = Modifier.size(coreDiameter),
            isPressed = interactionState.isCorePressed
        )

        ReactorGestureLayer(
            reactor = reactor,
            layoutMetrics = layoutMetrics,
            interactionState = interactionState,
            onInteractionStateChange = onInteractionStateChange,
            onCoreTap = onCoreTap,
            onSegmentTap = onSegmentTap,
            onSegmentLongPress = onSegmentLongPress,
            onHapticEvent = onHapticEvent,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ReactorRing(
    reactor: ReactorModel,
    modifier: Modifier = Modifier,
    palette: ReactorPalette = rememberIndustrialReactorPalette(),
    glowBreathScale: Float = 1f,
    rotationDegrees: Float = 0f,
    interactionState: ReactorInteractionState = ReactorInteractionState()
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val boxSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val boxSizePx = with(density) { boxSize.toPx() }
        val outerRadiusPx = boxSizePx * (0.5f - reactor.outerPaddingFraction.coerceIn(0.02f, 0.18f))
        val ringThicknessPx = boxSizePx * reactor.outerRingThicknessFraction.coerceIn(0.08f, 0.22f)
        val labelRadiusPx = boxSizePx * reactor.labelRadiusFraction.coerceIn(0.48f, 0.82f) * 0.5f
        val center = remember(boxSizePx) { Offset(boxSizePx / 2f, boxSizePx / 2f) }
        val segmentLayouts = remember(reactor, outerRadiusPx, ringThicknessPx, labelRadiusPx) {
            calculateSegmentLayouts(
                reactor = reactor,
                outerRadiusPx = outerRadiusPx,
                ringThicknessPx = ringThicknessPx,
                labelRadiusPx = labelRadiusPx
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = palette.chassisShadow.copy(alpha = 0.92f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = palette.chassisBase,
                radius = size.minDimension * 0.455f
            )
            drawCircle(
                color = palette.chassisLine.copy(alpha = 0.42f),
                radius = outerRadiusPx + ringThicknessPx * 0.32f,
                style = Stroke(width = ringThicknessPx * 0.08f)
            )
            drawCircle(
                color = palette.ringTrack.copy(alpha = 0.25f),
                radius = outerRadiusPx - ringThicknessPx * 0.5f,
                style = Stroke(width = ringThicknessPx)
            )

            reactor.supportRings.forEach { supportRing ->
                val ringRadius = size.minDimension * 0.5f * supportRing.radiusFraction.coerceIn(0.12f, 0.92f)
                val strokeWidth = size.minDimension * supportRing.strokeWidthFraction.coerceIn(0.004f, 0.04f)
                val ringColor = palette.accentColor(supportRing.accent).copy(
                    alpha = supportRing.alpha.coerceIn(0f, 1f) * glowBreathScale
                )
                if (supportRing.dashCount <= 0) {
                    drawCircle(
                        color = ringColor,
                        radius = ringRadius,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val dashSweep = (360f / supportRing.dashCount) * 0.42f
                    repeat(supportRing.dashCount) { index ->
                        drawArc(
                            color = ringColor,
                            startAngle = -90f + index * (360f / supportRing.dashCount),
                            sweepAngle = dashSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                            size = androidx.compose.ui.geometry.Size(ringRadius * 2f, ringRadius * 2f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationDegrees }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                segmentLayouts.forEach { layout ->
                    val isPressed = interactionState.pressedSegmentId == layout.model.id
                    drawReactorSegment(
                        layout = layout,
                        palette = palette,
                        glowBreathScale = glowBreathScale,
                        isPressed = isPressed
                    )
                }
            }

            segmentLayouts.forEach { layout ->
                ReactorSegmentLabel(
                    layout = layout,
                    palette = palette,
                    isPressed = interactionState.pressedSegmentId == layout.model.id,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
