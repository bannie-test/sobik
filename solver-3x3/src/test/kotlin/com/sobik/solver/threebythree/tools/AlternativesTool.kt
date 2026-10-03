package com.sobik.solver.threebythree.tools

import com.sobik.content.ContentRepository
import com.sobik.engine.CubieCube
import com.sobik.engine.applyMoves
import com.sobik.model.AlgorithmCategory
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.solver.threebythree.method.CaseLibrary
import com.sobik.solver.threebythree.method.Frame
import kotlin.test.Test

/**
 * Developer tool (SOBIK_TOOLS=1): proposes alternative algorithms per library case.
 * F2L: searched in several move groups. OLL/PLL: candidates below. Every proposal is verified
 * to solve the primary algorithm's case (allowing U turns before/after). Writes build/alternatives.json.
 */
class AlternativesTool {
    private val ollPll = mapOf(
        "OLL 1" to listOf("R U' R2 D' L F2 L' D R2 U R'", "F R U R' U' F' f R U R' U' f' U' F R U R' U' F'"),
        "OLL 5" to listOf("l' U2 L U L' U l", "R' F2 r U r' F R"),
        "OLL 6" to listOf("R U2 R' U' R U' R' U' R U2 R'", "r U2 R' U' R U' r'"),
        "OLL 7" to listOf("L' U2 L U2 L F' L' F", "r U R' U R U2 r'"),
        "OLL 8" to listOf("R U2 R' U2 R' F R F'", "r' U' R U' R' U2 r"),
        "OLL 9" to listOf("R' U' R U' R' U R' F R F' U R", "F R U R' U' R U R' U' F' U F R U R' U' F'"),
        "OLL 10" to listOf("R U R' U R' F R F' R U2 R'", "R U R' y R' F R U' R' F' R y'"),
        "OLL 11" to listOf("M R U R' U R U2 R' U M'", "r' R2 U R' U R U2 R' U M'"),
        "OLL 12" to listOf("F R U R' U' F' U F R U R' U' F'", "R' F R F' R' F R F' R U R' U' R U R'"),
        "OLL 13" to listOf("r U' r' U' r U r' F' U F", "F U R U2 R' U' R U R' F'"),
        "OLL 14" to listOf("r' U r U r' U' r F U' F'", "F' U' L' U2 L U L' U' L F"),
        "OLL 15" to listOf("l' U' l L' U' L U l' U l"),
        "OLL 16" to listOf("R' F R U R' U' F' R U' R' U2 R"),
        "OLL 18" to listOf("F R U R' U F' U2 F' L F L'", "R U2 R2 F R F' U2 M' U R U' r'"),
        "OLL 19" to listOf("M U R U R' U' M' R' F R F'", "R' U2 F R U R' U' F2 U2 F R"),
        "OLL 20" to listOf("M' U2 M U2 M' U M U2 M' U2 M", "r' R U R U R' U' M2 U R U' r'"),
        "OLL 21" to listOf("F R U R' U' R U R' U' R U R' U' F'", "R U R' U R U' R' U R U2 R'"),
        "OLL 22" to listOf("f R U R' U' f' F R U R' U' F'", "R' U2 R2 U R2 U R2 U2 R'"),
        "OLL 23" to listOf("R2 D R' U2 R D' R' U2 R'", "R U R' U' R' L F R F' L'"),
        "OLL 24" to listOf("L F R' F' L' F R F'", "R U R D R' U' R D' R2"),
        "OLL 25" to listOf("R' F R B' R' F' R B", "F R' F' r U R U' r'"),
        "OLL 26" to listOf("L' U' L U' L' U2 L", "R' U' R U' R' U2 R"),
        "OLL 27" to listOf("L' U2 L U L' U L", "R' U2 R U R' U R"),
        "OLL 28" to listOf("M' U M U2 M' U M", "M U M' U2 M U M'"),
        "OLL 29" to listOf("r2 D' r U r' D r2 U' r' U' r", "M U R U R' U' R' F R F' M'"),
        "OLL 30" to listOf("F U R U2 R' U' R U2 R' U' F'", "r' D' r U' r' D r2 U' r' U r U r'"),
        "OLL 31" to listOf("S' L' U' L U L F' L' f", "R' U' F U R U' R' F' R"),
        "OLL 32" to listOf("R U B' U' R' U R B R'", "S R U R' U' R' F R f'"),
        "OLL 33" to listOf("F R U' R' U' R U R' F'", "L' U' L U L F' L' F"),
        "OLL 34" to listOf("R U R' U' B' R' F R F' B", "F R U R' U' R' F' r U R U' r'"),
        "OLL 35" to listOf("R U2 R' R' F R F' R U2 R'", "f R U R' U' f' R U R' U R U2 R'"),
        "OLL 37" to listOf("F R U' R' U' R U R' F'", "R' F R F' U' F' U F"),
        "OLL 38" to listOf("L' U' L U' L' U L U L F' L' F"),
        "OLL 39" to listOf("R U R' F' U' F U R U2 R'", "L F' L' U' L U F U' L'"),
        "OLL 40" to listOf("R' U' R F U F' U' R' U2 R"),
        "OLL 41" to listOf("L' U' L U' L' U2 L F' L' U' L U F", "R U' R' U2 R U y R U' R' U' F' y'"),
        "OLL 42" to listOf("L U L' U L U2 L' F' L' U' L U F"),
        "OLL 43" to listOf("f' L' U' L U f", "R' U' F' U F R"),
        "OLL 44" to listOf("f R U R' U' f'", "L U F U' F' L'"),
        "OLL 45" to listOf("F R U R' U' F'", "f U R U' R' f'"),
        "OLL 46" to listOf("R' U' R' F R F' U R", "R' F' U' F U' F' U F R"),
        "OLL 47" to listOf("F' L' U' L U L' U' L U F", "b' U' R' U R U' R' U R B"),
        "OLL 48" to listOf("F R U R' U' R U R' U' F'", "R U2 R' U' R U R' U2 R' F R F'"),
        "OLL 49" to listOf("R B' R2 F R2 B R2 F' R", "l U' l2 U l2 U l2 U' l"),
        "OLL 50" to listOf("R' F R2 B' R2 F' R2 B R'", "l' U l2 U' l2 U' l2 U l'"),
        "OLL 51" to listOf("f R U R' U' R U R' U' f'", "F U R U' R' U R U' R' F'"),
        "OLL 52" to listOf("R' F' U' F U' R U R' U R", "R U R' U R d' R U' R' F'"),
        "OLL 53" to listOf("r' U' R U' R' U R U' R' U2 r", "l' U2 L U L' U' L U L' U l"),
        "OLL 54" to listOf("r U R' U R U' R' U R U2 r'", "r U2 R' U' R U R' U' R U' r'"),
        "OLL 55" to listOf("R U2 R2 U' R U' R' U2 F R F'", "r U2 R2 F R F' U2 r' F R F'"),
        "OLL 56" to listOf("F R U R' U' R F' r U R' U' r'", "r U r' U R U' R' U R U' R' r U' r'"),
        "OLL 57" to listOf("R U R' U' r R' U R U' r'", "M' U M U2 M' U M"),
        "PLL Aa" to listOf("x L2 D2 L' U' L D2 L' U L' x'", "R' F R' B2 R F' R' B2 R2"),
        "PLL Ab" to listOf("x' L2 D2 L U L' D2 L U' L x", "R B' R F2 R' B R F2 R2"),
        "PLL E" to listOf("R2 U R' U' y R U R' U' R U R' U' R U R' y' R U' R2"),
        "PLL F" to listOf("R' U R U' R2 F' U' F U R F R' F' R2", "M' U2 L F' R U2 r' U r' R2 U2 R2"),
        "PLL H" to listOf("R2 U2 R U2 R2 U2 R2 U2 R U2 R2", "M2 U' M2 U2 M2 U' M2"),
        "PLL Ja" to listOf("x R2 F R F' R U2 r' U r U2 x'", "R' U L' U2 R U' R' U2 R L"),
        "PLL Jb" to listOf("R U2 R' U' R U2 L' U R' U' L", "R U R' F' R U R' U' R' F R2 U' R' U'"),
        "PLL Ra" to listOf("R U R' F' R U2 R' U2 R' F R U R U2 R'", "L U2 L' U2 L F' L' U' L U L F L2"),
        "PLL Rb" to listOf("R' U2 R U2 R' F R U R' U' R' F' R2", "R2 F R U R U' R' F' R U2 R' U2 R"),
        "PLL T" to listOf("R2 U R2 U' R2 U' D R2 U' R2 U R2 D'", "R U R' U' R' F R2 U' R' U F' L' U L"),
        "PLL Ua" to listOf("R2 U' R' U' R U R U R U' R", "M2 U M U2 M' U M2", "R U R' U R' U' R2 U' R' U R' U R"),
        "PLL Ub" to listOf("R' U R' U' R' U' R' U R U R2", "M2 U' M U2 M' U' M2", "R2' U' R' U' R U R U R U' R"),
        "PLL V" to listOf("R' U R' U' y R' F' R2 U' R' U R' F R F", "R U2 R' D R U' R U' R U R2 D R' U' R D2"),
        "PLL Y" to listOf("F R' F R2 U' R' U' R U R' F' R U R' U' F'", "R2 U' R2 U' R2 U y' R2 U R2 U' R2 y"),
        "PLL Z" to listOf("M2 U2 M U' M2 U' M2 U' M", "R' U' R U' R U R U' R' U R U R2 U' R'", "M' U' M2 U' M2 U' M' U2 M2"),
        "PLL Na" to listOf("z D R' U R2 D' R D U' R' U R2 D' R U' R z'", "L U' R U2 L' U R' L U' R U2 L' U R'"),
        "PLL Nb" to listOf("r' D' F r U' r' F' D r2 U r' U' r' F r F'", "R' U L' U2 R U' L R' U L' U2 R U' L"),
        "PLL Ga" to listOf("R U2 R' U' R U R' U' R U R' U' R U2 R' D' R U' R' D"),
        "PLL Gc" to listOf("L' U' L U L U' F' L' U' L' U L F U' L2"),
    )

