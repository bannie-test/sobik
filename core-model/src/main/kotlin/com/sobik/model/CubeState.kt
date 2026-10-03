package com.sobik.model

/**
 * Immutable sticker-level cube state: the single source of truth for a scanned cube.
 *
 * Stickers are stored face by face in [Face] order (U, R, F, D, L, B), each face row-major
 * with the orientation documented on [Face]. Storage is one byte per sticker, so a 3x3 state
 * is 54 bytes and serializes to a 58-character string ("3:" + 54 color symbols).
 */
class CubeState private constructor(val type: CubeType, private val stickers: ByteArray) {

    val size: Int get() = type.size

    operator fun get(face: Face, index: Int): StickerColor = COLORS[stickers[face.ordinal * type.stickersPerFace + index].toInt()]
    operator fun get(ref: StickerRef): StickerColor = get(ref.face, ref.index)
    fun colorAt(globalIndex: Int): StickerColor = COLORS[stickers[globalIndex].toInt()]

    /** Center color for odd cubes, null for even cubes (which have no fixed centers). */
    fun center(face: Face): StickerColor? =
        if (type.hasFixedCenters) get(face, type.stickersPerFace / 2) else null

    fun with(ref: StickerRef, color: StickerColor): CubeState {
        val copy = stickers.copyOf()
        copy[ref.globalIndex(size)] = color.ordinal.toByte()
        return CubeState(type, copy)
    }

    fun withFace(face: Face, colors: List<StickerColor>): CubeState {
        require(colors.size == type.stickersPerFace)
        val copy = stickers.copyOf()
        val base = face.ordinal * type.stickersPerFace
        colors.forEachIndexed { i, c -> copy[base + i] = c.ordinal.toByte() }
        return CubeState(type, copy)
    }

    fun faceColors(face: Face): List<StickerColor> =
        List(type.stickersPerFace) { get(face, it) }

    /** Raw copy of the sticker bytes (color ordinals). */
    fun toByteArray(): ByteArray = stickers.copyOf()

    fun countColors(): IntArray {
        val counts = IntArray(StickerColor.entries.size)
        for (b in stickers) counts[b.toInt()]++
        return counts
    }

    fun isSolved(): Boolean {
        val n = type.stickersPerFace
        for (f in 0 until 6) {
            val first = stickers[f * n]
            for (i in 1 until n) if (stickers[f * n + i] != first) return false
        }
        return true
    }

    fun toColorString(): String = buildString(stickers.size) { stickers.forEach { append(COLORS[it.toInt()].symbol) } }

    /** Compact text form, e.g. "3:WWWWWWWWWRRR...". */
    fun serialize(): String = "${type.size}:${toColorString()}"

    override fun equals(other: Any?): Boolean =
        other is CubeState && other.type == type && other.stickers.contentEquals(stickers)

    override fun hashCode(): Int = 31 * type.hashCode() + stickers.contentHashCode()
    override fun toString(): String = serialize()

    companion object {
        private val COLORS = StickerColor.entries.toTypedArray()

        fun solved(type: CubeType, scheme: ColorScheme = ColorScheme.STANDARD): CubeState {
            val n = type.stickersPerFace
            val data = ByteArray(type.stickerCount) { scheme.colorOf(Face.entries[it / n]).ordinal.toByte() }
            return CubeState(type, data)
        }

        fun fromColors(type: CubeType, colors: List<StickerColor>): CubeState {
            require(colors.size == type.stickerCount) { "Expected ${type.stickerCount} stickers, got ${colors.size}" }
            return CubeState(type, ByteArray(colors.size) { colors[it].ordinal.toByte() })
        }

        fun fromBytes(type: CubeType, bytes: ByteArray): CubeState {
            require(bytes.size == type.stickerCount) { "Expected ${type.stickerCount} stickers, got ${bytes.size}" }
            require(bytes.all { it in 0 until COLORS.size }) { "Invalid color ordinal" }
            return CubeState(type, bytes.copyOf())
        }

        fun fromColorString(type: CubeType, text: String): CubeState {
            require(text.length == type.stickerCount) { "Expected ${type.stickerCount} stickers, got ${text.length}" }
            return CubeState(type, ByteArray(text.length) { StickerColor.fromSymbol(text[it]).ordinal.toByte() })
        }

        fun deserialize(text: String): CubeState {
            val sep = text.indexOf(':')
            require(sep > 0) { "Malformed cube state '$text'" }
            val type = CubeType.ofSize(text.substring(0, sep).toInt())
            return fromColorString(type, text.substring(sep + 1))
        }
    }
}
