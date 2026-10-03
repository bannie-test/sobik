package com.sobik.content

import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubieCube
import com.sobik.engine.applyMoves
import com.sobik.model.AlgorithmCategory
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Notation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ContentTest {
    private val repo = ContentRepository()
    private val solved3 = CubeState.solved(CubeType.CUBE_3X3)

    private fun cubie(alg: String, fromCase: Boolean = true): CubieCube {
        val moves = Notation.parse(alg)
        val s = if (fromCase) solved3.applyMoves(Notation.invert(moves)) else solved3.applyMoves(moves)
        return CubeLabeling.toCubieCube(s)!!
    }

    /** Does applying [alg] to a solved cube keep the listed corner/edge positions solved? */
    private fun preserves(alg: String, corners: IntRange, edges: List<Int>): Boolean {
        val c = cubie(alg, fromCase = false)
        return corners.all { c.cp[it] == it && c.co[it] == 0 } && edges.all { c.ep[it] == it && c.eo[it] == 0 }
    }

    private val f2lEdges = (4..11).toList()

    @Test
    fun `all content files load`() {
        assertEquals(8, repo.beginnerLessons.size)
        assertEquals((1..8).toList(), repo.beginnerLessons.map { it.order })
        assertEquals(41, repo.algorithms(AlgorithmCategory.F2L).size)
        assertEquals(57, repo.algorithms(AlgorithmCategory.OLL).size)
        assertEquals(21, repo.algorithms(AlgorithmCategory.PLL).size)
        assertTrue(repo.algorithms(AlgorithmCategory.BEGINNER).size >= 8)
        assertEquals(listOf(4, 5, 6, 7), repo.bigCubeGuides.map { it.cubeSize })
        assertEquals(repo.allAlgorithms.size, repo.allAlgorithms.map { it.id }.toSet().size, "ids must be unique")
    }

    @Test
    fun `every notation parses and plays on its cube`() {
        for (alg in repo.allAlgorithms) {
            val solved = CubeState.solved(alg.cubeType)
            for (text in listOf(alg.notation) + alg.alternatives + listOfNotNull(alg.setupMoves)) {
                val moves = Notation.parse(text)
                assertTrue(moves.isNotEmpty(), "${alg.id}: $text")
            }
            assertTrue(solved.applyMoves(alg.setup).applyMoves(alg.moves).isSolved() || alg.setupMoves != null, alg.id)
        }
        for (lesson in repo.beginnerLessons) for (case in lesson.cases) {
            case.algorithm?.let { assertTrue(Notation.parse(it).isNotEmpty(), "${lesson.id}/${case.title}") }
            case.setupMoves?.let { Notation.parse(it) }
        }
        for (g in repo.bigCubeGuides) for (s in g.sections) for (id in s.algorithmIds) assertNotNull(repo.algorithm(id), id)
    }

    @Test
    fun `beginner algorithms do what the guide says`() {
        // sexy move has order 6
        assertTrue(solved3.applyMoves(List(6) { "R U R' U'" }.joinToString(" ")).isSolved())
        // cross flip keeps the other cross edges
        assertTrue(preserves("U' R' F R", IntRange.EMPTY, listOf(4, 6, 7)))
        val flip = cubie("U' R' F R")
        assertEquals(CubieCube.UF, (0 until 12).first { flip.ep[it] == CubieCube.DF })
        // white corner repetitions
        for ((reps, whiteFace) in listOf(1 to 'R', 3 to 'U', 5 to 'F')) {
            val c = cubie(List(reps) { "R U R' U'" }.joinToString(" "))
            val pos = (0 until 8).first { c.cp[it] == CubieCube.DFR }
            assertEquals(CubieCube.URF, pos)
            assertEquals(whiteFace, "URFDLB"[CubieCube.CORNER_FACELET[pos][c.co[pos]] / 9])
        }
        // middle layer inserts keep the first layer
        assertTrue(preserves("U R U' R' U' F' U F", 4..7, listOf(4, 5, 6, 7, 9, 10, 11)))
        assertTrue(preserves("U' L' U L U F U' F'", 4..7, listOf(4, 5, 6, 7, 8, 10, 11)))
        // last layer algorithms keep F2L
        for (alg in listOf("F R U R' U' F'", "F U R U' R' F'", "F R U R' U' F' f R U R' U' f'", "R U R' U R U2 R'", "R U2 R' U' R U' R'",
                "R U2 R' U' R U R' U' R U' R'", "R' F R' B2 R F' R' B2 R2", "R U' R U R U R U' R' U' R2", "R2 U R U R' U' R' U' R' U R'")) {
            assertTrue(preserves(alg, 4..7, f2lEdges), alg)
        }
        // yellow cross shapes
        assertEquals(listOf(0, 1, 0, 1), cubie("F R U R' U' F'").eo.take(4), "line held horizontally")
        assertEquals(listOf(1, 1, 0, 0), cubie("F U R U' R' F'").eo.take(4), "L shape at back-left")
        assertEquals(listOf(1, 1, 1, 1), cubie("F R U R' U' F' f R U R' U' f'").eo.take(4), "dot")
        // corner/edge permutation algorithms keep orientation, A-perm keeps edges, U-perm keeps corners
        val a = cubie("R' F R' B2 R F' R' B2 R2", fromCase = false)
        assertTrue((0 until 4).all { a.co[it] == 0 && a.ep[it] == it && a.eo[it] == 0 })
        val u = cubie("R U' R U R U R U' R' U' R2", fromCase = false)
        assertTrue((0 until 4).all { u.cp[it] == it && u.co[it] == 0 && u.eo[it] == 0 })
        assertEquals(CubieCube.UB, u.inverse().ep[CubieCube.UB], "Ua keeps the back edge")
    }

    @Test
    fun `lesson demos match the lesson structure`() {
        for (lesson in repo.beginnerLessons) {
            assertTrue(lesson.goal.isNotBlank() && lesson.targetState.isNotBlank(), lesson.id)
            if (lesson.order > 1) assertTrue(lesson.cases.isNotEmpty() && lesson.cases.all { it.algorithm != null && it.explanation.isNotBlank() }, lesson.id)
        }
        assertTrue(repo.beginnerLessons.first().notation.size >= 8)
    }

    @Test
    fun `big cube parity algorithms are pure`() {
        fun changed(alg: String, type: CubeType): Int {
            val s0 = CubeState.solved(type)
            val s = s0.applyMoves(alg)
            return (0 until type.stickerCount).count { s.colorAt(it) != s0.colorAt(it) }
        }
        for (alg in repo.bigCubeAlgorithms.filter { it.category == AlgorithmCategory.PARITY }) {
            assertEquals(4, changed(alg.notation, alg.cubeType), "${alg.id} should only touch two wing pieces")
        }
        // edge pairing keeps centers
        val s0 = CubeState.solved(CubeType.CUBE_4X4)
        val s = s0.applyMoves(repo.algorithm("4x4-edge-pairing")!!.notation)
        for (f in 0 until 6) for (r in 1..2) for (c in 1..2) assertEquals(s0.colorAt(f * 16 + r * 4 + c), s.colorAt(f * 16 + r * 4 + c))
    }
}

