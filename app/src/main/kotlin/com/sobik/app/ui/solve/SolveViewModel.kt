package com.sobik.app.ui.solve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sobik.app.AppContainer
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.Notation
import com.sobik.model.Move
import com.sobik.model.SolutionKind
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.solver.threebythree.ThreeByThreeSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SolveUiState(
    val state: CubeState?,
    val goals: List<SolveGoal> = emptyList(),
    /** null = available; otherwise the reason the goal does not apply to this state. */
    val availability: Map<SolveGoal, String?> = emptyMap(),
    val progress: String? = null,
    val running: SolveGoal? = null,
    val goal: SolveGoal? = null,
    val outcome: SolveOutcome? = null,
    val selected: Int = 0,
    /** Scramble derived from the shortest solution (state stays the source of truth). */
    val scramble: List<Move>? = null,
)

/**
 * Runs solvers only on explicit user request, on a background dispatcher. Nothing is solved on
 * screen entry except the cheap stage analysis used to enable/disable goals.
 */
class SolveViewModel(private val container: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SolveUiState(container.cubeSession.current.value))
    val ui: StateFlow<SolveUiState> = _ui.asStateFlow()

    init { _ui.value.state?.let { analyze(it) } }

    private fun analyze(state: CubeState) {
        val solver = container.solverFor(state.type) ?: return
        viewModelScope.launch {
            val (availability, progress) = withContext(Dispatchers.Default) {
                val a = solver.goalAvailability(state)
                val p = (solver as? ThreeByThreeSolver)?.analyze(state)?.summary
                a to p
            }
            _ui.update {
                it.copy(
                    goals = SolveGoal.entries.filter { g -> g in solver.supportedGoals },
                    availability = availability,
                    progress = progress,
                )
            }
        }
    }

    fun solve(goal: SolveGoal) {
        val state = _ui.value.state ?: return
        val solver = container.solverFor(state.type) ?: return
        if (_ui.value.running != null) return
        _ui.update { it.copy(running = goal, goal = goal, outcome = null, selected = 0) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.Default) { solver.solve(state, goal) }
            val scramble = (outcome as? SolveOutcome.Success)?.solutions
                ?.firstOrNull { it.kind == SolutionKind.SHORT || it.kind == SolutionKind.OPTIMAL }
                ?.let { Notation.invert(it.moves) }
            _ui.update { it.copy(running = null, outcome = outcome, scramble = scramble ?: it.scramble) }
        }
    }

    fun select(index: Int) = _ui.update { it.copy(selected = index) }

    /** The user performed the shown (partial) solution on the real cube: continue from there. */
    fun applySelected() {
        val ui = _ui.value
        val state = ui.state ?: return
        val solution = (ui.outcome as? SolveOutcome.Success)?.solutions?.getOrNull(ui.selected) ?: return
        val next = state.applyMoves(solution.moves)
        container.cubeSession.select(next)
        viewModelScope.launch(Dispatchers.IO) { container.cubeSession.confirm(next) }
        _ui.value = SolveUiState(next)
        analyze(next)
    }
}
