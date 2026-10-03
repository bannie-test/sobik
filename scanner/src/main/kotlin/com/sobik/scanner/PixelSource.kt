package com.sobik.scanner

import java.nio.ByteBuffer

/** Minimal read-only image abstraction so the scanner core doesn't depend on Android types. */
interface PixelSource {
    val width: Int
    val height: Int
    fun argb(x: Int, y: Int): Int
    /** Luma 0..255; override when cheaper than full RGB. */
    fun luma(x: Int, y: Int): Int {
        val c = argb(x, y)
        return (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
    }
}

class IntArraySource(override val width: Int, override val height: Int, private val pixels: IntArray) : PixelSource {
    override fun argb(x: Int, y: Int): Int = pixels[y * width + x]
}

/**
 * YUV_420_888 planes (as delivered by CameraX ImageAnalysis). Only the sampled pixels are
 * converted, so a frame costs a few hundred conversions instead of a full-frame RGB copy.
 */
class Yuv420Source(
    override val width: Int,
    override val height: Int,
    private val y: ByteBuffer,
    private val yRowStride: Int,
    private val u: ByteBuffer,
    private val v: ByteBuffer,
    private val uvRowStride: Int,
    private val uvPixelStride: Int,
) : PixelSource {
    override fun luma(x: Int, y: Int): Int = this.y.get(y * yRowStride + x).toInt() and 0xFF

    override fun argb(x: Int, y: Int): Int {
        val yy = luma(x, y).toFloat()
        val uvIndex = (y / 2) * uvRowStride + (x / 2) * uvPixelStride
        val uu = (u.get(uvIndex).toInt() and 0xFF) - 128f
        val vv = (v.get(uvIndex).toInt() and 0xFF) - 128f
        val r = (yy + 1.402f * vv).toInt().coerceIn(0, 255)
        val g = (yy - 0.344136f * uu - 0.714136f * vv).toInt().coerceIn(0, 255)
        val b = (yy + 1.772f * uu).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
