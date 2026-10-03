package com.sobik.app.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.model.Face
import com.sobik.model.StickerScan
import com.sobik.scanner.CaptureSignal
import com.sobik.scanner.ScanMode
import com.sobik.scanner.camerax.CameraPermissionGate
import com.sobik.scanner.camerax.CameraScanPreview
import com.sobik.scanner.camerax.CubeFrameAnalyzer
import com.sobik.scanner.camerax.GuideSpec
import kotlin.math.ceil
import kotlin.math.min

/** Guide square size relative to the shorter side of the preview. */
private const val GUIDE_FRACTION = 0.72f

@Composable
fun ScanScreen(container: AppContainer, nav: Navigator, screen: Screen.Scan) {
    val vm: ScanViewModel = viewModel(key = "scan-${screen.sessionId}") {
        ScanViewModel(container, screen.sessionId, screen.type, screen.mode, screen.intervalMs)
    }
    val ui by vm.ui.collectAsState()
    val n = screen.type.size
    val analyzer = remember(vm) { CubeFrameAnalyzer(gridSize = { n }, onSamples = vm::onSamples) }

    LaunchedEffect(screen.retakeFace) { screen.retakeFace?.let { vm.retake(it) } }
    LaunchedEffect(ui.complete) {
        if (ui.complete && vm.finish()) nav.replace(Screen.Review)
    }

    CameraPermissionGate(onDenied = { nav.replace(Screen.ManualInput) }) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CameraScanPreview(
                analyzer = analyzer,
                torchEnabled = ui.torch,
                modifier = Modifier.fillMaxSize().onSizeChanged { size ->
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    analyzer.updateGuide(GuideSpec(w, h, min(w, h) * GUIDE_FRACTION))
                },
            )
            GuideOverlay(n, ui.live, Modifier.fillMaxSize())

            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(12.dp),
            ) {
                Text(
                    "${(ui.stepIndex + 1).coerceAtMost(ui.totalSteps)}/${ui.totalSteps} · ${ui.step?.title ?: "Hoàn tất"}",
                    color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                )
                ui.step?.let { Text(it.instruction, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                SignalText(ui.signal)
            }

            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.6f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.lastCapture?.let { cap ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniFace(cap.preview, n, Modifier.size(44.dp))
                        Text(
                            cap.warning ?: "Đã chụp mặt ${cap.face.symbol}.",
                            color = if (cap.warning != null) Color(0xFFFFCA28) else Color.White,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (f in listOf(Face.F, Face.R, Face.B, Face.L, Face.U, Face.D)) {
                        val cap = ui.captures[f]
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = cap != null) { vm.retake(f) }) {
                            if (cap != null) MiniFace(cap.preview, n, Modifier.size(36.dp))
                            else Box(Modifier.size(36.dp).border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
                            Text(f.symbol.toString(), color = if (ui.step?.face == f) Color(0xFF64B5F6) else Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Chạm vào mặt đã chụp để quét lại.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { nav.pop() }) { Text("Hủy", color = Color.White) }
                    OutlinedButton(onClick = vm::toggleTorch) { Text(if (ui.torch) "Tắt đèn" else "Bật đèn", color = Color.White) }
                    if (screen.mode == ScanMode.MANUAL || ui.signal is CaptureSignal.Waiting) {
                        Button(onClick = vm::captureNow, enabled = ui.step != null, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text("Chụp mặt ${ui.step?.face?.symbol ?: ""}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalText(signal: CaptureSignal) {
    val text = when (signal) {
        is CaptureSignal.Countdown -> "Tự chụp sau ${ceil(signal.remainingMs / 1000.0).toInt()} giây…"
        is CaptureSignal.Waiting -> signal.reason
        CaptureSignal.Capture -> "Đang chụp…"
        CaptureSignal.Idle -> null
    }
    if (text != null) Text(text, color = Color(0xFFFFE082), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
}

/** Dims everything outside the guide square, draws the grid and the live recognized colors. */
@Composable
private fun GuideOverlay(n: Int, live: List<StickerScan>, modifier: Modifier) {
    Canvas(modifier) {
        val side = min(size.width, size.height) * GUIDE_FRACTION
        val left = (size.width - side) / 2; val top = (size.height - side) / 2
        val dim = Color.Black.copy(alpha = 0.45f)
        drawRect(dim, Offset.Zero, Size(size.width, top))
        drawRect(dim, Offset(0f, top + side), Size(size.width, size.height - top - side))
        drawRect(dim, Offset(0f, top), Size(left, side))
        drawRect(dim, Offset(left + side, top), Size(size.width - left - side, side))
        val cell = side / n
        for (i in 0..n) {
            drawLine(Color.White, Offset(left + i * cell, top), Offset(left + i * cell, top + side), strokeWidth = 3f)
            drawLine(Color.White, Offset(left, top + i * cell), Offset(left + side, top + i * cell), strokeWidth = 3f)
        }
        if (live.size == n * n) for (i in live.indices) {
            val cx = left + (i % n + 0.5f) * cell; val cy = top + (i / n + 0.5f) * cell
            val r = cell * 0.13f
            drawCircle(Color(live[i].color.argb), r, Offset(cx, cy))
            drawCircle(if (live[i].confidence < 0.45f) Color(0xFFFFB300) else Color.Black, r, Offset(cx, cy), style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun MiniFace(stickers: List<StickerScan>, n: Int, modifier: Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val c = size.width / n
        for (i in stickers.indices) {
            drawRoundRect(
                Color(stickers[i].color.argb),
                Offset((i % n) * c + 1, (i / n) * c + 1),
                Size(c - 2, c - 2),
                CornerRadius(3f),
            )
        }
    }
}
