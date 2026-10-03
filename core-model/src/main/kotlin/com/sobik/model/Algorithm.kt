package com.sobik.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlgorithmCategory(val displayName: String) {
    @SerialName("NOTATION") NOTATION("Ký hiệu"),
    @SerialName("BEGINNER") BEGINNER("Người mới"),
    @SerialName("F2L") F2L("F2L"),
    @SerialName("OLL") OLL("OLL"),
    @SerialName("PLL") PLL("PLL"),
    @SerialName("PARITY") PARITY("Parity"),
    @SerialName("BIG_CUBE") BIG_CUBE("Cube lớn"),
}

@Serializable
enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * A named move sequence from the algorithm library (structured, version-controlled data).
 *
 * @param setupMoves moves that create the case from a solved cube for animation; when null the
 *                   inverse of [notation] is used.
 */
@Serializable
data class Algorithm(
    val id: String,
    val name: String,
    val cubeSize: Int = 3,
    val category: AlgorithmCategory,
    val group: String? = null,
    val recognition: String = "",
    val notation: String,
    val alternatives: List<String> = emptyList(),
    val setupMoves: String? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val explanation: String = "",
    val tags: List<String> = emptyList(),
) {
    val cubeType: CubeType get() = CubeType.ofSize(cubeSize)
    val moves: List<Move> get() = Notation.parse(notation)
    val setup: List<Move> get() = setupMoves?.let { Notation.parse(it) } ?: Notation.invert(moves)
}

/**
 * An algorithm matched against a concrete cube: what to do before (e.g. AUF "U'" or a "y"
 * rotation), the algorithm, and what to do after (final AUF for PLL).
 */
data class AlgorithmCase(
    val algorithm: Algorithm,
    val preMoves: List<Move> = emptyList(),
    val postMoves: List<Move> = emptyList(),
    val note: String? = null,
) {
    val allMoves: List<Move> get() = preMoves + algorithm.moves + postMoves
}
