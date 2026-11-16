package com.sinrok.materialdown

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.PathInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.cos
import kotlin.math.sin
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

    override fun onDestroy() {
        removePill()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun showPill() {
        if (attached) return
        val view = GesturePill()
        pill = view
        val params = WindowManager.LayoutParams(
            dp(144), dp(42),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(7)
        }

        try {
            windowManager.addView(view, params)
            attached = true
        } catch (_: Exception) {
            attached = false
        }
    }

    private fun removePill() {
        pill?.dispose()
        if (attached) {
            try {
                windowManager.removeView(pill)
            } catch (_: Exception) {
                // Android may detach an accessibility overlay before the service stops.
            }
            attached = false
        }
        pill = null
    }

    private inner class GesturePill : View(this@NavGestureService) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val indicator = RectF()
        private val particles = ArrayList<Particle>(11)
        private val motion = PathInterpolator(0.2f, 0f, 0f, 1f)

        private var downY = 0f
        private var downAt = 0L
        private var tracking = false
        private var dragProgress = 0f
        private var burstAnimator: ValueAnimator? = null
        private var pressAnimator: ValueAnimator? = null

        init {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF02D3440.toInt(), 0xF0181D26.toInt())
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(32).toFloat()
                setStroke(dp(1), 0x38FFFFFF)
            }
            elevation = dp(7).toFloat()
            isClickable = true
            contentDescription =
                "Navigation pill. Swipe up for Home. Swipe up and hold for Recents."
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()

            // A restrained highlight gives the capsule a soft Material surface finish.
            paint.style = Paint.Style.FILL
            paint.color = 0x20FFFFFF
            canvas.drawRoundRect(
                w * 0.18f, h * 0.13f, w * 0.82f, h * 0.34f,
                h, h, paint
            )

            val indicatorWidth = w * (0.34f + dragProgress * 0.10f)
            val indicatorHeight = dp(4f)
            val centerX = w / 2f
            val centerY = h * 0.53f
            indicator.set(
                centerX - indicatorWidth / 2f,
                centerY - indicatorHeight / 2f,
                centerX + indicatorWidth / 2f,
                centerY + indicatorHeight / 2f
            )
            paint.color = 0xFFF1F3FA.toInt()
            paint.alpha = (225 + dragProgress * 30).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(indicator, indicatorHeight, indicatorHeight, paint)

            if (dragProgress > 0.02f) {
                paint.color = 0xFFB8C9FF.toInt()
                paint.alpha = (dragProgress * 95).toInt().coerceIn(0, 95)
                canvas.drawRoundRect(
                    centerX - indicatorWidth * 0.72f,
                    centerY - dp(7),
                    centerX + indicatorWidth * 0.72f,
                    centerY + dp(7),
                    dp(12).toFloat(), dp(12).toFloat(), paint
                )
            }

            particles.forEach { particle ->
                paint.color = particle.color
                paint.alpha = (255f * (1f - particle.progress)).toInt().coerceIn(0, 255)
                val radius = particle.radius * (1f - particle.progress * 0.55f)
                canvas.drawCircle(particle.positionX(), particle.positionY(), radius, paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                    downAt = System.currentTimeMillis()
                    tracking = true
                    dragProgress = 0f
                    animatePress(true)
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (tracking) {
                        val upward = (downY - event.rawY).coerceAtLeast(0f)
                        dragProgress = (upward / dp(115f)).coerceIn(0f, 1f)
                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (!tracking) return true
                    tracking = false
                    val upward = downY - event.rawY
                    val heldFor = System.currentTimeMillis() - downAt
                    val recognized = upward > dp(30)
                    animatePress(false)

                    if (recognized) {
                        dragProgress = 0f
                        invalidate()
                        burst()
                        performGlobalAction(
                            if (heldFor >= 420L) GLOBAL_ACTION_RECENTS else GLOBAL_ACTION_HOME
                        )
                    } else {
                        dragProgress = 0f
                        invalidate()
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    tracking = false
                    animatePress(false)
                    dragProgress = 0f
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun animatePress(pressed: Boolean) {
            pressAnimator?.cancel()
            pressAnimator = ValueAnimator.ofFloat(
                if (pressed) 0f else 1f,
                if (pressed) 1f else 0f
            ).apply {
                duration = if (pressed) 110L else 310L
                interpolator = if (pressed) motion else OvershootInterpolator(1.18f)
                addUpdateListener { animator ->
                    val t = animator.animatedValue as Float
                    if (pressed) {
                        scaleX = 1f + 0.075f * t
                        scaleY = 1f - 0.09f * t
                        translationY = -dp(1) * t
                    } else {
                        scaleX = 1.075f - 0.075f * t
                        scaleY = 0.91f + 0.09f * t
                        translationY = -dp(1) * (1f - t)
                    }
                }
                start()
            }
        }

        private fun burst() {
            particles.clear()
            val palette = intArrayOf(
                0xFFB8C9FF.toInt(),
                0xFF9ED9D0.toInt(),
                0xFFF4D7A1.toInt()
            )
            repeat(11) { index ->
                val angle = (Math.PI * 2.0 * index / 11.0) + Random.nextFloat() * 0.22
                val speed = dp(22f) + Random.nextFloat() * dp(28f)
                particles += Particle(
                    startX = width / 2f,
                    startY = height / 2f,
                    velocityX = cos(angle).toFloat() * speed,
                    velocityY = sin(angle).toFloat() * speed * 0.62f,
                    radius = dp(1.5f + Random.nextFloat() * 1.8f),
                    color = palette[index % palette.size]
                )
            }

            burstAnimator?.cancel()
            burstAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 430L
                interpolator = motion
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    particles.forEach { it.progress = progress }
                    invalidate()
                }
                start()
            }
        }

        fun dispose() {
            pressAnimator?.cancel()
            burstAnimator?.cancel()
            particles.clear()
        }
    }

    private data class Particle(
        val startX: Float,
        val startY: Float,
        val velocityX: Float,
        val velocityY: Float,
        val radius: Float,
        val color: Int,
        var progress: Float = 0f
    ) {
        fun positionX() = startX + velocityX * progress
        fun positionY() = startY + velocityY * progress
    }
}
