package com.pooloverlay

import android.graphics.Bitmap
import kotlin.math.sqrt

data class Ball(val x: Float, val y: Float, val color: Int, val radius: Float)

data class TableBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
}

class BallDetector {

    fun detectTableBounds(bmp: Bitmap): TableBounds {
        var minX = bmp.width; var maxX = 0
        var minY = bmp.height; var maxY = 0
        val step = 8
        var y = 0
        while (y < bmp.height) {
            var x = 0
            while (x < bmp.width) {
                val p = bmp.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (g > 80 && g > r + 20 && g > b + 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }
        if (maxX == 0) return TableBounds(0, 0, bmp.width, bmp.height)
        return TableBounds(minX, minY, maxX, maxY)
    }

    fun detect(bmp: Bitmap): List<Ball> {
        val table = detectTableBounds(bmp)
        val step = 4
        val grid = HashMap<Long, Int>()

        var y = table.top
        while (y < table.bottom) {
            var x = table.left
            while (x < table.right) {
                val p = bmp.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val idx = matchColor(r, g, b)
                if (idx >= 0) grid[(x.toLong() shl 32) or y.toLong()] = idx
                x += step
            }
            y += step
        }

        val visited = HashSet<Long>()
        val balls = ArrayList<Ball>()
        for ((key, _) in grid) {
            if (key in visited) continue
            val cx = (key shr 32).toInt()
            val cy = (key and 0xFFFFFFFFL).toInt()
            val blobColor = grid[key] ?: continue
            val blob = ArrayList<Pair<Int, Int>>()
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.add(cx to cy)
            while (stack.isNotEmpty()) {
                val (px, py) = stack.removeLast()
                val k = (px.toLong() shl 32) or py.toLong()
                if (k in visited) continue
                visited.add(k)
                if (grid[k] != blobColor) continue
                blob.add(px to py)
                var dx = -step
                while (dx <= step) {
                    var dy = -step
                    while (dy <= step) {
                        stack.add((px + dx) to (py + dy))
                        dy += step
                    }
                    dx += step
                }
            }
            if (blob.size < 6) continue
            var sumX = 0f; var sumY = 0f
            for ((px, py) in blob) { sumX += px; sumY += py }
            val bx = sumX / blob.size
            val by = sumY / blob.size
            val radius = sqrt((blob.size * step * step / Math.PI).toFloat()).coerceIn(8f, 30f)
            balls.add(Ball(bx, by, blobColor, radius))
        }
        return balls
    }

    private fun matchColor(r: Int, g: Int, b: Int): Int {
        return when {
            r > 200 && g > 200 && b > 200 -> 0
            r > 180 && g > 150 && b < 100 -> 1
            r > 150 && g < 100 && b < 100 -> 2
            g > 100 && r < 100 && b < 100 -> 3
            b > 120 && r < 100 -> 4
            r < 60 && g < 60 && b < 60 -> 5
            else -> -1
        }
    }
}