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
            AlgorithmPlayer(alg.cubeType, alg.moves, alg.setup, cubeHeight = 260)
            SectionCard("Công thức") {
                NotationText(alg.notation)
                if (alg.alternatives.isNotEmpty()) {
                    Text("Công thức thay thế:", fontWeight = FontWeight.Medium)
                    alg.alternatives.forEach { NotationText(it) }
                }
            }
            if (alg.recognition.isNotBlank()) SectionCard("Nhận diện") { Text(alg.recognition) }
            if (alg.explanation.isNotBlank()) SectionCard("Giải thích") { Text(alg.explanation) }
        }
    }
}
