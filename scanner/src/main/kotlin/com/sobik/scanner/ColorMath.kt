package com.sobik.scanner

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/** A color sample in CIE L*a*b* (D65). */
data class Lab(val l: Float, val a: Float, val b: Float) {
    val chroma: Float get() = sqrt(a * a + b * b)
}

object ColorMath {
    private val LINEAR = FloatArray(256) { i ->
        val c = i / 255.0
        (if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)).toFloat()
    }

    fun rgbToLab(r: Int, g: Int, b: Int): Lab {
        val rl = LINEAR[r.coerceIn(0, 255)]; val gl = LINEAR[g.coerceIn(0, 255)]; val bl = LINEAR[b.coerceIn(0, 255)]
        val x = (0.4124f * rl + 0.3576f * gl + 0.1805f * bl) / 0.95047f
        val y = (0.2126f * rl + 0.7152f * gl + 0.0722f * bl)
        val z = (0.0193f * rl + 0.1192f * gl + 0.9505f * bl) / 1.08883f
        val fx = f(x); val fy = f(y); val fz = f(z)
        return Lab(116f * fy - 16f, 500f * (fx - fy), 200f * (fy - fz))
    }

    fun argbToLab(argb: Int): Lab = rgbToLab((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    private fun f(t: Float): Float = if (t > 0.008856f) cbrt(t.toDouble()).toFloat() else 7.787f * t + 16f / 116f

    /**
     * Perceptual distance used for sticker matching. Lightness is down-weighted because uneven
     * lighting changes L much more than hue between stickers of the same color.
     */
    fun distance(p: Lab, q: Lab, lightnessWeight: Float = 0.5f): Float {
        val dl = (p.l - q.l) * lightnessWeight
        val da = p.a - q.a
        val db = p.b - q.b
        return sqrt(dl * dl + da * da + db * db)
    }

    fun mean(samples: List<Lab>): Lab =
        Lab(samples.map { it.l }.average().toFloat(), samples.map { it.a }.average().toFloat(), samples.map { it.b }.average().toFloat())
}
