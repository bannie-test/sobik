package com.sobik.solver.threebythree.method

import com.sobik.engine.CubeValidator
import com.sobik.engine.CubieCube
import com.sobik.model.Algorithm
import com.sobik.model.CubeSolver
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.model.SolutionKind
import com.sobik.model.SolutionStep
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.model.Solution
import com.sobik.model.StickerColor

/** Progress of a cube with respect to CFOP, as seen with [bottomFace] held down. */
data class CfopAnalysis(
    val bottomFace: Face,
    val bottomColor: StickerColor,
    val crossSolved: Boolean,
    val solvedSlots: Int,
    val f2lSolved: Boolean,
    val ollSolved: Boolean,
    val solved: Boolean,
) {
    val summary: String
        get() = when {
            solved -> "Cube đã được giải."
            ollSolved -> "F2L và OLL xong (mặt ${bottomFace.opposite.symbol} đã cùng màu) — còn PLL."
            f2lSolved -> "F2L xong — tiếp theo là OLL."
            crossSolved -> "Cross ${bottomColor.displayName} xong, F2L $solvedSlots/4 slot."
            else -> "Chưa có Cross ${bottomColor.displayName}."
        }
}

/**
 * Human-method solver (CFOP): Cross -> F2L pairs -> OLL -> PLL, using the structured algorithm
 * library for F2L/OLL/PLL cases. Unlike Kociemba it produces steps a person can follow and
 * can answer partial requests ("next F2L pair only").
 */
