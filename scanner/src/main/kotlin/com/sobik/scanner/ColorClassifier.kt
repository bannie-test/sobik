package com.sobik.scanner

import com.sobik.model.CubeType
import com.sobik.model.ScanResult
import com.sobik.model.StickerColor
import com.sobik.model.StickerScan

/**
 * Turns sticker color samples into [StickerColor]s with a confidence per sticker.
 *
 * - Live preview: nearest reference color (default palette, or calibrated references).
 * - Whole cube: references are calibrated from the scan itself (3x3: the six centers;
 *   2x2: balanced k-means), then every sticker is assigned with the constraint that each color
 *   appears exactly n*n times (optimal assignment). Stickers that were close to two colors, or
 *   that the count constraint pushed away from their nearest color, get a low confidence so the
 *   UI asks the user to confirm them instead of rescanning.
 */
class ColorClassifier(private val palette: Map<StickerColor, Lab> = DEFAULT_PALETTE) {

    fun classifyPreview(samples: List<Lab>, references: Map<StickerColor, Lab> = palette): List<StickerScan> =
        samples.map { s ->
            val ranked = references.entries.sortedBy { ColorMath.distance(s, it.value) }
            val d1 = ColorMath.distance(s, ranked[0].value); val d2 = ColorMath.distance(s, ranked[1].value)
            StickerScan(ranked[0].key, confidence(d1, d2), ranked[1].key)
        }

    /** @param faces six faces in Face order (U, R, F, D, L, B), each n*n samples row-major. */
    fun classifyCube(type: CubeType, faces: List<List<Lab>>): ScanResult {
        require(faces.size == 6 && faces.all { it.size == type.stickersPerFace })
        val samples = faces.flatten()
        val perColor = type.stickersPerFace
        val colors = StickerColor.entries
        val fixed = IntArray(samples.size) { -1 }

        var refs: Array<Lab> = if (type.hasFixedCenters) {
            val centers = (0 until 6).map { f -> f * perColor + perColor / 2 }
            val labels = matchToPalette(centers.map { samples[it] })
            centers.forEachIndexed { f, idx -> fixed[idx] = labels[f] }
            Array(6) { c -> samples[centers[labels.indexOf(c)]] }
        } else {
            Array(6) { c -> palette.getValue(colors[c]) }
        }

        var assignment = IntArray(samples.size)
        repeat(if (type.hasFixedCenters) 3 else 8) {
            assignment = balancedAssign(samples, refs, perColor, fixed)
            refs = Array(6) { c ->
                val members = samples.indices.filter { assignment[it] == c }.map { samples[it] }
                val mean = ColorMath.mean(members)
                if (type.hasFixedCenters) {
                    val center = samples[fixed.indexOfFirst { it == c }]
                    Lab((center.l + mean.l) / 2, (center.a + mean.a) / 2, (center.b + mean.b) / 2)
                } else mean
            }
        }
        // Clusters of a center-less cube are only identified by their final colors.
        val relabel: IntArray = if (type.hasFixedCenters) IntArray(6) { it } else matchToPalette(refs.toList())
        val finalRefs = Array(6) { c -> refs[relabel.indexOf(c)] }

        val stickers = samples.indices.map { i ->
            val assigned = relabel[assignment[i]]
            if (fixed[i] >= 0) return@map StickerScan(colors[assigned], 1f, null)
            val dAssigned = ColorMath.distance(samples[i], finalRefs[assigned])
            val other = (0 until 6).filter { it != assigned }.minBy { ColorMath.distance(samples[i], finalRefs[it]) }
            val dOther = ColorMath.distance(samples[i], finalRefs[other])
            StickerScan(colors[assigned], confidence(dAssigned, dOther), colors[other])
        }
        return ScanResult(type, stickers)
    }

    /** Assignment of each sample to a color index with exactly [perColor] samples per color. */
    private fun balancedAssign(samples: List<Lab>, refs: Array<Lab>, perColor: Int, fixed: IntArray): IntArray {
        val n = samples.size
        val cost = Array(n) { i ->
            DoubleArray(n) { slot ->
                val c = slot / perColor
                when {
                    fixed[i] >= 0 -> if (fixed[i] == c) 0.0 else 1e9
                    else -> ColorMath.distance(samples[i], refs[c]).toDouble().let { it * it }
                }
            }
        }
        val rowToCol = Hungarian.solve(cost)
        return IntArray(n) { rowToCol[it] / perColor }
    }

    /** Best one-to-one mapping of 6 samples/centroids to palette colors; result[i] = color ordinal. */
    private fun matchToPalette(labs: List<Lab>): IntArray {
        val colors = StickerColor.entries
        val cost = Array(6) { i -> DoubleArray(6) { c -> ColorMath.distance(labs[i], palette.getValue(colors[c])).toDouble() } }
        return Hungarian.solve(cost)
    }

    companion object {
        /** Typical sticker colors under neutral indoor light, in L*a*b*. */
        val DEFAULT_PALETTE: Map<StickerColor, Lab> = mapOf(
            StickerColor.WHITE to Lab(88f, 0f, 2f),
            StickerColor.YELLOW to Lab(84f, -6f, 78f),
            StickerColor.GREEN to Lab(55f, -52f, 34f),
            StickerColor.BLUE to Lab(36f, 12f, -52f),
            StickerColor.RED to Lab(43f, 60f, 36f),
            StickerColor.ORANGE to Lab(63f, 42f, 66f),
        )

        /**
         * 0..1 from the distance to the chosen color and to the runner-up. Equal distances give
         * ~0.3; a chosen color that is not even the nearest gives < 0.3.
         */
        fun confidence(dChosen: Float, dOther: Float): Float {
            val margin = (dOther - dChosen) / (dOther + dChosen + 1e-3f)
            return if (margin <= 0f) (0.3f * (1f + margin)).coerceIn(0f, 0.3f)
            else (0.3f + 0.7f * minOf(1f, margin * 2.5f)).coerceIn(0f, 1f)
        }
    }
}
