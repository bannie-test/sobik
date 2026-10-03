package com.sobik.solver.threebythree.method

import com.sobik.engine.CubeGeometry
import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubieCube
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.model.StickerColor

/**
 * A cube seen from a particular holding orientation. Wraps the sticker state (source of truth,
 * handles rotations/slices) and the derived cubie view relative to the current centers.
 */
internal class Frame(val state: CubeState) {
    val cc: CubieCube = requireNotNull(CubeLabeling.toCubieCube(state)) { "invalid state" }

    fun apply(moves: List<Move>): Frame = if (moves.isEmpty()) this else Frame(state.applyMoves(moves))
    fun apply(notation: String): Frame = apply(Notation.parse(notation))
    fun color(face: Face): StickerColor = state.center(face)!!

    val crossSolved: Boolean get() = (4..7).all { cc.ep[it] == it && cc.eo[it] == 0 }
    fun slotSolved(slot: Int): Boolean {
        val c = SLOT_CORNER[slot]; val e = SLOT_EDGE[slot]
        return cc.cp[c] == c && cc.co[c] == 0 && cc.ep[e] == e && cc.eo[e] == 0
    }
    val solvedSlots: List<Int> get() = (0 until 4).filter { slotSolved(it) }
    val f2lSolved: Boolean get() = crossSolved && (0 until 4).all { slotSolved(it) }
    val ollSolved: Boolean get() = f2lSolved && (0 until 4).all { cc.co[it] == 0 && cc.eo[it] == 0 }
    val solved: Boolean get() = cc.isSolved()

    companion object {
        /** Corner and edge positions of the F2L slots FR, FL, BL, BR. */
        val SLOT_CORNER = intArrayOf(CubieCube.DFR, CubieCube.DLF, CubieCube.DBL, CubieCube.DRB)
        val SLOT_EDGE = intArrayOf(CubieCube.FR, CubieCube.FL, CubieCube.BL, CubieCube.BR)
        val SLOT_NAMES = arrayOf("trước-phải (FR)", "trước-trái (FL)", "sau-trái (BL)", "sau-phải (BR)")

        /** Whole-cube rotation that brings each face to the bottom. */
        val TO_BOTTOM: Map<Face, List<Move>> = mapOf(
            Face.D to emptyList(),
            Face.U to Notation.parse("z2"),
            Face.F to Notation.parse("x'"),
            Face.B to Notation.parse("x"),
            Face.R to Notation.parse("z"),
            Face.L to Notation.parse("z'"),
        )

        /** True when the sequence returns the centers to where they started (no net rotation). */
        fun hasNoNetRotation(moves: List<Move>): Boolean {
            val geo = CubeGeometry.of(3)
            val labels = geo.apply(ByteArray(54) { (it / 9).toByte() }, moves)
            return (0 until 6).all { labels[it * 9 + 4].toInt() == it }
        }
    }
}
