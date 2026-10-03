package com.sobik.app.ui.solve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.sobik.app.ui.common.MovesText
import com.sobik.app.ui.common.SectionCard
import com.sobik.model.CubeState
import com.sobik.model.Solution
import com.sobik.model.SolutionKind
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.visualization.Cube3DView
import com.sobik.visualization.MovePlayerControls
import com.sobik.visualization.MovePlayerCube
import com.sobik.visualization.rememberMovePlayerState

@Composable
fun SolveScreen(container: AppContainer, nav: Navigator) {
    val current by container.cubeSession.current.collectAsState()
    val cube = current ?: run {
        AppScaffold("Giải", onBack = { nav.pop() }) { p -> Text("Chưa có cube. Hãy quét hoặc nhập màu trước.", Modifier.padding(p).padding(16.dp)) }
        return
    }
    val vm: SolveViewModel = viewModel(key = "solve-${cube.serialize()}") { SolveViewModel(container) }
    val ui by vm.ui.collectAsState()
    val state = ui.state ?: cube
    var reasonShown by remember { mutableStateOf<String?>(null) }

    AppScaffold("Giải ${state.type.label}", onBack = { nav.pop() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val success = ui.outcome as? SolveOutcome.Success
            val solution = success?.solutions?.getOrNull(ui.selected)
            if (solution == null) {
                Cube3DView(state, Modifier.fillMaxWidth().height(260.dp))
            }
            ui.progress?.let { Text("Tiến độ (CFOP): $it", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }

            GoalButtons(ui) { goal ->
                val reason = ui.availability[goal]
                if (reason != null) reasonShown = "${goal.displayName}: $reason" else { reasonShown = null; vm.solve(goal) }
            }
            reasonShown?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (ui.running != null) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(8.dp))
                Text("Đang tìm lời giải (${ui.running!!.displayName})…")
            }

            when (val out = ui.outcome) {
                is SolveOutcome.NotApplicable -> SectionCard("Không phù hợp") { Text(out.reason) }
                is SolveOutcome.Failure -> SectionCard("Không giải được — có thể quét sai") {
                    Text(out.reason)
                    Button(onClick = { container.cubeSession.startManual(state.type, state); nav.push(Screen.Review) }) { Text("Kiểm tra lại màu") }
                }
                is SolveOutcome.Success -> SolutionSection(state, out.solutions, ui.selected, vm::select, ui.goal, vm::applySelected)
                null -> Unit
            }
            ui.scramble?.let {
                SectionCard("Scramble (suy ra từ trạng thái)") {
                    MovesText(it)
                    Text("Trạng thái cube là nguồn dữ liệu chính; scramble chỉ để tham khảo / chia sẻ.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalButtons(ui: SolveUiState, onGoal: (SolveGoal) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (goal in ui.goals) {
            val available = ui.availability[goal] == null
            if (goal == SolveGoal.SOLVE_ALL) {
                Button(onClick = { onGoal(goal) }, enabled = ui.running == null) { Text(goal.displayName) }
            } else {
                FilledTonalButton(
                    onClick = { onGoal(goal) },
                    enabled = ui.running == null,
                    colors = if (available) androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.Gray),
                ) { Text(goal.displayName) }
            }
        }
    }
}

@Composable
private fun SolutionSection(
    start: CubeState,
    solutions: List<Solution>,
    selected: Int,
    onSelect: (Int) -> Unit,
    goal: SolveGoal?,
    onApplied: () -> Unit,
) {
    if (solutions.size > 1) {
        TabRow(selectedTabIndex = selected) {
            solutions.forEachIndexed { i, s ->
                Tab(selected = i == selected, onClick = { onSelect(i) }, text = {
                    Text(
                        when (s.kind) {
                            SolutionKind.SHORT -> "Ngắn (${s.moveCount})"
                            SolutionKind.HUMAN_FRIENDLY -> "Dễ làm (${s.moveCount})"
                            else -> "${s.kind.displayName} (${s.moveCount})"
                        },
                    )
                })
            }
        }
    }
    val solution = solutions[selected.coerceIn(solutions.indices)]
    val player = rememberMovePlayerState(start, solution.moves)
    val stepStarts = remember(solution) {
        var i = 0
        if (solution.steps.size < 2) emptyMap()
        else buildMap { for (s in solution.steps) { if (s.moves.isNotEmpty()) put(i, s.label); i += s.moves.size } }
    }
    MovePlayerCube(player, Modifier.fillMaxWidth().height(280.dp))
    MovePlayerControls(player, stepLabels = stepStarts)
    SectionCard("${solution.kind.displayName} · ${solution.moveCount} bước · ${solution.computeTimeMs} ms") {
        if (solution.moves.isEmpty()) Text("Không cần bước nào.")
        solution.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        for (step in solution.steps) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(step.label, fontWeight = FontWeight.SemiBold)
                step.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                MovesText(step.moves)
            }
        }
        Text(
            "Ký hiệu: chữ cái = xoay mặt đó 90° theo chiều kim đồng hồ, ' = ngược chiều, 2 = 180°. x/y/z (màu tím) = xoay cả khối.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (goal != null && goal != SolveGoal.SOLVE_ALL) {
        OutlinedButton(onClick = onApplied, modifier = Modifier.fillMaxWidth()) { Text("Tôi đã làm xong bước này — cập nhật cube") }
    }
}
