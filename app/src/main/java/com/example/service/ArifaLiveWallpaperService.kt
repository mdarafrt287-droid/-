package com.example.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

class ArifaLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CyberNeonEngine()

    inner class CyberNeonEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var tick = 0f

        private val backgroundPaint = Paint().apply {
            color = Color.parseColor("#06040A")
            style = Paint.Style.FILL
        }

        private val cyanGridPaint = Paint().apply {
            color = Color.parseColor("#3306B6D4")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val purpleGridPaint = Paint().apply {
            color = Color.parseColor("#44A855F7")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val particlePaint = Paint().apply {
            color = Color.parseColor("#AAEC4899")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val drawRunnable = Runnable {
            drawFrame()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width.toFloat()
                    val height = canvas.height.toFloat()

                    // Draw deep void background
                    canvas.drawRect(0f, 0f, width, height, backgroundPaint)

                    tick += 0.03f
                    val horizon = height * 0.65f

                    // Perspective Cyber Neon Floor Grid
                    val gridCols = 12
                    val stepX = width / gridCols
                    for (i in 0..gridCols) {
                        val startX = stepX * i
                        canvas.drawLine(startX, horizon, (startX - width / 2) * 2.5f + width / 2, height, cyanGridPaint)
                    }

                    // Horizontal flowing lines
                    val numHoriz = 10
                    for (i in 1..numHoriz) {
                        val t = (i.toFloat() / numHoriz)
                        val y = horizon + (height - horizon) * (t * t)
                        val glowAlpha = (120 * t).toInt().coerceIn(30, 200)
                        purpleGridPaint.alpha = glowAlpha
                        canvas.drawLine(0f, y, width, y, purpleGridPaint)
                    }

                    // Floating glowing AI neural particles
                    for (p in 0..18) {
                        val px = (width * ((p * 37) % 100) / 100f)
                        val speed = (p % 4 + 1) * 0.5f
                        val py = ((height * 0.1f) + ((p * 73 + (tick * 40 * speed)) % (height * 0.6f)))
                        val radius = 3f + (sin(tick + p).toFloat() * 2f).coerceAtLeast(1f)
                        val alpha = (140 + (sin(tick * 2 + p) * 80).toInt()).coerceIn(40, 255)
                        particlePaint.alpha = alpha
                        canvas.drawCircle(px, py, radius, particlePaint)
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, 33) // ~30 fps smooth battery-efficient rendering
            }
        }
    }
}
