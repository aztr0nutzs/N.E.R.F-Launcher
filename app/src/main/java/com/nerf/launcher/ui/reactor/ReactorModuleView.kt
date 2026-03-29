package com.nerf.launcher.ui.reactor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.nerf.launcher.R
import com.nerf.launcher.util.assistant.AssistantState
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class ReactorModuleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onCoreTapped: (() -> Unit)? = null
    var onSectorTapped: ((Sector) -> Unit)? = null

    enum class Sector(val startAngle: Float, val sweepAngle: Float, val label: String) {
        TOP(225f, 90f, "SYS/NET"),
        RIGHT(315f, 90f, "AI ASST"),
        BOTTOM(45f, 90f, "DIAGNOSTIC"),
        LEFT(135f, 90f, "SETTINGS")
    }

    private val hudCyan = ContextCompat.getColor(context, R.color.nerf_hud_cyan)
    private val hudOrange = ContextCompat.getColor(context, R.color.nerf_hud_orange)
    private val hudMagenta = ContextCompat.getColor(context, R.color.nerf_hud_magenta)
    private val hudLime = ContextCompat.getColor(context, R.color.nerf_hud_lime)
    private val reactorMidA = ContextCompat.getColor(context, R.color.nerf_reactor_mid_a)
    private val reactorMidB = ContextCompat.getColor(context, R.color.nerf_reactor_mid_b)
    private val reactorAccent = ContextCompat.getColor(context, R.color.nerf_reactor_accent_ring)
    private val reactorCoreGlow = ContextCompat.getColor(context, R.color.nerf_reactor_core_glow)
    private val armorDark = Color.parseColor("#293039")
    private val armorMid = Color.parseColor("#58626B")
    private val interiorDark = Color.parseColor("#05070A")
    private val interiorMid = Color.parseColor("#0C1016")
    private val frameShadow = Color.parseColor("#020304")

    private var rotationAngle = 0f
    private var counterRotationAngle = 0f
    private var pulseScale = 1f
    private var corePulseAlpha = 235
    private var activeSector: Sector? = null
    private var highlightAlpha = 0
    private var assistantState = AssistantState.IDLE
    private var assistantSignalColor = Color.parseColor("#FF4400")
    private var assistantSignalStrength = 0f

    private var idleAnimator: ValueAnimator? = null
    private var highlightAnimator: ValueAnimator? = null
    private var assistantColorAnimator: ValueAnimator? = null
    private var assistantStrengthAnimator: ValueAnimator? = null

    private val armorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val activeBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val fineBandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val detailStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }

    private val orbitRect = RectF()
    private val armorInnerRect = RectF()
    private val activeBandRect = RectF()
    private val fineBandRect = RectF()
    private val coreRect = RectF()
    private val innerCoreRect = RectF()
    private val badgeRect = RectF()

    init {
        isClickable = true
        isFocusable = true
    }

    fun setAssistantState(state: AssistantState) {
        if (assistantState == state) return
        assistantState = state
        animateAssistantSignal(targetAssistantColor(state), targetAssistantStrength(state))
        if (state == AssistantState.IDLE) {
            activeSector = null
            highlightAlpha = 0
            invalidate()
        } else {
            triggerHighlightAnimation(Sector.RIGHT)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (windowVisibility == VISIBLE) {
            startIdleAnimations()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startIdleAnimations()
        } else {
            stopIdleAnimations()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (270 * resources.displayMetrics.density).toInt()
        val width = resolveSize(desired, widthMeasureSpec)
        val height = resolveSize(desired, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cx = width / 2f
            val cy = height / 2f
            val dx = event.x - cx
            val dy = event.y - cy
            val distance = hypot(dx, dy)

            val coreRadius = width * 0.18f
            val ringInnerRadius = width * 0.24f
            val ringOuterRadius = width * 0.45f

            if (distance <= coreRadius) {
                performClick()
                onCoreTapped?.invoke()
                return true
            } else if (distance in ringInnerRadius..ringOuterRadius) {
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angle < 0) angle += 360f

                determineSector(angle)?.let { sector ->
                    performClick()
                    triggerHighlightAnimation(sector)
                    onSectorTapped?.invoke(sector)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.47f

        orbitRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        armorInnerRect.set(
            cx - radius * 0.84f,
            cy - radius * 0.84f,
            cx + radius * 0.84f,
            cy + radius * 0.84f
        )
        activeBandRect.set(
            cx - radius * 0.66f,
            cy - radius * 0.66f,
            cx + radius * 0.66f,
            cy + radius * 0.66f
        )
        fineBandRect.set(
            cx - radius * 0.56f,
            cy - radius * 0.56f,
            cx + radius * 0.56f,
            cy + radius * 0.56f
        )
        coreRect.set(
            cx - radius * 0.38f,
            cy - radius * 0.38f,
            cx + radius * 0.38f,
            cy + radius * 0.38f
        )
        innerCoreRect.set(
            cx - radius * 0.25f,
            cy - radius * 0.25f,
            cx + radius * 0.25f,
            cy + radius * 0.25f
        )
        badgeRect.set(
            cx - radius * 0.15f,
            cy - radius * 0.15f,
            cx + radius * 0.15f,
            cy + radius * 0.15f
        )

        drawCircuitBackdrop(canvas, cx, cy, radius)
        drawArmorFrame(canvas)
        drawSectorBands(canvas)
        drawSatelliteNodes(canvas, cx, cy, radius)
        drawCore(canvas, cx, cy, radius)
    }

    override fun onDetachedFromWindow() {
        stopIdleAnimations()
        highlightAnimator?.cancel()
        assistantColorAnimator?.cancel()
        assistantStrengthAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun startIdleAnimations() {
        if (idleAnimator?.isRunning == true) return
        idleAnimator?.cancel()
        idleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            duration = 5600L
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                rotationAngle = fraction * 360f
                counterRotationAngle = -fraction * 220f
                pulseScale = 0.988f + (abs(sin(fraction * Math.PI * 2)).toFloat() * 0.018f)
                corePulseAlpha = (210 + abs(sin(fraction * Math.PI * 4)).toFloat() * 45f).toInt()
                invalidate()
            }
            start()
        }
    }

    private fun stopIdleAnimations() {
        idleAnimator?.cancel()
        idleAnimator = null
    }

    private fun triggerHighlightAnimation(sector: Sector) {
        activeSector = sector
        highlightAnimator?.cancel()
        highlightAnimator = ValueAnimator.ofInt(255, 0).apply {
            duration = 460L
            addUpdateListener { anim ->
                highlightAlpha = anim.animatedValue as Int
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeSector = null
                }
            })
            start()
        }
    }

    private fun animateAssistantSignal(color: Int, strength: Float) {
        assistantColorAnimator?.cancel()
        assistantStrengthAnimator?.cancel()

        assistantColorAnimator = ValueAnimator.ofArgb(assistantSignalColor, color).apply {
            duration = 260L
            addUpdateListener { anim ->
                assistantSignalColor = anim.animatedValue as Int
                invalidate()
            }
            start()
        }

        assistantStrengthAnimator = ValueAnimator.ofFloat(assistantSignalStrength, strength).apply {
            duration = 260L
            addUpdateListener { anim ->
                assistantSignalStrength = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun drawCircuitBackdrop(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        connectorPaint.strokeWidth = radius * 0.016f
        connectorPaint.color = Color.argb(70, 74, 94, 116)

        val horizontalInset = radius * 0.52f
        val midLeft = cx - radius * 0.93f
        val midRight = cx + radius * 0.93f
        val topY = cy - radius * 0.48f
        val bottomY = cy + radius * 0.48f

        canvas.drawLine(midLeft, cy, cx - horizontalInset, cy, connectorPaint)
        canvas.drawLine(cx + horizontalInset, cy, midRight, cy, connectorPaint)
        canvas.drawLine(cx, cy - horizontalInset, cx, topY, connectorPaint)
        canvas.drawLine(cx, cy + horizontalInset, cx, bottomY, connectorPaint)

        detailStrokePaint.strokeWidth = radius * 0.01f
        detailStrokePaint.color = Color.argb(46, 140, 160, 184)
        canvas.drawLine(midLeft - radius * 0.08f, cy - radius * 0.2f, cx - radius * 0.58f, cy - radius * 0.2f, detailStrokePaint)
        canvas.drawLine(midRight + radius * 0.08f, cy - radius * 0.2f, cx + radius * 0.58f, cy - radius * 0.2f, detailStrokePaint)
        canvas.drawLine(midLeft - radius * 0.08f, cy + radius * 0.22f, cx - radius * 0.58f, cy + radius * 0.22f, detailStrokePaint)
        canvas.drawLine(midRight + radius * 0.08f, cy + radius * 0.22f, cx + radius * 0.58f, cy + radius * 0.22f, detailStrokePaint)
    }

    private fun drawArmorFrame(canvas: Canvas) {
        armorPaint.strokeWidth = width * 0.06f
        repeat(18) { index ->
            armorPaint.color = if (index % 3 == 0) armorMid else armorDark
            canvas.drawArc(
                orbitRect,
                rotationAngle * 0.18f + index * 20f,
                if (index % 4 == 0) 14f else 10f,
                false,
                armorPaint
            )
        }

        armorPaint.strokeWidth = width * 0.018f
        armorPaint.color = Color.argb(180, Color.red(armorMid), Color.green(armorMid), Color.blue(armorMid))
        repeat(12) { index ->
            canvas.drawArc(armorInnerRect, counterRotationAngle + index * 30f, 10f, false, armorPaint)
        }
    }

    private fun drawSectorBands(canvas: Canvas) {
        activeBandPaint.strokeWidth = width * 0.055f
        Sector.values().forEach { sector ->
            val baseColor = sectorColor(sector)
            val blendedColor = if (sector == Sector.RIGHT && assistantSignalStrength > 0f) {
                ColorUtils.blendARGB(baseColor, assistantSignalColor, assistantSignalStrength.coerceIn(0f, 1f))
            } else {
                baseColor
            }
            activeBandPaint.color = blendedColor
            canvas.drawArc(activeBandRect, sector.startAngle + 4f, sector.sweepAngle - 8f, false, activeBandPaint)

            if (activeSector == sector) {
                activeBandPaint.color = blendedColor
                activeBandPaint.alpha = highlightAlpha
                activeBandPaint.setShadowLayer(width * 0.038f, 0f, 0f, blendedColor)
                canvas.drawArc(activeBandRect, sector.startAngle + 8f, sector.sweepAngle - 16f, false, activeBandPaint)
                activeBandPaint.clearShadowLayer()
                activeBandPaint.alpha = 255
            }
        }

        fineBandPaint.strokeWidth = width * 0.02f
        repeat(28) { index ->
            fineBandPaint.color = when (index % 4) {
                0 -> reactorMidA
                1 -> reactorMidB
                2 -> reactorAccent
                else -> hudMagenta
            }
            canvas.drawArc(fineBandRect, rotationAngle + index * 12f, if (index % 3 == 0) 6.5f else 4.5f, false, fineBandPaint)
        }

        detailStrokePaint.strokeWidth = width * 0.012f
        detailStrokePaint.color = Color.argb(210, Color.red(reactorAccent), Color.green(reactorAccent), Color.blue(reactorAccent))
        repeat(6) { index ->
            canvas.drawArc(coreRect, counterRotationAngle * 0.8f + index * 60f, 18f, false, detailStrokePaint)
        }
    }

    private fun drawSatelliteNodes(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val nodeOrbit = radius * 0.92f
        val nodeRadius = radius * 0.19f
        val secondaryRadius = radius * 0.13f

        Sector.values().forEach { sector ->
            val angleDegrees = when (sector) {
                Sector.TOP -> 270.0
                Sector.RIGHT -> 0.0
                Sector.BOTTOM -> 90.0
                Sector.LEFT -> 180.0
            }
            val radians = Math.toRadians(angleDegrees)
            val nx = cx + cos(radians).toFloat() * nodeOrbit
            val ny = cy + sin(radians).toFloat() * nodeOrbit
            val nodeColor = if (sector == Sector.RIGHT && assistantSignalStrength > 0f) {
                ColorUtils.blendARGB(sectorColor(sector), assistantSignalColor, 0.65f)
            } else {
                sectorColor(sector)
            }

            connectorPaint.strokeWidth = radius * 0.012f
            connectorPaint.color = ColorUtils.setAlphaComponent(nodeColor, 120)
            val linkStartRadius = radius * 0.72f
            val sx = cx + cos(radians).toFloat() * linkStartRadius
            val sy = cy + sin(radians).toFloat() * linkStartRadius
            canvas.drawLine(sx, sy, nx, ny, connectorPaint)

            fillPaint.color = Color.argb(230, Color.red(frameShadow), Color.green(frameShadow), Color.blue(frameShadow))
            canvas.drawCircle(nx, ny, nodeRadius * 1.12f, fillPaint)

            activeBandPaint.strokeWidth = radius * 0.028f
            activeBandPaint.color = nodeColor
            canvas.drawArc(
                RectF(nx - nodeRadius, ny - nodeRadius, nx + nodeRadius, ny + nodeRadius),
                rotationAngle * 0.55f + sector.startAngle,
                122f,
                false,
                activeBandPaint
            )
            activeBandPaint.alpha = if (sector == Sector.RIGHT) (160 + assistantSignalStrength * 95f).toInt() else 255
            canvas.drawArc(
                RectF(nx - nodeRadius * 0.82f, ny - nodeRadius * 0.82f, nx + nodeRadius * 0.82f, ny + nodeRadius * 0.82f),
                counterRotationAngle * 0.35f + sector.startAngle + 44f,
                88f,
                false,
                activeBandPaint
            )
            activeBandPaint.alpha = 255

            fillPaint.color = Color.argb(245, Color.red(interiorDark), Color.green(interiorDark), Color.blue(interiorDark))
            canvas.drawCircle(nx, ny, secondaryRadius, fillPaint)
            fillPaint.color = ColorUtils.setAlphaComponent(nodeColor, if (sector == Sector.RIGHT) 220 else 175)
            canvas.drawCircle(nx, ny, secondaryRadius * 0.52f, fillPaint)
        }
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val coreColor = if (assistantSignalStrength > 0f) assistantSignalColor else reactorCoreGlow

        fillPaint.color = ColorUtils.setAlphaComponent(coreColor, (110 + (assistantSignalStrength * 70f)).toInt())
        canvas.drawCircle(cx, cy, radius * 0.34f * pulseScale, fillPaint)

        fillPaint.color = Color.argb(235, Color.red(interiorMid), Color.green(interiorMid), Color.blue(interiorMid))
        canvas.drawCircle(cx, cy, radius * 0.28f, fillPaint)

        detailStrokePaint.strokeWidth = radius * 0.042f
        detailStrokePaint.color = ColorUtils.setAlphaComponent(coreColor, corePulseAlpha)
        canvas.drawArc(coreRect, rotationAngle * 1.15f, 278f, false, detailStrokePaint)

        detailStrokePaint.strokeWidth = radius * 0.018f
        repeat(8) { index ->
            detailStrokePaint.color = when (index % 4) {
                0 -> hudCyan
                1 -> hudOrange
                2 -> hudMagenta
                else -> hudLime
            }
            canvas.drawArc(innerCoreRect, counterRotationAngle + index * 44f, 16f, false, detailStrokePaint)
        }

        fillPaint.color = Color.argb(245, Color.red(interiorDark), Color.green(interiorDark), Color.blue(interiorDark))
        canvas.drawCircle(cx, cy, radius * 0.16f, fillPaint)

        fillPaint.color = ColorUtils.setAlphaComponent(coreColor, 245)
        canvas.drawRoundRect(badgeRect, radius * 0.04f, radius * 0.04f, fillPaint)
        fillPaint.color = Color.argb(215, Color.red(interiorDark), Color.green(interiorDark), Color.blue(interiorDark))
        canvas.drawRoundRect(
            RectF(
                badgeRect.left + radius * 0.018f,
                badgeRect.top + radius * 0.018f,
                badgeRect.right - radius * 0.018f,
                badgeRect.bottom - radius * 0.018f
            ),
            radius * 0.03f,
            radius * 0.03f,
            fillPaint
        )

        labelPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, 246)
        labelPaint.textSize = radius * 0.22f
        canvas.drawText("N", cx, cy + (labelPaint.textSize * 0.34f), labelPaint)

        labelPaint.color = ColorUtils.setAlphaComponent(reactorAccent, 220)
        labelPaint.textSize = radius * 0.052f
        canvas.drawText("NEXT GEN", cx, cy + radius * 0.44f, labelPaint)
    }

    private fun determineSector(touchAngle: Float): Sector? {
        for (sector in Sector.values()) {
            val start = sector.startAngle
            val end = (start + sector.sweepAngle) % 360f
            if (start < end) {
                if (touchAngle in start..end) return sector
            } else if (touchAngle >= start || touchAngle <= end) {
                return sector
            }
        }
        return null
    }

    private fun sectorColor(sector: Sector): Int {
        return when (sector) {
            Sector.TOP -> hudCyan
            Sector.RIGHT -> hudOrange
            Sector.BOTTOM -> hudMagenta
            Sector.LEFT -> hudLime
        }
    }

    private fun targetAssistantColor(state: AssistantState): Int {
        return when (state) {
            AssistantState.IDLE -> reactorCoreGlow
            AssistantState.WAKE -> reactorAccent
            AssistantState.LISTENING -> hudCyan
            AssistantState.THINKING -> hudOrange
            AssistantState.RESPONDING -> ColorUtils.blendARGB(hudOrange, reactorAccent, 0.38f)
            AssistantState.SPEAKING -> hudMagenta
            AssistantState.PROCESSING -> hudOrange
            AssistantState.AWAITING_INPUT -> hudCyan
            AssistantState.COOLING_DOWN -> reactorCoreGlow
            AssistantState.REBOOTING -> reactorAccent
            AssistantState.MUTED -> Color.parseColor("#9E9E9E")
            AssistantState.ERROR,
            AssistantState.SHUTTING_DOWN -> Color.parseColor("#FF5252")
        }
    }

    private fun targetAssistantStrength(state: AssistantState): Float {
        return when (state) {
            AssistantState.IDLE -> 0f
            AssistantState.WAKE -> 0.42f
            AssistantState.LISTENING -> 0.56f
            AssistantState.THINKING -> 0.74f
            AssistantState.RESPONDING -> 0.66f
            AssistantState.SPEAKING -> 0.88f
            AssistantState.PROCESSING -> 0.74f
            AssistantState.AWAITING_INPUT -> 0.56f
            AssistantState.COOLING_DOWN -> 0.18f
            AssistantState.REBOOTING -> 0.42f
            AssistantState.MUTED -> 0.3f
            AssistantState.ERROR,
            AssistantState.SHUTTING_DOWN -> 0.5f
        }
    }
}
