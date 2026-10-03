package com.sobik.engine

import com.sobik.model.Face
import com.sobik.model.Move

/** Corner-only cubie model (2x2 and the corner part of bigger cubes). Same conventions as [CubieCube]. */
class CornerCube(
    val cp: IntArray = IntArray(8) { it },
    val co: IntArray = IntArray(8),
) {
    fun copy() = CornerCube(cp.copyOf(), co.copyOf())

    fun multiply(b: CornerCube) {
        val nCp = IntArray(8); val nCo = IntArray(8)
        for (i in 0 until 8) {
            nCp[i] = cp[b.cp[i]]
            nCo[i] = (co[b.cp[i]] + b.co[i]) % 3
        }
        nCp.copyInto(cp); nCo.copyInto(co)
    }

    fun applyMoves(moves: List<Move>): CornerCube {
        for (m in moves) {
            val f = requireNotNull(m.face.toFace()) { "Corner model only supports face turns, got $m" }
            require(m.isOuterFaceTurn) { "Corner model only supports outer face turns, got $m" }
            repeat(m.turns) { multiply(MOVES[f.ordinal]) }
        }
        return this
    }

    fun isSolved(): Boolean = (0 until 8).all { cp[it] == it && co[it] == 0 }

    override fun equals(other: Any?) = other is CornerCube && other.cp.contentEquals(cp) && other.co.contentEquals(co)
    override fun hashCode() = cp.contentHashCode() * 31 + co.contentHashCode()

    /** Face labels for an NxN sticker array; only corner stickers are written, others stay -1. */
    fun toCornerLabels(n: Int): ByteArray {
        val facelets = CubieCube.cornerFacelets(n)
        val f = ByteArray(6 * n * n) { -1 }
        for (i in 0 until 8) for (k in 0 until 3)
            f[facelets[i][(k + co[i]) % 3]] = CubieCube.CORNER_COLOR[cp[i]][k].toByte()
        return f
    }

    companion object {
        val MOVES: Array<CornerCube> by lazy {
            Array(6) { f -> CubieCube.MOVES[f].let { CornerCube(it.cp.copyOf(), it.co.copyOf()) } }
        }

        /** Identifies corners from NxN face labels; null if a corner is not a real piece. */
        fun fromFaceLabels(f: ByteArray, n: Int): CornerCube? {
            val facelets = CubieCube.cornerFacelets(n)
            val cc = CornerCube(IntArray(8), IntArray(8))
            for (i in 0 until 8) {
                var ori = -1
                for (k in 0 until 3) {
                    val v = f[facelets[i][k]].toInt()
                    if (v == Face.U.ordinal || v == Face.D.ordinal) { if (ori >= 0) return null; ori = k }
                }
                if (ori < 0) return null
                val c0 = f[facelets[i][ori]].toInt()
                val c1 = f[facelets[i][(ori + 1) % 3]].toInt()
                val c2 = f[facelets[i][(ori + 2) % 3]].toInt()
                val j = (0 until 8).firstOrNull {
                    CubieCube.CORNER_COLOR[it][0] == c0 && CubieCube.CORNER_COLOR[it][1] == c1 && CubieCube.CORNER_COLOR[it][2] == c2
                } ?: return null
                cc.cp[i] = j; cc.co[i] = ori
            }
            return cc
        }
    }
}
