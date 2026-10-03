package com.sobik.engine

import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.FixSuggestion
import com.sobik.model.StickerColor
import com.sobik.model.StickerFix
import com.sobik.model.StickerRef
import com.sobik.model.ValidationError
import com.sobik.model.ValidationErrorCode
import com.sobik.model.ValidationResult

/**
 * Validates scanned cube states and proposes minimal fixes. Independent of the scanner:
 * it only sees colors (and optionally per-sticker confidence to rank suggestions).
 */
object CubeValidator {

    fun validate(state: CubeState, confidence: FloatArray? = null): ValidationResult {
        val errors = when (state.type) {
            CubeType.CUBE_3X3 -> check3x3(state.toByteArray(), detailed = true)
            CubeType.CUBE_2X2 -> check2x2(state.toByteArray(), detailed = true)
            else -> listOf(ValidationError(ValidationErrorCode.INVALID_CUBE_STATE, "Chưa hỗ trợ kiểm tra cube ${state.type.label}."))
        }
        if (errors.isEmpty()) return ValidationResult.VALID
        return ValidationResult(withFaceOrientation(state, errors), suggestFixes(state, confidence))
    }

    fun isValid(state: CubeState): Boolean = isValid(state.type, state.toByteArray())

    fun isValid(type: CubeType, colors: ByteArray): Boolean = when (type) {
        CubeType.CUBE_3X3 -> check3x3(colors, detailed = false).isEmpty()
        CubeType.CUBE_2X2 -> check2x2(colors, detailed = false).isEmpty()
        else -> false
    }

    // ---------------------------------------------------------------- 3x3

    private fun check3x3(colors: ByteArray, detailed: Boolean): List<ValidationError> {
        val errors = ArrayList<ValidationError>()
        if (colors.size != 54) return listOf(ValidationError(ValidationErrorCode.INVALID_STICKER_COUNT, "Cube 3x3 cần 54 ô màu."))
        countError(colors, 9, detailed)?.let { errors += it; if (!detailed) return errors }

        val map = CubeLabeling.colorToFace3x3(colors)
        if (map == null) {
            if (!detailed) return listOf(GENERIC)
            val centers = (0 until 6).map { StickerRef(Face.entries[it], 4) }
            val dup = centers.groupBy { colors[it.face.ordinal * 9 + 4] }.filter { it.value.size > 1 }
            errors += ValidationError(
                ValidationErrorCode.INVALID_CENTER_MAPPING,
                "Các ô trung tâm phải có 6 màu khác nhau. " + dup.entries.joinToString("; ") { (c, refs) ->
                    "Màu ${COLORS[c.toInt()].displayName} xuất hiện ở tâm các mặt ${refs.joinToString(", ") { it.face.symbol.toString() }}"
                } + ". Có thể bạn đã quét trùng một mặt.",
                dup.values.flatten(),
            )
            return errors
        }
        if (errors.isNotEmpty()) return errors // counts wrong: piece analysis would only add noise

        val labels = CubeLabeling.labels(colors, map)
        val faceColor = IntArray(6).also { fc -> for (c in 0 until 6) fc[map[c]] = c }
        val cp = IntArray(8) { -1 }; val co = IntArray(8)
        val ep = IntArray(12) { -1 }; val eo = IntArray(12)

        for (i in 0 until 8) {
            val fl = CubieCube.CORNER_FACELET[i]
            val piece = identifyCorner(labels, fl)
            if (piece == null) {
                if (!detailed) return listOf(GENERIC)
                errors += ValidationError(
                    ValidationErrorCode.INVALID_PIECE,
                    "Góc ở vị trí ${CubieCube.CORNER_NAMES[i]} có tổ hợp màu không tồn tại (${fl.joinToString("-") { COLORS[colors[it].toInt()].displayName }}).",
                    fl.map { refOf(it, 3) },
                )
            } else { cp[i] = piece.first; co[i] = piece.second }
        }
        for (i in 0 until 12) {
            val fl = CubieCube.EDGE_FACELET[i]
            val piece = identifyEdge(labels, fl)
            if (piece == null) {
                if (!detailed) return listOf(GENERIC)
                errors += ValidationError(
                    ValidationErrorCode.INVALID_PIECE,
                    "Cạnh ở vị trí ${CubieCube.EDGE_NAMES[i]} có tổ hợp màu không tồn tại (${fl.joinToString("-") { COLORS[colors[it].toInt()].displayName }}).",
                    fl.map { refOf(it, 3) },
                )
            } else { ep[i] = piece.first; eo[i] = piece.second }
        }
        duplicateErrors(cp, CubieCube.CORNER_FACELET, faceColor, isCorner = true, detailed)?.let { errors += it; if (!detailed) return errors }
        duplicateErrors(ep, CubieCube.EDGE_FACELET, faceColor, isCorner = false, detailed)?.let { errors += it; if (!detailed) return errors }
        if (errors.isNotEmpty()) return errors.withSummary()

        if (co.sum() % 3 != 0) {
            if (!detailed) return listOf(GENERIC)
            errors += ValidationError(
                ValidationErrorCode.INVALID_CORNER_ORIENTATION,
                "Có một góc bị xoay sai hướng (tổng hướng các góc không hợp lệ). Thường do đọc nhầm màu ở một ô góc, hoặc một góc đã bị vặn/lắp lại.",
                CubieCube.CORNER_FACELET.flatMap { f -> f.map { refOf(it, 3) } },
            )
        }
        if (eo.sum() % 2 != 0) {
            if (!detailed) return listOf(GENERIC)
            errors += ValidationError(
                ValidationErrorCode.INVALID_EDGE_ORIENTATION,
                "Có một cạnh bị lật ngược (tổng hướng các cạnh không hợp lệ). Thường do hai ô của một cạnh bị đọc đảo màu.",
                CubieCube.EDGE_FACELET.flatMap { f -> f.map { refOf(it, 3) } },
            )
        }
        if (CubieCube.permutationParity(cp) != CubieCube.permutationParity(ep)) {
            if (!detailed) return listOf(GENERIC)
            errors += ValidationError(
                ValidationErrorCode.INVALID_PERMUTATION_PARITY,
                "Có hai mảnh bị hoán đổi vị trí (lỗi parity). Trạng thái này không thể đạt được bằng cách xoay — kiểm tra lại các ô đã quét.",
            )
        }
        return errors.withSummary()
    }

