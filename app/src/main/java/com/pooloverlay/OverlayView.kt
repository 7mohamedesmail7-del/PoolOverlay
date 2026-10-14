package com.pooloverlay

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    private var aim: AimResult? = null

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    private val cueLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 255, 0)
        strokeWidth = 7f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ghostCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 0, 255, 100)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 60, 60)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }
    private val extendedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 220, 255)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(22f, 14f), 0f)
    }
    private val physicsPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 100, 255, 180)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }
    private val altPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 150, 150, 255)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }
    private val bounceFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 255, 150, 0); style = Paint.Style.FILL
    }
    private val bounceRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 255, 150, 0); strokeWidth = 4f; style = Paint.Style.STROKE
    }
    private val stopFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 255, 50, 50); style = Paint.Style.FILL
    }
    private val stopRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 255, 255, 255); strokeWidth = 3f; style = Paint.Style.STROKE
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 220, 220, 220); strokeWidth = 3f; style = Paint.Style.STROKE
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val scoreBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0); style = Paint.Style.FILL
    }

    fun update(a: AimResult?) {
        aim = a
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val a = aim ?: return
        if (!a.valid) return

        if (a.showAlternatives) {
            for (alt in a.alternatives) {
                canvas.drawLine(alt.cueX, alt.cueY, alt.ghostX, alt.ghostY, altPaint)
            }
        }

        if (a.showThreeLines) {
            val dx = a.ghostX - a.cueX
            val dy = a.ghostY - a.cueY
            val len = hypot(dx, dy)
            if (len > 1f) {
                val nx = dx / len
                val ny = dy / len
                val perpX = -ny * 150f
                val perpY = nx * 150f
                canvas.drawLine(a.cueX - perpX, a.cueY - perpY,
                    a.ghostX - perpX * 0.4f, a.ghostY - perpY * 0.4f, edgePaint)
                canvas.drawLine(a.cueX + perpX, a.cueY + perpY,
                    a.ghostX + perpX * 0.4f, a.ghostY + perpY * 0.4f, edgePaint)
            }
        }

        canvas.drawLine(a.cueX, a.cueY, a.ghostX, a.ghostY, cueLinePaint)
        canvas.drawLine(a.ghostX, a.ghostY, a.extendedX, a.extendedY, extendedLinePaint)

        if (a.showPhysicsPath && a.cueAfterPath.isNotEmpty()) {
            val path = Path()
            path.moveTo(a.cueAfterPath[0].x, a.cueAfterPath[0].y)
            for (i in 1 until a.cueAfterPath.size) {
                path.lineTo(a.cueAfterPath[i].x, a.cueAfterPath[i].y)
            }
            canvas.drawPath(path, physicsPathPaint)
        }

        if (a.showPhysicsPath && a.targetPath.isNotEmpty()) {
            val path = Path()
            path.moveTo(a.targetPath[0].x, a.targetPath[0].y)
            for (i in 1 until a.targetPath.size) {
                path.lineTo(a.targetPath[i].x, a.targetPath[i].y)
            }
            canvas.drawPath(path, physicsPathPaint)
        }

        canvas.drawCircle(a.ghostX, a.ghostY, 18f, ghostCirclePaint)
        canvas.drawLine(a.targetX, a.targetY, a.pocketX, a.pocketY, targetLinePaint)

        for ((bx, by) in a.bouncePoints) {
            canvas.drawCircle(bx, by, 14f, bounceRing)
            canvas.drawCircle(bx, by, 6f, bounceFill)
        }

        a.stopPoint?.let { (sx, sy) ->
            canvas.drawCircle(sx, sy, 16f, stopRing)
            canvas.drawCircle(sx, sy, 9f, stopFill)
        }

        val badgeX = 60f
        val badgeY = 100f
        val scoreText = "${a.topShotScore.toInt()} • ${a.difficulty}"
        canvas.drawRoundRect(badgeX - 20, badgeY - 45, badgeX + 260, badgeY + 10, 20f, 20f, scoreBgPaint)
        canvas.drawText(scoreText, badgeX, badgeY, scorePaint)
    }
}