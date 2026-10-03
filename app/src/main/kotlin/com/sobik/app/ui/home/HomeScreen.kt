package com.sobik.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AppScaffold
import com.sobik.engine.applyMoves
import com.sobik.model.ColorScheme
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Notation
import com.sobik.model.PuzzleShape
import com.sobik.model.StickerColor
import com.sobik.visualization.CubeImage
import com.sobik.visualization.CubeMasks
import com.sobik.visualization.LastLayerView
import com.sobik.visualization.PuzzleImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class Tile(val label: String, val accent: Color, val enabled: Boolean = true, val onClick: () -> Unit, val art: @Composable () -> Unit)

@Composable
fun HomeScreen(container: AppContainer, nav: Navigator) {
    val current by container.cubeSession.current.collectAsState()
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { container.cubeSession.loadSaved() } }
    val yellowTop = remember { CubeState.solved(CubeType.CUBE_3X3, ColorScheme.YELLOW_TOP) }
    val scrambled = remember { CubeState.solved(CubeType.CUBE_3X3).applyMoves("R U R' F2 D L'") }
    val ollCase = remember { yellowTop.applyMoves(Notation.invert(Notation.parse("R U R' U R U2 R'"))) }

    val tiles = listOf(
        Tile("Quét", Color(0xFF1B5E9E), onClick = { nav.push(Screen.ScanSetup) }) { ScanArt() },
        Tile("Giải", Color(0xFF2E7D32), enabled = current != null, onClick = { nav.push(Screen.Solve) }) {
            CubeImage(current ?: scrambled, Modifier.fillMaxSize(), arrows = Notation.parse("R"))
        },
        Tile("Nhập màu", Color(0xFF6A1B9A), onClick = { nav.push(Screen.ManualInput) }) { NetArt() },
        Tile("Hướng dẫn", Color(0xFFEF6C00), onClick = { nav.push(Screen.Guide) }) {
            CubeImage(CubeMasks.apply(yellowTop, "f2l"), 3, Modifier.fillMaxSize(), arrows = Notation.parse("U"))
        },
        Tile("Công thức", Color(0xFF00838F), onClick = { nav.push(Screen.Algorithms) }) {
            LastLayerView(ollCase, Modifier.fillMaxSize(), orientationOnly = true)
        },
        Tile("Cube lớn", Color(0xFF5D4037), onClick = { nav.push(Screen.OtherCubes) }) {
            CubeImage(CubeState.solved(CubeType.CUBE_5X5, ColorScheme.YELLOW_TOP), Modifier.fillMaxSize())
        },
        Tile("Kệ Rubik", Color(0xFFC62828), onClick = { nav.push(Screen.Puzzles) }) {
            val moyu = remember { container.content.brand("moyu") }
            PuzzleImage(PuzzleShape.CUBE, 3, "MY", com.sobik.app.ui.puzzles.brandColor(moyu), Modifier.fillMaxSize(), logo = moyu?.logo)
        },
    )

    AppScaffold(title = "Sobik", onBack = null) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            tiles.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { TileCard(it, Modifier.weight(1f)) }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            if (current == null) {
                Text("Quét hoặc nhập màu để mở mục Giải.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "from bannietesteverything with Claude",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TileCard(tile: Tile, modifier: Modifier) {
    Card(
        modifier.clickable(enabled = tile.enabled, onClick = tile.onClick),
        colors = CardDefaults.cardColors(containerColor = tile.accent.copy(alpha = if (tile.enabled) 0.13f else 0.05f)),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) { tile.art() }
            Text(
                tile.label,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.SemiBold,
                color = if (tile.enabled) tile.accent else Color.Gray,
            )
        }
    }
}

/** A cube face inside camera viewfinder brackets. */
@Composable
private fun ScanArt() {
    val face = listOf(
        StickerColor.RED, StickerColor.WHITE, StickerColor.GREEN,
        StickerColor.YELLOW, StickerColor.GREEN, StickerColor.BLUE,
        StickerColor.ORANGE, StickerColor.RED, StickerColor.WHITE,
    )
    Canvas(Modifier.fillMaxSize()) {
        val side = size.minDimension * 0.62f
        val left = (size.width - side) / 2; val top = (size.height - side) / 2
        val cell = side / 3
        drawRoundRect(Color(0xFF161616), Offset(left - 6, top - 6), Size(side + 12, side + 12), CornerRadius(12f))
        face.forEachIndexed { i, c ->
            drawRoundRect(Color(c.argb), Offset(left + (i % 3) * cell + 4, top + (i / 3) * cell + 4), Size(cell - 8, cell - 8), CornerRadius(8f))
        }
        val b = size.minDimension * 0.14f
        val m = size.minDimension * 0.06f
        val stroke = Stroke(width = size.minDimension * 0.035f)
        val l = m; val t = m; val r = size.width - m; val btm = size.height - m
        for ((x, y, dx, dy) in listOf(
            listOf(l, t, 1f, 1f), listOf(r, t, -1f, 1f), listOf(l, btm, 1f, -1f), listOf(r, btm, -1f, -1f),
        )) {
            drawLine(Color(0xFF1B5E9E), Offset(x, y), Offset(x + dx * b, y), stroke.width)
            drawLine(Color(0xFF1B5E9E), Offset(x, y), Offset(x, y + dy * b), stroke.width)
        }
    }
}

/** An unfolded cube (cross-shaped net). */
@Composable
private fun NetArt() {
    val faces = listOf(1 to 0 to StickerColor.WHITE, 0 to 1 to StickerColor.ORANGE, 1 to 1 to StickerColor.GREEN, 2 to 1 to StickerColor.RED, 3 to 1 to StickerColor.BLUE, 1 to 2 to StickerColor.YELLOW)
    Canvas(Modifier.fillMaxSize()) {
        val unit = size.minDimension / 4f
        val top = (size.height - unit * 3) / 2
        for ((pos, color) in faces) {
            val (fx, fy) = pos
            val cell = unit / 3
            for (i in 0 until 9) drawRoundRect(
                Color(color.argb),
                Offset(fx * unit + (i % 3) * cell + 1.5f, top + fy * unit + (i / 3) * cell + 1.5f),
                Size(cell - 3, cell - 3),
                CornerRadius(3f),
            )
        }
    }
}
