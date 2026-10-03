package com.sobik.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.sobik.model.CubeState
import com.sobik.model.Face
import com.sobik.model.StickerRef

/** Position of each face in the cross-shaped net (column, row in face units). */
private val NET_POSITION = mapOf(
    Face.U to (1 to 0), Face.L to (0 to 1), Face.F to (1 to 1), Face.R to (2 to 1), Face.B to (3 to 1), Face.D to (1 to 2),
)

/**
 * Unfolded cube (U on top, L F R B in the middle row, D at the bottom). Tap a sticker to edit it.
 *
 * @param errorStickers drawn with a red border (involved in a validation error)
 * @param lowConfidence drawn with a dashed amber border (recognition uncertain)
 * @param selected      drawn with a thick white border
 */
@Composable
fun CubeNetView(
    state: CubeState,
    modifier: Modifier = Modifier,
    errorStickers: Set<StickerRef> = emptySet(),
    lowConfidence: Set<StickerRef> = emptySet(),
    selected: StickerRef? = null,
    onStickerClick: ((StickerRef) -> Unit)? = null,
) {
    val n = state.size
    Canvas(
        modifier
            .aspectRatio(4f / 3f)
            .pointerInput(state, onStickerClick) {
                if (onStickerClick == null) return@pointerInput
                detectTapGestures { pos ->
                    val cell = minOf(size.width / (4f * n), size.height / (3f * n))
                    val col = (pos.x / cell).toInt(); val row = (pos.y / cell).toInt()
                    val face = NET_POSITION.entries.firstOrNull { (_, p) -> col / n == p.first && row / n == p.second }?.key
                    if (face != null) onStickerClick(StickerRef(face, (row % n) * n + col % n))
                }
            },
    ) {
        val cell = minOf(size.width / (4f * n), size.height / (3f * n))
        val gap = cell * 0.07f
        val dash = PathEffect.dashPathEffect(floatArrayOf(cell * 0.18f, cell * 0.12f))
        for ((face, p) in NET_POSITION) for (i in 0 until n * n) {
            val x = (p.first * n + i % n) * cell
            val y = (p.second * n + i / n) * cell
            val topLeft = Offset(x + gap, y + gap)
            val sz = Size(cell - 2 * gap, cell - 2 * gap)
            val radius = CornerRadius(cell * 0.12f)
            drawRoundRect(Color(state[face, i].argb), topLeft, sz, radius)
            val ref = StickerRef(face, i)
            when {
                ref == selected -> drawRoundRect(Color.White, topLeft, sz, radius, style = Stroke(width = cell * 0.14f))
                ref in errorStickers -> drawRoundRect(Color(0xFFE53935), topLeft, sz, radius, style = Stroke(width = cell * 0.11f))
                ref in lowConfidence -> drawRoundRect(Color(0xFFFFB300), topLeft, sz, radius, style = Stroke(width = cell * 0.1f, pathEffect = dash))
                else -> drawRoundRect(Color.Black.copy(alpha = 0.4f), topLeft, sz, radius, style = Stroke(width = 1.5f))
            }
        }
    }
}
