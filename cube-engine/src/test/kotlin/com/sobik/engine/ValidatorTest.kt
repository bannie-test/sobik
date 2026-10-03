package com.sobik.engine

import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.StickerColor
import com.sobik.model.StickerRef
import com.sobik.model.ValidationErrorCode
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatorTest {
    private val scheme = CubeState.solved(CubeType.CUBE_3X3)
    private fun randomState(seed: Int): CubeState =
        CubeLabeling.fromCubieCube(RandomCube.cubie(Random(seed))) { scheme.center(it)!! }

    @Test
    fun `random reachable states are valid`() {
        repeat(100) { assertTrue(CubeValidator.validate(randomState(it)).isValid) }
        assertTrue(CubeValidator.validate(scheme).isValid)
    }

    @Test
    fun `wrong color count is reported with a single-sticker suggestion`() {
        val s = randomState(7)
        val ref = StickerRef(Face.F, 1)
        val original = s[ref]
        val wrong = StickerColor.entries.first { it != original }
        val r = CubeValidator.validate(s.with(ref, wrong))
        assertFalse(r.isValid)
        assertTrue(ValidationErrorCode.INVALID_COLOR_COUNT in r.codes)
        assertTrue(r.suggestions.any { sug -> sug.fixes.size == 1 && sug.fixes[0].sticker == ref && sug.fixes[0].to == original })
    }

    @Test
    fun `twisted corner is detected`() {
        val cc = CubieCube(); cc.co[0] = 1; cc.co[1] = 0
        val s = CubeLabeling.fromCubieCube(cc) { scheme.center(it)!! }
        val r = CubeValidator.validate(s)
        assertTrue(ValidationErrorCode.INVALID_CORNER_ORIENTATION in r.codes)
        assertTrue(ValidationErrorCode.INVALID_CUBE_STATE in r.codes)
    }

    @Test
    fun `flipped edge is detected`() {
        val cc = CubieCube(); cc.eo[3] = 1
        val s = CubeLabeling.fromCubieCube(cc) { scheme.center(it)!! }
        assertTrue(ValidationErrorCode.INVALID_EDGE_ORIENTATION in CubeValidator.validate(s).codes)
    }

    @Test
    fun `swapped edges give parity error`() {
        val cc = CubieCube(); cc.ep[0] = 1; cc.ep[1] = 0
        val s = CubeLabeling.fromCubieCube(cc) { scheme.center(it)!! }
        assertTrue(ValidationErrorCode.INVALID_PERMUTATION_PARITY in CubeValidator.validate(s).codes)
    }

    @Test
    fun `duplicate centers are reported`() {
        val s = scheme.with(StickerRef(Face.U, 4), StickerColor.GREEN).with(StickerRef(Face.F, 4), StickerColor.WHITE)
            .with(StickerRef(Face.F, 4), StickerColor.GREEN).with(StickerRef(Face.U, 0), StickerColor.WHITE)
        val r = CubeValidator.validate(s)
        assertFalse(r.isValid)
    }

    @Test
    fun `rotated face scan is detected and fixable`() {
        val s = randomState(11)
        val bytes = s.toByteArray()
        CubeValidator.rotateFace(bytes, Face.R.ordinal, 3, 1)
        val rotated = CubeState.fromBytes(CubeType.CUBE_3X3, bytes)
        val r = CubeValidator.validate(rotated)
        assertFalse(r.isValid)
        assertTrue(ValidationErrorCode.INVALID_FACE_ORIENTATION in r.codes)
        val fix = r.suggestions.first()
        var fixed = rotated
        for (f in fix.fixes) fixed = fixed.with(f.sticker, f.to)
        assertEquals(s, fixed)
    }

    @Test
    fun `swapped sticker colors are suggested`() {
        val s = randomState(5)
        val a = StickerRef(Face.F, 0)
        val b = (0 until 9).map { StickerRef(Face.B, it) }.first { it.index != 4 && s[it] != s[a] }
        val broken = s.with(a, s[b]).with(b, s[a])
        val r = CubeValidator.validate(broken)
        if (!r.isValid) {
            assertTrue(r.suggestions.isNotEmpty())
            r.suggestions.forEach { sug ->
                var t = broken
                for (f in sug.fixes) t = t.with(f.sticker, f.to)
                assertTrue(CubeValidator.isValid(t))
            }
        }
    }

    @Test
    fun `2x2 states validate`() {
        val solved2 = CubeState.solved(CubeType.CUBE_2X2)
        assertTrue(CubeValidator.validate(solved2).isValid)
        repeat(50) {
            val s = solved2.applyMoves(List(20) { listOf("U", "R", "F", "D", "L", "B", "x", "y")[Random(it * 31 + 7 + it).nextInt(8)] }.joinToString(" ") + " R U2 F'")
            assertTrue(CubeValidator.validate(s).isValid, s.serialize())
        }
        val cc = CornerCube(); cc.co[2] = 2
        val labels = cc.toCornerLabels(2)
        val colors = CubeState.fromColors(CubeType.CUBE_2X2, labels.map { solved2.faceColors(Face.entries[it.toInt()])[0] })
        val r = CubeValidator.validate(colors)
        assertTrue(ValidationErrorCode.INVALID_CORNER_ORIENTATION in r.codes)
    }

    @Test
    fun `2x2 wrong sticker gives suggestion`() {
        val s = CubeState.solved(CubeType.CUBE_2X2).applyMoves("R U R' F2 U'")
        val ref = StickerRef(Face.L, 2)
        val bad = s.with(ref, StickerColor.entries.first { it != s[ref] })
        val r = CubeValidator.validate(bad)
        assertFalse(r.isValid)
        assertTrue(r.suggestions.any { it.fixes.any { f -> f.sticker == ref && f.to == s[ref] } })
    }
}
