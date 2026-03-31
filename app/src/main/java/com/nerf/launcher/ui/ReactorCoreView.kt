package com.nerf.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.nerf.launcher.R
import com.nerf.launcher.util.NerfTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the central NERF reactor/core motif with rotating segmented rings.
 */
class ReactorCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var neonCyan = Color.CYAN
    private var midRingA = Color.CYAN
    private var midRingB = Color.CYAN
    private var accentRing = Color.YELLOW
    private var coreGlow = Color.YELLOW
    private var coreText = Color.YELLOW
    private var hudOrange = Color.YELLOW
    private var hudMagenta = Color.MAGENTA
    private var hudLime = Color.GREEN
    private var ringPalette = intArrayOf(neonCyan, hudOrange, hudMagenta, hudLime)

    private val armorOuterRect = RectF()
    private val armorInnerRect = RectF()
    private val segmentedBandOuterRect = RectF()
    private val segmentedBandInnerRect = RectF()
    private val coreRingRect = RectF()

    private val armorStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val segmentedStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val accentStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val detailStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val detailFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
    }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 22_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationPhase = it.animatedValue as Float
            val basePhase = Math.toRadians(rotationPhase.toDouble())
            val secondaryPhase = Math.toRadians((rotationPhase * 0.47f).toDouble())
            val primaryPulse = abs(sin(basePhase)).toFloat()
            val layeredPulse = abs(cos(secondaryPhase)).toFloat()
            pulse = 0.965f + (primaryPulse * 0.023f) + (layeredPulse * 0.012f)
            invalidate()
        }
    }

    private val tapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 260L
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            val phase = it.animatedValue as Float
            val t = if (phase <= 0.5f) phase * 2f else (1f - phase) * 2f
            tapPulseBoost = t * 0.018f
            invalidate()
        }
    }

    private var rotationPhase: Float = 0f
    private var pulse: Float = 1f
    private var tapPulseBoost: Float = 0f

    init {
        updateTheme(ThemeManager.resolveActiveTheme(context))
    }

    fun updateTheme(theme: NerfTheme) {
        neonCyan = theme.hudInfoColor
        midRingA = theme.reactorMidAColor
        midRingB = theme.reactorMidBColor
        accentRing = theme.reactorAccentRingColor
        coreGlow = theme.reactorCoreGlowColor
        coreText = theme.accent
        hudOrange = theme.hudWarningColor
        hudMagenta = theme.hudAccentColor
        hudLime = theme.hudSuccessColor
        ringPalette = intArrayOf(neonCyan, hudOrange, hudMagenta, hudLime)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted && visibility == VISIBLE) animator.start()
    }

    override fun onDetachedFromWindow() {
        if (tapAnimator.isRunning) tapAnimator.cancel()
        if (animator.isRunning) animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            if (!animator.isStarted) animator.start()
        } else {
            if (animator.isRunning) animator.cancel()
        }
    }

    override fun performClick(): Boolean {
        if (tapAnimator.isRunning) tapAnimator.cancel()
        tapAnimator.start()
        return super.performClick()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (280 * resources.displayMetrics.density).toInt()
        val width = resolveSize(desired, widthMeasureSpec)
        val height = resolveSize(desired, heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.47f

        armorOuterRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        armorInnerRect.set(
            cx - radius * 0.83f,
            cy - radius * 0.83f,
            cx + radius * 0.83f,
            cy + radius * 0.83f
        )
        segmentedBandOuterRect.set(
            cx - radius * 0.72f,
            cy - radius * 0.72f,
            cx + radius * 0.72f,
            cy + radius * 0.72f
        )
        segmentedBandInnerRect.set(
            cx - radius * 0.6f,
            cy - radius * 0.6f,
            cx + radius * 0.6f,
            cy + radius * 0.6f
        )
        coreRingRect.set(
            cx - radius * 0.47f,
            cy - radius * 0.47f,
            cx + radius * 0.47f,
            cy + radius * 0.47f
        )
        // Base ambient layers and inset depth.
        fillPaint.color = Color.argb(70, Color.red(neonCyan), Color.green(neonCyan), Color.blue(neonCyan))
        canvas.drawCircle(cx, cy, radius * (pulse + tapPulseBoost), fillPaint)
        fillPaint.color = Color.argb(135, 12, 16, 20)
        canvas.drawCircle(cx, cy, radius * 0.91f, fillPaint)
        fillPaint.color = Color.argb(190, 5, 8, 10)
        canvas.drawCircle(cx, cy, radius * 0.78f, fillPaint)

        // Thick armor ring.
        armorStroke.strokeWidth = radius * 0.15f
        repeat(18) { index ->
            armorStroke.color = if (index % 2 == 0) Color.argb(220, 60, 72, 78) else Color.argb(150, 35, 45, 50)
            canvas.drawArc(
                armorOuterRect,
                rotationPhase * 0.18f + index * 20f,
                if (index % 3 == 0) 14f else 10f,
                false,
                armorStroke
            )
        }

        // Mechanical second armor pass to add edge definition.
        armorStroke.strokeWidth = radius * 0.038f
        armorStroke.color = Color.argb(185, 128, 143, 153)
        repeat(12) { index ->
            canvas.drawArc(armorInnerRect, -rotationPhase * 0.36f + index * 30f, 11f, false, armorStroke)
        }

        // Segmented active band (primary motion layer).
        segmentedStroke.strokeWidth = radius * 0.108f
        repeat(24) { index ->
            segmentedStroke.color = ringPalette[index % ringPalette.size]
            val sweep = if (index % 4 == 0) 8f else 5.5f
            canvas.drawArc(segmentedBandOuterRect, rotationPhase + index * 15f, sweep, false, segmentedStroke)
        }

        // Secondary opposite-moving segmented band for layered depth.
        segmentedStroke.strokeWidth = radius * 0.05f
        repeat(30) { index ->
            segmentedStroke.color = if (index % 2 == 0) midRingA else midRingB
            canvas.drawArc(segmentedBandInnerRect, -rotationPhase * 1.35f + index * 12f, 4.5f, false, segmentedStroke)
        }

        // Branded accent ring and neon gates.
        accentStroke.strokeWidth = radius * 0.072f
        accentStroke.color = accentRing
        repeat(6) { index ->
            canvas.drawArc(coreRingRect, index * 60f + rotationPhase * 0.7f, 20f, false, accentStroke)
        }

        detailStroke.strokeWidth = radius * 0.02f
        repeat(4) { index ->
            detailStroke.color = if (index % 2 == 0) hudOrange else hudMagenta
            canvas.drawArc(coreRingRect, index * 90f - rotationPhase * 1.8f + 12f, 17f, false, detailStroke)
        }

        // Mechanical asymmetry pods.
        val podOrbit = radius * 0.63f
        repeat(3) { index ->
            val angle = Math.toRadians((rotationPhase * 0.25f + index * 125f + 20f).toDouble())
            val px = cx + cos(angle).toFloat() * podOrbit
            val py = cy + sin(angle).toFloat() * podOrbit
            detailFill.color = if (index == 0) hudLime else Color.argb(210, 48, 60, 64)
            canvas.drawCircle(px, py, radius * if (index == 1) 0.026f else 0.02f, detailFill)
            detailStroke.color = Color.argb(180, 180, 200, 208)
            canvas.drawCircle(px, py, radius * 0.034f, detailStroke)
        }

        // Core inset stack.
        fillPaint.color = Color.argb(220, Color.red(coreGlow), Color.green(coreGlow), Color.blue(coreGlow))
        canvas.drawCircle(cx, cy, radius * 0.21f * (pulse + (tapPulseBoost * 0.85f)), fillPaint)
        fillPaint.color = Color.argb(230, 20, 26, 31)
        canvas.drawCircle(cx, cy, radius * 0.17f, fillPaint)
        fillPaint.color = Color.argb(255, 8, 10, 14)
        canvas.drawCircle(cx, cy, radius * 0.125f, fillPaint)

        // Center branding treatment.
        detailFill.color = coreText
        val barHeight = radius * 0.034f
        val barWidth = radius * 0.23f
        canvas.drawRect(cx - barWidth, cy - barHeight * 0.58f, cx + barWidth, cy + barHeight * 0.58f, detailFill)
        detailFill.color = Color.argb(230, 8, 10, 14)
        canvas.drawRect(cx - barWidth * 0.32f, cy - barHeight * 0.44f, cx - barWidth * 0.12f, cy + barHeight * 0.44f, detailFill)
        canvas.drawRect(cx + barWidth * 0.08f, cy - barHeight * 0.44f, cx + barWidth * 0.28f, cy + barHeight * 0.44f, detailFill)
        detailFill.color = coreText
        canvas.drawRect(cx - radius * 0.043f, cy - radius * 0.086f, cx + radius * 0.043f, cy + radius * 0.086f, detailFill)

        textPaint.color = neonCyan
        textPaint.textSize = radius * 0.075f
        canvas.drawText("NERF", cx, cy + radius * 0.29f, textPaint)
    }
}
