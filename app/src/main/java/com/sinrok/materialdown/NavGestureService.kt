package com.sinrok.materialdown

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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

class NavGestureService : AccessibilityService(),
    android.content.SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: android.content.SharedPreferences
    private var pill: GesturePill? = null
    private var attached = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(StylePrefs.FILE, 0)
        prefs.registerOnSharedPreferenceChangeListener(this)
        showPill()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == StylePrefs.STYLE || key == StylePrefs.BACKGROUND ||
            key == StylePrefs.INDICATOR || key == StylePrefs.WIDTH || key == StylePrefs.PNG_URI) {
            pill?.reloadAppearance()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::prefs.isInitialized) prefs.unregisterOnSharedPreferenceChangeListener(this)
        removePill()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun showPill() {
        if (attached) return
        val view = GesturePill()
        pill = view
        val params = WindowManager.LayoutParams(
            dp(view.targetWidth()), dp(42),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(7)
        }
        try {
            windowManager.addView(view, params)
            attached = true
        } catch (_: Exception) { attached = false }
    }

    private fun removePill() {
        pill?.dispose()
        if (attached) {
            try { windowManager.removeView(pill) } catch (_: Exception) { }
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
        private var customDrawable: Drawable? = null
        private var currentStyle = StylePrefs.MATERIAL
        private var backgroundColor = 0xFF252B35.toInt()
        private var indicatorColor = 0xFFF1F3FA.toInt()
        private var customWidth = 144

        init {
            reloadAppearance()
            elevation = dp(7).toFloat()
            isClickable = true
            contentDescription = "Navigation pill. Swipe up for Home. Swipe up and hold for Recents."
        }

        fun targetWidth(): Int = if (currentStyle == StylePrefs.CUSTOM) dp(customWidth) else dp(144)

        fun reloadAppearance() {
            currentStyle = prefs.getString(StylePrefs.STYLE, StylePrefs.MATERIAL) ?: StylePrefs.MATERIAL
            backgroundColor = prefs.getInt(StylePrefs.BACKGROUND, 0xFF252B35.toInt())
            indicatorColor = prefs.getInt(StylePrefs.INDICATOR, 0xFFF1F3FA.toInt())
            customWidth = prefs.getInt(StylePrefs.WIDTH, 144).coerceIn(100, 220)
            customDrawable = null
            if (currentStyle == StylePrefs.CUSTOM) {
                prefs.getString(StylePrefs.PNG_URI, null)?.let { raw ->
                    try {
                        contentResolver.openInputStream(Uri.parse(raw))?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) customDrawable = BitmapDrawable(resources, bitmap)
                        }
                    } catch (_: Exception) { }
                }
            }
            background = when (currentStyle) {
                StylePrefs.IOS -> GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(32).toFloat()
                    setColor(0xFF17191D.toInt())
                }
                StylePrefs.CUSTOM -> GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(32).toFloat()
                    setColor(backgroundColor)
                    setStroke(dp(1), 0x38FFFFFF)
                }
                else -> GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0xF02D3440.toInt(), 0xF0181D26.toInt())
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(32).toFloat()
                    setStroke(dp(1), 0x38FFFFFF)
                }
            }
            if (attached) {
                try {
                    val params = layoutParams as WindowManager.LayoutParams
                    params.width = targetWidth()
                    windowManager.updateViewLayout(this, params)
                } catch (_: Exception) { }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (currentStyle == StylePrefs.MATERIAL) {
                paint.style = Paint.Style.FILL
                paint.color = 0x20FFFFFF
                canvas.drawRoundRect(w * 0.18f, h * 0.13f, w * 0.82f, h * 0.34f, h, h, paint)
            }

            if (currentStyle == StylePrefs.CUSTOM && customDrawable != null) {
                customDrawable?.setBounds(0, 0, width, height)
                customDrawable?.draw(canvas)
            } else {
                val indicatorWidth = if (currentStyle == StylePrefs.CUSTOM) w * 0.34f else w * (0.34f + dragProgress * 0.10f)
                val indicatorHeight = dp(4f)
                val centerX = w / 2f
                val centerY = h * 0.53f
                indicator.set(centerX - indicatorWidth / 2f, centerY - indicatorHeight / 2f,
                    centerX + indicatorWidth / 2f, centerY + indicatorHeight / 2f)
                paint.color = if (currentStyle == StylePrefs.IOS) 0xFFFFFFFF.toInt() else indicatorColor
                paint.alpha = 255
                canvas.drawRoundRect(indicator, indicatorHeight, indicatorHeight, paint)
                if (currentStyle == StylePrefs.MATERIAL && dragProgress > 0.02f) {
                    paint.color = 0xFFB8C9FF.toInt()
                    paint.alpha = (dragProgress * 95).toInt().coerceIn(0, 95)
                    canvas.drawRoundRect(centerX - indicatorWidth * 0.72f, centerY - dp(7),
                        centerX + indicatorWidth * 0.72f, centerY + dp(7), dp(12).toFloat(), dp(12).toFloat(), paint)
                }
            }

            particles.forEach { particle ->
                paint.color = particle.color
                paint.alpha = (255f * (1f - particle.progress)).toInt().coerceIn(0, 255)
                canvas.drawCircle(particle.positionX(), particle.positionY(),
                    particle.radius * (1f - particle.progress * 0.55f), paint)
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
                        dragProgress = ((downY - event.rawY) / dp(115f)).coerceIn(0f, 1f)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!tracking) return true
                    tracking = false
                    val upward = downY - event.rawY
                    val heldFor = System.currentTimeMillis() - downAt
                    animatePress(false)
                    if (upward > dp(30)) {
                        dragProgress = 0f
                        invalidate()
                        burst()
                        performGlobalAction(if (heldFor >= 420L) GLOBAL_ACTION_RECENTS else GLOBAL_ACTION_HOME)
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
            pressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = if (pressed) 110L else 310L
                interpolator = if (pressed) motion else OvershootInterpolator(1.18f)
                addUpdateListener {
                    val t = it.animatedValue as Float
                    val amount = if (pressed) t else 1f - t
                    scaleX = 1f + 0.075f * amount
                    scaleY = 1f - 0.09f * amount
                    translationY = -dp(1) * amount
                }
                start()
            }
        }

        private fun burst() {
            particles.clear()
            val palette = intArrayOf(0xFFB8C9FF.toInt(), 0xFF9ED9D0.toInt(), 0xFFF4D7A1.toInt())
            repeat(11) { index ->
                val angle = Math.PI * 2.0 * index / 11.0 + Random.nextFloat() * 0.22
                val speed = dp(22f) + Random.nextFloat() * dp(28f)
                particles += Particle(width / 2f, height / 2f,
                    cos(angle).toFloat() * speed, sin(angle).toFloat() * speed * 0.62f,
                    dp(1.5f + Random.nextFloat() * 1.8f), palette[index % palette.size])
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
        val startX: Float, val startY: Float, val velocityX: Float, val velocityY: Float,
        val radius: Float, val color: Int, var progress: Float = 0f
    ) {
        fun positionX() = startX + velocityX * progress
        fun positionY() = startY + velocityY * progress
    }
}
