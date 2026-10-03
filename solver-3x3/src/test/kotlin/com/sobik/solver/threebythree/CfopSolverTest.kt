package com.sobik.solver.threebythree

import com.sobik.content.ContentRepository
import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubieCube
import com.sobik.engine.RandomCube
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.solver.threebythree.method.CaseLibrary
import com.sobik.solver.threebythree.method.CfopSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CfopSolverTest {
    private val content = ContentRepository()
    private val solver = CfopSolver(content.algorithms3x3)
    private val scheme = CubeState.solved(CubeType.CUBE_3X3)

    private fun randomState(rnd: Random) = CubeLabeling.fromCubieCube(RandomCube.cubie(rnd)) { scheme.center(it)!! }

    @Test
    fun `library algorithms are all accepted`() {
        val rejected = solver.rejectedAlgorithms.joinToString("\n") { "${it.first.name}: ${it.second} (${it.first.notation})" }
        assertTrue(solver.rejectedAlgorithms.isEmpty(), "Rejected:\n$rejected")
    }

    @Test
    fun `F2L library covers all 41 cases`() {
        val lib = solver.caseLibrary
        assertEquals(41, lib.f2l.size, "distinct F2L keys")
        // every playable FR configuration is reachable with some pre-AUF
        var uncovered = 0
        for (cp in listOf(0, 1, 2, 3, 4)) for (co in 0 until 3) for (ep in listOf(0, 1, 2, 3, 8)) for (eo in 0 until 2) {
            if (cp == 4 && co == 0 && ep == 8 && eo == 0) continue
            val c = CubieCube()
            c.cp[4] = c.cp[cp].also { c.cp[cp] = 4 }; c.co[cp] = co
            c.ep[8] = c.ep[ep].also { c.ep[ep] = 8 }; c.eo[ep] = eo
            val hit = (0 until 4).any { a ->
                val d = c.copy(); repeat(a) { d.applyMove(com.sobik.model.Face.U, 1) }
                lib.f2l.containsKey(CaseLibrary.f2lKey(d))
            }
            if (!hit) uncovered++
        }
        assertEquals(0, uncovered)
    }

    @Test
    fun `OLL library covers all orientation patterns`() {
        val lib = solver.caseLibrary
        var missing = 0
        for (code in 0 until 81 * 16) {
            val co = IntArray(4); val eo = IntArray(4)
            var r = code / 16
            for (i in 3 downTo 0) { co[i] = r % 3; r /= 3 }
            for (i in 0 until 4) eo[i] = (code shr (3 - i)) and 1
            if (co.sum() % 3 != 0 || eo.sum() % 2 != 0) continue
            if (co.all { it == 0 } && eo.all { it == 0 }) continue
            val c = CubieCube()
            for (i in 0 until 4) { c.co[i] = co[i]; c.eo[i] = eo[i] }
            val hit = (0 until 4).any { a ->
                val d = c.copy(); repeat(a) { d.applyMove(com.sobik.model.Face.U, 1) }
                lib.oll.containsKey(CaseLibrary.ollKey(d))
            }
            if (!hit) missing++
        }
        assertEquals(0, missing, "uncovered OLL patterns")
        assertEquals(57, content.algorithms3x3.count { it.category == com.sobik.model.AlgorithmCategory.OLL })
    }

    @Test
    fun `PLL library covers all permutations`() {
        val lib = solver.caseLibrary
        var missing = 0
        val perms4 = permutations(listOf(0, 1, 2, 3))
        for (cp in perms4) for (ep in perms4) {
            val c = CubieCube()
            for (i in 0 until 4) { c.cp[i] = cp[i]; c.ep[i] = ep[i] }
            if (CubieCube.permutationParity(c.cp) != CubieCube.permutationParity(c.ep)) continue
            val ok = (0 until 4).any { a ->
                val d = c.copy(); repeat(a) { d.applyMove(com.sobik.model.Face.U, 1) }
                d.isSolved() || lib.pll.containsKey(CaseLibrary.pllKey(d))
            }
            if (!ok) missing++
        }
        assertEquals(0, missing, "uncovered PLL permutations")
    }

    @Test
    fun `human friendly solve works on random cubes`() {
        val rnd = Random(77)
        var totalMoves = 0
        val n = 100
        repeat(n) {
            val state = randomState(rnd)
            val out = solver.solve(state, SolveGoal.SOLVE_ALL)
            assertIs<SolveOutcome.Success>(out, state.serialize())
            val sol = out.solutions.first()
            totalMoves += sol.moveCount
            assertTrue(state.applyMoves(sol.moves).isSolved(), state.serialize())
        }
        println("CFOP average: ${totalMoves / n} moves")
    }

    @Test
    fun `partial goals respect the current stage`() {
        val rnd = Random(5)
        val state = randomState(rnd)
        val avail = solver.goalAvailability(state)
        assertNotNull(avail[SolveGoal.OLL])
        assertNotNull(avail[SolveGoal.PLL])

        val cross = solver.solve(state, SolveGoal.CROSS) as SolveOutcome.Success
        val afterCross = state.applyMoves(cross.solutions[0].moves)
        assertTrue(solver.analyze(afterCross).crossSolved)

        var cur = afterCross
        repeat(4) { i ->
            val res = solver.solve(cur, SolveGoal.NEXT_F2L)
            assertIs<SolveOutcome.Success>(res)
            val before = solver.analyze(cur).solvedSlots
            cur = cur.applyMoves(res.solutions[0].moves)
            val a = solver.analyze(cur)
            assertTrue(a.crossSolved && a.solvedSlots == before + 1, "F2L step ${i + 1}")
        }
        assertIs<SolveOutcome.NotApplicable>(solver.solve(cur, SolveGoal.NEXT_F2L))
        val oll = solver.solve(cur, SolveGoal.OLL) as SolveOutcome.Success
        cur = cur.applyMoves(oll.solutions[0].moves)
        assertTrue(solver.analyze(cur).ollSolved)
        val pll = solver.solve(cur, SolveGoal.PLL) as SolveOutcome.Success
        assertTrue(cur.applyMoves(pll.solutions[0].moves).isSolved())
    }

    @Test
    fun `next F2L is refused without a cross`() {
        val state = scheme.applyMoves("R U F D' L2 B R' U2 F' D")
        if (!solver.analyze(state).crossSolved) {
            val out = solver.solve(state, SolveGoal.NEXT_F2L)
            assertIs<SolveOutcome.NotApplicable>(out)
        }
    }

    private fun permutations(xs: List<Int>): List<List<Int>> =
        if (xs.size <= 1) listOf(xs) else xs.flatMap { x -> permutations(xs - x).map { listOf(x) + it } }
}

