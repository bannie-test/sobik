package com.sobik.app.ui.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.SectionCard
import com.sobik.model.CubeType
import com.sobik.scanner.ScanMode
import kotlin.math.roundToInt

@Composable
fun ScanSetupScreen(nav: Navigator) {
    var type by rememberSaveable { mutableStateOf(CubeType.CUBE_3X3) }
    var mode by rememberSaveable { mutableStateOf(ScanMode.MANUAL) }
    var seconds by rememberSaveable { mutableFloatStateOf(3f) }
    AppScaffold(title = "Quét Rubik", onBack = { nav.pop() }, bottomBar = {
        Button(
            onClick = { nav.push(Screen.Scan(System.currentTimeMillis(), type, mode, (seconds * 1000).toLong())) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) { Text("Bắt đầu quét") }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard("Loại cube") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (t in CubeType.entries.filter { it.supportsScan }) {
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
                Text("Các cube 4x4 - 7x7 hiện chỉ có phần kiến thức (mục Cube khác).", style = MaterialTheme.typography.bodySmall)
            }
            SectionCard("Chế độ quét") {
                for (m in ScanMode.entries) {
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = mode == m, onClick = { mode = m }).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(m.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(m.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (mode == ScanMode.TIMED) {
                    Text("Khoảng thời gian: ${seconds.roundToInt()} giây")
                    Slider(value = seconds, onValueChange = { seconds = it }, valueRange = 2f..6f, steps = 3)
                }
            }
            SectionCard("Mẹo để quét chính xác") {
                Text("• Quét nơi đủ sáng, ánh sáng trắng đều, tránh đèn chiếu thẳng gây lóa.")
                Text("• Đưa mặt cube vừa khít khung lưới, giữ máy song song với mặt cube.")
                Text("• Làm theo thứ tự và hướng xoay được hướng dẫn trên màn hình.")
                Text("• Sau khi quét, bạn có thể chạm vào từng ô để sửa màu nếu nhận diện sai.")
            }
        }
    }
}
