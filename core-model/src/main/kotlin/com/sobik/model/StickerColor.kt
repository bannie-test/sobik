package com.sobik.model

/** Physical sticker colors. ARGB values are display colors, not recognition references. */
enum class StickerColor(val symbol: Char, val argb: Long, val displayName: String) {
    WHITE('W', 0xFFF5F5F5, "trắng"),
    YELLOW('Y', 0xFFFFD500, "vàng"),
    GREEN('G', 0xFF009B48, "xanh lá"),
    BLUE('B', 0xFF0046AD, "xanh dương"),
    RED('R', 0xFFC41E3A, "đỏ"),
    ORANGE('O', 0xFFFF5800, "cam");

    companion object {
        fun fromSymbol(c: Char): StickerColor =
            entries.firstOrNull { it.symbol == c.uppercaseChar() }
                ?: throw IllegalArgumentException("Unknown color symbol '$c'")
    }
}

/** Which color sits on which face of a solved cube. */
data class ColorScheme(val faceColors: Map<Face, StickerColor>) {
    init {
        require(faceColors.size == 6 && faceColors.values.toSet().size == 6) { "Color scheme must map 6 faces to 6 distinct colors" }
    }

    fun colorOf(face: Face): StickerColor = faceColors.getValue(face)
    fun faceOf(color: StickerColor): Face = faceColors.entries.first { it.value == color }.key

    companion object {
        /** Western scheme: white top, green front, red right. */
        val STANDARD = ColorScheme(
            mapOf(
                Face.U to StickerColor.WHITE,
                Face.R to StickerColor.RED,
                Face.F to StickerColor.GREEN,
                Face.D to StickerColor.YELLOW,
                Face.L to StickerColor.ORANGE,
                Face.B to StickerColor.BLUE,
            ),
        )

        /**
         * The standard scheme turned upside down (z2): yellow top, white bottom, green front.
         * This is how the cube is held for the beginner method and CFOP (white cross on the
         * bottom), so learning demos use it.
         */
        val YELLOW_TOP = ColorScheme(
            mapOf(
                Face.U to StickerColor.YELLOW,
                Face.R to StickerColor.ORANGE,
                Face.F to StickerColor.GREEN,
                Face.D to StickerColor.WHITE,
                Face.L to StickerColor.RED,
                Face.B to StickerColor.BLUE,
            ),
        )
    }
}
