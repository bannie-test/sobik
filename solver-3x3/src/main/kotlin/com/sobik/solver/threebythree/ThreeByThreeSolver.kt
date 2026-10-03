package com.sobik.solver.threebythree

import com.sobik.model.Algorithm
import com.sobik.model.CubeSolver
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.solver.threebythree.kociemba.KociembaSolver
import com.sobik.solver.threebythree.method.CfopAnalysis
import com.sobik.solver.threebythree.method.CfopSolver

/**
 * 3x3 entry point. "Solve All" returns both a Short solution (computational, Kociemba) and a
 * Human Friendly one (CFOP). Partial goals (Cross / Next F2L / OLL / PLL) go to the method
 * solver only. Another computational engine (e.g. an optimal IDA* solver) can be plugged in
 * through [computational].
 */
class ThreeByThreeSolver(
    algorithms: List<Algorithm>,
    private val computational: CubeSolver = KociembaSolver(),
) : CubeSolver {
    private val method = CfopSolver(algorithms)

    override val cubeType = CubeType.CUBE_3X3
    override val supportedGoals = method.supportedGoals

    override fun warmUp() {
        computational.warmUp()
        method.warmUp()
    }

    fun analyze(state: CubeState): CfopAnalysis = method.analyze(state)

    override fun goalAvailability(state: CubeState) = method.goalAvailability(state)

    override fun solve(state: CubeState, goal: SolveGoal): SolveOutcome {
        if (goal != SolveGoal.SOLVE_ALL) return method.solve(state, goal)
        val short = computational.solve(state, goal)
        val human = method.solve(state, goal)
        val solutions = listOf(short, human).filterIsInstance<SolveOutcome.Success>().flatMap { it.solutions }
        if (solutions.isNotEmpty()) return SolveOutcome.Success(solutions)
        return short as? SolveOutcome.Failure ?: human
    }
}
