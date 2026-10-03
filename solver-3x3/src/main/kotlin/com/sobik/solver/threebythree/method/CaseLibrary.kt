package com.sobik.solver.threebythree.method

import com.sobik.engine.CubieCube
import com.sobik.model.Algorithm
import com.sobik.model.AlgorithmCategory
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Notation

/**
 * Recognition tables built from the algorithm library. The case an algorithm solves is derived
 * by applying its inverse to a solved cube, so recognition can never disagree with the
 * algorithm data. Algorithms that don't do what their category promises are rejected and
 * listed in [rejected].
 */
internal class CaseLibrary(algorithms: List<Algorithm>) {
    /** F2L (FR slot) key -> algorithm. */
    val f2l = HashMap<Int, Algorithm>()
    /** OLL key -> algorithm. */
    val oll = HashMap<Int, Algorithm>()
    /** PLL key -> (algorithm, final U turns). */
    val pll = HashMap<Int, Pair<Algorithm, Int>>()
    val rejected = ArrayList<Pair<Algorithm, String>>()

    init {
        val solved = CubeState.solved(CubeType.CUBE_3X3)
        for (alg in algorithms.filter { it.cubeSize == 3 }) {
            val moves = runCatching { alg.moves }.getOrNull()
            if (moves == null) { rejected += alg to "notation"; continue }
            if (alg.category !in setOf(AlgorithmCategory.F2L, AlgorithmCategory.OLL, AlgorithmCategory.PLL)) continue
            if (!Frame.hasNoNetRotation(moves)) { rejected += alg to "net rotation"; continue }
            val case = Frame(solved).apply(Notation.invert(moves))
            when (alg.category) {
                AlgorithmCategory.F2L -> {
                    val c = case.cc
                    val ok = case.crossSolved && (1..3).all { case.slotSolved(it) } && !case.slotSolved(0) &&
                        f2lPlayable(c)
                    if (!ok) { rejected += alg to "not an FR-slot F2L case"; continue }
                    f2l.putIfAbsent(f2lKey(c), alg)
                }
                AlgorithmCategory.OLL -> {
                    if (!case.f2lSolved || case.ollSolved) { rejected += alg to "not an OLL case"; continue }
                    oll.putIfAbsent(ollKey(case.cc), alg)
                }
                AlgorithmCategory.PLL -> {
                    if (!case.ollSolved || case.solved) { rejected += alg to "not a PLL case"; continue }
                    for (b in 0 until 4) {
                        val withAuf = Frame(solved).apply(Notation.invert(moves + uTurns(b)))
                        pll.putIfAbsent(pllKey(withAuf.cc), alg to b)
                    }
                }
                else -> Unit
            }
        }
    }

    companion object {
        fun uTurns(n: Int) = if (n % 4 == 0) emptyList() else Notation.parse(listOf("U", "U2", "U'")[n % 4 - 1])

        /** Both FR pieces are in the U layer or already in the FR slot. */
        fun f2lPlayable(c: CubieCube): Boolean {
            val cp = (0 until 8).first { c.cp[it] == CubieCube.DFR }
            val ep = (0 until 12).first { c.ep[it] == CubieCube.FR }
            return (cp < 4 || cp == CubieCube.DFR) && (ep < 4 || ep == CubieCube.FR)
        }

        fun f2lKey(c: CubieCube): Int {
            val cp = (0 until 8).first { c.cp[it] == CubieCube.DFR }
            val ep = (0 until 12).first { c.ep[it] == CubieCube.FR }
            return (cp * 3 + c.co[cp]) * 24 + ep * 2 + c.eo[ep]
        }

        fun ollKey(c: CubieCube): Int {
            var k = 0
            for (i in 0 until 4) k = k * 3 + c.co[i]
            for (i in 0 until 4) k = k * 2 + c.eo[i]
            return k
        }

        fun pllKey(c: CubieCube): Int {
            var k = 0
            for (i in 0 until 4) k = k * 4 + c.cp[i]
            for (i in 0 until 4) k = k * 4 + c.ep[i]
            return k
        }
    }
}
