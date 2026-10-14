package com.pooloverlay

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    private var balls: List<Ball> = emptyList()
    private var aim: AimResult? = null
    private var table: TableBounds? = null

    private val linePaint = Paint().apply {
        color = Color.argb(220, 255, 255, 0)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val ghostPaint = Paint().apply {
        color = Color.argb(180, 0, 255, 0)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun update(b: List<Ball>, a: AimResult?, t: TableBounds?) {
        balls = b; aim = a; table = t
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val a = aim ?: return
        if (!a.valid) return

        canvas.drawLine(a.cueX, a.cueY, a.ghostX, a.ghostY, linePaint)
        canvas.drawCircle(a.ghostX, a.ghostY, 14f, ghostPaint)
        canvas.drawLine(a.targetX, a.targetY, a.pocketX, a.pocketY, ghostPaint)

        if (a.showThreeLines) {
            val dx = a.ghostX - a.cueX
            val dy = a.ghostY - a.cueY
            val len = hypot(dx, dy)
            if (len > 1f) {
                val nx = dx / len; val ny = dy / len
                val perpX = -ny * 100f; val perpY = nx * 100f
                canvas.drawLine(a.cueX - perpX, a.cueY - perpY, a.ghostX, a.ghostY, ghostPaint)
                canvas.drawLine(a.cueX + perpX, a.cueY + perpY, a.ghostX, a.ghostY, ghostPaint)
            }
        }
    }
}