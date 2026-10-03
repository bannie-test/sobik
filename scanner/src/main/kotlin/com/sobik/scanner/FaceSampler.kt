package com.sobik.scanner

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Square scan area in the *upright* (display-oriented) image, in normalized units:
 * center in 0..1 of upright width/height, size as a fraction of the shorter upright side.
 */
data class GuideRegion(val centerX: Float = 0.5f, val centerY: Float = 0.5f, val size: Float = 0.6f) {
    companion object {
        /**
         * Maps a centered square guide drawn on a preview that scales the camera image with
         * "fill center" (crop to fill) to normalized upright-image coordinates.
         */
        fun fromPreview(viewWidth: Float, viewHeight: Float, uprightImageWidth: Int, uprightImageHeight: Int, guideSizePx: Float): GuideRegion {
            val scale = max(viewWidth / uprightImageWidth, viewHeight / uprightImageHeight)
            val sizeInImage = guideSizePx / scale
            return GuideRegion(0.5f, 0.5f, sizeInImage / min(uprightImageWidth, uprightImageHeight))
        }
    }
}

/** Robust color of one sticker cell plus how uniform it looked. */
data class CellSample(val lab: Lab, val spread: Float)

data class FrameQuality(
    val meanLightness: Float,
    /** Average within-cell color spread; high when the cube is misaligned, blurry or absent. */
    val meanSpread: Float,
    /** Mean luma gradient across the grid; low values mean blur. */
    val sharpness: Float,
    /** Fraction of cells with specular highlights (very bright, colorless pixels). */
    val glare: Float,
) {
    val isTooDark get() = meanLightness < 18f
    val isBlurry get() = sharpness < 4f
    val isMisaligned get() = meanSpread > 14f
    val hasGlare get() = glare > 0.34f
    val isGood get() = !isTooDark && !isMisaligned && !hasGlare

    /** User-facing hint (Vietnamese) or null when the frame looks fine. */
    val hint: String?
        get() = when {
            isTooDark -> "Thiếu sáng — hãy di chuyển tới chỗ sáng hơn."
            hasGlare -> "Có phản chiếu/lóa sáng — nghiêng cube hoặc tránh nguồn sáng trực tiếp."
            isMisaligned -> "Đưa mặt cube vừa khít khung lưới và giữ yên."
            isBlurry -> "Ảnh bị mờ — giữ máy và cube yên hơn."
            else -> null
        }
}

data class FaceSamples(val n: Int, val cells: List<CellSample>, val quality: FrameQuality)

/**
 * Samples an n x n grid inside the guide region. For each cell only the central ~40% is read
 * (avoiding black borders and neighboring stickers) on a sparse sub-grid; the per-channel median
 * rejects glare spots. Cells are returned row-major as the user sees the face on screen.
 */
class FaceSampler(private val pointsPerSide: Int = 7, private val cellCoverage: Float = 0.4f) {

    fun sample(source: PixelSource, rotationDegrees: Int, region: GuideRegion, n: Int): FaceSamples {
        val rot = ((rotationDegrees % 360) + 360) % 360
        val uprightW = if (rot == 90 || rot == 270) source.height else source.width
        val uprightH = if (rot == 90 || rot == 270) source.width else source.height
        val side = region.size * min(uprightW, uprightH)
        val left = region.centerX * uprightW - side / 2
        val top = region.centerY * uprightH - side / 2
        val cell = side / n

        fun toImage(ux: Float, uy: Float): Pair<Int, Int> {
            val x = ux.toInt().coerceIn(0, uprightW - 1)
            val y = uy.toInt().coerceIn(0, uprightH - 1)
            return when (rot) {
                90 -> y to (source.height - 1 - x)
                180 -> (source.width - 1 - x) to (source.height - 1 - y)
                270 -> (source.width - 1 - y) to x
                else -> x to y
            }
        }

        val cells = ArrayList<CellSample>(n * n)
        var glareCells = 0
        val ls = FloatArray(pointsPerSide * pointsPerSide)
        val As = FloatArray(ls.size); val bs = FloatArray(ls.size)
        for (r in 0 until n) for (c in 0 until n) {
            val cx = left + (c + 0.5f) * cell
            val cy = top + (r + 0.5f) * cell
            val half = cell * cellCoverage / 2
            var k = 0
            var specular = 0
            for (i in 0 until pointsPerSide) for (j in 0 until pointsPerSide) {
                val ux = cx - half + 2 * half * (j + 0.5f) / pointsPerSide
                val uy = cy - half + 2 * half * (i + 0.5f) / pointsPerSide
                val (x, y) = toImage(ux, uy)
                val lab = ColorMath.argbToLab(source.argb(x, y))
                if (lab.l > 95f && lab.chroma < 8f) specular++
                ls[k] = lab.l; As[k] = lab.a; bs[k] = lab.b; k++
            }
            val median = Lab(median(ls), median(As), median(bs))
            var spread = 0f
            for (i in 0 until k) spread += abs(As[i] - median.a) + abs(bs[i] - median.b) + abs(ls[i] - median.l) * 0.5f
            spread /= k
            if (specular > k / 4 && median.chroma > 15f) glareCells++
            cells += CellSample(median, spread)
        }
        val sharp = sharpness(source, ::toImage, left, top, side)
        val quality = FrameQuality(
            meanLightness = cells.map { it.lab.l }.average().toFloat(),
            meanSpread = cells.map { it.spread }.average().toFloat(),
            sharpness = sharp,
            glare = glareCells.toFloat() / cells.size,
        )
        return FaceSamples(n, cells, quality)
    }

    /** Mean absolute luma difference between neighboring samples along the two diagonals. */
    private fun sharpness(source: PixelSource, toImage: (Float, Float) -> Pair<Int, Int>, left: Float, top: Float, side: Float): Float {
        val steps = 96
        var sum = 0f
        var prevA = -1; var prevB = -1
        for (i in 0 until steps) {
            val t = (i + 0.5f) / steps
            val (ax, ay) = toImage(left + t * side, top + t * side)
            val (bx, by) = toImage(left + t * side, top + (1 - t) * side)
            val la = source.luma(ax, ay); val lb = source.luma(bx, by)
            if (prevA >= 0) sum += abs(la - prevA) + abs(lb - prevB)
            prevA = la; prevB = lb
        }
        return sum / (2 * (steps - 1))
    }

    private fun median(values: FloatArray): Float {
        val copy = values.copyOf(); copy.sort()
        return copy[copy.size / 2]
    }

    companion object {
        /** Mean color difference between two samplings of the same grid (used for stability). */
        fun difference(a: FaceSamples, b: FaceSamples): Float {
            if (a.cells.size != b.cells.size) return Float.MAX_VALUE
            var s = 0f
            for (i in a.cells.indices) s += ColorMath.distance(a.cells[i].lab, b.cells[i].lab, 1f)
            return s / a.cells.size
        }
    }
}
