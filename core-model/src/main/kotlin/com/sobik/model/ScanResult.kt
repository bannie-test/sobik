package com.sobik.model

/** Recognition result for one sticker. */
data class StickerScan(
    val color: StickerColor,
    /** 0..1, how clearly the sample matches [color] versus the runner-up. */
    val confidence: Float,
    val secondChoice: StickerColor? = null,
)

/** Recognition result for a full cube. Stickers are indexed like [CubeState] (global index). */
data class ScanResult(
    val cubeType: CubeType,
    val stickers: List<StickerScan>,
) {
    init {
        require(stickers.size == cubeType.stickerCount)
    }

    val state: CubeState get() = CubeState.fromColors(cubeType, stickers.map { it.color })

    fun confidenceArray(): FloatArray = FloatArray(stickers.size) { stickers[it].confidence }

    fun lowConfidence(threshold: Float = LOW_CONFIDENCE): List<StickerRef> =
        stickers.indices.filter { stickers[it].confidence < threshold }
            .map { StickerRef.fromGlobal(it, cubeType.size) }

    companion object {
        const val LOW_CONFIDENCE = 0.45f
    }
}
