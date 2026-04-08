package com.nerf.launcher.ui.reactor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

internal sealed interface ReactorHitResult {
    data object None : ReactorHitResult

    data object Core : ReactorHitResult

    data class Segment(val segmentId: String) : ReactorHitResult
}

@Composable
internal fun ReactorGestureLayer(
    reactor: ReactorModel,
    layoutMetrics: ReactorLayoutMetrics,
    interactionState: ReactorInteractionState,
    onInteractionStateChange: (ReactorInteractionState) -> Unit,
    onCoreTap: () -> Unit,
    onSegmentTap: (String) -> Unit,
    onSegmentLongPress: (String) -> Unit,
    onHapticEvent: (ReactorHapticEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestInteractionState by rememberUpdatedState(interactionState)
    val latestOnInteractionStateChange by rememberUpdatedState(onInteractionStateChange)
    val latestOnCoreTap by rememberUpdatedState(onCoreTap)
    val latestOnSegmentTap by rememberUpdatedState(onSegmentTap)
    val latestOnSegmentLongPress by rememberUpdatedState(onSegmentLongPress)
    val latestOnHapticEvent by rememberUpdatedState(onHapticEvent)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(reactor, layoutMetrics) {
                detectTapGestures(
                    onPress = { offset ->
                        when (val hit = hitTestReactor(offset, reactor, layoutMetrics)) {
                            ReactorHitResult.Core -> {
                                latestOnInteractionStateChange(
                                    latestInteractionState.copy(
                                        isCorePressed = true,
                                        pressedSegmentId = null
                                    )
                                )
                            }

                            is ReactorHitResult.Segment -> {
                                latestOnInteractionStateChange(
                                    latestInteractionState.copy(
                                        isCorePressed = false,
                                        pressedSegmentId = hit.segmentId
                                    )
                                )
                            }

                            ReactorHitResult.None -> {
                                latestOnInteractionStateChange(
                                    ReactorInteractionState()
                                )
                            }
                        }

                        try {
                            tryAwaitRelease()
                        } finally {
                            latestOnInteractionStateChange(ReactorInteractionState())
                        }
                    },
                    onTap = { offset ->
                        when (val hit = hitTestReactor(offset, reactor, layoutMetrics)) {
                            ReactorHitResult.Core -> {
                                latestOnHapticEvent(ReactorHapticEvent.CoreTap)
                                latestOnCoreTap()
                            }

                            is ReactorHitResult.Segment -> {
                                latestOnHapticEvent(ReactorHapticEvent.SegmentTap)
                                latestOnSegmentTap(hit.segmentId)
                            }

                            ReactorHitResult.None -> Unit
                        }
                    },
                    onLongPress = { offset ->
                        when (val hit = hitTestReactor(offset, reactor, layoutMetrics)) {
                            is ReactorHitResult.Segment -> {
                                latestOnHapticEvent(ReactorHapticEvent.SegmentLongPress)
                                latestOnSegmentLongPress(hit.segmentId)
                            }

                            ReactorHitResult.Core,
                            ReactorHitResult.None -> Unit
                        }
                    }
                )
            }
    )
}

internal fun hitTestReactor(
    point: Offset,
    reactor: ReactorModel,
    layoutMetrics: ReactorLayoutMetrics
): ReactorHitResult {
    if (layoutMetrics.isCoreHit(point)) {
        return ReactorHitResult.Core
    }

    val geometryPoint = layoutMetrics.pointInGeometrySpace(point)
    val segmentLayouts = calculateSegmentLayouts(
        reactor = reactor,
        outerRadiusPx = layoutMetrics.outerRadiusPx,
        ringThicknessPx = layoutMetrics.ringThicknessPx,
        labelRadiusPx = 0f
    )

    val segment = segmentLayouts.firstOrNull { layout ->
        layout.contains(point = geometryPoint, center = layoutMetrics.center)
    }

    return if (segment != null) {
        ReactorHitResult.Segment(segment.model.id)
    } else {
        ReactorHitResult.None
    }
}