    private fun identifyCorner(labels: ByteArray, fl: IntArray): Pair<Int, Int>? {
        val f = IntArray(3) { labels[fl[it]].toInt() }
        val ori = (0 until 3).singleOrNull { f[it] == Face.U.ordinal || f[it] == Face.D.ordinal } ?: return null
        val j = (0 until 8).firstOrNull { c ->
            CubieCube.CORNER_COLOR[c][0] == f[ori] && CubieCube.CORNER_COLOR[c][1] == f[(ori + 1) % 3] &&
                CubieCube.CORNER_COLOR[c][2] == f[(ori + 2) % 3]
        } ?: return null
        return j to ori
    }

    private fun identifyEdge(labels: ByteArray, fl: IntArray): Pair<Int, Int>? {
        val a = labels[fl[0]].toInt(); val b = labels[fl[1]].toInt()
        for (j in 0 until 12) {
            if (CubieCube.EDGE_COLOR[j][0] == a && CubieCube.EDGE_COLOR[j][1] == b) return j to 0
            if (CubieCube.EDGE_COLOR[j][0] == b && CubieCube.EDGE_COLOR[j][1] == a) return j to 1
        }
        return null
    }

    // ---------------------------------------------------------------- 2x2

    private fun check2x2(colors: ByteArray, detailed: Boolean): List<ValidationError> {
        val errors = ArrayList<ValidationError>()
        if (colors.size != 24) return listOf(ValidationError(ValidationErrorCode.INVALID_STICKER_COUNT, "Cube 2x2 cần 24 ô màu."))
        countError(colors, 4, detailed)?.let { return listOf(it) }
        val facelets = CubieCube.cornerFacelets(2)

        for ((i, fl) in facelets.withIndex()) {
            val cs = fl.map { colors[it] }
            if (cs.toSet().size != 3) {
                if (!detailed) return listOf(GENERIC)
                errors += ValidationError(
                    ValidationErrorCode.INVALID_PIECE,
                    "Góc ở vị trí ${CubieCube.CORNER_NAMES[i]} có hai ô cùng màu (${cs.joinToString("-") { COLORS[it.toInt()].displayName }}).",
                    fl.map { refOf(it, 2) },
                )
            }
        }
        if (errors.isNotEmpty()) return errors.withSummary()

        val map = CubeLabeling.colorToFace2x2(colors)
        if (map == null) {
            if (!detailed) return listOf(GENERIC)
            return listOf(
                ValidationError(
                    ValidationErrorCode.INVALID_COLOR_SCHEME,
                    "Không xác định được các cặp màu đối diện (mỗi màu phải có đúng một màu không bao giờ nằm chung góc với nó). Có thể một vài ô bị đọc nhầm màu.",
                ),
            ).withSummary()
        }
        val labels = CubeLabeling.labels(colors, map)
        val faceColor = IntArray(6).also { fc -> for (c in 0 until 6) fc[map[c]] = c }
        val cp = IntArray(8) { -1 }; val co = IntArray(8)
        val bad = ArrayList<Int>()
        for (i in 0 until 8) {
            val p = identifyCorner(labels, facelets[i])
            if (p == null) bad += i else { cp[i] = p.first; co[i] = p.second }
        }
        // If most corners look mirrored, the reference corner (DBL) is the odd one out.
        val reported = if (bad.size > 4) listOf(CubieCube.DBL) else bad
        for (i in reported) {
            if (!detailed) return listOf(GENERIC)
            errors += ValidationError(
                ValidationErrorCode.INVALID_PIECE,
                "Góc ở vị trí ${CubieCube.CORNER_NAMES[i]} có thứ tự màu không tồn tại trên cube thật (${facelets[i].joinToString("-") { COLORS[colors[it].toInt()].displayName }}).",
                facelets[i].map { refOf(it, 2) },
            )
        }
        if (errors.isNotEmpty()) return errors.withSummary()
        duplicateErrors(cp, facelets, faceColor, isCorner = true, detailed, n = 2)?.let { return listOf(it).withSummary() }
        if (co.sum() % 3 != 0) {
            if (!detailed) return listOf(GENERIC)
            errors += ValidationError(
                ValidationErrorCode.INVALID_CORNER_ORIENTATION,
                "Có một góc bị xoay sai hướng (tổng hướng các góc không hợp lệ). Thường do đọc nhầm màu ở một ô.",
                facelets.flatMap { f -> f.map { refOf(it, 2) } },
            )
        }
        return errors.withSummary()
    }

