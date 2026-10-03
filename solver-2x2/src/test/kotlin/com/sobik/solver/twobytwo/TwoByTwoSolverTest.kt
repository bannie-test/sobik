package com.sobik.solver.twobytwo

import com.sobik.engine.CubeLabeling
import com.sobik.engine.RandomCube
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.SolveOutcome
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwoByTwoSolverTest {
    private val solver = TwoByTwoSolver()
    private val solved = CubeState.solved(CubeType.CUBE_2X2)

    @Test
    fun `solves scrambles including whole-cube rotations`() {
        val rnd = Random(42)
        repeat(100) {
            val scramble = List(25) { listOf("U", "R", "F", "D", "L", "B", "x", "y", "z")[rnd.nextInt(9)] + listOf("", "2", "'")[rnd.nextInt(3)] }
                .joinToString(" ")
            val state = solved.applyMoves(scramble)
            val out = solver.solve(state)
            assertIs<SolveOutcome.Success>(out, scramble)
            val sol = out.solutions.first()
            assertTrue(sol.moveCount <= 11, "God's number for 2x2 is 11, got ${sol.moveCount}")
            assertTrue(state.applyMoves(sol.moves).isSolved(), scramble)
        }
    }

    @Test
    fun `random states solve optimally within 11 moves`() {
        val rnd = Random(9)
        var total = 0L
        repeat(200) {
            val cc = RandomCube.corners(rnd)
            val start = System.nanoTime()
            val moves = solver.solveCorners(cc)!!
            total += System.nanoTime() - start
            assertTrue(moves.size <= 11)
            assertTrue(cc.copy().applyMoves(moves).isSolved())
        }
        println("2x2 avg solve: ${total / 200 / 1000} us")
    }

    @Test
    fun `short scramble gets an optimal solution`() {
        val state = solved.applyMoves("R U F'")
        val sol = (solver.solve(state) as SolveOutcome.Success).solutions.first()
        assertEquals(3, sol.moveCount)
        assertTrue(CubeLabeling.toCornerCube(state) != null)
        assertTrue(state[Face.U, 0] != null)
    }

    @Test
    fun `invalid state is rejected`() {
        val bytes = solved.toByteArray(); bytes[0] = bytes[5]
        val out = solver.solve(CubeState.fromBytes(CubeType.CUBE_2X2, bytes))
        assertIs<SolveOutcome.Failure>(out)
    }
}
