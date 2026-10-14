package com.pooloverlay

import kotlin.math.hypot

data class AimResult(
    val valid: Boolean,
    val cueX: Float, val cueY: Float,
    val ghostX: Float, val ghostY: Float,
    val targetX: Float, val targetY: Float,
    val pocketX: Float, val pocketY: Float,
    val extendedX: Float, val extendedY: Float,
    val cueAfterPath: List<PhysicsEngine.Vec2>,
    val targetPath: List<PhysicsEngine.Vec2>,
    val bouncePoints: List<Pair<Float, Float>>,
    val stopPoint: Pair<Float, Float>?,
    val alternatives: List<ShotAdvisor.Shot>,
    val topShotScore: Float,
    val difficulty: String,
    val showEdges: Boolean,
    val showThreeLines: Boolean,
    val showAlternatives: Boolean,
    val showPhysicsPath: Boolean
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
        var showStopPoint: Boolean = true,
        var showAlternatives: Boolean = true,
        var showPhysicsPath: Boolean = true
    )

    var config = Config()

    private val physics = PhysicsEngine()
    private val advisor = ShotAdvisor()

    fun computeAim(balls: List<Ball>, table: TableBounds): AimResult? {
        val cue = balls.firstOrNull { it.color == 0 } ?: return null

        val topShots = advisor.evaluateAll(cue, balls, table)
        if (topShots.isEmpty()) return null

        val best = topShots[0]

        val dx = best.ghostX - cue.x
        val dy = best.ghostY - cue.y
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val ux = dx / len
        val uy = dy / len
        val extendedX = best.ghostX + ux * 800f
        val extendedY = best.ghostY + uy * 800f

        val bounces = if (config.showBounce)
            physics.simulateBounces(
                PhysicsEngine.Vec2(best.ghostX, best.ghostY),
                PhysicsEngine.Vec2(ux, uy),
                table
            ).map { it.x to it.y }
        else emptyList()

        val stopPoint = best.cuePathAfter.lastOrNull()?.let { it.x to it.y }

        return AimResult(
            valid = true,
            cueX = cue.x, cueY = cue.y,
            ghostX = best.ghostX, ghostY = best.ghostY,
            targetX = best.targetX, targetY = best.targetY,
            pocketX = best.pocketX, pocketY = best.pocketY,
            extendedX = extendedX, extendedY = extendedY,
            cueAfterPath = best.cuePathAfter,
            targetPath = best.targetPathAfter,
            bouncePoints = bounces,
            stopPoint = stopPoint,
            alternatives = if (config.showAlternatives) topShots.drop(1) else emptyList(),
            topShotScore = best.score,
            difficulty = best.difficulty,
            showEdges = config.showEdges,
            showThreeLines = config.show3Lines,
            showAlternatives = config.showAlternatives,
            showPhysicsPath = config.showPhysicsPath
        )
    }
}