    // ---------------------------------------------------------------- shared helpers

    private val COLORS = StickerColor.entries
    private val GENERIC = ValidationError(ValidationErrorCode.INVALID_CUBE_STATE, "Trạng thái Rubik không hợp lệ.")

    private fun refOf(global: Int, n: Int) = StickerRef.fromGlobal(global, n)

    private fun List<ValidationError>.withSummary(): List<ValidationError> =
        if (isEmpty()) this
        else listOf(
            ValidationError(
                ValidationErrorCode.INVALID_CUBE_STATE,
                "Trạng thái Rubik không hợp lệ: không thể đạt được bằng cách xoay cube. Hãy kiểm tra các ô được đánh dấu.",
            ),
        ) + this

    private fun countError(colors: ByteArray, expected: Int, detailed: Boolean): ValidationError? {
        val counts = IntArray(6)
        for (b in colors) counts[b.toInt()]++
        if (counts.all { it == expected }) return null
        if (!detailed) return GENERIC
        val parts = COLORS.filter { counts[it.ordinal] != expected }.joinToString(", ") {
            val c = counts[it.ordinal]
            "${it.displayName}: $c (${if (c > expected) "thừa ${c - expected}" else "thiếu ${expected - c}"})"
        }
        return ValidationError(
            ValidationErrorCode.INVALID_COLOR_COUNT,
            "Số lượng màu không đúng — mỗi màu phải có đúng $expected ô. $parts.",
        )
    }

