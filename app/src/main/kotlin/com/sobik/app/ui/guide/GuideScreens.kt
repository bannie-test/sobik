package com.sobik.app.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AlgorithmPlayer
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.NotationText
import com.sobik.app.ui.common.SectionCard
import com.sobik.engine.applyMoves
import com.sobik.model.BeginnerLesson
import com.sobik.model.ColorScheme
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.LessonCase
import com.sobik.model.Notation
import com.sobik.visualization.CubeImage
import com.sobik.visualization.CubeMasks
import com.sobik.visualization.VIEW_PITCH_BOTTOM
import com.sobik.visualization.VIEW_PITCH_TOP

@Composable
fun GuideListScreen(container: AppContainer, nav: Navigator) {
    val lessons = remember { container.content.beginnerLessons }
    AppScaffold(container.content.beginnerTitle, onBack = { nav.pop() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lessons, key = { it.id }) { lesson ->
                Card(Modifier.fillMaxWidth().clickable { nav.push(Screen.Lesson(lesson.id)) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(lesson.goal, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(container: AppContainer, nav: Navigator, id: String) {
    val lessons = container.content.beginnerLessons
    val lesson = container.content.lesson(id) ?: return
    val index = lessons.indexOf(lesson)
    AppScaffold(lesson.title, onBack = { nav.pop() }, bottomBar = {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { nav.replace(Screen.Lesson(lessons[index - 1].id)) }, enabled = index > 0) { Text("◀ Bài trước") }
            TextButton(onClick = { nav.replace(Screen.Lesson(lessons[index + 1].id)) }, enabled = index < lessons.lastIndex) { Text("Bài tiếp ▶") }
        }
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Mục tiêu") { Text(lesson.goal) }
            SectionCard("Trạng thái cần đạt") {
                TargetPicture(lesson)
                Text(lesson.targetState)
            }
            if (lesson.recognition.isNotBlank()) SectionCard("Cách nhận diện") { Text(lesson.recognition) }
            if (lesson.paragraphs.isNotEmpty()) SectionCard("Giải thích") { lesson.paragraphs.forEach { Text(it) } }
            if (lesson.notation.isNotEmpty()) SectionCard("Bảng ký hiệu") {
                for (e in lesson.notation) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    val move = e.move
                    if (move != null) {
                        val solved = remember { CubeState.solved(CubeType.CUBE_3X3, ColorScheme.YELLOW_TOP) }
                        CubeImage(solved, Modifier.size(84.dp), arrows = remember(move) { Notation.parse(move) })
                    } else {
                        Spacer(Modifier.width(84.dp))
                    }
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(e.symbol, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(e.meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Mũi tên hồng chỉ chiều xoay của lớp; hai đầu mũi tên nghĩa là xoay 180°.", style = MaterialTheme.typography.bodySmall)
            }
            lesson.cases.forEach { CaseCard(it) }
            if (lesson.tips.isNotEmpty()) SectionCard("Mẹo") { lesson.tips.forEach { Text("• $it") } }
        }
    }
}

@Composable
private fun CaseCard(case: LessonCase) {
    var showDemo by remember { mutableStateOf(false) }
    SectionCard(case.title) {
        CasePicture(case)
        Text("Nhận diện: ${case.recognition}", style = MaterialTheme.typography.bodyMedium)
        case.algorithm?.let { NotationText(it) }
        if (case.explanation.isNotBlank()) Text("Vì sao: ${case.explanation}", style = MaterialTheme.typography.bodyMedium)
        if (case.whenToUse.isNotBlank()) Text("Khi nào dùng: ${case.whenToUse}", style = MaterialTheme.typography.bodySmall)
        val alg = case.algorithm
        if (alg != null) {
            TextButton(onClick = { showDemo = !showDemo }) { Text(if (showDemo) "Ẩn mô phỏng 3D" else "Xem mô phỏng 3D từng bước") }
            if (showDemo) {
                val moves = remember(alg) { Notation.parse(alg) }
                val setup = remember(case.setupMoves, moves) { case.setupMoves?.let { Notation.parse(it) } ?: Notation.invert(moves) }
                AlgorithmPlayer(CubeType.CUBE_3X3, moves, setup)
            }
        }
    }
}

/** Solved cube with the part this lesson builds highlighted (the rest gray). */
@Composable
private fun TargetPicture(lesson: BeginnerLesson) {
    val mask = lesson.targetMask ?: return
    val colors = remember(lesson.id) { CubeMasks.apply(CubeState.solved(CubeType.CUBE_3X3, ColorScheme.YELLOW_TOP), mask) }
    val pitch = if (lesson.targetView == "bottom") VIEW_PITCH_BOTTOM else VIEW_PITCH_TOP
    Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        CubeImage(colors, 3, Modifier.size(180.dp), pitch = pitch)
        if (lesson.targetView == "bottom") Text("(nhìn từ phía dưới — mặt trắng)", style = MaterialTheme.typography.labelSmall)
    }
}

/** Static picture of the case (before the algorithm) with an arrow for the first move. */
@Composable
private fun CasePicture(case: LessonCase) {
    val alg = case.algorithm ?: return
    val moves = remember(alg) { Notation.parse(alg) }
    val start = remember(alg, case.setupMoves) {
        val setup = case.setupMoves?.let { Notation.parse(it) } ?: Notation.invert(moves)
        CubeState.solved(CubeType.CUBE_3X3, ColorScheme.YELLOW_TOP).applyMoves(setup)
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        CubeImage(start, Modifier.size(132.dp), arrows = moves.take(1))
        Text(
            "Bước đầu tiên: ${moves.first().notation}",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
