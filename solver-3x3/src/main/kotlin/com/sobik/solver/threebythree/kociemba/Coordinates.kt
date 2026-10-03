package com.sobik.solver.threebythree.kociemba

import com.sobik.engine.CubieCube

/** Coordinate encodings used by the two-phase algorithm. All "solved" coordinates are 0. */
internal object Coordinates {
    const val N_TWIST = 2187      // 3^7 corner orientations
    const val N_FLIP = 2048       // 2^11 edge orientations
    const val N_SLICE = 495       // C(12,4) positions of the 4 UD-slice edges
    const val N_PERM8 = 40320     // 8! corner permutations / U-D edge permutations (phase 2)
    const val N_SLICE_PERM = 24   // 4! slice edge permutations (phase 2)

    /** 4-of-12 position masks; index 0 is the solved mask (positions 8..11). */
    private val SLICE_MASKS: IntArray = (0 until 4096).filter { Integer.bitCount(it) == 4 }.sortedDescending().toIntArray()
    private val SLICE_INDEX = IntArray(4096) { -1 }.also { idx -> SLICE_MASKS.forEachIndexed { i, m -> idx[m] = i } }

    fun twist(c: CubieCube): Int { var r = 0; for (i in 0 until 7) r = r * 3 + c.co[i]; return r }

    fun setTwist(c: CubieCube, v: Int) {
        var r = v; var sum = 0
        for (i in 6 downTo 0) { c.co[i] = r % 3; sum += c.co[i]; r /= 3 }
        c.co[7] = (3 - sum % 3) % 3
    }

    fun flip(c: CubieCube): Int { var r = 0; for (i in 0 until 11) r = r * 2 + c.eo[i]; return r }

    fun setFlip(c: CubieCube, v: Int) {
        var r = v; var sum = 0
        for (i in 10 downTo 0) { c.eo[i] = r % 2; sum += c.eo[i]; r /= 2 }
        c.eo[11] = sum % 2
    }

    fun slice(c: CubieCube): Int {
        var mask = 0
        for (i in 0 until 12) if (c.ep[i] >= 8) mask = mask or (1 shl i)
        return SLICE_INDEX[mask]
    }

    fun setSlice(c: CubieCube, v: Int) {
        val mask = SLICE_MASKS[v]
        var s = 8; var o = 0
        for (i in 0 until 12) c.ep[i] = if (mask and (1 shl i) != 0) s++ else o++
    }

    fun cornerPerm(c: CubieCube): Int = rank(c.cp, 0, 8, 0)
    fun setCornerPerm(c: CubieCube, v: Int) = unrank(v, c.cp, 0, 8, 0)
    fun edgePerm8(c: CubieCube): Int = rank(c.ep, 0, 8, 0)
    fun setEdgePerm8(c: CubieCube, v: Int) = unrank(v, c.ep, 0, 8, 0)
    fun slicePerm(c: CubieCube): Int = rank(c.ep, 8, 4, 8)
    fun setSlicePerm(c: CubieCube, v: Int) = unrank(v, c.ep, 8, 4, 8)

    /** Lehmer-code rank of p[off until off+n] whose values are base until base+n. */
    private fun rank(p: IntArray, off: Int, n: Int, base: Int): Int {
        var r = 0
        for (i in 0 until n) {
            var smaller = 0
            val v = p[off + i]
            for (j in i + 1 until n) if (p[off + j] < v) smaller++
            r = r * (n - i) + smaller
        }
        return r
    }

    private fun unrank(rank: Int, p: IntArray, off: Int, n: Int, base: Int) {
        val digits = IntArray(n)
        var r = rank
        for (i in n - 1 downTo 0) { digits[i] = r % (n - i); r /= (n - i) }
        val avail = MutableList(n) { base + it }
        for (i in 0 until n) p[off + i] = avail.removeAt(digits[i])
    }
}

/** 4-bit packed table (values 0..14, 15 = unvisited) to halve pruning-table memory. */
internal class NibbleTable(val size: Int) {
    private val data = ByteArray((size + 1) / 2) { -1 }

    operator fun get(i: Int): Int = (data[i shr 1].toInt() shr ((i and 1) shl 2)) and 0xF

    operator fun set(i: Int, v: Int) {
        val k = i shr 1
        val shift = (i and 1) shl 2
        data[k] = ((data[k].toInt() and (0xF shl shift).inv()) or (v shl shift)).toByte()
    }

    val bytes: Int get() = data.size
}
