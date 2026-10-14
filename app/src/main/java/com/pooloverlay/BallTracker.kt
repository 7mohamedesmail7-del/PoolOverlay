package com.pooloverlay

import kotlin.math.abs

class BallTracker {

    private data class Tracked(
        var x: Float,
        var y: Float,
        var radius: Float,
        var color: Int,
        var age: Int,
        var lastSeen: Long,
        var missedFrames: Int
    )

    private val tracks = HashMap<Int, Tracked>()
    private var nextId = 1

    fun update(detected: List<Ball>): List<Ball> {
        val now = System.currentTimeMillis()
        val maxDist = 80f
        val maxMissedFrames = 3

        tracks.entries.removeIf { now - it.value.lastSeen > 1500 }

        val matched = HashSet<Int>()

        for (det in detected) {
            var bestId = -1
            var bestDist = maxDist

            for ((id, tr) in tracks) {
                if (id in matched) continue
                if (abs(tr.color - det.color) > 1) continue
                val d = kotlin.math.hypot(tr.x - det.x, tr.y - det.y)
                if (d < bestDist) {
                    bestDist = d
                    bestId = id
                }
            }

            if (bestId > 0) {
                val tr = tracks[bestId]!!
                val alpha = 0.55f
                tr.x = tr.x * (1 - alpha) + det.x * alpha
                tr.y = tr.y * (1 - alpha) + det.y * alpha
                tr.radius = tr.radius * 0.7f + det.radius * 0.3f
                tr.lastSeen = now
                tr.missedFrames = 0
                matched.add(bestId)
            } else {
                tracks[nextId] = Tracked(
                    x = det.x, y = det.y, radius = det.radius, color = det.color,
                    age = 0, lastSeen = now, missedFrames = 0
                )
                matched.add(nextId)
                nextId++
            }
        }

        val result = ArrayList<Ball>()
        for ((id, tr) in tracks) {
            if (id in matched) {
                result.add(Ball(tr.x, tr.y, tr.color, tr.radius))
            } else {
                tr.missedFrames++
                if (tr.missedFrames <= maxMissedFrames) {
                    result.add(Ball(tr.x, tr.y, tr.color, tr.radius))
                }
            }
        }
        return result
    }

    fun reset() {
        tracks.clear()
        nextId = 1
    }
}