package com.sobik.visualization

import com.sobik.engine.CubeGeometry
import com.sobik.model.Move
import kotlin.math.cos
import kotlin.math.sin

/** A projected, depth-sorted polygon ready to draw. */
class ScreenQuad(val xs: FloatArray, val ys: FloatArray, val depth: Float, val argb: Int, val shade: Float, val isSticker: Boolean)

/**
 * Software 3D model of an NxN cube: cubie bodies plus inset stickers, with an optional partial
 * layer rotation for move animation. Pure math, no GL and no allocations of bitmaps — a 3x3
 * frame is ~150 visible quads, cheap enough for a Canvas at 60 fps.
 */
class CubeScene(val n: Int) {
    private val geo = CubeGeometry.of(n)
    private val m = n - 1
    private val cubies: List<IntArray> = buildList {
        val coords = (0 until n).map { -m + 2 * it }
        for (x in coords) for (y in coords) for (z in coords) {
            if (maxOf(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z)) == m) add(intArrayOf(x, y, z))
        }
    }
    /** Stickers grouped per cubie: (sticker index, normal axis/sign). */
    private val stickersOf: Map<Long, List<Int>> = (0 until geo.stickerCount).groupBy { key(geo.position[it]) }

    private fun key(p: IntArray) = ((p[0] + 32L) * 64 + (p[1] + 32)) * 64 + (p[2] + 32)

    /**
     * @param colors ARGB per sticker (geometry order), 0 = unknown
     * @param move   move being animated (or null), [progress] 0..1
     * @param yaw/pitch view rotation in radians
     */
    fun project(
        colors: IntArray,
        move: Move?,
        progress: Float,
        yaw: Float,
        pitch: Float,
        width: Float,
        height: Float,
    ): List<ScreenQuad> {
        val g = move?.let { geo.moveGeometry(it) }
        val turnAngle = g?.let {
            val q = if (it.quarterTurns == 3) -1 else it.quarterTurns
            (q * Math.PI / 2 * progress).toFloat()
        } ?: 0f
        val scale = minOf(width, height) / (2f * n) * 0.52f
        val cx = width / 2; val cy = height / 2
        val camDist = 7f * n
        val cyw = cos(yaw); val syw = sin(yaw); val cp = cos(pitch); val sp = sin(pitch)

        val out = ArrayList<ScreenQuad>(cubies.size * 4)
        val corner = FloatArray(3)
        val xs = FloatArray(4); val ys = FloatArray(4)

        fun transform(v: FloatArray, rotate: Boolean): Float {
            if (rotate && g != null) rotateAxis(v, g.axis, turnAngle)
            // view: yaw about y, then pitch about x
            val x1 = v[0] * cyw + v[2] * syw
            val z1 = -v[0] * syw + v[2] * cyw
            val y2 = v[1] * cp - z1 * sp
            val z2 = v[1] * sp + z1 * cp
            v[0] = x1; v[1] = y2; v[2] = z2
            return z2
        }

        fun emitQuad(center: FloatArray, normal: IntArray, half: Float, offset: Float, rotate: Boolean, argb: Int, sticker: Boolean, cubieDepth: Float) {
            // two tangent axes perpendicular to the normal
            val axis = normal.indexOfFirst { it != 0 }
            val t1 = (axis + 1) % 3; val t2 = (axis + 2) % 3
            val nrm = FloatArray(3) { normal[it].toFloat() }
            transform(nrm, rotate)
            if (nrm[2] <= 0.02f) return // facing away
            for (k in 0 until 4) {
                val su = if (k == 0 || k == 3) -half else half
                val sv = if (k < 2) -half else half
                corner[0] = center[0]; corner[1] = center[1]; corner[2] = center[2]
                corner[axis] += normal[axis] * offset
                corner[t1] += su; corner[t2] += sv
                val z = transform(corner, rotate)
                val f = camDist / (camDist - z)
                xs[k] = cx + corner[0] * scale * f
                ys[k] = cy - corner[1] * scale * f
            }
            val light = 0.72f + 0.28f * nrm[2]
            out += ScreenQuad(xs.copyOf(), ys.copyOf(), cubieDepth, argb, light, sticker)
        }

        val center = FloatArray(3)
        for (c in cubies) {
            val rotate = g != null && c[g.axis] in g.layers
            // Painter's order per cubie: equal-sized cubies on a grid never interpenetrate, so
            // sorting whole cubies by center depth (body faces first, then stickers) is exact.
            val cubieDepth = transform(floatArrayOf(c[0].toFloat(), c[1].toFloat(), c[2].toFloat()), rotate)
            for (axis in 0 until 3) for (sign in intArrayOf(-1, 1)) {
                val normal = IntArray(3).also { it[axis] = sign }
                center[0] = c[0].toFloat(); center[1] = c[1].toFloat(); center[2] = c[2].toFloat()
                emitQuad(center.copyOf(), normal, 0.98f, 0.98f, rotate, BODY, false, cubieDepth)
            }
            for (s in stickersOf[key(c)].orEmpty()) {
                val p = geo.position[s]
                val normal = intArrayOf(p[3], p[4], p[5])
                center[0] = c[0].toFloat(); center[1] = c[1].toFloat(); center[2] = c[2].toFloat()
                emitQuad(center.copyOf(), normal, 0.82f, 1.0f, rotate, colors[s].takeIf { it != 0 } ?: UNKNOWN, true, cubieDepth)
            }
        }
        out.sortBy { it.depth } // stable: keeps body-before-sticker within a cubie
        return out
    }

    private fun rotateAxis(v: FloatArray, axis: Int, angle: Float) {
        val c = cos(angle); val s = sin(angle)
        val x = v[0]; val y = v[1]; val z = v[2]
        when (axis) {
            0 -> { v[1] = y * c - z * s; v[2] = y * s + z * c }
            1 -> { v[0] = x * c + z * s; v[2] = -x * s + z * c }
            else -> { v[0] = x * c - y * s; v[1] = x * s + y * c }
        }
    }

    companion object {
        const val BODY = 0xFF151515.toInt()
        const val UNKNOWN = 0xFF7A7A7A.toInt()
    }
}
