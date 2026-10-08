package com.goodwy.smsmessenger.views

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Thinking Orbs "working" state, ported from the original MIT-licensed
 * thinking-orbs geometry (Jakub Antalik). This Android implementation keeps
 * the same orbit/particle math but renders directly with Android Canvas.
 *
 * Original project: https://github.com/Jakubantalik/thinking-orbs
 * License: MIT
 */
class ThinkingOrbView(context: Context) : View(context) {

    private data class Dot(
        val x: Float,
        val y: Float,
        val z: Float,
        val radius: Float,
        val white: Float,
        val alpha: Float = 1f
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val density = resources.displayMetrics.density
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 10_000L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidate() }
    }

    private val darkTheme: Boolean
        get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ValueAnimator.areAnimatorsEnabled()) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val logicalSize = min(width, height) / density
        if (logicalSize <= 0f) return

        val t = if (ValueAnimator.areAnimatorsEnabled()) {
            animator.animatedFraction.toDouble() * 10.0 * 1.885
        } else {
            0.0
        }

        canvas.save()
        canvas.scale(density, density)

        val dots = buildFrame(logicalSize, t)
        for (dot in dots) {
            val white = dot.white.coerceIn(0f, 1f)
            val gray = if (darkTheme) {
                ((1f - white) * 255f).toInt()
            } else {
                (white * 255f).toInt()
            }

            paint.color = android.graphics.Color.rgb(gray, gray, gray)
            paint.alpha = (dot.alpha.coerceIn(0f, 1f) * 255f).toInt()

            canvas.drawCircle(dot.x, dot.y, dot.radius, paint)
        }

        canvas.restore()
    }

    private fun buildFrame(size: Float, t: Double): List<Dot> {
        val cx = size / 2f
        val cy = size / 2f
        val radius = (size / 2f) * 0.82f
        val radiusScale = (size / 300f).toDouble().pow(0.6)

        val yaw = t * 0.12
        val tilt = 0.3
        val st = sin(tilt)
        val ct = cos(tilt)
        val sy = sin(yaw)
        val cyaw = cos(yaw)

        fun project(x: Double, y: Double, z: Double): Triple<Float, Float, Float> {
            val x1 = x * cyaw + z * sy
            val z1 = -x * sy + z * cyaw
            val y1 = y * ct - z1 * st
            val z2 = y * st + z1 * ct
            return Triple(
                (cx + x1).toFloat(),
                (cy - y1).toFloat(),
                z2.toFloat()
            )
        }

        val dots = ArrayList<Dot>(12 * (40 + 3))
        val orbitCount = 12
        val ghostCount = 40
        val particles = 3

        for (orbit in 0 until orbitCount) {
            val h1 = hashD(orbit.toDouble(), 1.7)
            val h2 = hashD(orbit.toDouble(), 5.2)
            val h3 = hashD(orbit.toDouble(), 8.9)

            val orbitRadius = radius * (0.45 + 0.52 * h1)
            val theta = h1 * 2.0 * Math.PI
            val phi = acos(2.0 * h2 - 1.0)

            val nx = sin(phi) * cos(theta)
            val ny = cos(phi)
            val nz = sin(phi) * sin(theta)

            var ux = -ny
            var uy = nx
            val uz = 0.0
            val ul = max(1e-6, sqrt(ux * ux + uy * uy))
            ux /= ul
            uy /= ul

            val vx = ny * uz - nz * uy
            val vy = nz * ux - nx * uz
            val vz = nx * uy - ny * ux
            val speed = (0.25 + 0.55 * h3) * if (h3 > 0.5) 1.0 else -1.0

            for (k in 0 until ghostCount) {
                val a = (k.toDouble() / ghostCount) * 2.0 * Math.PI
                val projected = project(
                    (ux * cos(a) + vx * sin(a)) * orbitRadius,
                    (uy * cos(a) + vy * sin(a)) * orbitRadius,
                    (uz * cos(a) + vz * sin(a)) * orbitRadius
                )
                val depth = (projected.third / orbitRadius.toFloat() + 1f) / 2f
                dots.add(
                    Dot(
                        projected.first,
                        projected.second,
                        projected.third,
                        (0.9 * radiusScale).toFloat().coerceAtLeast(0.3f),
                        0.72f,
                        (0.5 * (0.4 + 0.6 * depth)).toFloat()
                    )
                )
            }

            for (particle in 0 until particles) {
                val a = t * speed +
                    (particle.toDouble() / particles) * 2.0 * Math.PI +
                    h2 * 6.0
                val projected = project(
                    (ux * cos(a) + vx * sin(a)) * orbitRadius,
                    (uy * cos(a) + vy * sin(a)) * orbitRadius,
                    (uz * cos(a) + vz * sin(a)) * orbitRadius
                )
                val depth = (projected.third / orbitRadius.toFloat() + 1f) / 2f
                dots.add(
                    Dot(
                        projected.first,
                        projected.second,
                        projected.third,
                        ((1.2 + 1.6 * depth) * radiusScale).toFloat().coerceAtLeast(0.3f),
                        (0.3 - 0.22 * depth).toFloat()
                    )
                )
            }
        }

        dots.sortBy { it.z }
        return dots
    }

    private fun hashD(a: Double, b: Double): Double {
        val h = sin(a * 12.9898 + b * 78.233) * 43758.5453
        return h - kotlin.math.floor(h)
    }

    private fun Double.pow(power: Double): Double = Math.pow(this, power)
}
