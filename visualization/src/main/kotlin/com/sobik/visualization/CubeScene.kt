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

    /** Screen position of a 3D point (cube coordinates, faces at +-n) for a static view. */
    fun projectPoint(p: FloatArray, yaw: Float, pitch: Float, width: Float, height: Float): FloatArray {
        val scale = minOf(width, height) / (2f * n) * 0.52f
        val camDist = 7f * n
        val cyw = cos(yaw); val syw = sin(yaw); val cp = cos(pitch); val sp = sin(pitch)
        val x1 = p[0] * cyw + p[2] * syw
        val z1 = -p[0] * syw + p[2] * cyw
        val y2 = p[1] * cp - z1 * sp
        val z2 = p[1] * sp + z1 * cp
        val f = camDist / (camDist - z2)
        return floatArrayOf(width / 2 + x1 * scale * f, height / 2 - y2 * scale * f, z2)
    }

    /**
     * Arrow showing which way the layers of [move] turn, drawn along the turning band on the
     * visible side face that is most turned towards the viewer. Returns screen points
     * (start x, start y, end x, end y); the arrow head belongs at the end.
     */
    fun moveArrow(move: Move, yaw: Float, pitch: Float, width: Float, height: Float): FloatArray {
        val g = geo.moveGeometry(move)
        val a = g.axis
        val c = g.layers.average().toFloat()
        val sign = if (g.quarterTurns == 3) -1f else 1f
        // Faces whose normal is perpendicular to the turning axis show the band; take the most visible one.
        var best: Pair<Int, Int>? = null
        var bestZ = -Float.MAX_VALUE
        for (b in 0 until 3) {
            if (b == a) continue
            for (sb in intArrayOf(-1, 1)) {
                val nrm = FloatArray(3).also { it[b] = sb * (n + 1f) }
                val z = projectPoint(nrm, yaw, pitch, width, height)[2]
                if (z > bestZ) { bestZ = z; best = b to sb }
            }
        }
        val (b, sb) = best!!
        val t = 3 - a - b
        fun point(u: Float) = FloatArray(3).also { it[a] = c; it[b] = sb * (n + 0.25f); it[t] = u }
        // Direction of motion: velocity of a point on the band under +90 degrees about the axis is e_a x p.
        val mid = point(0f)
        val velocity = FloatArray(3)
        when (a) {
            0 -> { velocity[1] = -mid[2]; velocity[2] = mid[1] }
            1 -> { velocity[0] = mid[2]; velocity[2] = -mid[0] }
            else -> { velocity[0] = -mid[1]; velocity[1] = mid[0] }
        }
        val forward = velocity[t] * sign > 0
        val reach = n - 0.35f
        val start = projectPoint(point(if (forward) -reach else reach), yaw, pitch, width, height)
        val end = projectPoint(point(if (forward) reach else -reach), yaw, pitch, width, height)
        return floatArrayOf(start[0], start[1], end[0], end[1])
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
