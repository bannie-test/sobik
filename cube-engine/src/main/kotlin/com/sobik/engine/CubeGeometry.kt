package com.sobik.engine

import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.MoveFace
import java.util.concurrent.ConcurrentHashMap

/**
 * 3D geometry of an NxN cube and sticker permutations for every move.
 *
 * Coordinates: x points to R, y to U, z to F. Cubie centers use odd/even integer coordinates
 * in {-(n-1), -(n-3), ..., n-1}. Each sticker is identified by its cubie position + outward normal,
 * which lets us derive the permutation of any move (face, wide, slice, rotation) for any size
 * from pure geometry instead of hand-written tables.
 */
class CubeGeometry private constructor(val n: Int) {
    val stickerCount = 6 * n * n
    private val m = n - 1

    /** Per sticker: x, y, z of the cubie, then nx, ny, nz of the outward normal. */
    val position: Array<IntArray> = Array(stickerCount) { stickerCoords(it) }

    private val lookup = HashMap<Long, Int>(stickerCount * 2).apply {
        position.forEachIndexed { i, p -> put(key(p), i) }
    }

    private val permCache = ConcurrentHashMap<Move, IntArray>()

    fun index(face: Face, row: Int, col: Int) = face.ordinal * n * n + row * n + col

    private fun stickerCoords(i: Int): IntArray {
        val face = Face.entries[i / (n * n)]
        val r = (i % (n * n)) / n
        val c = i % n
        val a = -m + 2 * c
        val b = -m + 2 * r
        return when (face) {
            Face.U -> intArrayOf(a, m, b, 0, 1, 0)
            Face.D -> intArrayOf(a, -m, -b, 0, -1, 0)
            Face.F -> intArrayOf(a, -b, m, 0, 0, 1)
            Face.B -> intArrayOf(-a, -b, -m, 0, 0, -1)
            Face.R -> intArrayOf(m, -b, -a, 1, 0, 0)
            Face.L -> intArrayOf(-m, -b, a, -1, 0, 0)
        }
    }

    private fun key(p: IntArray): Long {
        var k = 0L
        for (v in p) k = k * 64 + (v + 32)
        return k
    }

    /**
     * Source permutation: after the move, sticker j shows the color previously at `perm[j]`.
     */
    fun permutation(move: Move): IntArray = permCache.getOrPut(move) { computePermutation(move) }

    /** Layers affected by [move] and rotation about the positive axis, see [MoveGeometry]. */
    fun moveGeometry(move: Move): MoveGeometry {
        val spec = AXIS_SPEC.getValue(move.face)
        val q = Math.floorMod(spec.sign * move.turns, 4)
        val layers: Set<Int> = when {
            move.face.isRotation -> (0 until n).map { -m + 2 * it }.toSet()
            move.face.isSlice -> if (n % 2 == 1) setOf(0) else (1 until n - 1).map { -m + 2 * it }.toSet()
            else -> {
                val ks = if (move.wide) 1..move.depth else move.depth..move.depth
                ks.filter { it <= n }.map { k -> if (spec.positive) m - 2 * (k - 1) else -m + 2 * (k - 1) }.toSet()
            }
        }
        return MoveGeometry(spec.axis, layers, q)
    }

    private fun computePermutation(move: Move): IntArray {
        val g = moveGeometry(move)
        val perm = IntArray(stickerCount) { it }
        for (i in 0 until stickerCount) {
            val p = position[i]
            if (p[g.axis] !in g.layers) continue
            val rotated = p.copyOf()
            repeat(g.quarterTurns) { rotate90(rotated, g.axis) }
            val j = lookup.getValue(key(rotated))
            perm[j] = i
        }
        return perm
    }

    /** Applies [moves] to a sticker array (any per-sticker payload stored as bytes). */
    fun apply(stickers: ByteArray, moves: List<Move>): ByteArray {
        var cur = stickers
        for (mv in moves) {
            val perm = permutation(mv)
            val next = ByteArray(cur.size)
            for (j in next.indices) next[j] = cur[perm[j]]
            cur = next
        }
        return if (cur === stickers) stickers.copyOf() else cur
    }

    data class MoveGeometry(
        /** 0 = x, 1 = y, 2 = z. */
        val axis: Int,
        /** Coordinates along [axis] of the layers that turn. */
        val layers: Set<Int>,
        /** Number of +90 degree (right-hand) rotations about the positive axis. */
        val quarterTurns: Int,
    )

    private class AxisSpec(val axis: Int, val sign: Int, val positive: Boolean)

    companion object {
        private val cache = ConcurrentHashMap<Int, CubeGeometry>()
        fun of(n: Int): CubeGeometry = cache.getOrPut(n) { CubeGeometry(n) }

        /** Clockwise (seen from the face) is -90 degrees about the outward normal. */
        private val AXIS_SPEC = mapOf(
            MoveFace.U to AxisSpec(1, -1, true),
            MoveFace.D to AxisSpec(1, 1, false),
            MoveFace.R to AxisSpec(0, -1, true),
            MoveFace.L to AxisSpec(0, 1, false),
            MoveFace.F to AxisSpec(2, -1, true),
            MoveFace.B to AxisSpec(2, 1, false),
            MoveFace.M to AxisSpec(0, 1, false),
            MoveFace.E to AxisSpec(1, 1, false),
            MoveFace.S to AxisSpec(2, -1, true),
            MoveFace.X to AxisSpec(0, -1, true),
            MoveFace.Y to AxisSpec(1, -1, true),
            MoveFace.Z to AxisSpec(2, -1, true),
        )

        /** In-place +90 degree rotation about [axis] of both position and normal. */
        fun rotate90(p: IntArray, axis: Int) {
            for (o in intArrayOf(0, 3)) {
                val x = p[o]; val y = p[o + 1]; val z = p[o + 2]
                when (axis) {
                    0 -> { p[o + 1] = -z; p[o + 2] = y }
                    1 -> { p[o] = z; p[o + 2] = -x }
                    else -> { p[o] = -y; p[o + 1] = x }
                }
            }
        }
    }
}
