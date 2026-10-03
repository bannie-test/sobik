package com.sobik.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.PuzzleShape
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val WHITE = Color(0xFFF7F7F7)
private val BODY = Color(0xFF161616)

/**
 * Static catalog picture of a puzzle: its white face plus a brand mark on the center.
 * Cubes are drawn in 3D (white facing the viewer); other puzzles as a flat white face outline.
 */
@Composable
fun PuzzleImage(shape: PuzzleShape, gridSize: Int, mark: String, markColor: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val n = gridSize.coerceIn(2, 7)
    val scene = remember(n) { CubeScene(n) }
    // Standard scheme turned so white faces front (x'): blue on top, red on the right.
    val colors = remember(n) { CubeState.solved(CubeType.ofSize(n)).applyMoves("x'").toArgb() }
    Canvas(modifier) {
        val center: Offset = when (shape) {
            PuzzleShape.CUBE -> {
                val yaw = -0.45f; val pitch = 0.35f
                for (q in scene.project(colors, null, 0f, yaw, pitch, size.width, size.height)) {
                    val p = Path().apply { moveTo(q.xs[0], q.ys[0]); for (k in 1 until 4) lineTo(q.xs[k], q.ys[k]); close() }
                    val c = Color(q.argb)
                    drawPath(p, Color(c.red * q.shade, c.green * q.shade, c.blue * q.shade, 1f))
                }
                val pt = scene.projectPoint(floatArrayOf(0f, 0f, n + 0.05f), yaw, pitch, size.width, size.height)
                Offset(pt[0], pt[1])
            }
            PuzzleShape.PYRAMINX -> drawPyraminx()
            PuzzleShape.MEGAMINX -> drawMegaminx()
            PuzzleShape.SKEWB -> drawSkewb()
            PuzzleShape.SQUARE1 -> drawSquare1()
            PuzzleShape.CLOCK -> drawClock()
        }
        val text = measurer.measure(mark, TextStyle(color = markColor, fontWeight = FontWeight.Black, fontSize = (min(this.size.width, this.size.height) / 9f / density).sp))
        drawText(text, topLeft = Offset(center.x - text.size.width / 2f, center.y - text.size.height / 2f))
    }
}

private fun DrawScope.polygon(points: List<Offset>, fill: Color, stroke: Color = BODY, width: Float = 4f) {
    val p = Path().apply { moveTo(points[0].x, points[0].y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
    drawPath(p, fill)
    drawPath(p, stroke, style = Stroke(width = width))
}

private fun DrawScope.drawPyraminx(): Offset {
    val s = min(size.width, size.height) * 0.9f
    val top = Offset(size.width / 2, (size.height - s * 0.866f) / 2)
    val left = Offset(size.width / 2 - s / 2, top.y + s * 0.866f)
    val right = Offset(size.width / 2 + s / 2, top.y + s * 0.866f)
    polygon(listOf(top, left, right), WHITE, BODY, 8f)
    fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    for (t in listOf(1f / 3, 2f / 3)) {
        drawLine(BODY, lerp(top, left, t), lerp(top, right, t), 5f)
        drawLine(BODY, lerp(left, top, t), lerp(left, right, t), 5f)
        drawLine(BODY, lerp(right, top, t), lerp(right, left, t), 5f)
    }
    return Offset(size.width / 2, top.y + s * 0.866f * 2 / 3)
}

private fun DrawScope.drawMegaminx(): Offset {
    val c = Offset(size.width / 2, size.height / 2)
    val r = min(size.width, size.height) * 0.45f
    fun pt(radius: Float, i: Int) = Offset(c.x + radius * cos(-Math.PI / 2 + i * 2 * Math.PI / 5).toFloat(), c.y + radius * sin(-Math.PI / 2 + i * 2 * Math.PI / 5).toFloat())
    polygon((0 until 5).map { pt(r, it) }, WHITE, BODY, 8f)
    polygon((0 until 5).map { pt(r * 0.45f, it) }, WHITE, BODY, 5f)
    for (i in 0 until 5) drawLine(BODY, pt(r * 0.45f, i), pt(r, i), 5f)
    return c
}

private fun DrawScope.drawSkewb(): Offset {
    val c = Offset(size.width / 2, size.height / 2)
    val h = min(size.width, size.height) * 0.42f
    polygon(listOf(Offset(c.x - h, c.y - h), Offset(c.x + h, c.y - h), Offset(c.x + h, c.y + h), Offset(c.x - h, c.y + h)), WHITE, BODY, 8f)
    polygon(listOf(Offset(c.x, c.y - h), Offset(c.x + h, c.y), Offset(c.x, c.y + h), Offset(c.x - h, c.y)), WHITE, BODY, 5f)
    return c
}

private fun DrawScope.drawSquare1(): Offset {
    val c = Offset(size.width / 2, size.height / 2)
    val h = min(size.width, size.height) * 0.42f
    polygon(listOf(Offset(c.x - h, c.y - h), Offset(c.x + h, c.y - h), Offset(c.x + h, c.y + h), Offset(c.x - h, c.y + h)), WHITE, BODY, 8f)
    // Wedges: lines from the center to the corners and to points 30 degrees apart on the edges.
    for (i in 0 until 12) {
        val a = Math.toRadians(15.0 + i * 30.0)
        val dx = cos(a).toFloat(); val dy = sin(a).toFloat()
        val t = h / maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
        drawLine(BODY, c, Offset(c.x + dx * t, c.y + dy * t), 4f)
    }
    drawCircle(WHITE, h * 0.32f, c)
    drawCircle(BODY, h * 0.32f, c, style = Stroke(width = 4f))
    return c
}

private fun DrawScope.drawClock(): Offset {
    val c = Offset(size.width / 2, size.height / 2)
    val r = min(size.width, size.height) * 0.46f
    drawCircle(WHITE, r, c)
    drawCircle(BODY, r, c, style = Stroke(width = 8f))
    val step = r * 0.55f
    for (i in -1..1) for (j in -1..1) {
        val o = Offset(c.x + i * step, c.y + j * step)
        if (i == 0 && j == 0) continue
        drawCircle(BODY, r * 0.16f, o, style = Stroke(width = 4f))
        drawLine(BODY, o, Offset(o.x, o.y - r * 0.12f), 4f)
    }
    return c
}
