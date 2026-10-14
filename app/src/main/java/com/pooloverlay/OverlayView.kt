package com.pooloverlay

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    private var aim: AimResult? = null

    private val aimPaint = Paint().apply {
        color = Color.argb(240, 255, 255, 0)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val ghostPaint = Paint().apply {
        color = Color.argb(220, 0, 255, 0)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val targetPaint = Paint().apply {
        color = Color.argb(220, 255, 80, 80)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
    }

    private val extendedPaint = Paint().apply {
        color = Color.argb(180, 0, 200, 255)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }

    private val bouncePaint = Paint().apply {
        color = Color.argb(220, 255, 150, 0)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val stopPaint = Paint().apply {
        color = Color.argb(230, 255, 60, 60)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val edgePaint = Paint().apply {
        color = Color.argb(180, 200, 200, 200)
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun update(a: AimResult?) {
        aim = a
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val a = aim ?: return
        if (!a.valid) return

        // 1. خطوط الـ 3 (اختياري)
        if (a.showThreeLines) {
            val dx = a.ghostX - a.cueX
            val dy = a.ghostY - a.cueY
            val len = hypot(dx, dy)
            if (len > 1f) {
                val nx = dx / len
                val ny = dy / len
                val perpX = -ny * 120f
                val perpY = nx * 120f
                canvas.drawLine(a.cueX - perpX, a.cueY - perpY, a.ghostX - perpX * 0.3f, a.ghostY - perpY * 0.3f, edgePaint)
                canvas.drawLine(a.cueX + perpX, a.cueY + perpY, a.ghostX + perpX * 0.3f, a.ghostY + perpY * 0.3f, edgePaint)
            }
        }

        // 2. الخط الأساسي الطويل (Cue → Ghost)
        canvas.drawLine(a.cueX, a.cueY, a.ghostX, a.ghostY, aimPaint)

        // 3. امتداد الخط بعد الـ Ghost (يوضح المسار)
        canvas.drawLine(a.ghostX, a.ghostY, a.extendedX, a.extendedY, extendedPaint)

        // 4. دايرة الـ Ghost
        canvas.drawCircle(a.ghostX, a.ghostY, 16f, ghostPaint)

        // 5. خط الهدف → Pocket
        canvas.drawLine(a.targetX, a.targetY, a.pocketX, a.pocketY, targetPaint)

        // 6. نقاط الارتداد
        for ((bx, by) in a.bouncePoints) {
            canvas.drawCircle(bx, by, 10f, bouncePaint)
        }

        // 7. نقطة التوقف
        a.stopPoint?.let { (sx, sy) ->
            canvas.drawCircle(sx, sy, 12f, stopPaint)
        }

        // 8. Edges around cue ball
        if (a.showEdges) {
            canvas.drawCircle(a.cueX, a.cueY, 22f, edgePaint)
        }
    }
}