class AlternativeAlgorithmsTest {
    @Test
    fun `alternatives solve the same case as the primary algorithm`() {
        val algs = ContentRepository().algorithms3x3
        val bad = ArrayList<String>()
        for (alg in algs.filter { it.alternatives.isNotEmpty() && it.category != com.sobik.model.AlgorithmCategory.BEGINNER }) {
            val primary = com.sobik.model.Notation.parse(alg.notation)
            val case = CubeState.solved(CubeType.CUBE_3X3).applyMoves(com.sobik.model.Notation.invert(primary))
            for (alt in alg.alternatives) {
                val moves = com.sobik.model.Notation.parse(alt)
                // the alternative must solve the primary's case up to AUF before/after and a final rotation
                val ok = (0 until 4).any { a -> (0 until 4).any { b ->
                    val pre = CaseLibrary.uTurns(a); val post = CaseLibrary.uTurns(b)
                    val f = com.sobik.solver.threebythree.method.Frame(case.applyMoves(pre + moves + post))
                    when (alg.category) {
                        com.sobik.model.AlgorithmCategory.F2L -> f.f2lSolved
                        com.sobik.model.AlgorithmCategory.OLL -> f.ollSolved
                        else -> f.solved
                    }
                } }
                if (!ok) bad += "${alg.name}: $alt"
            }
        }
        assertTrue(bad.isEmpty(), "Wrong alternatives:\n" + bad.joinToString("\n"))
    }
}
