package com.sobik.model

import kotlinx.serialization.Serializable

/** One recognizable situation inside a lesson and how to handle it. */
@Serializable
data class LessonCase(
    val title: String,
    val recognition: String,
    val algorithm: String? = null,
    val explanation: String = "",
    val whenToUse: String = "",
    /** Moves from solved state that create this case for the 3D demo; defaults to inverse(algorithm). */
    val setupMoves: String? = null,
)

/** One chapter of the 3x3 beginner (layer-by-layer) guide. */
@Serializable
data class BeginnerLesson(
    val id: String,
    val order: Int,
    val title: String,
    val goal: String,
    val targetState: String,
    val recognition: String = "",
    val paragraphs: List<String> = emptyList(),
    val cases: List<LessonCase> = emptyList(),
    val tips: List<String> = emptyList(),
    /** Optional notation entries (only for the notation lesson). */
    val notation: List<NotationEntry> = emptyList(),
    /** Which part of a solved cube to highlight in the target picture (see visualization CubeMasks). */
    val targetMask: String? = null,
    /** "top" or "bottom": which side of the cube the target picture looks at. */
    val targetView: String = "top",
)

@Serializable
data class NotationEntry(
    val symbol: String,
    val meaning: String,
    /** Move(s) to draw as an arrow picture, e.g. "R'" — null when the entry has no single picture. */
    val move: String? = null,
)

@Serializable
data class GuideSection(
    val title: String,
    val paragraphs: List<String> = emptyList(),
    val algorithmIds: List<String> = emptyList(),
)

/** Learning page for a cube type without scanner/solver support (4x4 - 7x7). */
@Serializable
data class CubeGuide(
    val cubeSize: Int,
    val title: String,
    val summary: String,
    val sections: List<GuideSection>,
) {
    val cubeType: CubeType get() = CubeType.ofSize(cubeSize)
}

/** Shape family of a puzzle, used to draw its picture. */
@Serializable
enum class PuzzleShape { CUBE, PYRAMINX, MEGAMINX, SKEWB, SQUARE1, CLOCK }

/** A puzzle manufacturer in the catalog. */
@Serializable
data class PuzzleBrand(
    val id: String,
    val name: String,
    val country: String,
    val summary: String,
    /** Short mark drawn on the white center of the picture (a plain monogram, not the official logo). */
    val mark: String,
    /** Accent color of the mark, ARGB hex like "FF1565C0". */
    val color: String,
)

/** A puzzle model in the catalog. */
@Serializable
data class PuzzleProduct(
    val id: String,
    val name: String,
    val brandId: String,
    /** Category used for grouping, e.g. "3x3", "2x2", "Megaminx". */
    val category: String,
    val shape: PuzzleShape,
    /** Grid size for cube-shaped puzzles. */
    val size: Int = 3,
    val features: List<String> = emptyList(),
    val reviews: List<String> = emptyList(),
    val audience: String = "",
)
