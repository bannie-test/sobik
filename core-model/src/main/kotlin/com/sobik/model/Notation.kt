package com.sobik.model

/** Parser/formatter for WCA-style notation: R U R' U2, Rw, r, 3Rw', 2R, M2, x y' z2, (R U R' U'). */
object Notation {

    fun parse(text: String): List<Move> =
        text.replace('(', ' ').replace(')', ' ').replace('[', ' ').replace(']', ' ')
            .replace('’', '\'').replace('`', '\'')
            .split(' ', '\t', '\n', ',')
            .filter { it.isNotBlank() }
            .flatMap { parseToken(it) }

    /** Parses one token; tokens like "R2'" and "x2" are supported. Repetitions like "(R U)2" are not. */
    fun parseToken(token: String): List<Move> {
        var i = 0
        var prefix = 0
        while (i < token.length && token[i].isDigit()) {
            prefix = prefix * 10 + (token[i] - '0'); i++
        }
        require(i < token.length) { "Invalid move '$token'" }
        val c = token[i]; i++
        var wide = false
        val face: MoveFace = when (c) {
            'U', 'R', 'F', 'D', 'L', 'B' -> MoveFace.valueOf(c.toString())
            'u', 'r', 'f', 'd', 'l', 'b' -> { wide = true; MoveFace.valueOf(c.uppercase()) }
            'M', 'E', 'S' -> MoveFace.valueOf(c.toString())
            'x', 'X' -> MoveFace.X
            'y', 'Y' -> MoveFace.Y
            'z', 'Z' -> MoveFace.Z
            else -> throw IllegalArgumentException("Invalid move '$token'")
        }
        if (i < token.length && token[i] == 'w') {
            require(face.isFace) { "Invalid move '$token'" }
            wide = true; i++
        }
        var amount = 1
        if (i < token.length && token[i].isDigit()) {
            amount = token[i] - '0'; i++
        }
        var prime = false
        if (i < token.length && token[i] == '\'') { prime = true; i++ }
        require(i == token.length) { "Invalid move '$token'" }
        var turns = ((amount % 4) + 4) % 4
        if (prime) turns = (4 - turns) % 4
        if (turns == 0) return emptyList()
        val depth = when {
            !face.isFace -> 1
            wide -> if (prefix > 0) prefix else 2
            else -> if (prefix > 0) prefix else 1
        }
        if (face.isFace && !wide && prefix == 0) return listOf(Move(face, turns))
        return listOf(Move(face, turns, depth, wide && depth > 1))
    }

    fun format(moves: List<Move>): String = moves.joinToString(" ") { it.notation }

    fun invert(moves: List<Move>): List<Move> = moves.asReversed().map { it.inverse() }

    /** Merges consecutive turns of the same layer (R R -> R2, R R' -> nothing). */
    fun simplify(moves: List<Move>): List<Move> {
        val out = ArrayList<Move>(moves.size)
        for (m in moves) {
            val last = out.lastOrNull()
            if (last != null && last.face == m.face && last.depth == m.depth && last.wide == m.wide) {
                out.removeAt(out.lastIndex)
                val t = (last.turns + m.turns) % 4
                if (t != 0) out.add(m.copy(turns = t))
            } else {
                out.add(m)
            }
        }
        return out
    }

    /** Half-turn metric length; whole-cube rotations are not counted. */
    fun htm(moves: List<Move>): Int = moves.count { !it.isRotation }
}
