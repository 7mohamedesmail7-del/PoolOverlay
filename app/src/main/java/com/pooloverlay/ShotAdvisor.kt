package com.pooloverlay

import kotlin.math.hypot
import kotlin.math.cos
import kotlin.math.atan2

class ShotAdvisor {

    private val physics = PhysicsEngine()

    data class Shot(
        val cueX: Float, val cueY: Float,
        val ghostX: Float, val ghostY: Float,
        val targetX: Float, val targetY: Float,
        val pocketX: Float, val pocketY: Float,
        val aimAngle: Float,
        val power: Float,
        val score: Float,
        val difficulty: String,
        val cuePathAfter: List<PhysicsEngine.Vec2>,
        val targetPathAfter: List<PhysicsEngine.Vec2>
    )

    fun evaluateAll(
        cue: Ball,
        balls: List<Ball>,
        table: TableBounds,
        myBalls: Set<Int>? = null
    ): List<Shot> {
        val pockets = pocketPositions(table)
        val shots = ArrayList<Shot>()

        for ((idx, ball) in balls.withIndex()) {
            if (ball === cue || ball.color == 0) continue
            if (myBalls != null && ball.color !in myBalls) continue

            for (pocket in pockets) {
                val shot = evaluateShot(cue, ball, pocket, balls, table, idx) ?: continue
                shots.add(shot)
            }
        }

        shots.sortByDescending { it.score }
        return shots.take(3)
    }

    private fun evaluateShot(
        cue: Ball,
        target: Ball,
        pocket: Pair<Float, Float>,
        balls: List<Ball>,
        table: TableBounds,
        targetIdx: Int
    ): Shot? {
        val tpx = pocket.first - target.x
        val tpy = pocket.second - target.y
        val dist = hypot(tpx, tpy).coerceAtLeast(0.001f)
        val ghostX = target.x - (tpx / dist) * target.radius * 2f
        val ghostY = target.y - (tpy / dist) * target.radius * 2f

        val cueDx = ghostX - cue.x
        val cueDy = ghostY - cue.y
        val cueDist = hypot(cueDx, cueDy)
        if (cueDist < 1f) return null

        val aimDir = PhysicsEngine.Vec2(cueDx / cueDist, cueDy / cueDist)

        val pathToGhost = physics.tracePath(
            PhysicsEngine.Vec2(cue.x, cue.y),
            aimDir,
            balls,
            table,
            ignoreBallIdx = -1
        )

        if (pathToGhost.hitBall != null && pathToGhost.hitBall != targetIdx) return null

        val targetDir = PhysicsEngine.Vec2(tpx / dist, tpy / dist)
        val targetPath = physics.tracePath(
            PhysicsEngine.Vec2(target.x, target.y),
            targetDir,
            balls,
            table,
            ignoreBallIdx = targetIdx
        )

        val endToPocket = hypot(
            targetPath.endPoint.x - pocket.first,
            targetPath.endPoint.y - pocket.second
        )
        if (endToPocket > 40f) return null

        var score = 100f

        val cutAngle = calculateCutAngle(cue, target, aimDir, targetDir)
        score -= (1f - cos(cutAngle)) * 40f
        score -= cueDist / 30f
        score -= dist / 40f

        if (pathToGhost.hitBall != null && pathToGhost.hitBall != targetIdx) score -= 30f

        val edgeDist = minOf(
            target.x - table.left, table.right - target.x,
            target.y - table.top, table.bottom - target.y
        )
        if (edgeDist < 60) score -= 10f

        score = score.coerceIn(0f, 100f)

        val difficulty = when {
            score > 75 -> "Easy"
            score > 50 -> "Medium"
            else -> "Hard"
        }

        val power = (0.4f + (cueDist + dist) / 1500f).coerceIn(0.35f, 1f)

        val cueAfterDir = physics.tangentDirection(
            aimDir,
            PhysicsEngine.Vec2(target.x, target.y),
            PhysicsEngine.Vec2(ghostX, ghostY)
        )
        val cuePathAfter = physics.tracePath(
            PhysicsEngine.Vec2(ghostX, ghostY),
            cueAfterDir,
            balls,
            table,
            ignoreBallIdx = targetIdx
        ).positions

        return Shot(
            cueX = cue.x, cueY = cue.y,
            ghostX = ghostX, ghostY = ghostY,
            targetX = target.x, targetY = target.y,
            pocketX = pocket.first, pocketY = pocket.second,
            aimAngle = atan2(aimDir.y, aimDir.x),
            power = power,
            score = score,
            difficulty = difficulty,
            cuePathAfter = cuePathAfter,
            targetPathAfter = targetPath.positions
        )
    }

    private fun calculateCutAngle(
        cue: Ball, target: Ball,
        aimDir: PhysicsEngine.Vec2,
        targetDir: PhysicsEngine.Vec2
    ): Float {
        val dot = aimDir.dot(targetDir).coerceIn(-1f, 1f)
        return kotlin.math.acos(dot)
    }

    private fun pocketPositions(t: TableBounds): List<Pair<Float, Float>> {
        val inset = t.width * 0.025f
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