class PuzzleCatalogTest {
    private val repo = ContentRepository()

    @Test
    fun `puzzle catalog is consistent`() {
        assertTrue(repo.puzzleBrands.size >= 40)
        assertTrue(repo.puzzleBrands.all { it.logo != null && it.logo!!.width > 0 }, "every brand has a wordmark")
        assertEquals(repo.puzzleBrands.size, repo.puzzleBrands.map { it.id }.toSet().size)
        for (b in repo.puzzleBrands) b.parentId?.let { assertNotNull(repo.brand(it), "parent of ${b.id}") }
        assertEquals("x-man-design", repo.findBrand("xman")?.id)
        assertEquals("pbcube", repo.brand("pbcube")?.id)
        assertTrue(repo.puzzleSources.size >= 5)
        assertTrue(repo.puzzles.size >= 15)
        assertEquals(repo.puzzles.size, repo.puzzles.map { it.id }.toSet().size)
        for (p in repo.puzzles) {
            assertNotNull(repo.brand(p.brandId), p.id)
            assertTrue(p.features.isNotEmpty() && p.reviews.isNotEmpty(), p.id)
            assertTrue(p.size in 2..7, p.id)
        }
        for (b in repo.puzzleBrands) assertTrue(b.color.toLongOrNull(16) != null, b.id)
        assertTrue(repo.puzzleDisclaimer.isNotBlank())
    }

    @Test
    fun `lessons have target pictures and notation moves parse`() {
        val masks = setOf("cross", "firstLayer", "f2l", "yellowCross", "yellowFace", "yellowCorners", "solved")
        for (l in repo.beginnerLessons) {
            assertTrue(l.targetMask in masks, l.id)
            assertTrue(l.targetView in setOf("top", "bottom"), l.id)
            l.notation.mapNotNull { it.move }.forEach { assertTrue(Notation.parse(it).isNotEmpty()) }
        }
    }
}
