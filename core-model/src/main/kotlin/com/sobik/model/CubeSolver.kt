package com.sobik.model

/**
 * Solver contract: takes a [CubeState], returns solutions. Implementations must not touch UI
 * and should be called from a background thread (they may build lookup tables on first use).
 */
interface CubeSolver {
    val cubeType: CubeType
    val supportedGoals: Set<SolveGoal>

    fun solve(state: CubeState, goal: SolveGoal = SolveGoal.SOLVE_ALL): SolveOutcome

    /** For each goal: null when it can be requested now, otherwise a user-facing reason why not. */
    fun goalAvailability(state: CubeState): Map<SolveGoal, String?> =
        supportedGoals.associateWith { null }

    /** Optional: build tables ahead of time (e.g. while the user is scanning). */
    fun warmUp() {}
}
