package com.sobik.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.ColorPicker
import com.sobik.app.ui.common.SectionCard
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.ScanResult
import com.sobik.model.ValidationErrorCode
import com.sobik.scanner.ScanMode
import com.sobik.visualization.Cube3DView
import com.sobik.visualization.CubeNetView

/** Manual input: pick a cube type, then reuse the correction screen starting from a solved cube. */
@Composable
fun ManualInputScreen(container: AppContainer, nav: Navigator) {
    var type by remember { mutableStateOf(CubeType.CUBE_3X3) }
    AppScaffold("Nhập màu thủ công", onBack = { nav.pop() }, bottomBar = {
        Button(
            onClick = { container.cubeSession.startManual(type, container.cubeSession.current.value); nav.replace(Screen.Review) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) { Text("Bắt đầu nhập") }
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Chọn loại cube. Bạn sẽ thấy sơ đồ trải phẳng: chạm vào từng ô và chọn màu cho đúng với cube thật.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (t in listOf(CubeType.CUBE_2X2, CubeType.CUBE_3X3)) FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
            }
            Text(
                "Hướng của sơ đồ: mặt U ở trên cùng (cạnh trên của U giáp mặt B), hàng giữa là L - F - R - B, mặt D ở dưới cùng (cạnh trên của D giáp mặt F).",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun ReviewScreen(container: AppContainer, nav: Navigator) {
    val pending by container.cubeSession.pendingScan.collectAsState()
    val scan = pending ?: run {
        AppScaffold("Kiểm tra kết quả quét", onBack = { nav.pop() }) { p -> Text("Chưa có dữ liệu quét.", Modifier.padding(p).padding(16.dp)) }
        return
    }
    ReviewContent(container, nav, scan)
}

@Composable
private fun ReviewContent(container: AppContainer, nav: Navigator, scan: ScanResult) {
    val vm: ReviewViewModel = viewModel(key = "review-${System.identityHashCode(scan)}") { ReviewViewModel(container, scan) }
    val ui by vm.ui.collectAsState()
    val validation = ui.validation
    val n = ui.state.size
    AppScaffold(
        title = "Kiểm tra & sửa màu",
        onBack = { nav.pop() },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.selected != null) {
                    Text("Chọn màu cho ô mặt ${ui.selected!!.face.symbol} (hàng ${ui.selected!!.row(n) + 1}, cột ${ui.selected!!.col(n) + 1}):", style = MaterialTheme.typography.bodySmall)
                    ColorPicker(selected = ui.state[ui.selected!!], onPick = vm::setColor)
                    if (ui.selected in ui.unconfirmed) TextButton(onClick = vm::confirmSelected) { Text("Màu này đúng — xác nhận") }
                }
                Button(
                    onClick = { if (vm.accept()) nav.replace(Screen.Solve) },
                    enabled = ui.canSolve,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(
                        when {
                            ui.validating -> "Đang kiểm tra…"
                            ui.canSolve -> "Hợp lệ — Lưu và chuyển sang Giải"
                            else -> "Cần sửa trước khi giải"
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CubeNetView(
                ui.state,
                Modifier.fillMaxWidth(),
                errorStickers = validation?.let { v ->
                    // Prefer the concrete stickers a suggestion would change; fall back to error stickers.
                    v.suggestions.flatMap { s -> s.fixes.map { it.sticker } }.toSet().ifEmpty { v.suspectStickers.takeIf { it.size <= 24 }.orEmpty() }
                }.orEmpty(),
                lowConfidence = ui.unconfirmed,
                selected = ui.selected,
                onStickerClick = vm::select,
            )
            Text(
                "Chạm vào một ô để sửa màu. Viền đỏ: ô liên quan tới lỗi. Viền vàng nét đứt: nhận diện chưa chắc chắn.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (ui.unconfirmed.isNotEmpty()) {
                SectionCard("Cần xác nhận ${ui.unconfirmed.size} ô") {
                    Text("Các ô viền vàng có màu khó phân biệt (thiếu sáng, lóa, đỏ/cam...). Hãy chạm để kiểm tra, sửa nếu sai — không cần quét lại cả cube.")
                    OutlinedButton(onClick = vm::confirmAll) { Text("Tất cả đều đúng") }
                }
            }
            when {
                ui.validating && validation == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(8.dp))
                    Text("Đang kiểm tra trạng thái cube…")
                }
                validation != null && validation.isValid -> SectionCard("Trạng thái hợp lệ ✓") {
                    Text("Cube có thể giải được. Kiểm tra lại mô hình 3D rồi bấm Lưu và chuyển sang Giải.")
                    Cube3DView(ui.state, Modifier.fillMaxWidth().height(220.dp))
                }
                validation != null -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Trạng thái Rubik không hợp lệ", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                            for (e in validation.errors.filter { it.code != ValidationErrorCode.INVALID_CUBE_STATE }) {
                                Text("• ${e.message}", color = Color(0xFF4E342E))
                            }
                        }
                    }
                    if (validation.suggestions.isNotEmpty()) SectionCard("Gợi ý sửa") {
                        for (s in validation.suggestions) {
                            Text(s.message)
                            Button(onClick = { vm.apply(s) }) { Text("Áp dụng") }
                        }
                    }
                    val origin = container.cubeSession.pendingOrigin
                    SectionCard("Quét lại") {
                        if (origin != null) {
                            Text("Nếu một mặt bị quét sai nhiều ô, chỉ cần quét lại riêng mặt đó (các mặt khác được giữ nguyên).", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (f in Face.entries) TextButton(onClick = {
                                    nav.replace(Screen.Scan(origin.sessionId, ui.state.type, origin.mode, origin.intervalMs, retakeFace = f))
                                }) { Text(f.symbol.toString()) }
                            }
                        }
                        OutlinedButton(onClick = { nav.replace(Screen.Scan(System.currentTimeMillis(), ui.state.type, origin?.mode ?: ScanMode.MANUAL, origin?.intervalMs ?: 3000)) }) {
                            Text("Quét lại toàn bộ")
                        }
                    }
                }
            }
        }
    }
}
