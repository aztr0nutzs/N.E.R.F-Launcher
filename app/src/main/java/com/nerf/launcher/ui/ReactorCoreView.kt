package com.nerf.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.nerf.launcher.R
import kotlin.math.min

/**
 * Draws the central NERF reactor/core motif with rotating segmented rings.
 */
class ReactorCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val neonCyan = ContextCompat.getColor(context, R.color.nerf_hud_cyan)
    private val midRingA = ContextCompat.getColor(context, R.color.nerf_reactor_mid_a)
    private val midRingB = ContextCompat.getColor(context, R.color.nerf_reactor_mid_b)
    private val accentRing = ContextCompat.getColor(context, R.color.nerf_reactor_accent_ring)
    private val coreGlow = ContextCompat.getColor(context, R.color.nerf_reactor_core_glow)
    private val coreText = ContextCompat.getColor(context, R.color.nerf_accent)

    private val ringPalette = intArrayOf(
        neonCyan,
        ContextCompat.getColor(context, R.color.nerf_hud_orange),
        ContextCompat.getColor(context, R.color.nerf_hud_magenta),
        ContextCompat.getColor(context, R.color.nerf_hud_lime)
    )

    private val outerRect = RectF()
    private val midRect = RectF()
    private val innerRect = RectF()

    private val outerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val midStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val innerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val accentStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
    }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 18_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationPhase = it.animatedValue as Float
            pulse = 0.93f + 0.07f * kotlin.math.abs(kotlin.math.sin(Math.toRadians(rotationPhase.toDouble()))).toFloat()
            invalidate()
        }
    }

    private var rotationPhase: Float = 0f
    private var pulse: Float = 1f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        if (animator.isRunning) animator.cancel()
        super.onDetachedFromWindow()
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

        val outerWidth = radius * 0.13f
        outerStroke.strokeWidth = outerWidth

        val midWidth = radius * 0.1f
        midStroke.strokeWidth = midWidth

        accentStroke.strokeWidth = radius * 0.05f

        outerRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        midRect.set(
            cx - radius * 0.73f,
            cy - radius * 0.73f,
            cx + radius * 0.73f,
            cy + radius * 0.73f
        )
        innerRect.set(
            cx - radius * 0.5f,
            cy - radius * 0.5f,
            cx + radius * 0.5f,
            cy + radius * 0.5f
        )

        // Ambient glow
        innerFill.color = Color.argb(
            90,
            Color.red(neonCyan),
            Color.green(neonCyan),
            Color.blue(neonCyan)
        )
        canvas.drawCircle(cx, cy, radius * 0.98f * pulse, innerFill)

        // Outer segmented ring
        repeat(16) { idx ->
            outerStroke.color = ringPalette[idx % ringPalette.size]
            val start = rotationPhase + idx * 22.5f
            canvas.drawArc(outerRect, start, 14f, false, outerStroke)
        }

        // Middle ring
        repeat(10) { idx ->
            midStroke.color = if (idx % 2 == 0) midRingA else midRingB
            canvas.drawArc(midRect, -rotationPhase * 1.5f + idx * 36f, 20f, false, midStroke)
        }

        // Core ring accents
        accentStroke.color = accentRing
        repeat(4) { idx ->
            canvas.drawArc(innerRect, idx * 90f + rotationPhase * 2f, 42f, false, accentStroke)
        }

        // Core center and label
        innerFill.color = coreGlow
        canvas.drawCircle(cx, cy, radius * 0.22f * pulse, innerFill)
        innerFill.color = Color.parseColor("#1A1A1A")
        canvas.drawCircle(cx, cy, radius * 0.13f, innerFill)

        textPaint.color = coreText
        textPaint.textSize = radius * 0.26f
        canvas.drawText("N", cx, cy + radius * 0.1f, textPaint)
    }
}