    private fun duplicateErrors(
        perm: IntArray,
        facelets: Array<IntArray>,
        faceColor: IntArray,
        isCorner: Boolean,
        detailed: Boolean,
        n: Int = 3,
    ): ValidationError? {
        val positions = perm.indices.groupBy { perm[it] }.filter { it.key >= 0 && it.value.size > 1 }
        if (positions.isEmpty()) return null
        if (!detailed) return GENERIC
        val pieceColors = { piece: Int ->
            val faces = if (isCorner) CubieCube.CORNER_COLOR[piece] else CubieCube.EDGE_COLOR[piece]
            faces.joinToString("-") { COLORS[faceColor[it]].displayName }
        }
        val msg = positions.entries.joinToString("; ") { (piece, pos) ->
            "${if (isCorner) "Góc" else "Cạnh"} ${pieceColors(piece)} xuất hiện ${pos.size} lần"
        }
        return ValidationError(
            ValidationErrorCode.DUPLICATE_PIECE,
            "$msg — trong khi một mảnh khác bị thiếu. Có thể một ô ở các vị trí này bị đọc nhầm màu.",
            positions.values.flatten().flatMap { p -> facelets[p].map { refOf(it, n) } },
        )
    }

    // ---------------------------------------------------------------- fixes

    /** Faces whose scan looks rotated: rotating them makes the cube valid. */
    private fun withFaceOrientation(state: CubeState, errors: List<ValidationError>): List<ValidationError> {
        val rotated = findFaceRotation(state) ?: return errors
        val n = state.size
        val desc = rotated.entries.joinToString(", ") { (face, q) -> "mặt ${face.symbol} (${q * 90}°)" }
        return errors + ValidationError(
            ValidationErrorCode.INVALID_FACE_ORIENTATION,
            "Có vẻ $desc đã được quét bị xoay. Hãy dùng gợi ý \"Xoay lại mặt\" hoặc quét lại theo đúng hướng dẫn.",
            rotated.keys.flatMap { f -> (0 until n * n).map { StickerRef(f, it) } },
        )
    }

    /** Smallest set of face rotations (face -> clockwise quarter turns) that makes the cube valid. */
    fun findFaceRotation(state: CubeState): Map<Face, Int>? {
        val colors = state.toByteArray()
        val n = state.size
        var best: Map<Face, Int>? = null
        var bestCount = Int.MAX_VALUE
        val total = 1 shl 12 // 4^6
        for (code in 1 until total) {
            val qs = IntArray(6) { (code shr (2 * it)) and 3 }
            val count = qs.count { it != 0 }
            if (count >= bestCount || count > 2) continue
            val c = colors.copyOf()
            for (f in 0 until 6) if (qs[f] != 0) rotateFace(c, f, n, qs[f])
            if (isValid(state.type, c)) {
                best = Face.entries.filter { qs[it.ordinal] != 0 }.associateWith { qs[it.ordinal] }
                bestCount = count
            }
        }
        return best
    }

    fun rotateFace(colors: ByteArray, face: Int, n: Int, quarterTurns: Int) {
        repeat(quarterTurns) {
            val base = face * n * n
            val old = colors.copyOfRange(base, base + n * n)
            for (r in 0 until n) for (c in 0 until n) colors[base + c * n + (n - 1 - r)] = old[r * n + c]
        }
    }

