package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslamicPatternBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val patternColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = 180f
            val cols = (size.width / cellSize).toInt() + 2
            val rows = (size.height / cellSize).toInt() + 2

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val cx = col * cellSize + if (row % 2 == 1) cellSize / 2f else 0f
                    val cy = row * cellSize
                    drawEightPointStar(
                        center = Offset(cx, cy),
                        radius = cellSize * 0.36f,
                        strokeColor = patternColor,
                        innerColor = accentColor
                    )
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawEightPointStar(
    center: Offset,
    radius: Float,
    strokeColor: Color,
    innerColor: Color
) {
    val points = 16
    val innerRadius = radius * 0.44f
    val path = Path()

    for (i in 0 until points) {
        val angle = (i * 22.5) * Math.PI / 180.0
        val r = if (i % 2 == 0) radius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(path = path, color = strokeColor, style = Stroke(width = 1.6f))
    drawCircle(
        color = innerColor,
        radius = innerRadius * 0.45f,
        center = center,
        style = Stroke(width = 1.2f)
    )
}
