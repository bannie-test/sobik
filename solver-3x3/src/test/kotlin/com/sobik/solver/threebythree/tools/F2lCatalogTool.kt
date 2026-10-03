package com.sobik.solver.threebythree.tools

import com.sobik.engine.CubieCube
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.Notation
import kotlin.test.Test

/**
 * Developer tool (runs only with SOBIK_TOOLS=1): verifies candidate F2L algorithms, groups them
 * into the 41 cases (modulo pre-AUF) and searches <R, U, F, L> for any case left uncovered.
 * Output: build/f2l-catalog.txt, used to author content/algorithms_3x3.json.
 */
class F2lCatalogTool {
    private val candidates = listOf(
        "U R U' R'", "U' F' U F", "F' U' F", "R U R'",
        "U' R U R' U2 R U' R'", "U F' U' F U2 F' U F", "U' R U2 R' U2 R U' R'", "U F' U2 F U2 F' U F",
        "U' R U' R' U F' U' F", "U' R U R' U R U R'", "U' R U2 R' U F' U' F", "R U' R' U R U' R' U2 R U' R'",
        "U F' U F U' F' U' F", "U' R U' R' U R U R'", "R U' R' U2 F' U' F", "R U2 R' U' R U R'",
        "F' U2 F U F' U' F", "U R U2 R' U R U' R'", "U' F' U2 F U' F' U F", "U2 R U R' U R U' R'",
        "U2 F' U' F U' F' U F", "U R U' R' U' R U' R' U R U' R'", "U' F' U F U F' U F U' F' U F",
        "U' R' F R F' R U R'", "U R U' R' F R' F' R", "R U' R' U R U' R'", "F' U F U' F' U F",
        "R U R' U' R U R'", "R U R' U' R U R' U' R U R'", "U' R U' R' U2 R U' R'", "U R U R' U2 R U R'",
        "U' R U R' U F' U' F", "U F' U' F U' R U R'", "R2 U2 F R2 F' U2 R' U R'", "R U' R' U' R U R' U2 R U' R'",
        "R U' R' U R U2 R' U R U' R'", "R U' R' U F' U2 F U F' U2 F", "R U R' U2 R U' R' U R U' R'",
        "U F' U F U F' U2 F", "U' R U' R' U' R U2 R'", "U F' U2 F U' R U R'", "U R U' R' U' F' U F",
        "U' F' U F U R U' R'", "U2 R U' R' U' R U R'", "U R U R' U' R U R' U' R U R'",
        "F' U F U2 R U R'", "R U R' U2 F' U' F", "U F R' F' R", "R U' R' F R' F' R", "F U R U' R' F' R U' R'",
        "U2 R U2 R' U R U' R'", "U2 F' U2 F U' F' U F", "R U2 R' U R U' R'", "F' U2 F U' F' U F",
        "U R U2 R2 F R F'", "U' F' U2 F2 R' F' R", "R U R' U' U' R U R' U' R U R'", "R' F R F' U R U' R'",
    )

    @Test
    fun generate() {
        if (System.getenv("SOBIK_TOOLS") == null) return
        val out = StringBuilder()
        val classes = allClasses()
        out.appendLine("classes: ${classes.size}")
        val covered = HashMap<Int, MutableList<String>>()
        for (alg in candidates) {
            val moves = Notation.parse(alg)
            val case = CubieCube().applyMoves(Notation.invert(moves))
            if (!f2lOthersSolved(case) || !playable(case)) { out.appendLine("INVALID: $alg"); continue }
            covered.getOrPut(canonical(case)) { mutableListOf() } += alg
        }
        for ((cls, rep) in classes) {
            val algs = covered[cls]
            if (algs != null) out.appendLine("CASE ${describe(rep)} => ${algs.joinToString(" | ")}")
            else {
                val found = search(rep)
                out.appendLine("CASE ${describe(rep)} => SEARCH: ${found?.let { Notation.format(it) }}")
            }
        }
        java.io.File("build/f2l-catalog.txt").writeText(out.toString())
        println(out)
    }

    private fun uTurn(c: CubieCube, a: Int): CubieCube = c.copy().apply { repeat(a) { applyMove(Face.U, 1) } }

    private fun key(c: CubieCube): Int {
        val cp = (0 until 8).first { c.cp[it] == CubieCube.DFR }
        val ep = (0 until 12).first { c.ep[it] == CubieCube.FR }
        return (cp * 3 + c.co[cp]) * 24 + ep * 2 + c.eo[ep]
    }

