package com.sobik.model

/** Turnable layer groups. Face letters, slices (M/E/S) and whole-cube rotations (x/y/z). */
enum class MoveFace(val symbol: String, val isRotation: Boolean = false, val isSlice: Boolean = false) {
    U("U"), R("R"), F("F"), D("D"), L("L"), B("B"),
    M("M", isSlice = true), E("E", isSlice = true), S("S", isSlice = true),
    X("x", isRotation = true), Y("y", isRotation = true), Z("z", isRotation = true);

    val isFace: Boolean get() = !isRotation && !isSlice
    fun toFace(): Face? = if (isFace) Face.valueOf(name) else null

    companion object {
        fun of(face: Face): MoveFace = valueOf(face.name)
    }
}

/**
 * A single turn in standard notation.
 *
 * @param turns clockwise quarter turns: 1 = "R", 2 = "R2", 3 = "R'".
 * @param depth for face moves: number of the layer counted from the face (1 = outer layer).
 * @param wide  when true all layers 1..depth turn (Rw, 3Rw); otherwise only layer [depth] (R, 2R).
 */
data class Move(val face: MoveFace, val turns: Int, val depth: Int = 1, val wide: Boolean = false) {
    init {
        require(turns in 1..3) { "turns must be 1..3" }
        require(depth >= 1) { "depth must be >= 1" }
    }

    val isRotation: Boolean get() = face.isRotation
    /** Plain outer face turn (U, R2, F' ...), the only kind cubie-level solvers emit. */
    val isOuterFaceTurn: Boolean get() = face.isFace && depth == 1 && !wide

    fun inverse(): Move = copy(turns = 4 - turns)

    val notation: String
        get() = buildString {
            if (face.isFace) {
                if (wide && depth > 2) append(depth)
                if (!wide && depth > 1) append(depth)
            }
            append(face.symbol)
            if (wide) append('w')
            when (turns) {
                2 -> append('2')
                3 -> append('\'')
            }
        }

    override fun toString(): String = notation

    companion object {
        fun of(face: Face, turns: Int) = Move(MoveFace.of(face), turns)
    }
}
