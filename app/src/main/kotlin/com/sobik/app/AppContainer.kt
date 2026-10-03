package com.sobik.app

import com.sobik.content.ContentRepository
import com.sobik.data.CubeStateStore
import com.sobik.data.FileCubeStateStore
import com.sobik.model.CubeSolver
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.ScanResult
import com.sobik.model.StickerScan
import com.sobik.scanner.ScanMode
import com.sobik.solver.threebythree.ThreeByThreeSolver
import com.sobik.solver.twobytwo.TwoByTwoSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manual dependency container (no DI framework: fewer classes to load, faster startup).
 * Everything heavy is lazy: solver tables are only built when a solver is first used.
 */
class AppContainer(filesDir: File) {
    val content: ContentRepository by lazy { ContentRepository() }
    val store: CubeStateStore by lazy { FileCubeStateStore(filesDir) }
    val cubeSession: CubeSessionRepository by lazy { CubeSessionRepository(store) }

    private val solvers = HashMap<CubeType, CubeSolver>()

    /** Solver registry: add new cube types here without touching UI code. */
    fun solverFor(type: CubeType): CubeSolver? = synchronized(solvers) {
        solvers.getOrPut(type) {
            when (type) {
                CubeType.CUBE_2X2 -> TwoByTwoSolver()
                CubeType.CUBE_3X3 -> ThreeByThreeSolver(content.algorithms3x3)
                else -> return null
            }
        }
    }
}

/** Identifies the camera session a scan came from, so a single face can be re-scanned. */
data class ScanOrigin(val sessionId: Long, val mode: ScanMode, val intervalMs: Long)

/**
 * Hand-off between scan -> review -> solve. The validated [CubeState] is the source of truth;
 * it is persisted only after validation succeeded.
 */
class CubeSessionRepository(private val store: CubeStateStore) {
    private val _pendingScan = MutableStateFlow<ScanResult?>(null)
    val pendingScan: StateFlow<ScanResult?> = _pendingScan.asStateFlow()

    private val _current = MutableStateFlow<CubeState?>(null)
    val current: StateFlow<CubeState?> = _current.asStateFlow()
    private var loaded = false

    /** Loads the last saved cube (call off the main thread). */
    fun loadSaved() {
        if (loaded) return
        loaded = true
        if (_current.value == null) _current.value = store.latest()?.state
    }

    /** Where the pending scan came from; null for manual input. */
    var pendingOrigin: ScanOrigin? = null
        private set

    fun submitScan(result: ScanResult, origin: ScanOrigin) {
        pendingOrigin = origin
        _pendingScan.value = result
    }

    /** Manual input: start from a solved cube where every sticker is "confirmed". */
    fun startManual(type: CubeType, from: CubeState? = null) {
        val state = from?.takeIf { it.type == type } ?: CubeState.solved(type)
        pendingOrigin = null
        _pendingScan.value = ScanResult(type, List(type.stickerCount) { StickerScan(state.colorAt(it), 1f) })
    }

    /** Called only with a validated state. */
    fun confirm(state: CubeState) {
        store.save(state)
        _current.value = state
    }

    fun select(state: CubeState) { _current.value = state }
}