class CfopSolver(
    algorithms: List<Algorithm>,
    private val preferredCrossColor: StickerColor = StickerColor.WHITE,
) : CubeSolver {
    override val cubeType = CubeType.CUBE_3X3
    override val supportedGoals = setOf(SolveGoal.SOLVE_ALL, SolveGoal.CROSS, SolveGoal.NEXT_F2L, SolveGoal.OLL, SolveGoal.PLL)

    private val library = CaseLibrary(algorithms)

    /** Algorithms that could not be used for recognition (wrong category/case); for diagnostics. */
    val rejectedAlgorithms: List<Pair<Algorithm, String>> get() = library.rejected
    internal val caseLibrary: CaseLibrary get() = library

    override fun warmUp() { CrossSolver.distance(CubieCube()) }

    fun analyze(state: CubeState): CfopAnalysis {
        val frames = Face.entries.map { it to Frame(state).apply(Frame.TO_BOTTOM.getValue(it)) }
        fun score(f: Frame): Int = when {
            f.solved -> 100
            f.ollSolved -> 50
            f.f2lSolved -> 40
            f.crossSolved -> 10 + f.solvedSlots.size
            else -> 0
        }
        val best = frames.maxWith(
            compareBy<Pair<Face, Frame>> { score(it.second) }
                .thenBy { if (it.second.color(Face.D) == preferredCrossColor) 1 else 0 }
                .thenBy { if (it.first == Face.D) 1 else 0 },
        )
        val (face, f) = best
        return CfopAnalysis(face, f.color(Face.D), f.crossSolved, f.solvedSlots.size, f.f2lSolved, f.ollSolved, f.solved)
    }

    override fun goalAvailability(state: CubeState): Map<SolveGoal, String?> {
        if (!CubeValidator.isValid(state)) return supportedGoals.associateWith { "Trạng thái cube không hợp lệ." }
        val a = analyze(state)
        return mapOf(
            SolveGoal.SOLVE_ALL to if (a.solved) "Cube đã được giải." else null,
            SolveGoal.CROSS to when {
                a.solved -> "Cube đã được giải."
                a.crossSolved -> "Cross ${a.bottomColor.displayName} đã hoàn thành — hãy chọn Next F2L."
                else -> null
            },
            SolveGoal.NEXT_F2L to when {
                !a.crossSolved -> "Cần hoàn thành Cross trước: chưa có mặt nào có dấu cộng khớp với các tâm bên cạnh."
                a.f2lSolved -> "F2L đã hoàn thành — hãy chọn OLL${if (a.ollSolved) "/PLL" else ""}."
                else -> null
            },
            SolveGoal.OLL to when {
                !a.f2lSolved -> "OLL chỉ dùng khi đã xong F2L (hai tầng đầu). Hiện tại: ${a.summary}"
                a.ollSolved -> "Mặt trên đã cùng màu — hãy chọn PLL."
                else -> null
            },
            SolveGoal.PLL to when {
                a.solved -> "Cube đã được giải."
                !a.ollSolved -> "PLL chỉ dùng khi mặt trên đã cùng màu (xong OLL). Hiện tại: ${a.summary}"
                else -> null
            },
        )
    }

    override fun solve(state: CubeState, goal: SolveGoal): SolveOutcome {
        val validation = CubeValidator.validate(state)
        if (!validation.isValid) return SolveOutcome.Failure(validation.errors.first().message)
        goalAvailability(state)[goal]?.let { return SolveOutcome.NotApplicable(it) }
        val start = System.nanoTime()
        val analysis = analyze(state)
        val steps = ArrayList<SolutionStep>()
        val rotation = Frame.TO_BOTTOM.getValue(analysis.bottomFace)
        var frame = Frame(state)
        if (rotation.isNotEmpty()) {
            steps += SolutionStep(
                "Cầm cube",
                rotation,
                "Xoay cả khối để mặt ${analysis.bottomColor.displayName} (mặt ${analysis.bottomFace.symbol}) xuống dưới.",
            )
            frame = frame.apply(rotation)
        }
        try {
            when (goal) {
                SolveGoal.CROSS -> steps += crossStep(frame)
                SolveGoal.NEXT_F2L -> steps += f2lStep(frame)!!.first
                SolveGoal.OLL -> steps += ollStep(frame)
                SolveGoal.PLL -> steps += pllStep(frame)
                SolveGoal.SOLVE_ALL -> {
                    if (!frame.crossSolved) { val s = crossStep(frame); steps += s; frame = frame.apply(s.moves) }
                    while (!frame.f2lSolved) {
                        val (s, next) = f2lStep(frame) ?: return SolveOutcome.Failure("Thư viện F2L thiếu trường hợp hiện tại.")
                        steps += s; frame = next
                    }
                    if (!frame.ollSolved) { val s = ollStep(frame); steps += s; frame = frame.apply(s.moves) }
                    if (!frame.solved) { val s = pllStep(frame); steps += s; frame = frame.apply(s.moves) }
                    check(frame.solved)
                }
            }
        } catch (e: MissingCaseException) {
            return SolveOutcome.Failure(e.message ?: "Không nhận diện được trường hợp.")
        }
        val kind = if (goal == SolveGoal.SOLVE_ALL) SolutionKind.HUMAN_FRIENDLY else SolutionKind.PARTIAL
        val ms = (System.nanoTime() - start) / 1_000_000
        return SolveOutcome.Success(listOf(Solution(cubeType, kind, steps, ms, note = analysis.summary)))
    }

    // ------------------------------------------------------------------ steps

    private fun crossStep(frame: Frame): SolutionStep {
        val moves = CrossSolver.solve(frame.cc)
        return SolutionStep(
            "Cross",
            moves,
            "Tạo dấu cộng ${frame.color(Face.D).displayName} ở mặt dưới, mỗi cạnh khớp màu với tâm bên cạnh (${moves.size} bước, tối ưu).",
        )
    }

    /** Best next F2L pair over all unsolved slots; returns the step and the resulting frame. */
    private fun f2lStep(frame: Frame): Pair<SolutionStep, Frame>? {
        var best: Triple<List<Move>, Int, Algorithm>? = null
        var bestCost = Int.MAX_VALUE
        for (slot in 0 until 4) {
            if (frame.slotSolved(slot)) continue
            val plan = planPair(frame, slot) ?: continue
            val cost = Notation.htm(plan.first) + if (slot != 0) 1 else 0
            if (cost < bestCost) { bestCost = cost; best = Triple(plan.first, slot, plan.second) }
        }
        val (moves, slot, alg) = best ?: throw MissingCaseException("Thư viện F2L thiếu trường hợp cho trạng thái hiện tại.")
        val target = frame.apply(Y_TO_FR[slot])
        val corner = listOf(Face.D, Face.F, Face.R).joinToString("-") { target.color(it).displayName }
        val edge = listOf(Face.F, Face.R).joinToString("-") { target.color(it).displayName }
        val after = frame.apply(moves)
        val step = SolutionStep(
            "F2L ${frame.solvedSlots.size + 1}",
            moves,
            "Ghép góc $corner với cạnh $edge vào slot ${Frame.SLOT_NAMES[slot]}" +
                (if (Y_TO_FR[slot].isNotEmpty()) " (xoay ${Notation.format(Y_TO_FR[slot])} để slot về phía trước-phải)" else "") +
                ". Trường hợp: ${alg.name}.",
        )
        return step to after
    }

    /** Moves (rotation + extraction + AUF + algorithm) that solve [slot] without breaking solved parts. */
    private fun planPair(frame: Frame, slot: Int): Pair<List<Move>, Algorithm>? {
        val rot = Y_TO_FR[slot]
        val moves = ArrayList<Move>(rot)
        var cur = frame.apply(rot)
        repeat(2) {
            if (CaseLibrary.f2lPlayable(cur.cc)) return@repeat
            val c = cur.cc
            val cornerPos = (0 until 8).first { c.cp[it] == CubieCube.DFR }
            val edgePos = (0 until 12).first { c.ep[it] == CubieCube.FR }
            val stuck = Frame.SLOT_CORNER.indexOf(cornerPos).takeIf { it > 0 } ?: Frame.SLOT_EDGE.indexOf(edgePos).takeIf { it > 0 } ?: return null
            val ext = Notation.parse(EXTRACT[stuck])
            moves += ext
            cur = cur.apply(ext)
        }
        if (!CaseLibrary.f2lPlayable(cur.cc)) return null
        var best: Pair<List<Move>, Algorithm>? = null
        for (a in 0 until 4) {
            val auf = CaseLibrary.uTurns(a)
            val alg = library.f2l[CaseLibrary.f2lKey(cur.apply(auf).cc)] ?: continue
            val total = Notation.simplify(moves + auf + alg.moves)
            if (best == null || Notation.htm(total) < Notation.htm(best.first)) best = total to alg
        }
        return best
    }

    private fun ollStep(frame: Frame): SolutionStep {
        var best: Pair<List<Move>, Algorithm>? = null
        for (a in 0 until 4) {
            val auf = CaseLibrary.uTurns(a)
            val alg = library.oll[CaseLibrary.ollKey(frame.apply(auf).cc)] ?: continue
            val total = Notation.simplify(auf + alg.moves)
            if (best == null || Notation.htm(total) < Notation.htm(best.first)) best = total to alg
        }
        val (moves, alg) = best ?: throw MissingCaseException("Không tìm thấy công thức OLL phù hợp trong thư viện.")
        return SolutionStep(
            "OLL",
            moves,
            "${alg.name}: định hướng để mặt trên cùng màu ${frame.color(Face.U).displayName}." +
                (if (alg.recognition.isNotBlank()) " Nhận diện: ${alg.recognition}" else ""),
        )
    }

    private fun pllStep(frame: Frame): SolutionStep {
        var best: Pair<List<Move>, Algorithm?>? = null
        for (a in 0 until 4) {
            val auf = CaseLibrary.uTurns(a)
            val f = frame.apply(auf)
            if (f.solved) {
                if (best == null || Notation.htm(auf) < Notation.htm(best.first)) best = auf to null
                continue
            }
            val (alg, b) = library.pll[CaseLibrary.pllKey(f.cc)] ?: continue
            val total = Notation.simplify(auf + alg.moves + CaseLibrary.uTurns(b))
            if (best == null || Notation.htm(total) < Notation.htm(best.first)) best = total to alg
        }
        val (moves, alg) = best ?: throw MissingCaseException("Không tìm thấy công thức PLL phù hợp trong thư viện.")
        return SolutionStep(
            "PLL",
            moves,
            if (alg == null) "Chỉ cần xoay mặt trên (AUF) để hoàn thành."
            else "${alg.name}: hoán vị các mảnh tầng trên về đúng chỗ." + (if (alg.recognition.isNotBlank()) " Nhận diện: ${alg.recognition}" else ""),
        )
    }

    private class MissingCaseException(message: String) : RuntimeException(message)

    private companion object {
        /** y rotation that brings slot FR/FL/BL/BR to the front-right. */
        val Y_TO_FR: List<List<Move>> = listOf("", "y'", "y2", "y").map { Notation.parse(it) }
        /** Moves that lift the pieces out of slot FL/BL/BR (index 1..3) while keeping cross and other slots. */
        val EXTRACT = arrayOf("", "L' U' L", "L U L'", "R' U' R")
    }
}
