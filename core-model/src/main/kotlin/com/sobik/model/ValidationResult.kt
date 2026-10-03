package com.sobik.model

enum class ValidationErrorCode {
    INVALID_STICKER_COUNT,
    INVALID_COLOR_COUNT,
    INVALID_CENTER_MAPPING,
    INVALID_COLOR_SCHEME,
    INVALID_FACE_ORIENTATION,
    INVALID_PIECE,
    DUPLICATE_PIECE,
    INVALID_CORNER_ORIENTATION,
    INVALID_EDGE_ORIENTATION,
    INVALID_PERMUTATION_PARITY,
    INVALID_CUBE_STATE,
    UNSOLVABLE,
}

data class ValidationError(
    val code: ValidationErrorCode,
    /** User-facing explanation (Vietnamese). */
    val message: String,
    /** Stickers involved in the problem, to highlight in the correction UI. */
    val stickers: List<StickerRef> = emptyList(),
)

data class StickerFix(val sticker: StickerRef, val from: StickerColor, val to: StickerColor)

/** A concrete edit that would make the cube valid, e.g. one mis-read sticker or a rotated face scan. */
data class FixSuggestion(
    val message: String,
    val fixes: List<StickerFix>,
)

data class ValidationResult(
    val errors: List<ValidationError>,
    val suggestions: List<FixSuggestion> = emptyList(),
) {
    val isValid: Boolean get() = errors.isEmpty()
    val suspectStickers: Set<StickerRef> get() = errors.flatMap { it.stickers }.toSet()
    val codes: Set<ValidationErrorCode> get() = errors.map { it.code }.toSet()

    companion object {
        val VALID = ValidationResult(emptyList())
    }
}