    private fun canonical(c: CubieCube) = (0 until 4).minOf { key(uTurn(c, it)) }

    private fun playable(c: CubieCube): Boolean {
        val cp = (0 until 8).first { c.cp[it] == CubieCube.DFR }
        val ep = (0 until 12).first { c.ep[it] == CubieCube.FR }
        return (cp < 4 || cp == 4) && (ep < 4 || ep == 8)
    }

    private fun f2lOthersSolved(c: CubieCube): Boolean =
        (4..7).all { c.ep[it] == it && c.eo[it] == 0 } &&
            listOf(5 to 9, 6 to 10, 7 to 11).all { (k, e) -> c.cp[k] == k && c.co[k] == 0 && c.ep[e] == e && c.eo[e] == 0 } &&
            !(c.cp[4] == 4 && c.co[4] == 0 && c.ep[8] == 8 && c.eo[8] == 0)

    /** One representative cube per F2L case class (other U-layer pieces arbitrary). */
    private fun allClasses(): List<Pair<Int, CubieCube>> {
        val res = LinkedHashMap<Int, CubieCube>()
        for (cp in listOf(0, 1, 2, 3, 4)) for (co in 0 until 3) for (ep in listOf(0, 1, 2, 3, 8)) for (eo in 0 until 2) {
            val c = CubieCube()
            c.cp[4] = c.cp[cp].also { c.cp[cp] = 4 }
            c.ep[8] = c.ep[ep].also { c.ep[ep] = 8 }
            c.co[cp] = co
            c.eo[ep] = eo
            // fix orientation sums / parity using other U-layer pieces
            val uc = (0 until 4).first { it != cp }
            c.co[uc] = (c.co[uc] + 3 - c.co.sum() % 3) % 3
            val ue = (0 until 4).first { it != ep }
            c.eo[ue] = (c.eo[ue] + c.eo.sum()) % 2
            if (CubieCube.permutationParity(c.cp) != CubieCube.permutationParity(c.ep)) {
                val a = (0 until 4).filter { it != ep }
                c.ep[a[0]] = c.ep[a[1]].also { c.ep[a[1]] = c.ep[a[0]] }
            }
            check(c.isSolvable())
            if (cp == 4 && co == 0 && ep == 8 && eo == 0) continue
            res.putIfAbsent(canonical(c), c)
        }
        return res.entries.map { it.key to it.value }
    }

    private fun describe(c: CubieCube): String {
        val cp = (0 until 8).first { c.cp[it] == CubieCube.DFR }
        val ep = (0 until 12).first { c.ep[it] == CubieCube.FR }
        return "corner=${CubieCube.CORNER_NAMES[cp]}/${c.co[cp]} edge=${CubieCube.EDGE_NAMES[ep]}/${c.eo[ep]}"
    }

    // ---- meet-in-the-middle search over <R, U, F, L>
    private val faces = listOf(Face.R, Face.U, Face.F, Face.L)

    private fun f2lKey(c: CubieCube): Long {
        var k = 0L
        for (piece in 4..7) { val p = (0 until 8).first { c.cp[it] == piece }; k = k * 24 + p * 3 + c.co[p] }
        for (piece in 4..11) { val p = (0 until 12).first { c.ep[it] == piece }; k = k * 24 + p * 2 + c.eo[p] }
        return k
    }

    private fun sequences(depth: Int, emit: (List<Move>) -> Unit) {
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

    private val backward: Map<Long, List<Move>> by lazy {
        val m = HashMap<Long, List<Move>>()
        sequences(5) { seq ->
            val c = CubieCube().applyMoves(seq)
            val k = f2lKey(c)
            val prev = m[k]
            if (prev == null || prev.size > seq.size) m[k] = seq.toList()
        }
        m
    }

    private fun search(start: CubieCube): List<Move>? {
        var best: List<Move>? = null
        sequences(6) { fwd ->
            val c = start.copy().applyMoves(fwd)
            val b = backward[f2lKey(c)] ?: return@sequences
            val total = Notation.simplify(fwd + Notation.invert(b))
            if (best == null || score(total) < score(best!!)) best = total
        }
        return best
    }

    private fun score(m: List<Move>) = m.size * 10 + m.count { it.face.symbol == "L" } * 3 + m.count { it.face.symbol == "F" }
}
