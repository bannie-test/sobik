package com.sobik.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sobik.engine.CubeGeometry
import com.sobik.model.CubeState
import com.sobik.model.Move
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Default static view: front, right and top visible. */
const val VIEW_YAW = -0.62f
const val VIEW_PITCH_TOP = 0.5f
/** View from below, to show the bottom (white cross / first layer). */
const val VIEW_PITCH_BOTTOM = -0.5f

private val MASK_GRAY = 0xFF4A4A4A.toInt()

/**
 * Static, non-interactive cube picture with optional arrows showing how [arrows] turn.
 * Used for illustrations (notation, lesson targets, case pictures) where an animation is not needed.
 */
@Composable
fun CubeImage(
    colors: IntArray,
    n: Int,
    modifier: Modifier = Modifier,
    arrows: List<Move> = emptyList(),
    yaw: Float = VIEW_YAW,
    pitch: Float = VIEW_PITCH_TOP,
    arrowColor: Color = Color(0xFFE91E63),
) {
    val scene = remember(n) { CubeScene(n) }
    Canvas(modifier) {
        val quads = scene.project(colors, null, 0f, yaw, pitch, size.width, size.height)
        val path = Path()
        for (q in quads) {
            path.reset()
            path.moveTo(q.xs[0], q.ys[0])
            for (k in 1 until 4) path.lineTo(q.xs[k], q.ys[k])
            path.close()
            val base = Color(q.argb)
            drawPath(path, Color(base.red * q.shade, base.green * q.shade, base.blue * q.shade, 1f))
            if (q.isSticker) drawPath(path, Color.Black.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
        }
        for (m in arrows) {
            val a = scene.moveArrow(m, yaw, pitch, size.width, size.height)
            drawArrow(Offset(a[0], a[1]), Offset(a[2], a[3]), arrowColor, double = m.turns == 2)
        }
    }
}

@Composable
fun CubeImage(state: CubeState, modifier: Modifier = Modifier, arrows: List<Move> = emptyList(), yaw: Float = VIEW_YAW, pitch: Float = VIEW_PITCH_TOP) {
    val colors = remember(state) { state.toArgb() }
    CubeImage(colors, state.size, modifier, arrows, yaw, pitch)
}

/** Arrow with an outlined shaft and a filled head; a double head marks a half turn. */
fun DrawScope.drawArrow(from: Offset, to: Offset, color: Color, double: Boolean = false) {
    val width = size.minDimension * 0.035f
    drawLine(Color.White, from, to, strokeWidth = width + 6f, cap = StrokeCap.Round)
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    val angle = atan2(to.y - from.y, to.x - from.x)
    val head = width * 2.6f
    fun headAt(tip: Offset) {
        val p = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - head * cos(angle - 0.45f), tip.y - head * sin(angle - 0.45f))
            lineTo(tip.x - head * cos(angle + 0.45f), tip.y - head * sin(angle + 0.45f))
            close()
        }
        drawPath(p, Color.White, style = Stroke(width = 5f))
        drawPath(p, color)
    }
    headAt(to)
    if (double) headAt(Offset(to.x - head * 0.9f * cos(angle), to.y - head * 0.9f * sin(angle)))
}

/**
 * Highlights part of a solved cube (yellow on top, white on the bottom) and grays out the rest,
 * to show the goal of a step. Presets refer to the layer-by-layer method.
 */
object CubeMasks {
    val PRESETS = listOf("cross", "firstLayer", "f2l", "yellowCross", "yellowFace", "yellowCorners", "solved")

    fun apply(state: CubeState, preset: String): IntArray {
        val n = state.size
        val m = n - 1
        val geo = CubeGeometry.of(n)
        return IntArray(state.type.stickerCount) { i ->
            val p = geo.position[i]
            val x = p[0]; val y = p[1]; val z = p[2]
            val nonZero = listOf(x, y, z).count { it != 0 }
            val isCenter = nonZero == 1
            val isEdge = nonZero == 2
            val stickerOnTop = p[4] == 1
            val keep = isCenter || when (preset) {
                "cross" -> y == -m && isEdge
                "firstLayer" -> y == -m
                "f2l" -> y < m
                "yellowCross" -> y < m || (y == m && isEdge && stickerOnTop)
                "yellowFace" -> y < m || stickerOnTop
                "yellowCorners" -> y < m || stickerOnTop || (y == m && !isEdge)
                else -> true
            }
            if (keep) state.colorAt(i).argb.toInt() else MASK_GRAY
        }
    }
}
