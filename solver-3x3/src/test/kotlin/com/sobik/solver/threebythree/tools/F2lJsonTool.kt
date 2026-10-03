package com.sobik.solver.threebythree.tools

import com.sobik.engine.CubieCube
import com.sobik.model.Notation
import kotlin.test.Test

/** Developer tool (SOBIK_TOOLS=1): prints F2L library JSON with recognition derived from each case. */
class F2lJsonTool {
    // primary | alternatives..., ordered by group
    private val entries = listOf(
        "U R U' R'", "F' U' F", "R U R'", "U' F' U F | U F R' F' R",
        "R U2 R' U' R U R'", "F U R U' R' F' R U' R' | U' F' U F U F' U F U' F' U F",
        "R U R' U2 R U R' U' R U R' | U R U' R' U' R U' R' U R U' R'", "F' U2 F U F' U' F",
        "U2 R U R' U R U' R'", "F' U2 F U' F' U F | U' F' U2 F U' F' U F", "R U2 R' U R U' R' | U R U2 R' U R U' R'",
        "U2 F' U' F U' F' U F", "U' R U' R' U R U R'", "R U' R' U2 F' U' F", "U F' U2 F U' R U R' | R U' R' U R U' R' U2 R U' R'",
        "U' R U R' U R U R' | U2 R U' R' U' R U R'", "U F' U' F U2 F' U F", "U F' U2 F U2 F' U F",
        "U' R U2 R' U F' U' F", "F' U F U2 R U R' | R U R' U2 R U' R' U R U' R'", "U F' U F U' F' U' F",
        "U' R U2 R' U2 R U' R'", "U' R U R' U2 R U' R'", "R U R' U2 F' U' F | U' R U' R' U F' U' F",
        "R U R' U' R U R' U' R U R'", "R U' R' F' U2 F",
        "U R U R' U2 R U R' | U F' U F U F' U2 F", "U F' U' F U' R U R'",
        "U' R U' R' U2 R U' R' | U' R U' R' U' R U2 R'", "U' R U R' U F' U' F",
        "R U R' U' R U R'", "F' U F U' F' U F", "U' R' F R F' R U R' | U' F' U F U R U' R'",
        "U R U' R' U' F' U F | R U' R' F R' F' R",
        "R U' R' U R U' R'", "R' F R F' U R U' R'",
        "R U' R' U F' U2 F U F' U2 F | R2 U2 F R2 F' U2 R' U R'", "R U' R' U' R U R' U2 R U' R'",
        "F' L' U2 L F R U R'", "R U' R' U R U2 R' U R U' R'", "R U' R' F' L' U2 L F",
    )

    @Test
    fun generate() {
        if (System.getenv("SOBIK_TOOLS") == null) return
        val sb = StringBuilder()
        entries.forEachIndexed { i, e ->
            val algs = e.split("|").map { it.trim() }
            val case = CubieCube().applyMoves(Notation.invert(Notation.parse(algs[0])))
            val cp = (0 until 8).first { case.cp[it] == CubieCube.DFR }
            val ep = (0 until 12).first { case.ep[it] == CubieCube.FR }
            val group = when {
                cp < 4 && ep < 4 -> "Góc và cạnh ở tầng trên"
                cp < 4 -> "Góc ở tầng trên, cạnh trong slot"
                ep < 4 -> "Góc trong slot, cạnh ở tầng trên"
                else -> "Góc và cạnh trong slot"
            }
            val recog = cornerText(case, cp) + "; " + edgeText(case, ep) + "."
            val len = Notation.parse(algs[0]).size
            val diff = if (len <= 4) "EASY" else if (len <= 8) "MEDIUM" else "HARD"
            sb.append("""    {"id": "f2l-${(i + 1).toString().padStart(2, '0')}", "name": "F2L ${i + 1}", "category": "F2L", "group": "$group", "recognition": "$recog", "notation": "${algs[0]}", "alternatives": [${algs.drop(1).joinToString(", ") { "\"$it\"" }}], "difficulty": "$diff"},""").append('\n')
        }
        java.io.File("build/f2l.json").writeText(sb.toString())
    }

    private fun stickerFace(idx: Int) = "URFDLB"[idx / 9]

    private fun facing(face: Char) = when (face) {
        'U' -> "hướng lên trên"; 'F' -> "hướng ra trước"; 'R' -> "hướng sang phải"
        'L' -> "hướng sang trái"; 'B' -> "hướng ra sau"; else -> "nằm ở mặt dưới"
    }

    private fun cornerText(c: CubieCube, pos: Int): String {
        val where = when (pos) {
            0 -> "Góc ở tầng trên, ngay trên slot (URF)"
            1 -> "Góc ở tầng trên, vị trí trước-trái (UFL)"
            2 -> "Góc ở tầng trên, vị trí sau-trái (ULB)"
            3 -> "Góc ở tầng trên, vị trí sau-phải (UBR)"
            else -> "Góc nằm trong slot (DFR)"
        }
        val white = CubieCube.CORNER_FACELET[pos][c.co[pos]]
        return if (pos == 4 && c.co[pos] == 0) "$where, đúng hướng (màu trắng ở dưới)"
        else "$where, màu trắng ${facing(stickerFace(white))}"
    }

    private fun edgeText(c: CubieCube, pos: Int): String {
        if (pos == 8) return if (c.eo[pos] == 0) "cạnh nằm trong slot, đúng chiều" else "cạnh nằm trong slot nhưng bị lật"
        val where = when (pos) { 0 -> "UR (trên-phải)"; 1 -> "UF (trên-trước)"; 2 -> "UL (trên-trái)"; else -> "UB (trên-sau)" }
        // sticker k of the FR piece (k=0: F color, k=1: R color) sits at EDGE_FACELET[pos][(k + eo) % 2]
        val topColor = if (stickerFace(CubieCube.EDGE_FACELET[pos][(0 + c.eo[pos]) % 2]) == 'U') "màu tâm trước (F)" else "màu tâm phải (R)"
        return "cạnh ở $where, $topColor hướng lên trên"
    }
}
