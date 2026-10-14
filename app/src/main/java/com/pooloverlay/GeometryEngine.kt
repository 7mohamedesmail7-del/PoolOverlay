package com.pooloverlay

import kotlin.math.hypot

data class AimResult(
    val valid: Boolean,
    val cueX: Float, val cueY: Float,
    val ghostX: Float, val ghostY: Float,
    val targetX: Float, val targetY: Float,
    val pocketX: Float, val pocketY: Float,
    val showEdges: Boolean,
    val showThreeLines: Boolean,
    val bankShot: Boolean
)

class GeometryEngine {

    data class Config(
        var lineLength: Int = 2000,
        var lineThickness: Int = 8,
        var lineOpacity: Int = 220,
        var lineStyle: LineStyle = LineStyle.THIN,
        var show3Lines: Boolean = false,
        var showEdges: Boolean = false,
        var cushionGuideline: Boolean = false,
        var autoLengthen: Boolean = true
    )

    enum class LineStyle { THIN, NORMAL, THICK }

    var config = Config()

    fun computeAim(balls: List<Ball>, table: TableBounds): AimResult {
        val cue = balls.firstOrNull { it.color == 0 }
            ?: return AimResult(false, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, false, false, false)

        val pockets = pocketPositions(table)
        var best: AimResult? = null
        var bestScore = -Float.MAX_VALUE

        for (ball in balls) {
            if (ball === cue || ball.color == 0) continue
            for (p in pockets) {
                val aim = ghostBall(cue, ball, p)
                val dxCue = hypot(aim.ghostX - cue.x, aim.ghostY - cue.y)
                val dxTgt = hypot(p.first - ball.x, p.second - ball.y)
                val score = 200f - dxCue * 0.05f - dxTgt * 0.1f
                if (score > bestScore) {
                    bestScore = score
                    best = aim.copy(
                        showEdges = config.showEdges,
                        showThreeLines = config.show3Lines,
                        bankShot = false
                    )
                }
            }
        }
        return best ?: AimResult(false, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, false, false, false)
    }

    private fun ghostBall(cue: Ball, target: Ball, pocket: Pair<Float, Float>): AimResult {
        val tpx = pocket.first - target.x
        val tpy = pocket.second - target.y
        val dist = hypot(tpx, tpy).coerceAtLeast(0.001f)
        val ghostX = target.x - (tpx / dist) * target.radius * 2f
        val ghostY = target.y - (tpy / dist) * target.radius * 2f

        return AimResult(
            valid = true,
            cueX = cue.x, cueY = cue.y,
            ghostX = ghostX, ghostY = ghostY,
            targetX = target.x, targetY = target.y,
            pocketX = pocket.first, pocketY = pocket.second,
            showEdges = false, showThreeLines = false, bankShot = false
        )
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