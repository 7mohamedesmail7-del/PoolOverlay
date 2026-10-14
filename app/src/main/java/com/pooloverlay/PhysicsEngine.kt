package com.pooloverlay

import kotlin.math.hypot

/**
 * محاكاة فيزياء حقيقية لـ 8 Ball Pool
 */
class PhysicsEngine {

    data class Vec2(val x: Float, val y: Float) {
        operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
        operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
        operator fun times(s: Float) = Vec2(x * s, y * s)
        fun len() = hypot(x, y)
        fun norm(): Vec2 {
            val l = len()
            return if (l < 0.001f) Vec2(0f, 0f) else Vec2(x / l, y / l)
        }
        fun perp() = Vec2(-y, x)
        fun dot(o: Vec2) = x * o.x + y * o.y
    }

    data class BallPath(
        val positions: List<Vec2>,
        val hitBall: Int?,
        val hitCushion: Vec2?,
        val endPoint: Vec2,
        val isValid: Boolean
    )

    companion object {
        const val FRICTION = 0.985f
        const val CUSHION_DAMP = 0.75f
        const val BALL_RADIUS_RATIO = 0.028f
        const val STEP = 3f
        const val MAX_STEPS = 600
    }

    fun tracePath(
        start: Vec2,
        direction: Vec2,
        balls: List<Ball>,
        table: TableBounds,
        ignoreBallIdx: Int = -1
    ): BallPath {
        val dir = direction.norm()
        if (dir.len() < 0.001f) return BallPath(emptyList(), null, null, start, false)

        val positions = ArrayList<Vec2>()
        var pos = start
        var velocity = dir
        var speed = 1f

        val ballRadius = minOf(table.width, table.height) * BALL_RADIUS_RATIO
        val left = table.left + ballRadius
        val right = table.right - ballRadius
        val top = table.top + ballRadius
        val bottom = table.bottom - ballRadius

        var hitBall: Int? = null
        var hitCushion: Vec2? = null

        for (step in 0 until MAX_STEPS) {
            speed *= FRICTION
            if (speed < 0.05f) break

            val next = pos + velocity * STEP

            var collided = false
            for ((idx, ball) in balls.withIndex()) {
                if (idx == ignoreBallIdx) continue
                val d = (next - Vec2(ball.x, ball.y)).len()
                if (d < ballRadius * 2f) {
                    hitBall = idx
                    collided = true
                    val collision = Vec2(ball.x, ball.y) - velocity.norm() * ballRadius * 2f
                    positions.add(collision)
                    pos = collision
                    break
                }
            }
            if (collided) break

            var bounced = false
            var newVel = velocity
            if (next.x < left) {
                hitCushion = Vec2(left, next.y)
                newVel = Vec2(-velocity.x, velocity.y)
                bounced = true
            } else if (next.x > right) {
                hitCushion = Vec2(right, next.y)
                newVel = Vec2(-velocity.x, velocity.y)
                bounced = true
            }
            if (next.y < top) {
                hitCushion = Vec2(next.x, top)
                newVel = Vec2(newVel.x, -newVel.y)
                bounced = true
            } else if (next.y > bottom) {
                hitCushion = Vec2(next.x, bottom)
                newVel = Vec2(newVel.x, -newVel.y)
                bounced = true
            }

            if (bounced) {
                velocity = newVel.norm()
                speed *= CUSHION_DAMP
            }

            pos = next
            if (step % 2 == 0) positions.add(pos)
        }

        return BallPath(
            positions = positions,
            hitBall = hitBall,
            hitCushion = hitCushion,
            endPoint = pos,
            isValid = positions.isNotEmpty()
        )
    }

    fun tangentDirection(cueBallDir: Vec2, target: Vec2, cuePos: Vec2): Vec2 {
        val connection = (target - cuePos).norm()
        return connection.perp()
    }

    fun simulateBounces(
        start: Vec2,
        direction: Vec2,
        table: TableBounds,
        maxBounces: Int = 4
    ): List<Vec2> {
        val bounces = ArrayList<Vec2>()
        var pos = start
        var vel = direction.norm()

        val ballRadius = minOf(table.width, table.height) * BALL_RADIUS_RATIO
        val left = table.left + ballRadius
        val right = table.right - ballRadius
        val top = table.top + ballRadius
        val bottom = table.bottom - ballRadius

        repeat(maxBounces) {
            val tR = if (vel.x > 0.001f) (right - pos.x) / vel.x else Float.MAX_VALUE
            val tL = if (vel.x < -0.001f) (left - pos.x) / vel.x else Float.MAX_VALUE
            val tB = if (vel.y > 0.001f) (bottom - pos.y) / vel.y else Float.MAX_VALUE
            val tT = if (vel.y < -0.001f) (top - pos.y) / vel.y else Float.MAX_VALUE

            val t = minOf(tR, tL, tB, tT)
            if (t == Float.MAX_VALUE || t < 0) return bounces

            val hit = pos + vel * t
            bounces.add(hit)

            if (t == tR || t == tL) vel = Vec2(-vel.x, vel.y)
            if (t == tT || t == tB) vel = Vec2(vel.x, -vel.y)

            pos = hit + vel * 0.5f
        }

        return bounces
    }
}