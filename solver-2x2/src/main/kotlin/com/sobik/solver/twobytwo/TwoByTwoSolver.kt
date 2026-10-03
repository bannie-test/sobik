package com.sobik.solver.twobytwo

import com.sobik.engine.CornerCube
import com.sobik.engine.CubeLabeling
import com.sobik.engine.CubeValidator
import com.sobik.engine.CubieCube
import com.sobik.model.CubeSolver
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.Move
import com.sobik.model.SolutionKind
import com.sobik.model.SolutionStep
import com.sobik.model.SolveGoal
import com.sobik.model.SolveOutcome
import com.sobik.model.Solution

/**
 * Optimal 2x2 solver (half-turn metric).
 *
 * The DBL corner is kept fixed, so only U, R and F turns are needed and the state space is
 * 7! * 3^6 = 3,674,160. IDA* with two exact sub-problem distance tables (permutation: 5040
 * entries, orientation: 729 entries, < 6 KB total) finds optimal solutions (max 11 moves)
 * in milliseconds without the 3.6 MB full distance table.
 */
class TwoByTwoSolver : CubeSolver {
    override val cubeType = CubeType.CUBE_2X2
    override val supportedGoals = setOf(SolveGoal.SOLVE_ALL)

    private val tables by lazy { Tables() }

    override fun warmUp() { tables }

    override fun solve(state: CubeState, goal: SolveGoal): SolveOutcome {
        if (goal != SolveGoal.SOLVE_ALL) return SolveOutcome.NotApplicable("Cube 2x2 chỉ hỗ trợ giải toàn bộ.")
        val validation = CubeValidator.validate(state)
        if (!validation.isValid) return SolveOutcome.Failure(validation.errors.first().message)
        val cc = CubeLabeling.toCornerCube(state) ?: return SolveOutcome.Failure("Không đọc được trạng thái cube.")
        val start = System.nanoTime()
        val moves = solveCorners(cc) ?: return SolveOutcome.Failure("Không tìm được lời giải — có thể trạng thái quét bị sai.")
        val ms = (System.nanoTime() - start) / 1_000_000
        val solution = Solution(
            cubeType = cubeType,
            kind = SolutionKind.OPTIMAL,
            steps = listOf(SolutionStep("Giải 2x2", moves, "Lời giải tối ưu ${moves.size} bước (giữ cố định góc DBL)")),
            computeTimeMs = ms,
            isOptimal = true,
        )
        return SolveOutcome.Success(listOf(solution))
    }

    /** Optimal move sequence for a corner state whose DBL corner is solved; null if unsolvable. */
    fun solveCorners(cc: CornerCube): List<Move>? {
        require(cc.cp[CubieCube.DBL] == CubieCube.DBL && cc.co[CubieCube.DBL] == 0) { "DBL corner must be solved" }
        val t = tables
        val perm = t.permCoord(cc)
        val twist = t.twistCoord(cc)
        val path = IntArray(MAX_DEPTH)
        val h0 = maxOf(t.permPrune[perm].toInt(), t.twistPrune[twist].toInt())
        for (depth in h0..MAX_DEPTH) {
            if (search(perm, twist, 0, depth, -1, path)) {
                return (0 until depth).map { Move.of(FACES[path[it] / 3], path[it] % 3 + 1) }
            }
        }
        return null
    }

    private fun search(perm: Int, twist: Int, depth: Int, togo: Int, lastFace: Int, path: IntArray): Boolean {
        if (togo == 0) return perm == 0 && twist == 0
        val t = tables
        for (f in 0 until 3) {
            if (f == lastFace) continue
            for (p in 0 until 3) {
                val m = f * 3 + p
                val np = t.permMove[perm * N_MOVES + m].toInt()
                val nt = t.twistMove[twist * N_MOVES + m].toInt()
                if (t.permPrune[np] >= togo || t.twistPrune[nt] >= togo) continue
                path[depth] = m
                if (search(np, nt, depth + 1, togo - 1, f, path)) return true
            }
        }
        return false
    }

    internal class Tables {
        val permMove = ShortArray(N_PERM * N_MOVES)
        val twistMove = ShortArray(N_TWIST * N_MOVES)
        val permPrune = ByteArray(N_PERM) { -1 }
        val twistPrune = ByteArray(N_TWIST) { -1 }

        init {
            for (i in 0 until N_PERM) for (m in 0 until N_MOVES) {
                val c = permCube(i); repeat(m % 3 + 1) { c.multiply(CornerCube.MOVES[FACES[m / 3].ordinal]) }
                permMove[i * N_MOVES + m] = permCoord(c).toShort()
            }
            for (i in 0 until N_TWIST) for (m in 0 until N_MOVES) {
                val c = twistCube(i); repeat(m % 3 + 1) { c.multiply(CornerCube.MOVES[FACES[m / 3].ordinal]) }
                twistMove[i * N_MOVES + m] = twistCoord(c).toShort()
            }
            bfs(permPrune, permMove, N_PERM)
            bfs(twistPrune, twistMove, N_TWIST)
        }

        private fun bfs(prune: ByteArray, move: ShortArray, size: Int) {
            prune[0] = 0
            var depth = 0; var filled = 1
            while (filled < size) {
                for (i in 0 until size) {
                    if (prune[i].toInt() != depth) continue
                    for (m in 0 until N_MOVES) {
                        val j = move[i * N_MOVES + m].toInt()
                        if (prune[j] < 0) { prune[j] = (depth + 1).toByte(); filled++ }
                    }
                }
                depth++
            }
        }

        fun permCoord(c: CornerCube): Int {
            val p = IntArray(7) { SLOT[c.cp[POS[it]]] }
            var r = 0
            for (i in 0 until 7) {
                var smaller = 0
                for (j in i + 1 until 7) if (p[j] < p[i]) smaller++
                r = r * (7 - i) + smaller
            }
            return r
        }

        fun twistCoord(c: CornerCube): Int {
            var r = 0
            for (i in 0 until 6) r = r * 3 + c.co[POS[i]]
            return r
        }

        private fun permCube(rank: Int): CornerCube {
            val digits = IntArray(7)
            var r = rank
            for (i in 6 downTo 0) { digits[i] = r % (7 - i); r /= (7 - i) }
            val avail = (0 until 7).toMutableList()
            val c = CornerCube()
            for (i in 0 until 7) c.cp[POS[i]] = POS[avail.removeAt(digits[i])]
            return c
        }

        private fun twistCube(rank: Int): CornerCube {
            val c = CornerCube()
            var r = rank; var sum = 0
            for (i in 5 downTo 0) { c.co[POS[i]] = r % 3; sum += r % 3; r /= 3 }
            c.co[POS[6]] = (3 - sum % 3) % 3
            return c
        }
    }

    private companion object {
        const val N_PERM = 5040
        const val N_TWIST = 729
        const val N_MOVES = 9
        const val MAX_DEPTH = 14
        val FACES = arrayOf(Face.U, Face.R, Face.F)
        /** Corner positions that move (all but DBL) and their slot index. */
        val POS = intArrayOf(0, 1, 2, 3, 4, 5, 7)
        val SLOT = intArrayOf(0, 1, 2, 3, 4, 5, -1, 6)
    }
}
