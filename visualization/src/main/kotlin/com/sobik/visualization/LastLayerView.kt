package com.sobik.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.sobik.model.CubeState
import com.sobik.model.Face
import com.sobik.model.StickerColor

/**
 * Top view of the last layer: the U face plus the top row of each side face around it
 * (the usual OLL/PLL case diagram). When [orientationOnly] is true, only the U color is shown
 * (OLL style) and everything else is gray.
 */
@Composable
fun LastLayerView(state: CubeState, modifier: Modifier = Modifier, orientationOnly: Boolean = false) {
    val n = state.size
    val topColor = state.center(Face.U) ?: state[Face.U, 0]
    Canvas(modifier.aspectRatio(1f)) {
        val unit = size.width / (n + 1f)
        val strip = unit * 0.45f
        val origin = (size.width - n * unit) / 2
        fun color(c: StickerColor) = if (orientationOnly && c != topColor) Color(0xFF616161) else Color(c.argb)
        fun cell(x: Float, y: Float, w: Float, h: Float, c: StickerColor) =
            drawRoundRect(color(c), Offset(x + 2, y + 2), Size(w - 4, h - 4), CornerRadius(4f))
        for (r in 0 until n) for (c in 0 until n) cell(origin + c * unit, origin + r * unit, unit, unit, state[Face.U, r * n + c])
        for (j in 0 until n) {
            // B appears mirrored when seen from above; R runs front-to-back on its own face.
            cell(origin + j * unit, origin - strip, unit, strip, state[Face.B, n - 1 - j])
            cell(origin + j * unit, origin + n * unit, unit, strip, state[Face.F, j])
            cell(origin - strip, origin + j * unit, strip, unit, state[Face.L, j])
            cell(origin + n * unit, origin + j * unit, strip, unit, state[Face.R, n - 1 - j])
        }
    }
}
