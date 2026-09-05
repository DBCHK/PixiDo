package com.example.widget

import android.graphics.Path

/**
 * Smooth cubic through a series of points — same midpoint-cubic recipe
 * as the in-app spending chart, so the widget curve matches Wallet.
 */
object WidgetCurve {

    fun path(points: List<Pair<Float, Float>>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].first, points[0].second)
        if (points.size == 1) return path
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            val midX = (prev.first + cur.first) / 2f
            path.cubicTo(midX, prev.second, midX, cur.second, cur.first, cur.second)
        }
        return path
    }

    fun fillUnder(
        line: Path,
        firstX: Float,
        lastX: Float,
        baselineY: Float
    ): Path {
        return Path().apply {
            addPath(line)
            lineTo(lastX, baselineY)
            lineTo(firstX, baselineY)
            close()
        }
    }
}
