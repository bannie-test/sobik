package com.sobik.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sobik.engine.applyMoves
import com.sobik.model.ColorScheme
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.visualization.MovePlayerControls
import com.sobik.visualization.MovePlayerCube
import com.sobik.visualization.rememberMovePlayerState

/**
 * Demo player for an algorithm: starts from the case (setup applied to a solved cube) and plays
 * the algorithm move by move. Learning content is shown with yellow on top / white on the
 * bottom, the way the cube is held for the beginner method and CFOP.
 */
@Composable
fun AlgorithmPlayer(
    type: CubeType,
    moves: List<Move>,
    setup: List<Move> = Notation.invert(moves),
    modifier: Modifier = Modifier,
    cubeHeight: Int = 240,
    scheme: ColorScheme = ColorScheme.YELLOW_TOP,
) {
    val start = remember(type, setup, scheme) { CubeState.solved(type, scheme).applyMoves(setup) }
    val player = rememberMovePlayerState(start, moves)
    Column(modifier) {
        MovePlayerCube(player, Modifier.fillMaxWidth().height(cubeHeight.dp))
        MovePlayerControls(player)
    }
}
