package com.sobik.solver.threebythree

import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubieCube
import com.sobik.engine.RandomCube
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.SolveOutcome
import com.sobik.solver.threebythree.kociemba.KociembaSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KociembaSolverTest {
    private val solver = KociembaSolver()

    @Test
    fun `solves random states with short solutions`() {
        val t0 = System.nanoTime()
        solver.warmUp()
        println("tables: ${(System.nanoTime() - t0) / 1_000_000} ms")
        val rnd = Random(2024)
        var total = 0L; var maxLen = 0; var sumLen = 0
        val n = 60
        repeat(n) {
            val cc = RandomCube.cubie(rnd)
            val start = System.nanoTime()
            val moves = solver.solveCubie(cc)!!
            total += System.nanoTime() - start
            maxLen = maxOf(maxLen, moves.size); sumLen += moves.size
            assertTrue(cc.copy().applyMoves(moves).isSolved())
        }
        println("kociemba avg ${total / n / 1_000_000} ms, avg len ${sumLen.toDouble() / n}, max $maxLen")
        assertTrue(maxLen <= 24)
    }

    @Test
    fun `solves via CubeState and handles solved cube`() {
        val solved = CubeState.solved(CubeType.CUBE_3X3)
        val s = solved.applyMoves("R U R' U' F2 D L' B")
        val out = solver.solve(s)
        assertIs<SolveOutcome.Success>(out)
        assertTrue(s.applyMoves(out.solutions[0].moves).isSolved())
        val empty = solver.solve(solved) as SolveOutcome.Success
        assertTrue(empty.solutions[0].moves.isEmpty())
        assertTrue(CubeLabeling.toCubieCube(solved)!!.isSolved())
        assertTrue(CubieCube().isSolvable())
    }
}
