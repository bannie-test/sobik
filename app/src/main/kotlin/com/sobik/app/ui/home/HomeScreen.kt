package com.sobik.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.BigActionButton
import com.sobik.visualization.Cube3DView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(container: AppContainer, nav: Navigator) {
    val current by container.cubeSession.current.collectAsState()
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { container.cubeSession.loadSaved() } }
    AppScaffold(title = "Sobik — Rubik Solver", onBack = null) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            current?.let { cube ->
                Cube3DView(cube, Modifier.fillMaxWidth().height(180.dp))
                Text(
                    "Cube đã lưu: ${cube.type.label}${if (cube.isSolved()) " (đã giải)" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            BigActionButton("Quét Rubik", "Dùng camera quét từng mặt 2x2 hoặc 3x3", onClick = { nav.push(Screen.ScanSetup) })
            BigActionButton(
                "Giải",
                if (current != null) "Giải cube vừa quét: Solve All, Cross, F2L, OLL, PLL" else "Quét hoặc nhập cube trước",
                onClick = { nav.push(Screen.Solve) },
                enabled = current != null,
                accent = Color(0xFF2E7D32),
            )
            BigActionButton("Nhập màu thủ công", "Tô màu từng ô trên sơ đồ — không cần camera", onClick = { nav.push(Screen.ManualInput) }, accent = Color(0xFF6A1B9A))
            BigActionButton("Hướng dẫn 3x3 cho người mới", "8 bước Layer-by-Layer có mô phỏng 3D", onClick = { nav.push(Screen.Guide) }, accent = Color(0xFFEF6C00))
            BigActionButton("Thư viện công thức", "Beginner, F2L, OLL, PLL", onClick = { nav.push(Screen.Algorithms) }, accent = Color(0xFF00838F))
            BigActionButton("Cube khác (4x4 - 7x7)", "Reduction, ghép tâm, ghép cạnh, parity", onClick = { nav.push(Screen.OtherCubes) }, accent = Color(0xFF5D4037))
        }
    }
}
