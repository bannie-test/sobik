package com.sobik.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.sobik.model.CubeState
import com.sobik.model.Move

private const val DEFAULT_YAW = -0.62f
private const val DEFAULT_PITCH = 0.5f

/** ARGB per sticker for a [CubeState]. */
fun CubeState.toArgb(): IntArray = IntArray(type.stickerCount) { colorAt(it).argb.toInt() }

/**
 * Interactive 3D cube. Drag to orbit, double-tap to reset the view.
 * When [move] is non-null its layers are drawn rotated by [progress] (0..1) of the turn.
 */
@Composable
fun Cube3DView(
    state: CubeState,
    modifier: Modifier = Modifier,
    move: Move? = null,
    progress: Float = 0f,
    highlightTurningLayer: Boolean = true,
) {
    val scene = remember(state.size) { CubeScene(state.size) }
    val colors = remember(state) { state.toArgb() }
    var yaw by remember { mutableFloatStateOf(DEFAULT_YAW) }
    var pitch by remember { mutableFloatStateOf(DEFAULT_PITCH) }
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    yaw += drag.x / size.width * 3.5f
                    pitch = (pitch + drag.y / size.height * 3.5f).coerceIn(-1.4f, 1.4f)
                }
            }
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { yaw = DEFAULT_YAW; pitch = DEFAULT_PITCH }) },
    ) {
        val quads = scene.project(colors, move, progress, yaw, pitch, size.width, size.height)
        val path = Path()
        for (q in quads) {
            path.reset()
            path.moveTo(q.xs[0], q.ys[0])
            for (k in 1 until 4) path.lineTo(q.xs[k], q.ys[k])
            path.close()
            val base = Color(q.argb)
            val shaded = Color(base.red * q.shade, base.green * q.shade, base.blue * q.shade, 1f)
            drawPath(path, shaded)
            if (q.isSticker && highlightTurningLayer && move != null && progress > 0f && progress < 1f) {
                drawPath(path, Color.White.copy(alpha = 0.10f))
            }
            if (q.isSticker) drawPath(path, Color.Black.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
        }
        if (quads.isEmpty()) drawCircle(Color.Gray, radius = 4f, center = Offset(size.width / 2, size.height / 2))
    }
}
