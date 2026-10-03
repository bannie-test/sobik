package com.sobik.engine

import com.sobik.model.Face
import com.sobik.model.Move

/**
 * Cubie-level representation of a 3x3 (centers fixed): corner permutation/orientation and
 * edge permutation/orientation, using Kociemba's conventions.
 *
 * Corners: URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB.
 * Edges:   UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR.
 *
 * `cp[i] = j` means corner position i holds corner piece j. Multiplication `a * b` means
 * "apply a, then b".
 */
class CubieCube(
    val cp: IntArray = IntArray(8) { it },
    val co: IntArray = IntArray(8),
    val ep: IntArray = IntArray(12) { it },
    val eo: IntArray = IntArray(12),
) {
    fun copy() = CubieCube(cp.copyOf(), co.copyOf(), ep.copyOf(), eo.copyOf())

    /** this = this * b (corners only). */
    fun cornerMultiply(b: CubieCube) {
        val nCp = IntArray(8); val nCo = IntArray(8)
        for (i in 0 until 8) {
            nCp[i] = cp[b.cp[i]]
            nCo[i] = (co[b.cp[i]] + b.co[i]) % 3
        }
        nCp.copyInto(cp); nCo.copyInto(co)
    }

    /** this = this * b (edges only). */
    fun edgeMultiply(b: CubieCube) {
        val nEp = IntArray(12); val nEo = IntArray(12)
        for (i in 0 until 12) {
            nEp[i] = ep[b.ep[i]]
            nEo[i] = (eo[b.ep[i]] + b.eo[i]) % 2
        }
        nEp.copyInto(ep); nEo.copyInto(eo)
    }

    fun multiply(b: CubieCube) { cornerMultiply(b); edgeMultiply(b) }

    fun applyMove(face: Face, turns: Int) {
        val mc = MOVES[face.ordinal]
        repeat(turns) { multiply(mc) }
    }

    /** Applies outer face turns only (no slices, wide moves or rotations). */
    fun applyMoves(moves: List<Move>): CubieCube {
        for (m in moves) {
            val f = requireNotNull(m.face.toFace()) { "Cubie model only supports face turns, got $m" }
            require(m.isOuterFaceTurn) { "Cubie model only supports outer face turns, got $m" }
            applyMove(f, m.turns)
        }
        return this
    }

    fun inverse(): CubieCube {
        val r = CubieCube()
        for (i in 0 until 8) r.cp[cp[i]] = i
        for (i in 0 until 8) r.co[i] = (3 - co[r.cp[i]]) % 3
        for (i in 0 until 12) r.ep[ep[i]] = i
        for (i in 0 until 12) r.eo[i] = eo[r.ep[i]]
        return r
    }

    fun cornerParity(): Int = permutationParity(cp)
    fun edgeParity(): Int = permutationParity(ep)
    fun twistSum(): Int = co.sum() % 3
    fun flipSum(): Int = eo.sum() % 2

    /** Reachable from the solved state by face turns. */
    fun isSolvable(): Boolean {
        if (cp.sorted() != (0 until 8).toList() || ep.sorted() != (0 until 12).toList()) return false
        return twistSum() == 0 && flipSum() == 0 && cornerParity() == edgeParity()
    }

    fun isSolved(): Boolean {
        for (i in 0 until 8) if (cp[i] != i || co[i] != 0) return false
        for (i in 0 until 12) if (ep[i] != i || eo[i] != 0) return false
        return true
    }

    /** Facelet labels (face ordinal per sticker, 54 entries) with centers in place. */
    fun toFaceLabels(): ByteArray {
        val f = ByteArray(54)
        for (face in 0 until 6) f[face * 9 + 4] = face.toByte()
        for (i in 0 until 8) for (k in 0 until 3)
            f[CORNER_FACELET[i][(k + co[i]) % 3]] = CORNER_COLOR[cp[i]][k].toByte()
        for (i in 0 until 12) for (k in 0 until 2)
            f[EDGE_FACELET[i][(k + eo[i]) % 2]] = EDGE_COLOR[ep[i]][k].toByte()
        return f
    }

    override fun equals(other: Any?) = other is CubieCube && other.cp.contentEquals(cp) &&
        other.co.contentEquals(co) && other.ep.contentEquals(ep) && other.eo.contentEquals(eo)

    override fun hashCode() = cp.contentHashCode() * 31 + co.contentHashCode() * 17 + ep.contentHashCode() * 7 + eo.contentHashCode()

    override fun toString() = "CubieCube(cp=${cp.toList()}, co=${co.toList()}, ep=${ep.toList()}, eo=${eo.toList()})"

    companion object {
        const val URF = 0; const val UFL = 1; const val ULB = 2; const val UBR = 3
        const val DFR = 4; const val DLF = 5; const val DBL = 6; const val DRB = 7
        const val UR = 0; const val UF = 1; const val UL = 2; const val UB = 3
        const val DR = 4; const val DF = 5; const val DL = 6; const val DB = 7
        const val FR = 8; const val FL = 9; const val BL = 10; const val BR = 11

        val CORNER_NAMES = arrayOf("URF", "UFL", "ULB", "UBR", "DFR", "DLF", "DBL", "DRB")
        val EDGE_NAMES = arrayOf("UR", "UF", "UL", "UB", "DR", "DF", "DL", "DB", "FR", "FL", "BL", "BR")

        private const val U = 0; private const val R = 1; private const val F = 2
        private const val D = 3; private const val L = 4; private const val B = 5

        /** Faces of each corner piece, in clockwise order starting with the U/D sticker. */
        val CORNER_COLOR: Array<IntArray> = arrayOf(
            intArrayOf(U, R, F), intArrayOf(U, F, L), intArrayOf(U, L, B), intArrayOf(U, B, R),
            intArrayOf(D, F, R), intArrayOf(D, L, F), intArrayOf(D, B, L), intArrayOf(D, R, B),
        )

        val EDGE_COLOR: Array<IntArray> = arrayOf(
            intArrayOf(U, R), intArrayOf(U, F), intArrayOf(U, L), intArrayOf(U, B),
            intArrayOf(D, R), intArrayOf(D, F), intArrayOf(D, L), intArrayOf(D, B),
            intArrayOf(F, R), intArrayOf(F, L), intArrayOf(B, L), intArrayOf(B, R),
        )

        private fun fl(face: Int, k: Int) = face * 9 + k - 1

        /** Sticker indices (3x3 layout) of each corner position, same order as [CORNER_COLOR]. */
        val CORNER_FACELET: Array<IntArray> = arrayOf(
            intArrayOf(fl(U, 9), fl(R, 1), fl(F, 3)), intArrayOf(fl(U, 7), fl(F, 1), fl(L, 3)),
            intArrayOf(fl(U, 1), fl(L, 1), fl(B, 3)), intArrayOf(fl(U, 3), fl(B, 1), fl(R, 3)),
            intArrayOf(fl(D, 3), fl(F, 9), fl(R, 7)), intArrayOf(fl(D, 1), fl(L, 9), fl(F, 7)),
            intArrayOf(fl(D, 7), fl(B, 9), fl(L, 7)), intArrayOf(fl(D, 9), fl(R, 9), fl(B, 7)),
        )

        val EDGE_FACELET: Array<IntArray> = arrayOf(
            intArrayOf(fl(U, 6), fl(R, 2)), intArrayOf(fl(U, 8), fl(F, 2)),
            intArrayOf(fl(U, 4), fl(L, 2)), intArrayOf(fl(U, 2), fl(B, 2)),
            intArrayOf(fl(D, 6), fl(R, 8)), intArrayOf(fl(D, 2), fl(F, 8)),
            intArrayOf(fl(D, 4), fl(L, 8)), intArrayOf(fl(D, 8), fl(B, 8)),
            intArrayOf(fl(F, 6), fl(R, 4)), intArrayOf(fl(F, 4), fl(L, 6)),
            intArrayOf(fl(B, 6), fl(L, 4)), intArrayOf(fl(B, 4), fl(R, 6)),
        )

        /** Corner facelets mapped to an NxN layout (used by 2x2 and big-cube corner logic). */
        fun cornerFacelets(n: Int): Array<IntArray> = Array(8) { i ->
            IntArray(3) { k ->
                val idx = CORNER_FACELET[i][k]
                val face = idx / 9; val r = (idx % 9) / 3; val c = idx % 3
                face * n * n + (if (r == 0) 0 else n - 1) * n + (if (c == 0) 0 else n - 1)
            }
        }

        /** Builds a cube from face labels; returns null if any piece is not a real cubie. */
        fun fromFaceLabels(f: ByteArray): CubieCube? {
            val cc = CubieCube(IntArray(8) { -1 }, IntArray(8), IntArray(12) { -1 }, IntArray(12))
            for (i in 0 until 8) {
                var ori = -1
                for (k in 0 until 3) {
                    val v = f[CORNER_FACELET[i][k]].toInt()
                    if (v == U || v == D) { if (ori >= 0) return null; ori = k }
                }
                if (ori < 0) return null
                val c1 = f[CORNER_FACELET[i][(ori + 1) % 3]].toInt()
                val c2 = f[CORNER_FACELET[i][(ori + 2) % 3]].toInt()
                val j = (0 until 8).firstOrNull { CORNER_COLOR[it][1] == c1 && CORNER_COLOR[it][2] == c2 && CORNER_COLOR[it][0] == f[CORNER_FACELET[i][ori]].toInt() }
                    ?: return null
                cc.cp[i] = j; cc.co[i] = ori
            }
            for (i in 0 until 12) {
                val a = f[EDGE_FACELET[i][0]].toInt(); val b = f[EDGE_FACELET[i][1]].toInt()
                var found = false
                for (j in 0 until 12) {
                    if (EDGE_COLOR[j][0] == a && EDGE_COLOR[j][1] == b) { cc.ep[i] = j; cc.eo[i] = 0; found = true; break }
                    if (EDGE_COLOR[j][0] == b && EDGE_COLOR[j][1] == a) { cc.ep[i] = j; cc.eo[i] = 1; found = true; break }
                }
                if (!found) return null
            }
            return cc
        }

        /** The six basic clockwise face turns, derived from sticker geometry (index = Face.ordinal). */
        val MOVES: Array<CubieCube> by lazy {
            val geo = CubeGeometry.of(3)
            val solved = ByteArray(54) { (it / 9).toByte() }
            Array(6) { f ->
                val labels = geo.apply(solved, listOf(Move.of(Face.entries[f], 1)))
                requireNotNull(fromFaceLabels(labels))
            }
        }

        fun permutationParity(p: IntArray): Int {
            var s = 0
            for (i in p.indices) for (j in i + 1 until p.size) if (p[i] > p[j]) s++
            return s % 2
        }
    }
}
