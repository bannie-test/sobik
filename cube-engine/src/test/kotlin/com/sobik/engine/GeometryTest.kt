package com.sobik.engine

import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.Notation
import com.sobik.model.StickerColor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeometryTest {
    private val solved = CubeState.solved(CubeType.CUBE_3X3)

    @Test
    fun `R moves front stickers to the top`() {
        val s = solved.applyMoves("R")
        for (i in intArrayOf(2, 5, 8)) assertEquals(StickerColor.GREEN, s[Face.U, i])
        for (i in intArrayOf(0, 3, 6)) assertEquals(StickerColor.BLUE, s[Face.D, 8 - i].let { s[Face.D, i + 2] })
        assertEquals(StickerColor.YELLOW, s[Face.F, 2])
        assertEquals(StickerColor.WHITE, s[Face.B, 6])
    }

    @Test
    fun `U moves front stickers to the left`() {
        val s = solved.applyMoves("U")
        assertEquals(StickerColor.GREEN, s[Face.L, 0])
        assertEquals(StickerColor.RED, s[Face.F, 0])
        assertEquals(StickerColor.BLUE, s[Face.R, 1])
    }

    @Test
    fun `basic identities hold for every size`() {
        for (type in CubeType.entries) {
            val start = CubeState.solved(type)
            for (m in listOf("R", "U", "F", "D", "L", "B", "x", "y", "z", "Rw", "M", "E", "S")) {
                assertEquals(start, start.applyMoves("$m $m $m $m"), "$m^4 on ${type.label}")
            }
            assertEquals(start, start.applyMoves(List(6) { "R U R' U'" }.joinToString(" ")), "sexy^6 on ${type.label}")
        }
    }

    @Test
    fun `rotations keep the cube solved`() {
        val s = solved.applyMoves("x y z2 y'")
        assertTrue(s.isSolved())
    }

    @Test
    fun `wide move equals face move plus slice`() {
        assertEquals(solved.applyMoves("r"), solved.applyMoves("R M'"))
        assertEquals(solved.applyMoves("x"), solved.applyMoves("R M' L'"))
        assertEquals(solved.applyMoves("y"), solved.applyMoves("U E' D'"))
        assertEquals(solved.applyMoves("z"), solved.applyMoves("F S B'"))
    }

    @Test
    fun `facelet and cubie models agree`() {
        val rnd = Random(1)
        repeat(200) {
            val moves = Notation.parse(List(25) { listOf("U", "R", "F", "D", "L", "B")[rnd.nextInt(6)] + listOf("", "2", "'")[rnd.nextInt(3)] }.joinToString(" "))
            val viaFacelets = solved.applyMoves(moves)
            val viaCubies = CubieCube().applyMoves(moves)
            assertEquals(viaCubies, CubeLabeling.toCubieCube(viaFacelets))
            assertEquals(viaFacelets, CubeLabeling.fromCubieCube(viaCubies) { f -> solved.center(f)!! })
        }
    }

    @Test
    fun `cubie move tables match Kociemba`() {
        val r = CubieCube.MOVES[Face.R.ordinal]
        assertEquals(listOf(4, 1, 2, 0, 7, 5, 6, 3), r.cp.toList())
        assertEquals(listOf(2, 0, 0, 1, 1, 0, 0, 2), r.co.toList())
        assertEquals(listOf(8, 1, 2, 3, 11, 5, 6, 7, 4, 9, 10, 0), r.ep.toList())
        val f = CubieCube.MOVES[Face.F.ordinal]
        assertEquals(listOf(0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0), f.eo.toList())
        assertEquals(listOf(1, 2, 0, 0, 2, 1, 0, 0), f.co.toList())
    }

    @Test
    fun `inverse undoes a state`() {
        val cc = RandomCube.cubie(Random(3))
        val p = cc.copy(); p.multiply(cc.inverse())
        assertTrue(p.isSolved())
    }

    @Test
    fun `serialization round trip`() {
        val s = solved.applyMoves("R U F' L2 D")
        assertEquals(s, CubeState.deserialize(s.serialize()))
        val two = CubeState.solved(CubeType.CUBE_2X2).applyMoves("R U'")
        assertEquals(two, CubeState.deserialize(two.serialize()))
    }

    @Test
    fun `notation round trip`() {
        val text = "R U R' U2 Rw 3Rw' 2R2 M' x y2 z'"
        assertEquals(text, Notation.format(Notation.parse(text)))
        assertEquals("R2", Notation.format(Notation.simplify(Notation.parse("R R"))))
        assertEquals("", Notation.format(Notation.simplify(Notation.parse("R R'"))))
        assertEquals(Notation.parse("Rw"), Notation.parse("r"))
    }
}
