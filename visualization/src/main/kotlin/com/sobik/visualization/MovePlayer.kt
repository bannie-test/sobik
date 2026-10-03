package com.sobik.visualization

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.Move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Playback state for a move sequence on a cube: index = number of moves already applied.
 * Independent of any solver — it only needs a start state and a list of moves.
 */
@Stable
class MovePlayerState(val start: CubeState, val moves: List<Move>, private val scope: CoroutineScope) {
    /** states[i] = cube after the first i moves (a 3x3 state is 54 bytes, so this stays tiny). */
    private val states: List<CubeState> = buildList {
        var s = start
        add(s)
        for (m in moves) { s = s.applyMoves(listOf(m)); add(s) }
    }

    var index by mutableIntStateOf(0)
        private set
    var animatingMove by mutableStateOf<Move?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
    val progress = Animatable(0f)
    /** Serializes animations: a step waits for the move in flight instead of being dropped. */
    private val animation = Mutex()
    private var playJob: Job? = null

    /** State to draw (the animating move, if any, is applied on top by the renderer). */
    val baseState: CubeState get() = states[index]
    val atEnd: Boolean get() = index >= moves.size
    val finalState: CubeState get() = states.last()

    private fun durationFor(m: Move): Int = ((if (m.turns == 2) 520 else 380) / speed).toInt()

    suspend fun next() = animation.withLock {
        if (atEnd) return@withLock
        val m = moves[index]
        try {
            animatingMove = m
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationFor(m), easing = FastOutSlowInEasing))
            index++
        } finally {
            animatingMove = null
            progress.snapTo(0f)
        }
    }

    suspend fun previous() = animation.withLock {
        if (index == 0) return@withLock
        index--
        val m = moves[index]
        try {
            animatingMove = m
            progress.snapTo(1f)
            progress.animateTo(0f, tween(durationFor(m), easing = FastOutSlowInEasing))
        } finally {
            animatingMove = null
            progress.snapTo(0f)
        }
    }

    fun jumpTo(i: Int) {
        if (animation.isLocked) return
        isPlaying = false
        index = i.coerceIn(0, moves.size)
    }

    fun togglePlay() {
        if (isPlaying) { isPlaying = false; return }
        if (atEnd) index = 0
        isPlaying = true
        // A loop paused mid-move is still finishing that move: let it continue instead of starting a second one.
        if (playJob?.isActive == true) return
        playJob = scope.launch {
            while (isPlaying && !atEnd) next()
            isPlaying = false
        }
    }

    fun stepForward() { isPlaying = false; scope.launch { next() } }
    fun stepBack() { isPlaying = false; scope.launch { previous() } }
}

@Composable
fun rememberMovePlayerState(start: CubeState, moves: List<Move>): MovePlayerState {
    val scope = rememberCoroutineScope()
    return remember(start, moves) { MovePlayerState(start, moves, scope) }
}

/** 3D cube for the player's current position, with the turning layer animated. */
@Composable
fun MovePlayerCube(state: MovePlayerState, modifier: Modifier = Modifier) {
    Cube3DView(
        state = state.baseState,
        modifier = modifier,
        move = state.animatingMove,
        progress = state.progress.value,
    )
}

/**
 * Transport controls + the move list (current move highlighted, tap to jump).
 * [stepLabels] optionally maps a move index to the label of the step starting there.
 */
@Composable
fun MovePlayerControls(
    state: MovePlayerState,
    modifier: Modifier = Modifier,
    stepLabels: Map<Int, String> = emptyMap(),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.index) {
        if (state.moves.isNotEmpty()) listState.animateScrollToItem((state.index - 2).coerceAtLeast(0))
    }
    Column(modifier) {
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(state.moves) { i, m ->
                val current = i == state.index
                val done = i < state.index
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (stepLabels.isNotEmpty()) Text(
                        stepLabels[i] ?: " ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Box(
                        Modifier
                            .background(
                                when {
                                    current -> MaterialTheme.colorScheme.primary
                                    done -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                RoundedCornerShape(8.dp),
                            )
                            .clickable { state.jumpTo(i) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            m.notation,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            color = if (current) MaterialTheme.colorScheme.onPrimary else if (m.isRotation) Color(0xFF8E24AA) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val compact = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            TextButton(onClick = { state.jumpTo(0) }, Modifier.weight(1f), contentPadding = compact) { Text("|◀") }
            FilledTonalButton(onClick = { state.stepBack() }, Modifier.weight(1.3f), enabled = state.index > 0, contentPadding = compact) { Text("◀") }
            FilledTonalButton(onClick = { state.togglePlay() }, Modifier.weight(1.6f), enabled = state.moves.isNotEmpty(), contentPadding = compact) {
                Text(if (state.isPlaying) "❚❚ Dừng" else "▶ Chạy")
            }
            FilledTonalButton(onClick = { state.stepForward() }, Modifier.weight(1.3f), enabled = !state.atEnd, contentPadding = compact) { Text("▶") }
            TextButton(onClick = { state.jumpTo(state.moves.size) }, Modifier.weight(1f), contentPadding = compact) { Text("▶|") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("${state.index}/${state.moves.size} bước · Tốc độ:", style = MaterialTheme.typography.bodySmall)
            for (s in listOf(0.5f, 1f, 2f)) {
                TextButton(onClick = { state.speed = s }) {
                    Text("${if (s == 0.5f) "0.5" else s.toInt().toString()}x", fontWeight = if (state.speed == s) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
