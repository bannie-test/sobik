package com.sobik.solver.threebythree.kociemba

import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubeValidator
import com.sobik.engine.CubieCube
import com.sobik.model.CubeSolver
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.SolutionKind
import com.sobik.model.SolutionStep
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.model.Solution

/**
 * Computational 3x3 solver (Kociemba two-phase). Finds a short path to the solved state;
 * it knows nothing about human methods. Tables are shared across instances and built lazily.
 */
class KociembaSolver(
    private val targetLength: Int = 20,
    private val improveTimeMs: Long = 400,
) : CubeSolver {
    override val cubeType = CubeType.CUBE_3X3
    override val supportedGoals = setOf(SolveGoal.SOLVE_ALL)

    override fun warmUp() { Tables.get() }

    override fun solve(state: CubeState, goal: SolveGoal): SolveOutcome {
        if (goal != SolveGoal.SOLVE_ALL) return SolveOutcome.NotApplicable("Kociemba chỉ giải toàn bộ cube.")
        val validation = CubeValidator.validate(state)
        if (!validation.isValid) return SolveOutcome.Failure(validation.errors.first().message)
        val cc = CubeLabeling.toCubieCube(state) ?: return SolveOutcome.Failure("Không đọc được trạng thái cube.")
        val start = System.nanoTime()
        val moves = solveCubie(cc) ?: return SolveOutcome.Failure("Không tìm được lời giải trong thời gian cho phép.")
        val ms = (System.nanoTime() - start) / 1_000_000
        return SolveOutcome.Success(
            listOf(
                Solution(
                    cubeType = cubeType,
                    kind = SolutionKind.SHORT,
                    steps = listOf(SolutionStep("Kociemba", moves, "${moves.size} bước (HTM), thuật toán Two-Phase")),
                    computeTimeMs = ms,
                ),
            ),
        )
    }

    fun solveCubie(cc: CubieCube): List<Move>? =
        TwoPhaseSearch().solve(cc, targetLength = targetLength, improveTimeMs = improveTimeMs)
            ?.map { Move.of(Face.entries[it / 3], it % 3 + 1) }
}
