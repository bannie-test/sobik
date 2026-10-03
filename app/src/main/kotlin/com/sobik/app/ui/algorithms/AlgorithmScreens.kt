package com.sobik.app.ui.algorithms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AlgorithmPlayer
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.NotationText
import com.sobik.app.ui.common.Pill
import com.sobik.app.ui.common.SectionCard
import com.sobik.engine.applyMoves
import com.sobik.model.Algorithm
import com.sobik.model.AlgorithmCategory
import com.sobik.model.ColorScheme
import com.sobik.model.CubeState
import com.sobik.model.Difficulty
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.visualization.Cube3DView
import com.sobik.visualization.LastLayerView

private val CATEGORIES = listOf(AlgorithmCategory.BEGINNER, AlgorithmCategory.F2L, AlgorithmCategory.OLL, AlgorithmCategory.PLL)

@Composable
fun AlgorithmLibraryScreen(container: AppContainer, nav: Navigator) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { container.content.algorithms3x3 }
    val items = remember(tab, query) {
        all.filter { it.category == CATEGORIES[tab] }
            .filter { query.isBlank() || it.name.contains(query, true) || (it.group ?: "").contains(query, true) || it.notation.contains(query, true) }
    }
    AppScaffold("Thư viện công thức 3x3", onBack = { nav.pop() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                CATEGORIES.forEachIndexed { i, c ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text("${c.displayName} (${all.count { it.category == c }})") })
                }
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(8.dp), label = { Text("Tìm theo tên, nhóm hoặc công thức") }, singleLine = true)
            LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { alg -> AlgorithmRow(alg) { nav.push(Screen.AlgorithmDetail(alg.id)) } }
            }
        }
    }
}

/** Case picture: last-layer diagram for OLL/PLL, small 3D cube otherwise. */
@Composable
fun CaseImage(alg: Algorithm, modifier: Modifier) {
    val case = remember(alg.id) { CubeState.solved(alg.cubeType, ColorScheme.YELLOW_TOP).applyMoves(alg.setup) }
    when (alg.category) {
        AlgorithmCategory.OLL -> LastLayerView(case, modifier, orientationOnly = true)
        AlgorithmCategory.PLL -> LastLayerView(case, modifier)
        else -> Cube3DView(case, modifier)
    }
}

@Composable
private fun AlgorithmRow(alg: Algorithm, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CaseImage(alg, Modifier.size(84.dp))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(alg.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                NotationText(alg.notation)
                Text(listOfNotNull(alg.group, difficultyLabel(alg.difficulty)).joinToString(" · "), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun difficultyLabel(d: Difficulty) = when (d) {
    Difficulty.EASY -> "Dễ"
    Difficulty.MEDIUM -> "Trung bình"
    Difficulty.HARD -> "Khó"
}

@Composable
fun AlgorithmDetailScreen(container: AppContainer, nav: Navigator, id: String) {
    val alg = remember(id) { container.content.algorithm(id) } ?: return
    AppScaffold(alg.name, onBack = { nav.pop() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(alg.category.displayName)
                alg.group?.let { Pill(it) }
                Pill(difficultyLabel(alg.difficulty))
            }
            if (alg.category == AlgorithmCategory.OLL || alg.category == AlgorithmCategory.PLL) {
                CaseImage(alg, Modifier.size(160.dp).align(Alignment.CenterHorizontally))
            }
            var selected by rememberSaveable(id) { mutableStateOf(0) }
            val versions = remember(id) { listOf(alg.notation) + alg.alternatives }
            val case = remember(id) { CubeState.solved(alg.cubeType, ColorScheme.YELLOW_TOP).applyMoves(alg.setup) }
            val shown = remember(id, selected) { alignToCase(case, Notation.parse(versions[selected]), alg.category) }
            AlgorithmPlayer(alg.cubeType, shown, alg.setup, cubeHeight = 260)
            SectionCard(if (versions.size > 1) "Các cách giải (${versions.size}) — chạm để xem" else "Công thức") {
                versions.forEachIndexed { i, v ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = i }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == i, onClick = { selected = i })
                        Column {
                            Text(if (i == 0) "Chính" else "Cách ${i + 1}", style = MaterialTheme.typography.labelSmall)
                            NotationText(v)
                        }
                    }
                }
                if (shown != Notation.parse(versions[selected])) {
                    Text("Mô phỏng: ${Notation.format(shown)} (đã thêm xoay U để khớp với trường hợp).", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (alg.recognition.isNotBlank()) SectionCard("Nhận diện") { Text(alg.recognition) }
            if (alg.explanation.isNotBlank()) SectionCard("Giải thích") { Text(alg.explanation) }
        }
    }
}

/**
 * An alternative algorithm may expect the case turned by a U move, or leave a final U turn.
 * Returns the moves with the needed U turns added so it solves [case] for its category.
 */
fun alignToCase(case: CubeState, moves: List<Move>, category: AlgorithmCategory): List<Move> {
    if (case.size != 3) return moves
    val u = listOf(emptyList(), Notation.parse("U"), Notation.parse("U2"), Notation.parse("U'"))
    for (a in u) for (b in u) {
        val candidate = a + moves + b
        if (reached(case.applyMoves(candidate), category)) return Notation.simplify(candidate)
    }
    return moves
}

private fun reached(s: CubeState, category: AlgorithmCategory): Boolean {
    fun faceDone(f: Face, rows: IntRange) = rows.all { r -> (0 until 3).all { c -> s[f, r * 3 + c] == s.center(f) } }
    val f2l = faceDone(Face.D, 0..2) && listOf(Face.F, Face.R, Face.B, Face.L).all { faceDone(it, 1..2) }
    return when (category) {
        AlgorithmCategory.F2L -> f2l
        AlgorithmCategory.OLL -> f2l && faceDone(Face.U, 0..2)
        else -> s.isSolved()
    }
}
