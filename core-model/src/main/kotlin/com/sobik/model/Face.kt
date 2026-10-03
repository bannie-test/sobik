package com.sobik.model

/**
 * The six faces, in the URFDLB order used by the Kociemba facelet convention.
 * Sticker layout of every face (row-major, index = row * n + col):
 *  - U: seen from above, B edge on top          - D: seen from below, F edge on top
 *  - F, R, B, L: seen from the front of that face with U on top
 */
enum class Face(val symbol: Char, val displayName: String) {
    U('U', "Trên (U)"),
    R('R', "Phải (R)"),
    F('F', "Trước (F)"),
    D('D', "Dưới (D)"),
    L('L', "Trái (L)"),
    B('B', "Sau (B)");

    val opposite: Face get() = entries[(ordinal + 3) % 6]

    companion object {
        fun fromSymbol(c: Char): Face =
            entries.firstOrNull { it.symbol == c.uppercaseChar() } ?: throw IllegalArgumentException("Unknown face '$c'")
    }
}

/** A single sticker position on a cube of a given size. */
data class StickerRef(val face: Face, val index: Int) {
    fun row(size: Int) = index / size
    fun col(size: Int) = index % size
    fun globalIndex(size: Int) = face.ordinal * size * size + index

    companion object {
        fun fromGlobal(globalIndex: Int, size: Int): StickerRef {
            val perFace = size * size
            return StickerRef(Face.entries[globalIndex / perFace], globalIndex % perFace)
        }
    }
}
