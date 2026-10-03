package com.sobik.model

enum class SolutionKind(val displayName: String) {
    OPTIMAL("Tối ưu"),
    SHORT("Ngắn nhất (Kociemba)"),
    HUMAN_FRIENDLY("Dễ thực hiện (CFOP)"),
    PARTIAL("Từng bước"),
}

/** What the user asked the solver for. */
enum class SolveGoal(val displayName: String) {
    SOLVE_ALL("Solve All"),
    CROSS("Cross"),
    NEXT_F2L("Next F2L"),
    OLL("OLL"),
    PLL("PLL"),
}

data class SolutionStep(
    val label: String,
    val moves: List<Move>,
    val description: String? = null,
)

data class Solution(
    val cubeType: CubeType,
    val kind: SolutionKind,
    val steps: List<SolutionStep>,
    val computeTimeMs: Long = 0,
    val isOptimal: Boolean = false,
    val note: String? = null,
) {
    val moves: List<Move> get() = steps.flatMap { it.moves }
    /** Face-turn count (half turn metric), rotations excluded. */
    val moveCount: Int get() = Notation.htm(moves)
    val notation: String get() = Notation.format(moves)
}

sealed interface SolveOutcome {
    data class Success(val solutions: List<Solution>) : SolveOutcome
    /** The request can't be served for this state; [reason] is user-facing. */
    data class NotApplicable(val reason: String) : SolveOutcome
    data class Failure(val reason: String) : SolveOutcome
}
