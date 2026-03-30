package com.nerf.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.nerf.launcher.R
import kotlin.math.max

/**
 * Lightweight segmented HUD progress bar with configurable segment count.
 */
class SegmentedBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.nerf_hud_cyan)
    }

    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.nerf_segmented_bar_inactive)
    }

    private val rect = RectF()
    private var activeGradient: LinearGradient? = null
    private var activeGradientHighlightColor: Int = ContextCompat.getColor(context, R.color.nerf_hud_panel_text_primary)
    private var cachedWidth = -1
    private var cachedHeight = -1
    private var animatedProgress: Float = 0f
    private var progressAnimator: ValueAnimator? = null

    var progress: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, 100)
            if (field == clamped && animatedProgress == clamped.toFloat()) return
            field = clamped
            animateProgressTo(clamped.toFloat())
        }

    var segments: Int = 14
        set(value) {
            field = max(4, value)
            invalidate()
        }

    fun setActiveColor(color: Int) {
        activePaint.color = color
        activePaint.shader = null
        activeGradient = null
        invalidate()
    }

    fun setInactiveColor(color: Int) {
        inactivePaint.color = color
        invalidate()
    }

    fun setGradientHighlightColor(color: Int) {
        activeGradientHighlightColor = color
        activePaint.shader = null
        activeGradient = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        ensureGradient()

        val gap = width * 0.01f
        val segWidth = (width - gap * (segments - 1)) / segments
        val scaledProgress = (animatedProgress / 100f) * segments
        val activeSegments = scaledProgress.toInt()
        val partialFill = (scaledProgress - activeSegments).coerceIn(0f, 1f)
        val radius = height * 0.24f

        for (i in 0 until segments) {
            val left = i * (segWidth + gap)
            val right = left + segWidth
            val altitude = if (i % 2 == 0) 1f else 0.84f
            val top = (height * (1f - altitude)) * 0.5f
            val bottom = top + (height * altitude)
            rect.set(left, top, right, bottom)
            val paint = when {
                i < activeSegments -> activePaint
                i == activeSegments && partialFill > 0f -> {
                    activePaint.alpha = (90 + (partialFill * 165f)).toInt()
                    activePaint
                }
                else -> inactivePaint
            }
            canvas.drawRoundRect(rect, radius, radius, paint)
            activePaint.alpha = 255
        }
    }

    private fun ensureGradient() {
        if (cachedWidth == width && cachedHeight == height && activeGradient != null) return
        cachedWidth = width
        cachedHeight = height
        activeGradient = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            activePaint.color,
            activeGradientHighlightColor,
            Shader.TileMode.CLAMP
        )
        activePaint.shader = activeGradient
    }

    private fun animateProgressTo(target: Float) {
        progressAnimator?.cancel()
        val delta = kotlin.math.abs(target - animatedProgress)
        progressAnimator = ValueAnimator.ofFloat(animatedProgress, target).apply {
            duration = (160L + (delta * 2.2f).toLong()).coerceIn(160L, 420L)
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                animatedProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        progressAnimator = null
        activePaint.shader = null
        activeGradient = null
        super.onDetachedFromWindow()
    }
}
