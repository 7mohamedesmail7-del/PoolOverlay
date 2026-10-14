package com.pooloverlay

import kotlin.math.hypot
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

data class AimResult(
    val valid: Boolean,
    val cueX: Float, val cueY: Float,
    val ghostX: Float, val ghostY: Float,
    val targetX: Float, val targetY: Float,
    val pocketX: Float, val pocketY: Float,
    val extendedX: Float, val extendedY: Float,
    val bouncePoints: List<Pair<Float, Float>>,
    val stopPoint: Pair<Float, Float>?,
    val showEdges: Boolean,
    val showThreeLines: Boolean
)

class GeometryEngine {

    data class Config(
        var lineLength: Int = 2000,
        var lineThickness: Int = 6,
        var lineOpacity: Int = 230,
        var show3Lines: Boolean = true,
        var showEdges: Boolean = false,
        var autoLengthen: Boolean = true,
        var showBounce: Boolean = true,
        var showStopPoint: Boolean = true
    )

    var config = Config()

    fun computeAim(balls: List<Ball>, table: TableBounds): AimResult? {
        val cue = balls.firstOrNull { it.color == 0 } ?: return null
        val pockets = pocketPositions(table)
        var best: AimResult? = null
        var bestScore = -Float.MAX_VALUE

        for (ball in balls) {
            if (ball === cue || ball.color == 0) continue
            for (p in pockets) {
                val aim = buildAim(cue, ball, p, table)
                val dxCue = hypot(aim.ghostX - cue.x, aim.ghostY - cue.y)
                val dxTgt = hypot(p.first - ball.x, p.second - ball.y)
                val score = 300f - dxCue * 0.05f - dxTgt * 0.1f
                if (score > bestScore) {
                    bestScore = score
                    best = aim
                }
            }
        }
        return best
    }

    private fun buildAim(cue: Ball, target: Ball, pocket: Pair<Float, Float>, t: TableBounds): AimResult {
        val tpx = pocket.first - target.x
        val tpy = pocket.second - target.y
        val dist = hypot(tpx, tpy).coerceAtLeast(0.001f)

        // Ghost ball = موقع الكورة البيضاء لما تلمس الهدف
        val ghostX = target.x - (tpx / dist) * target.radius * 2f
        val ghostY = target.y - (tpy / dist) * target.radius * 2f

        // اتجاه الخط
        val dx = ghostX - cue.x
        val dy = ghostY - cue.y
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val ux = dx / len
        val uy = dy / len

        // امتداد الخط بعد الـ Ghost (يوضح مسار الكورة البيضاء بعد الضربة)
        val extLen = 800f
        val extendedX = ghostX + ux * extLen
        val extendedY = ghostY + uy * extLen

        // توقع الارتدادات (Bank Shot)
        val bounces = if (config.showBounce) predictBounces(ghostX, ghostY, ux, uy, t) else emptyList()

        // مكان توقف الكورة البيضاء
        val stop = if (config.showStopPoint) {
            bounces.lastOrNull() ?: (extendedX to extendedY)
        } else null

        return AimResult(
            valid = true,
            cueX = cue.x, cueY = cue.y,
            ghostX = ghostX, ghostY = ghostY,
            targetX = target.x, targetY = target.y,
            pocketX = pocket.first, pocketY = pocket.second,
            extendedX = extendedX, extendedY = extendedY,
            bouncePoints = bounces,
            stopPoint = stop,
            showEdges = config.showEdges,
            showThreeLines = config.show3Lines
        )
    }

    /**
     * توقع ارتدادات الكورة على الحدود (Cushions)
     * انعكاس بسيط: angle of incidence = angle of reflection
     */
    private fun predictBounces(sx: Float, sy: Float, uxIn: Float, uyIn: Float,
                                t: TableBounds, maxBounces: Int = 3): List<Pair<Float, Float>> {
        val bounces = ArrayList<Pair<Float, Float>>()
        var x = sx
        var y = sy
        var ux = uxIn
        var uy = uyIn

        val padding = 30f

        repeat(maxBounces) {
            // حساب مسافة الوصول لكل حيطة
            val tRight = if (ux > 0) (t.right - padding - x) / ux else Float.MAX_VALUE
            val tLeft = if (ux < 0) (t.left + padding - x) / ux else Float.MAX_VALUE
            val tBottom = if (uy > 0) (t.bottom - padding - y) / uy else Float.MAX_VALUE
            val tTop = if (uy < 0) (t.top + padding - y) / uy else Float.MAX_VALUE

            val tMin = minOf(tRight, tLeft, tBottom, tTop)
            if (tMin == Float.MAX_VALUE || tMin < 0) return bounces

            val bx = x + ux * tMin
            val by = y + uy * tMin
            bounces.add(bx to by)

            // انعكاس
            if (tMin == tRight || tMin == tLeft) ux = -ux
            if (tMin == tTop || tMin == tBottom) uy = -uy

            x = bx + ux * 2
            y = by + uy * 2
        }
        return bounces
    }

    private fun pocketPositions(t: TableBounds): List<Pair<Float, Float>> {
        val inset = t.width * 0.03f
        return listOf(
            (t.left + inset) to (t.top + inset),
            (t.right - inset) to (t.top + inset),
            (t.left + t.width / 2f) to (t.top + inset),
            (t.left + inset) to (t.bottom - inset),
            (t.right - inset) to (t.bottom - inset),
            (t.left + t.width / 2f) to (t.bottom - inset)
        )
    }
}