    @Test
    fun generate() {
        if (System.getenv("SOBIK_TOOLS") == null) return
        val repo = ContentRepository()
        val out = LinkedHashMap<String, List<String>>()
        for (alg in repo.algorithms3x3.filter { it.category in setOf(AlgorithmCategory.F2L, AlgorithmCategory.OLL, AlgorithmCategory.PLL) }) {
            val known = (listOf(alg.notation) + alg.alternatives).map { norm(it) }.toMutableSet()
            val candidates = if (alg.category == AlgorithmCategory.F2L) f2lCandidates(alg.notation) else ollPll[alg.name].orEmpty()
            val accepted = ArrayList<String>()
            for (c in candidates) {
                if (norm(c) in known) continue
                val ok = runCatching { solvesSameCase(alg.category, alg.notation, c) }.getOrDefault(false)
                if (ok) { accepted += c; known += norm(c) } else println("REJECT ${alg.name}: $c")
            }
            if (accepted.isNotEmpty()) out[alg.id] = accepted
        }
        val json = out.entries.joinToString(",\n", "{\n", "\n}") { (k, v) -> "  \"$k\": [${v.joinToString(", ") { "\"$it\"" }}]" }
        java.io.File("build/alternatives.json").writeText(json)
        println("cases with new alternatives: ${out.size}, total ${out.values.sumOf { it.size }}")
    }

    private fun norm(s: String) = Notation.format(Notation.parse(s))

    private fun solvesSameCase(cat: AlgorithmCategory, primary: String, alt: String): Boolean {
        val case = CubeState.solved(CubeType.CUBE_3X3).applyMoves(Notation.invert(Notation.parse(primary)))
        val moves = Notation.parse(alt)
        return (0 until 4).any { a -> (0 until 4).any { b ->
            val f = Frame(case.applyMoves(CaseLibrary.uTurns(a) + moves + CaseLibrary.uTurns(b)))
            when (cat) {
                AlgorithmCategory.F2L -> f.f2lSolved
                AlgorithmCategory.OLL -> f.ollSolved
                else -> f.solved
            }
        } }
    }

    // ---- F2L search in several move groups (meet in the middle on the F2L pieces)
    private val groups = listOf(listOf(Face.R, Face.U), listOf(Face.R, Face.U, Face.F), listOf(Face.R, Face.U, Face.L), listOf(Face.R, Face.U, Face.F, Face.L))
    private val backward = HashMap<List<Face>, Map<Long, List<Move>>>()

    private fun f2lCandidates(primary: String): List<String> {
        val start = CubieCube().applyMoves(Notation.invert(Notation.parse(primary)))
        val limit = Notation.parse(primary).size + 4
        return groups.mapNotNull { faces -> search(start, faces, limit)?.let { Notation.format(it) } }.distinct()
    }

    private fun f2lKey(c: CubieCube): Long {
        var k = 0L
        for (piece in 4..7) { val p = (0 until 8).first { c.cp[it] == piece }; k = k * 24 + p * 3 + c.co[p] }
        for (piece in 4..11) { val p = (0 until 12).first { c.ep[it] == piece }; k = k * 24 + p * 2 + c.eo[p] }
        return k
    }

    private fun sequences(faces: List<Face>, depth: Int, emit: (List<Move>) -> Unit) {
        fun rec(cur: MutableList<Move>, d: Int) {
            emit(cur)
            if (d == depth) return
            for (f in faces) {
                if (cur.isNotEmpty() && cur.last().face.toFace() == f) continue
                for (t in 1..3) { cur += Move.of(f, t); rec(cur, d + 1); cur.removeAt(cur.lastIndex) }
            }
        }
        rec(mutableListOf(), 0)
    }

    private fun search(start: CubieCube, faces: List<Face>, limit: Int): List<Move>? {
        val half = if (faces.size <= 2) 7 else 5
        val back = backward.getOrPut(faces) {
            val m = HashMap<Long, List<Move>>()
            sequences(faces, half) { seq ->
                val k = f2lKey(CubieCube().applyMoves(seq))
                val prev = m[k]
                if (prev == null || prev.size > seq.size) m[k] = seq.toList()
            }
            m
        }
        var best: List<Move>? = null
        sequences(faces, if (faces.size <= 2) 7 else 6) { fwd ->
            val b = back[f2lKey(start.copy().applyMoves(fwd))] ?: return@sequences
            val total = Notation.simplify(fwd + Notation.invert(b))
            if (total.size <= limit && (best == null || total.size < best!!.size)) best = total
        }
        return best
    }
}
