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
)

@Serializable
data class NotationEntry(val symbol: String, val meaning: String)

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