    /**
     * Minimal edits that make the cube valid, most plausible first:
     * rotated face scans, single mis-read stickers (when color counts are off), or two stickers
     * whose colors were swapped (when counts are right). Low-confidence stickers rank first.
     */
    fun suggestFixes(state: CubeState, confidence: FloatArray? = null, limit: Int = 3): List<FixSuggestion> {
        val type = state.type
        val n = state.size
        val colors = state.toByteArray()
        val conf = confidence ?: FloatArray(colors.size) { 1f }
        val out = ArrayList<Pair<Float, FixSuggestion>>()

        findFaceRotation(state)?.let { rot ->
            val c = colors.copyOf()
            for ((face, q) in rot) rotateFace(c, face.ordinal, n, q)
            val fixes = diff(colors, c, n)
            out += -1f to FixSuggestion(
                "Xoay lại ${rot.entries.joinToString(", ") { (f, q) -> "mặt ${f.symbol} ${q * 90}°" }} (mặt này có thể đã được quét sai hướng).",
                fixes,
            )
        }

        val fixed = BooleanArray(colors.size)
        if (type.hasFixedCenters) for (f in 0 until 6) fixed[f * n * n + (n * n) / 2] = true
        val expected = colors.size / 6
        val counts = IntArray(6).also { for (b in colors) it[b.toInt()]++ }
        val over = (0 until 6).filter { counts[it] > expected }
        val under = (0 until 6).filter { counts[it] < expected }

        if (over.isNotEmpty()) {
            // Single sticker re-color from an over-represented color to a missing one.
            for (i in colors.indices) {
                if (fixed[i] || colors[i].toInt() !in over) continue
                for (to in under) {
                    val c = colors.copyOf(); c[i] = to.toByte()
                    if (isValid(type, c)) out += conf[i] to singleFix(i, colors[i].toInt(), to, n)
                }
            }
            if (out.isEmpty() && over.sumOf { counts[it] - expected } == 2) {
                // Two re-colors, restricted to the least confident stickers to stay cheap.
                val candidates = colors.indices.filter { !fixed[it] && colors[it].toInt() in over }
                    .sortedBy { conf[it] }.take(14)
                for (a in candidates.indices) for (b in a + 1 until candidates.size) {
                    val i = candidates[a]; val j = candidates[b]
                    for (ti in under) for (tj in under) {
                        val c = colors.copyOf(); c[i] = ti.toByte(); c[j] = tj.toByte()
                        if (c.count { it.toInt() == ti } != expected || c.count { it.toInt() == tj } != expected) continue
                        if (isValid(type, c)) out += (conf[i] + conf[j]) to FixSuggestion(
                            "Có thể 2 ô bị nhận diện sai: ${describe(i, n)} nên là màu ${COLORS[ti].displayName}, ${describe(j, n)} nên là màu ${COLORS[tj].displayName}.",
                            listOf(fix(i, colors[i].toInt(), ti, n), fix(j, colors[j].toInt(), tj, n)),
                        )
                    }
                }
            }
        } else {
            // Counts are right: look for two stickers whose colors were swapped.
            val idx = colors.indices.filter { !fixed[it] }
            for (a in idx.indices) for (b in a + 1 until idx.size) {
                val i = idx[a]; val j = idx[b]
                if (colors[i] == colors[j]) continue
                val c = colors.copyOf(); c[i] = colors[j]; c[j] = colors[i]
                if (isValid(type, c)) out += (conf[i] + conf[j]) to FixSuggestion(
                    "Có thể 2 ô bị đọc nhầm màu cho nhau: ${describe(i, n)} (${COLORS[colors[i].toInt()].displayName}) và ${describe(j, n)} (${COLORS[colors[j].toInt()].displayName}).",
                    listOf(fix(i, colors[i].toInt(), colors[j].toInt(), n), fix(j, colors[j].toInt(), colors[i].toInt(), n)),
                )
            }
        }
        return out.sortedBy { it.first }.map { it.second }.distinctBy { it.fixes.toSet() }.take(limit)
    }

    private fun singleFix(i: Int, from: Int, to: Int, n: Int): FixSuggestion {
        val ref = refOf(i, n)
        return FixSuggestion(
            "Có thể sticker màu ${COLORS[to].displayName} ở mặt ${ref.face.symbol} (hàng ${ref.row(n) + 1}, cột ${ref.col(n) + 1}) đang bị nhận diện thành màu ${COLORS[from].displayName}.",
            listOf(fix(i, from, to, n)),
        )
    }

    private fun fix(i: Int, from: Int, to: Int, n: Int) = StickerFix(refOf(i, n), COLORS[from], COLORS[to])

    private fun describe(i: Int, n: Int): String {
        val r = refOf(i, n)
        return "ô mặt ${r.face.symbol} (hàng ${r.row(n) + 1}, cột ${r.col(n) + 1})"
    }

    private fun diff(a: ByteArray, b: ByteArray, n: Int): List<StickerFix> =
        a.indices.filter { a[it] != b[it] }.map { fix(it, a[it].toInt(), b[it].toInt(), n) }
}
