package com.nerf.launcher.ui.nodehunter

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.nerf.launcher.util.network.NetworkNode
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class NodeHunterGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // --- Paint Objects ---
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFFF") // Neon Cyan
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(5f, 0f, 0f, Color.parseColor("#00FFFF"))
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0A0A0") // Tactical Grey
        textSize = 24f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    private val floatingTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF00") // Success Green
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#00AA00"))
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9900") // Tactical Orange
        style = Paint.Style.STROKE
        strokeWidth = 6f
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#FF4400"))
    }

    private val dartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0077FF") // Elite Blue dart
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#0044FF"))
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 40 // Very subtle background element
    }

    private var nodeBitmap: Bitmap? = null
    private val destRect = RectF()

    // --- Game State ---
    private var targets = mutableListOf<TargetNode>()
    private var activeDarts = mutableListOf<Dart>()
    private var activeParticles = mutableListOf<Particle>()
    private var floatingTexts = mutableListOf<FloatingText>()
    private var crosshairPos = PointF(0f, 0f)
    private var radarAngle = 0f

    private var gameLoopAnimator: ValueAnimator? = null
    private var lastTime = System.currentTimeMillis()

    // --- Data Classes ---
    data class TargetNode(
        var x: Float,
        var y: Float,
        var vx: Float, // Velocity X for drift
        var vy: Float, // Velocity Y for drift
        val radius: Float = 110f,
        val nodeData: NetworkNode,
        var isHit: Boolean = false,
        var alpha: Int = 255
    )

    data class Dart(
        var startX: Float,
        var startY: Float,
        var targetX: Float,
        var targetY: Float,
        var progress: Float = 0f,
        val isHeavy: Boolean = false
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float = 1.0f,
        val color: Int
    )

    data class FloatingText(
        var x: Float,
        var y: Float,
        val text: String,
        var life: Float = 1.0f
    )

    init {
        setBackgroundColor(Color.parseColor("#0a0a0c"))
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        startGameLoop()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        crosshairPos.set(w / 2f, h / 2f)
    }

    fun populateNetworkNodes(nodes: List<NetworkNode>) {
        targets.clear()
        val padding = 150f
        val safeWidth = if (width > 0) width else 1080
        val safeHeight = if (height > 0) height else 1920

        for (node in nodes) {
            // Give each node a random, slow drift velocity
            val angle = Random.nextFloat() * Math.PI * 2
            val speed = Random.nextFloat() * 30f + 10f // Pixels per second

            targets.add(
                TargetNode(
                    x = Random.nextFloat() * (safeWidth - 2 * padding) + padding,
                    y = Random.nextFloat() * (safeHeight - 2 * padding) + padding,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    nodeData = node
                )
            )
        }
        invalidate()
    }

    private fun startGameLoop() {
        gameLoopAnimator?.cancel()
        gameLoopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            duration = 1000L
            addUpdateListener {
                val now = System.currentTimeMillis()
                val delta = (now - lastTime) / 1000f
                lastTime = now
                updateGameLogic(delta)
                invalidate()
            }
        }
        gameLoopAnimator?.start()
    }

    private fun updateGameLogic(delta: Float) {
        // Update Radar
        radarAngle = (radarAngle + delta * 90f) % 360f

        // Update Darts
        val dartsToRemove = mutableListOf<Dart>()
        for (dart in activeDarts) {
            dart.progress += delta * 6f 
            if (dart.progress >= 1f) {
                checkCollisions(dart.targetX, dart.targetY, dart.isHeavy)
                dartsToRemove.add(dart)
            }
        }
        activeDarts.removeAll(dartsToRemove)

        // Update Targets (Drift and Bounds Check)
        val targetsToRemove = mutableListOf<TargetNode>()
        for (target in targets) {
            if (target.isHit) {
                target.alpha -= (delta * 400).toInt() 
                if (target.alpha <= 0) {
                    targetsToRemove.add(target)
                }
            } else {
                // Apply Drift Physics
                target.x += target.vx * delta
                target.y += target.vy * delta

                // Bounce off edges softly
                if (target.x - target.radius < 0 || target.x + target.radius > width) {
                    target.vx *= -1f
                    target.x = target.x.coerceIn(target.radius, width - target.radius)
                }
                if (target.y - target.radius < 0 || target.y + target.radius > height) {
                    target.vy *= -1f
                    target.y = target.y.coerceIn(target.radius, height - target.radius)
                }
            }
        }
        targets.removeAll(targetsToRemove)

        // Update Particles
        val particlesToRemove = mutableListOf<Particle>()
        for (p in activeParticles) {
            p.x += p.vx * delta * 60f
            p.y += p.vy * delta * 60f
            p.life -= delta * 1.5f
            if (p.life <= 0f) {
                particlesToRemove.add(p)
            }
        }
        activeParticles.removeAll(particlesToRemove)

        // Update Floating Texts
        val textsToRemove = mutableListOf<FloatingText>()
        for (t in floatingTexts) {
            t.y -= delta * 50f // Float up slowly
            t.life -= delta * 0.8f // Fade out
            if (t.life <= 0f) {
                textsToRemove.add(t)
            }
        }
        floatingTexts.removeAll(textsToRemove)
    }

    private fun checkCollisions(hitX: Float, hitY: Float, isHeavy: Boolean) {
        val effectiveRadius = if (isHeavy) 250f else 0f 
        
        for (target in targets) {
            if (!target.isHit) {
                val distance = hypot(target.x - hitX, target.y - hitY)
                if (distance <= target.radius + effectiveRadius) {
                    target.isHit = true
                    spawnExplosion(target.x, target.y, if (isHeavy) Color.RED else Color.parseColor("#FF9900"))
                    floatingTexts.add(FloatingText(target.x, target.y - target.radius, "+ DATA EXTRACTED"))
                }
            }
        }
    }

    private fun spawnExplosion(x: Float, y: Float, primaryColor: Int) {
        val colors = listOf(primaryColor, Color.parseColor("#00FFFF"), Color.WHITE)
        for (i in 0..30) { 
            val angle = Random.nextFloat() * Math.PI * 2
            val speed = Random.nextFloat() * 20f + 5f
            activeParticles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    color = colors.random()
                )
            )
        }
    }

    // --- Input Handling ---

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                crosshairPos.set(event.x, event.y)
                if (event.action == MotionEvent.ACTION_DOWN) {
                    fireDart(event.x, event.y, false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun fireYellowAction() {
        fireDart(crosshairPos.x, crosshairPos.y, false)
    }

    fun fireRedAction() {
        fireDart(crosshairPos.x, crosshairPos.y, true)
        crosshairPos.y -= 50f 
    }

    private fun fireDart(targetX: Float, targetY: Float, isHeavy: Boolean) {
        activeDarts.add(Dart(width / 2f, height.toFloat(), targetX, targetY, 0f, isHeavy))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val speed = 50f
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { crosshairPos.y -= speed; return true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { crosshairPos.y += speed; return true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { crosshairPos.x -= speed; return true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { crosshairPos.x += speed; return true }
            KeyEvent.KEYCODE_PROG_RED -> { fireRedAction(); return true }
            KeyEvent.KEYCODE_PROG_YELLOW -> { fireYellowAction(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    // --- Rendering ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f

        // 1. Render Background Radar Sweep
        canvas.drawCircle(cx, cy, width * 0.4f, radarPaint)
        canvas.drawCircle(cx, cy, width * 0.2f, radarPaint)
        canvas.drawLine(cx, cy, cx + cos(Math.toRadians(radarAngle.toDouble())).toFloat() * width * 0.5f, cy + sin(Math.toRadians(radarAngle.toDouble())).toFloat() * width * 0.5f, radarPaint)

        // 2. Render Targets and Enhanced Data Readouts
        for (target in targets) {
            crosshairPaint.alpha = target.alpha
            canvas.drawCircle(target.x, target.y, target.radius, crosshairPaint)
            canvas.drawCircle(target.x, target.y, target.radius * 0.7f, crosshairPaint)

            textPaint.alpha = target.alpha
            subTextPaint.alpha = target.alpha

            // Main IP Display
            canvas.drawText(target.nodeData.ipAddress, target.x, target.y - target.radius - 20, textPaint)
            
            // Sub-data (MAC & Device Type) rendered below the target
            val subTextY = target.y + target.radius + 30
            if (target.isHit) {
                textPaint.color = Color.parseColor("#FF0000") 
                canvas.drawText("OFFLINE", target.x, subTextY + 10, textPaint)
                textPaint.color = Color.parseColor("#00FFFF") 
            } else {
                canvas.drawText("${target.nodeData.pingMs}ms", target.x, subTextY, textPaint)
                canvas.drawText(target.nodeData.macAddress, target.x, subTextY + 30, subTextPaint)
                canvas.drawText("[ ${target.nodeData.deviceType} ]", target.x, subTextY + 55, subTextPaint)
            }
        }

        // 3. Render Darts
        for (dart in activeDarts) {
            val currentX = dart.startX + (dart.targetX - dart.startX) * dart.progress
            val currentY = dart.startY + (dart.targetY - dart.startY) * dart.progress
            
            if (dart.isHeavy) {
                dartPaint.color = Color.RED
                canvas.drawCircle(currentX, currentY, 20f, dartPaint)
            } else {
                dartPaint.color = Color.parseColor("#0077FF")
                val dartRect = RectF(currentX - 8f, currentY - 25f, currentX + 8f, currentY + 25f)
                canvas.drawRoundRect(dartRect, 8f, 8f, dartPaint)
            }
        }

        // 4. Render Particles
        for (p in activeParticles) {
            particlePaint.color = p.color
            particlePaint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.life * 10f, particlePaint) 
        }

        // 5. Render Floating Text (Scores/Status)
        for (t in floatingTexts) {
            floatingTextPaint.alpha = (t.life * 255).toInt().coerceIn(0, 255)
            canvas.drawText(t.text, t.x, t.y, floatingTextPaint)
        }

        // 6. Render Crosshair Overlay (Always on top)
        crosshairPaint.alpha = 255
        canvas.drawLine(crosshairPos.x - 50f, crosshairPos.y, crosshairPos.x + 50f, crosshairPos.y, crosshairPaint)
        canvas.drawLine(crosshairPos.x, crosshairPos.y - 50f, crosshairPos.x, crosshairPos.y + 50f, crosshairPaint)
        canvas.drawCircle(crosshairPos.x, crosshairPos.y, 30f, crosshairPaint)
        canvas.drawPoint(crosshairPos.x, crosshairPos.y, crosshairPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameLoopAnimator?.cancel()
        nodeBitmap?.recycle()
        nodeBitmap = null
    }
}
