package com.sinrok.materialdown
import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.OvershootInterpolator
import kotlin.random.Random

class NavGestureService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private var pill: GesturePill? = null
    private var attached = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPill()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { removePill(); super.onDestroy() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun showPill() {
        if (attached) return
        val view = GesturePill()
        pill = view
        val params = WindowManager.LayoutParams(
            dp(132), dp(34), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(10) }
        try { windowManager.addView(view, params); attached = true } catch (_: Exception) { attached = false }
    }
    private fun removePill() {
        if (attached) { try { windowManager.removeView(pill) } catch (_: Exception) { }; attached = false }
        pill = null
    }

    private inner class GesturePill : View(this@NavGestureService) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var downY = 0f
        private var downAt = 0L
        private var tracking = false
        private val particles = mutableListOf<Particle>()
        private var particleAnimator: ValueAnimator? = null

        init {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(30).toFloat()
                setColor(0xE62A2D32.toInt())
            }
            elevation = dp(5).toFloat()
            isClickable = true
            contentDescription = "Swipe up for Home; swipe up and hold for Recents"
        }
        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            paint.color = 0xFFF4F1FA.toInt(); paint.alpha = 235
            canvas.drawRoundRect(w * .28f, h * .39f, w * .72f, h * .61f, h, h, paint)
            particles.forEach { p ->
                paint.color = 0xFFB8C9FF.toInt()
                paint.alpha = (255 * (1f - p.progress)).toInt().coerceIn(0, 255)
                canvas.drawCircle(p.x, p.y, p.radius * (1f - p.progress * .45f), paint)
            }
        }
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY; downAt = System.currentTimeMillis()
                    tracking = true; animatePress(true); return true
                }
                MotionEvent.ACTION_MOVE -> return true
                MotionEvent.ACTION_UP -> {
                    if (!tracking) return true
                    tracking = false; animatePress(false)
                    val swipeUp = downY - event.rawY
                    val elapsed = System.currentTimeMillis() - downAt
                    if (swipeUp > dp(28)) {
                        burst()
                        performGlobalAction(if (elapsed >= 420) GLOBAL_ACTION_RECENTS else GLOBAL_ACTION_HOME)
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> { tracking = false; animatePress(false); return true }
            }
            return true
        }
        private fun animatePress(pressed: Boolean) {
            animate().scaleX(if (pressed) .91f else 1f).scaleY(if (pressed) .82f else 1f)
                .setDuration(if (pressed) 90L else 220L)
                .setInterpolator(OvershootInterpolator(1.15f)).start()
        }
        private fun burst() {
            particles.clear()
            repeat(9) {
                particles += Particle(width * .5f + Random.nextFloat() * dp(30) - dp(15),
                    height * .5f, dp(2).toFloat() + Random.nextFloat() * dp(2))
            }
            particleAnimator?.cancel()
            particleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 360
                addUpdateListener { a ->
                    val progress = a.animatedValue as Float
                    particles.forEach { it.progress = progress }
                    invalidate()
                }
                start()
            }
        }
    }
    private data class Particle(val x: Float, val y: Float, val radius: Float, var progress: Float = 0f)
}
