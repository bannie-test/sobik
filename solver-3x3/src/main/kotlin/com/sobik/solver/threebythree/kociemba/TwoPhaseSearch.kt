package com.sobik.solver.threebythree.kociemba

import com.sobik.engine.CubieCube

/**
 * Kociemba's two-phase algorithm.
 *
 * Phase 1 (IDA*, 18 moves) brings the cube into G1 = <U, D, R2, L2, F2, B2>: all orientations
 * solved and the UD-slice edges in the slice. Phase 2 (IDA*, 10 moves) solves within G1.
 * Iterating the phase-1 depth and then repeatedly lowering the length limit trades time for
 * shorter solutions. Not thread-safe: use one instance per thread (tables are shared).
 */
internal class TwoPhaseSearch(private val t: Tables = Tables.get()) {
    private val path = IntArray(MAX_PATH)
    private lateinit var cube: CubieCube
    private var lengthLimit = 0
    private var deadline = Long.MAX_VALUE
    private var aborted = false
    private var nodes = 0L
    private var solutionLength = -1

    /**
     * @param maxLength first solution must not be longer than this (moves, HTM)
     * @param targetLength stop improving once a solution this short is found
     * @param improveTimeMs time budget for searching shorter solutions after the first one
     * @return move indices (face * 3 + quarterTurns - 1), or null if none found in time
     */
    fun solve(cc: CubieCube, maxLength: Int = 24, targetLength: Int = 20, improveTimeMs: Long = 300, hardTimeoutMs: Long = 10_000): IntArray? {
        require(cc.isSolvable()) { "Cube state is not solvable" }
        cube = cc
        val start = System.nanoTime()
        var best: IntArray? = search(maxLength, start + hardTimeoutMs * 1_000_000)
        val improveDeadline = System.nanoTime() + improveTimeMs * 1_000_000
        while (best != null && best.size > targetLength) {
            val shorter = search(best.size - 1, improveDeadline) ?: break
            best = shorter
        }
        return best
    }

    private fun search(limit: Int, deadlineNs: Long): IntArray? {
        lengthLimit = limit
        deadline = deadlineNs
        aborted = false
        solutionLength = -1
        val tw = Coordinates.twist(cube); val fl = Coordinates.flip(cube); val sl = Coordinates.slice(cube)
        val h = maxOf(t.twistSlicePrune[tw * Coordinates.N_SLICE + sl], t.flipSlicePrune[fl * Coordinates.N_SLICE + sl])
        for (depth1 in h..limit) {
            if (phase1(tw, fl, sl, 0, depth1, -1)) return path.copyOf(solutionLength)
            if (aborted) return null
        }
        return null
    }

    private fun phase1(tw: Int, fl: Int, sl: Int, depth: Int, togo: Int, lastFace: Int): Boolean {
        if (togo == 0) {
            if (tw != 0 || fl != 0 || sl != 0) return false
            // A phase-1 path ending in a G1 move was already tried with a shorter phase 1.
            if (depth > 0 && isP2Move(path[depth - 1])) return false
            return startPhase2(depth)
        }
        if ((++nodes and 0x3FF) == 0L && System.nanoTime() > deadline) { aborted = true; return false }
        for (f in 0 until 6) {
            if (f == lastFace || f == lastFace - 3) continue
            for (p in 0 until 3) {
                val m = f * 3 + p
                val ntw = t.twistMove[tw * Tables.N_MOVES + m].code
                val nfl = t.flipMove[fl * Tables.N_MOVES + m].code
                val nsl = t.sliceMove[sl * Tables.N_MOVES + m].code
                if (t.twistSlicePrune[ntw * Coordinates.N_SLICE + nsl] >= togo) continue
                if (t.flipSlicePrune[nfl * Coordinates.N_SLICE + nsl] >= togo) continue
                path[depth] = m
                if (phase1(ntw, nfl, nsl, depth + 1, togo - 1, f)) return true
                if (aborted) return false
            }
        }
        return false
    }

    private fun startPhase2(depth1: Int): Boolean {
        val c = cube.copy()
        for (i in 0 until depth1) {
            val mc = CubieCube.MOVES[path[i] / 3]
            repeat(path[i] % 3 + 1) { c.multiply(mc) }
        }
        val cp = Coordinates.cornerPerm(c)
        val ep = Coordinates.edgePerm8(c)
        val sp = Coordinates.slicePerm(c)
        val maxDepth2 = minOf(lengthLimit - depth1, MAX_PHASE2)
        val h = maxOf(t.cornerSlicePrune[cp * Coordinates.N_SLICE_PERM + sp], t.edgeSlicePrune[ep * Coordinates.N_SLICE_PERM + sp])
        val lastFace = if (depth1 > 0) path[depth1 - 1] / 3 else -1
        for (d2 in h..maxDepth2) {
            if (phase2(cp, ep, sp, depth1, d2, lastFace)) {
                solutionLength = depth1 + d2
                return true
            }
            if (aborted) return false
        }
        return false
    }

    private fun phase2(cp: Int, ep: Int, sp: Int, depth: Int, togo: Int, lastFace: Int): Boolean {
        if (togo == 0) return cp == 0 && ep == 0 && sp == 0
        if ((++nodes and 0x3FF) == 0L && System.nanoTime() > deadline) { aborted = true; return false }
        for (k in 0 until Tables.N_P2) {
            val m = Tables.P2_MOVES[k]
            val f = m / 3
            if (f == lastFace || f == lastFace - 3) continue
            val ncp = t.cornerPermMove[cp * Tables.N_P2 + k].code
            val nep = t.edgePermMove[ep * Tables.N_P2 + k].code
            val nsp = t.slicePermMove[sp * Tables.N_P2 + k].code
            if (t.cornerSlicePrune[ncp * Coordinates.N_SLICE_PERM + nsp] >= togo) continue
            if (t.edgeSlicePrune[nep * Coordinates.N_SLICE_PERM + nsp] >= togo) continue
            path[depth] = m
            if (phase2(ncp, nep, nsp, depth + 1, togo - 1, f)) return true
            if (aborted) return false
        }
        return false
    }

    private fun isP2Move(m: Int): Boolean = Tables.P2_MOVES.contains(m)

    private companion object {
        const val MAX_PATH = 40
        const val MAX_PHASE2 = 18
    }
}
