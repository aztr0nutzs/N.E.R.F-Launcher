package com.nerf.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
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

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var segments: Int = 14
        set(value) {
            field = max(4, value)
            invalidate()
        }

    fun setActiveColor(color: Int) {
        activePaint.color = color
        invalidate()
    }

    fun setInactiveColor(color: Int) {
        inactivePaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val gap = width * 0.012f
        val segWidth = (width - gap * (segments - 1)) / segments
        val activeSegments = ((progress / 100f) * segments).toInt()
        val radius = height * 0.18f

        for (i in 0 until segments) {
            val left = i * (segWidth + gap)
            val right = left + segWidth
            rect.set(left, 0f, right, height.toFloat())
            canvas.drawRoundRect(rect, radius, radius, if (i < activeSegments) activePaint else inactivePaint)
        }
    }
}
