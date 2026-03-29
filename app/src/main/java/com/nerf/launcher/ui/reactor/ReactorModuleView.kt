package com.nerf.launcher.ui.reactor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.atan2
import kotlin.math.hypot

class ReactorModuleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onCoreTapped: (() -> Unit)? = null
    var onSectorTapped: ((Sector) -> Unit)? = null

    enum class Sector(val startAngle: Float, val sweepAngle: Float, val color: Int, val label: String) {
        TOP(225f, 90f, Color.parseColor("#00FFFF"), "SYS/NET"),
        RIGHT(315f, 90f, Color.parseColor("#FFCC00"), "AI ASST"),
        BOTTOM(45f, 90f, Color.parseColor("#FF00FF"), "DIAGNOSTIC"),
        LEFT(135f, 90f, Color.parseColor("#0077FF"), "SETTINGS")
    }

    private var rotationAngle = 0f
    private var corePulseAlpha = 255
    private var activeSector: Sector? = null
    private var highlightAlpha = 0

    private var gameLoopAnimator: ValueAnimator? = null
    private var highlightAnimator: ValueAnimator? = null

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4400") 
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#FF0000"))
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 25f
        strokeCap = Paint.Cap.ROUND
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 35f
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val outerTechRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f, 5f, 10f), 0f)
    }

    private val rectF = RectF()

    init {
        startIdleAnimations()
    }

    private fun startIdleAnimations() {
        gameLoopAnimator?.cancel()
        gameLoopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            duration = 4000L 
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                rotationAngle = fraction * 360f
                corePulseAlpha = (150 + Math.abs(Math.sin(fraction * Math.PI * 4)) * 105).toInt()
                invalidate()
            }
        }
        gameLoopAnimator?.start()
    }

    private fun triggerHighlightAnimation(sector: Sector) {
        activeSector = sector
        highlightAnimator?.cancel()
        highlightAnimator = ValueAnimator.ofInt(255, 0).apply {
            duration = 400L
            addUpdateListener { anim ->
                highlightAlpha = anim.animatedValue as Int
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeSector = null
                }
            })
        }
        highlightAnimator?.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val cx = width / 2f
            val cy = height / 2f
            
            val dx = event.x - cx
            val dy = event.y - cy
            val distance = hypot(dx, dy)

            val coreRadius = width * 0.15f
            val ringInnerRadius = width * 0.25f
            val ringOuterRadius = width * 0.45f

            if (distance <= coreRadius) {
                onCoreTapped?.invoke()
                return true
            } else if (distance in ringInnerRadius..ringOuterRadius) {
                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angle < 0) angle += 360f

                val tappedSector = determineSector(angle)
                if (tappedSector != null) {
                    triggerHighlightAnimation(tappedSector)
                    onSectorTapped?.invoke(tappedSector)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun determineSector(touchAngle: Float): Sector? {
        for (sector in Sector.values()) {
            val start = sector.startAngle
            val end = (start + sector.sweepAngle) % 360f
            
            if (start < end) {
                if (touchAngle in start..end) return sector
            } else {
                if (touchAngle >= start || touchAngle <= end) return sector
            }
        }
        return null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) / 2f

        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)
        canvas.drawCircle(cx, cy, radius * 0.9f, outerTechRingPaint)
        canvas.restore()

        val ringRadius = radius * 0.65f
        rectF.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)

        for (sector in Sector.values()) {
            ringPaint.color = sector.color
            canvas.drawArc(rectF, sector.startAngle + 2f, sector.sweepAngle - 4f, false, ringPaint)

            if (activeSector == sector) {
                highlightPaint.color = sector.color
                highlightPaint.alpha = highlightAlpha
                highlightPaint.setShadowLayer(20f, 0f, 0f, sector.color)
                canvas.drawArc(rectF, sector.startAngle + 2f, sector.sweepAngle - 4f, false, highlightPaint)
                highlightPaint.clearShadowLayer()
            }
        }

        corePaint.alpha = corePulseAlpha
        val coreRadius = radius * 0.3f
        canvas.drawCircle(cx, cy, coreRadius, corePaint)
        
        canvas.drawText("N", cx, cy + (textPaint.textSize / 3), textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameLoopAnimator?.cancel()
        highlightAnimator?.cancel()
    }
}
