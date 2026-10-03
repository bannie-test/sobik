package com.sobik.solver.threebythree.kociemba

import com.sobik.engine.CubieCube
import com.sobik.solver.threebythree.kociemba.Coordinates.N_FLIP
import com.sobik.solver.threebythree.kociemba.Coordinates.N_PERM8
import com.sobik.solver.threebythree.kociemba.Coordinates.N_SLICE
import com.sobik.solver.threebythree.kociemba.Coordinates.N_SLICE_PERM
import com.sobik.solver.threebythree.kociemba.Coordinates.N_TWIST

/**
 * Move and pruning tables for the two-phase algorithm. Built once (~4 MB in total:
 * move tables as 16-bit chars, pruning tables packed as nibbles).
 *
 * Moves are indexed face * 3 + (quarterTurns - 1) with faces in U, R, F, D, L, B order.
 * Phase 2 uses the 10 moves of <U, D, R2, L2, F2, B2>, see [P2_MOVES].
 */
internal class Tables {
    val twistMove = CharArray(N_TWIST * N_MOVES)
    val flipMove = CharArray(N_FLIP * N_MOVES)
    val sliceMove = CharArray(N_SLICE * N_MOVES)
    val cornerPermMove = CharArray(N_PERM8 * N_P2)
    val edgePermMove = CharArray(N_PERM8 * N_P2)
    val slicePermMove = CharArray(N_SLICE_PERM * N_P2)

    val twistSlicePrune: NibbleTable
    val flipSlicePrune: NibbleTable
    val cornerSlicePrune: NibbleTable
    val edgeSlicePrune: NibbleTable

    init {
        val moves = CubieCube.MOVES
        val c = CubieCube()
        for (i in 0 until N_TWIST) {
            Coordinates.setTwist(c, i)
            for (f in 0 until 6) {
                val d = c.copy()
                for (p in 0 until 3) { d.cornerMultiply(moves[f]); twistMove[i * N_MOVES + f * 3 + p] = Coordinates.twist(d).toChar() }
            }
        }
        val e = CubieCube()
        for (i in 0 until N_FLIP) {
            Coordinates.setFlip(e, i)
            for (f in 0 until 6) {
                val d = e.copy()
                for (p in 0 until 3) { d.edgeMultiply(moves[f]); flipMove[i * N_MOVES + f * 3 + p] = Coordinates.flip(d).toChar() }
            }
        }
        for (i in 0 until N_SLICE) {
            val s = CubieCube(); Coordinates.setSlice(s, i)
            for (f in 0 until 6) {
                val d = s.copy()
                for (p in 0 until 3) { d.edgeMultiply(moves[f]); sliceMove[i * N_MOVES + f * 3 + p] = Coordinates.slice(d).toChar() }
            }
        }
        for (i in 0 until N_PERM8) {
            val cc = CubieCube(); Coordinates.setCornerPerm(cc, i)
            val ec = CubieCube(); Coordinates.setEdgePerm8(ec, i)
            for (k in 0 until N_P2) {
                val m = P2_MOVES[k]
                val dc = cc.copy(); val de = ec.copy()
                repeat(m % 3 + 1) { dc.cornerMultiply(moves[m / 3]); de.edgeMultiply(moves[m / 3]) }
                cornerPermMove[i * N_P2 + k] = Coordinates.cornerPerm(dc).toChar()
                edgePermMove[i * N_P2 + k] = Coordinates.edgePerm8(de).toChar()
            }
        }
        for (i in 0 until N_SLICE_PERM) {
            val sc = CubieCube(); Coordinates.setSlicePerm(sc, i)
            for (k in 0 until N_P2) {
                val m = P2_MOVES[k]
                val d = sc.copy()
                repeat(m % 3 + 1) { d.edgeMultiply(moves[m / 3]) }
                slicePermMove[i * N_P2 + k] = Coordinates.slicePerm(d).toChar()
            }
        }

        twistSlicePrune = buildPrune(N_TWIST, N_SLICE, N_MOVES, twistMove, sliceMove)
        flipSlicePrune = buildPrune(N_FLIP, N_SLICE, N_MOVES, flipMove, sliceMove)
        cornerSlicePrune = buildPrune(N_PERM8, N_SLICE_PERM, N_P2, cornerPermMove, slicePermMove)
        edgeSlicePrune = buildPrune(N_PERM8, N_SLICE_PERM, N_P2, edgePermMove, slicePermMove)
    }

    /** Breadth-first distances over the product of two coordinates (index = a * nB + b). */
    private fun buildPrune(nA: Int, nB: Int, nMoves: Int, moveA: CharArray, moveB: CharArray): NibbleTable {
        val total = nA * nB
        val t = NibbleTable(total)
        t[0] = 0
        var done = 1
        var depth = 0
        while (done < total) {
            for (i in 0 until total) {
                if (t[i] != depth) continue
                val a = i / nB; val b = i % nB
                for (m in 0 until nMoves) {
                    val j = moveA[a * nMoves + m].code * nB + moveB[b * nMoves + m].code
                    if (t[j] == 0xF) { t[j] = depth + 1; done++ }
                }
            }
            depth++
            check(depth < 15) { "pruning depth overflow" }
        }
        return t
    }

    companion object {
        const val N_MOVES = 18
        const val N_P2 = 10
        /** U, U2, U', R2, F2, D, D2, D', L2, B2 as indices into the 18-move list. */
        val P2_MOVES = intArrayOf(0, 1, 2, 4, 7, 9, 10, 11, 13, 16)

        @Volatile private var instance: Tables? = null
        fun get(): Tables = instance ?: synchronized(this) { instance ?: Tables().also { instance = it } }
    }
}
