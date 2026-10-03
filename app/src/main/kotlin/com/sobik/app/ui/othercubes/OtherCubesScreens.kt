package com.sobik.app.ui.othercubes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AlgorithmPlayer
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.NotationText
import com.sobik.app.ui.common.SectionCard
import com.sobik.model.ColorScheme
import com.sobik.model.CubeState
import com.sobik.visualization.Cube3DView

@Composable
fun OtherCubesScreen(container: AppContainer, nav: Navigator) {
    val guides = remember { container.content.bigCubeGuides }
    AppScaffold("Cube khác", onBack = { nav.pop() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "4x4 - 7x7 hiện có phần kiến thức và công thức. Quét và giải tự động cho các cube này sẽ được bổ sung sau.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(guides, key = { it.cubeSize }) { g ->
                Card(Modifier.fillMaxWidth().clickable { nav.push(Screen.BigCubeGuide(g.cubeSize)) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(g.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(g.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun BigCubeGuideScreen(container: AppContainer, nav: Navigator, size: Int) {
    val guide = remember(size) { container.content.guide(size) } ?: return
    AppScaffold(guide.title, onBack = { nav.pop() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cube3DView(remember(size) { CubeState.solved(guide.cubeType, ColorScheme.YELLOW_TOP) }, Modifier.fillMaxWidth().height(200.dp))
            Text(guide.summary, style = MaterialTheme.typography.bodyLarge)
            for (section in guide.sections) {
                SectionCard(section.title) {
                    section.paragraphs.forEach { Text(it) }
                    for (algId in section.algorithmIds) {
                        val alg = container.content.algorithm(algId) ?: continue
                        Text(alg.name, fontWeight = FontWeight.SemiBold)
                        NotationText(alg.notation)
                        if (alg.recognition.isNotBlank()) Text("Nhận diện: ${alg.recognition}", style = MaterialTheme.typography.bodySmall)
                        if (alg.explanation.isNotBlank()) Text(alg.explanation, style = MaterialTheme.typography.bodySmall)
                        AlgorithmPlayer(alg.cubeType, alg.moves, alg.setup, cubeHeight = 220)
                    }
                }
            }
        }
    }
}
