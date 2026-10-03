package com.sobik.solver.threebythree.method

import com.sobik.engine.CubieCube
import com.sobik.model.Face
import com.sobik.model.Move

/**
 * Optimal cross solver for the D face. The state of the four D edges (position + flip of each)
 * is small enough (24^4 index space, 190,080 reachable) for an exact distance table, so the
 * solution is read off greedily in microseconds. Ties prefer U/R/F/L turns and quarter turns.
 */
internal object CrossSolver {
    private const val N = 24 * 24 * 24 * 24
    private val moveCubes: Array<CubieCube> = Array(18) { m ->
        val c = CubieCube(); repeat(m % 3 + 1) { c.multiply(CubieCube.MOVES[m / 3]) }; c
    }
    /** next[s * 18 + m]: new state (pos * 2 + flip) of one edge. */
    private val next = IntArray(24 * 18).also { t ->
        for (s in 0 until 24) for (m in 0 until 18) {
            val pos = s / 2; val ori = s % 2
            val mc = moveCubes[m]
            val np = (0 until 12).first { mc.ep[it] == pos }
            t[s * 18 + m] = np * 2 + (ori + mc.eo[np]) % 2
        }
    }
    private val dist: ByteArray by lazy { buildTable() }
    private val solvedIndex = index(intArrayOf(8, 10, 12, 14))

    private val PREFERRED: IntArray = run {
        val faces = listOf(Face.U, Face.R, Face.F, Face.L, Face.D, Face.B)
        faces.flatMap { f -> listOf(0, 2, 1).map { p -> f.ordinal * 3 + p } }.toIntArray()
    }

    private fun index(s: IntArray) = ((s[0] * 24 + s[1]) * 24 + s[2]) * 24 + s[3]

    private fun buildTable(): ByteArray {
        val d = ByteArray(N) { -1 }
        d[solvedIndex] = 0
        var frontier = intArrayOf(solvedIndex)
        var depth = 0
        while (frontier.isNotEmpty()) {
            val nextFrontier = ArrayList<Int>()
            for (idx in frontier) {
                val s = decode(idx)
                for (m in 0 until 18) {
                    val j = index(IntArray(4) { next[s[it] * 18 + m] })
                    if (d[j] < 0) { d[j] = (depth + 1).toByte(); nextFrontier += j }
                }
            }
            frontier = nextFrontier.toIntArray()
            depth++
        }
        return d
    }

    private fun decode(idx: Int): IntArray {
        var r = idx
        val s = IntArray(4)
        for (i in 3 downTo 0) { s[i] = r % 24; r /= 24 }
        return s
    }

    fun stateOf(cc: CubieCube): IntArray = IntArray(4) { k ->
        val piece = 4 + k
        val pos = (0 until 12).first { cc.ep[it] == piece }
        pos * 2 + cc.eo[pos]
    }

    fun distance(cc: CubieCube): Int = dist[index(stateOf(cc))].toInt()

    fun solve(cc: CubieCube): List<Move> {
        var s = stateOf(cc)
        var d = dist[index(s)].toInt()
        val out = ArrayList<Move>()
        while (d > 0) {
            for (m in PREFERRED) {
                val ns = IntArray(4) { next[s[it] * 18 + m] }
                if (dist[index(ns)].toInt() == d - 1) {
                    out += Move.of(Face.entries[m / 3], m % 3 + 1)
                    s = ns; d--
                    break
                }
            }
        }
        return out
    }
}
