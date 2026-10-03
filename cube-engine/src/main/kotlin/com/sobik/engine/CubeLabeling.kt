package com.sobik.engine

import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.StickerColor

/**
 * Converts colors to face labels (which face a sticker "belongs" to when solved).
 *  - 3x3: the center colors define the mapping.
 *  - 2x2: no centers, so the corner at DBL defines D, B and L and opposites (colors that never
 *    share a corner) define U, F and R. Solvers then keep DBL fixed (moves U, R, F only).
 */
object CubeLabeling {

    /** color ordinal -> face ordinal, or null if impossible. */
    fun colorToFace(state: CubeState): IntArray? = when (state.type) {
        CubeType.CUBE_3X3 -> colorToFace3x3(state.toByteArray())
        CubeType.CUBE_2X2 -> colorToFace2x2(state.toByteArray())
        else -> null
    }

    fun colorToFace3x3(colors: ByteArray): IntArray? {
        val map = IntArray(6) { -1 }
        for (f in 0 until 6) {
            val c = colors[f * 9 + 4].toInt()
            if (map[c] >= 0) return null
            map[c] = f
        }
        return map
    }

    /** Opposite color of each color on a 2x2, derived from corner co-occurrence; null if inconsistent. */
    fun opposites2x2(colors: ByteArray): IntArray? {
        val facelets = CubieCube.cornerFacelets(2)
        val touch = Array(6) { BooleanArray(6) }
        for (corner in facelets) {
            val a = colors[corner[0]].toInt(); val b = colors[corner[1]].toInt(); val c = colors[corner[2]].toInt()
            if (a == b || b == c || a == c) return null
            touch[a][b] = true; touch[b][a] = true; touch[a][c] = true; touch[c][a] = true; touch[b][c] = true; touch[c][b] = true
        }
        val opp = IntArray(6) { -1 }
        for (c in 0 until 6) {
            val candidates = (0 until 6).filter { it != c && !touch[c][it] }
            if (candidates.size != 1) return null
            opp[c] = candidates[0]
        }
        for (c in 0 until 6) if (opp[opp[c]] != c) return null
        return opp
    }

    fun colorToFace2x2(colors: ByteArray): IntArray? {
        val opp = opposites2x2(colors) ?: return null
        val dbl = CubieCube.cornerFacelets(2)[CubieCube.DBL]
        val d = colors[dbl[0]].toInt(); val b = colors[dbl[1]].toInt(); val l = colors[dbl[2]].toInt()
        val map = IntArray(6) { -1 }
        val assign = listOf(Face.D to d, Face.B to b, Face.L to l, Face.U to opp[d], Face.F to opp[b], Face.R to opp[l])
        for ((face, color) in assign) {
            if (map[color] >= 0) return null
            map[color] = face.ordinal
        }
        return map
    }

    fun labels(colors: ByteArray, colorToFace: IntArray): ByteArray =
        ByteArray(colors.size) { colorToFace[colors[it].toInt()].toByte() }

    fun toCubieCube(state: CubeState): CubieCube? {
        if (state.type != CubeType.CUBE_3X3) return null
        val bytes = state.toByteArray()
        val map = colorToFace3x3(bytes) ?: return null
        return CubieCube.fromFaceLabels(labels(bytes, map))
    }

    fun toCornerCube(state: CubeState): CornerCube? {
        if (state.type != CubeType.CUBE_2X2) return null
        val bytes = state.toByteArray()
        val map = colorToFace2x2(bytes) ?: return null
        return CornerCube.fromFaceLabels(labels(bytes, map), 2)
    }

    /** Builds a 3x3 color state from a cubie cube using the given face -> color mapping. */
    fun fromCubieCube(cc: CubieCube, faceColor: (Face) -> StickerColor): CubeState {
        val labels = cc.toFaceLabels()
        return CubeState.fromColors(CubeType.CUBE_3X3, labels.map { faceColor(Face.entries[it.toInt()]) })
    }
}
