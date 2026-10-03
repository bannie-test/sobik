package com.sobik.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sobik.app.AppContainer
import com.sobik.app.ScanOrigin
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.StickerScan
import com.sobik.scanner.CaptureSignal
import com.sobik.scanner.FaceCapture
import com.sobik.scanner.FaceSamples
import com.sobik.scanner.FaceStep
import com.sobik.scanner.ScanMode
import com.sobik.scanner.ScanSession
import com.sobik.scanner.createStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val step: FaceStep?,
    val stepIndex: Int,
    val totalSteps: Int,
    val live: List<StickerScan> = emptyList(),
    val signal: CaptureSignal = CaptureSignal.Idle,
    val captures: Map<Face, FaceCapture> = emptyMap(),
    val lastCapture: FaceCapture? = null,
    val captureCount: Int = 0,
    val complete: Boolean = false,
    val torch: Boolean = false,
)

/**
 * Owns the [ScanSession]. Frames arrive on the camera analysis thread via [onSamples]; all
 * session access is serialized with [lock], UI state is published through a StateFlow.
 */
class ScanViewModel(
    private val container: AppContainer,
    private val sessionId: Long,
    val type: CubeType,
    val mode: ScanMode,
    private val intervalMs: Long,
) : ViewModel() {
    private val lock = Any()
    private val session = ScanSession(type)
    private val strategy = mode.createStrategy(intervalMs)
    private var latest: FaceSamples? = null
    /** Frame used by the last capture; a double tap must not store the same frame as the next face. */
    private var lastCaptured: FaceSamples? = null

    private val _ui = MutableStateFlow(ScanUiState(session.currentStep, 0, session.steps.size))
    val ui: StateFlow<ScanUiState> = _ui.asStateFlow()

    init {
        // Build solver tables while the user is scanning, so "Solve" is instant afterwards.
        viewModelScope.launch(Dispatchers.Default) { container.solverFor(type)?.warmUp() }
    }

    fun onSamples(samples: FaceSamples) = synchronized(lock) {
        if (session.isComplete) return@synchronized
        latest = samples
        val now = System.currentTimeMillis()
        val signal = strategy.onFrame(samples, now)
        if (signal is CaptureSignal.Capture) {
            capture(samples, now)
        } else {
            val live = session.preview(samples)
            _ui.update { it.copy(live = live, signal = signal) }
        }
    }

    fun captureNow() = synchronized(lock) {
        val frame = latest
        if (session.isComplete || frame == null || frame === lastCaptured) return@synchronized
        capture(frame, System.currentTimeMillis())
    }

    private fun capture(samples: FaceSamples, now: Long) {
        val cap = session.capture(samples)
        lastCaptured = samples
        strategy.onCaptured(samples, now)
        publish(lastCapture = cap)
    }

    fun retake(face: Face) = synchronized(lock) {
        session.retake(face)
        strategy.reset(System.currentTimeMillis())
        publish(lastCapture = null)
    }

    fun toggleTorch() = _ui.update { it.copy(torch = !it.torch) }

    private fun publish(lastCapture: FaceCapture?) {
        _ui.update {
            it.copy(
                step = session.currentStep,
                stepIndex = session.stepIndex,
                captures = Face.entries.mapNotNull { f -> session.captureOf(f)?.let { c -> f to c } }.toMap(),
                lastCapture = lastCapture,
                captureCount = it.captureCount + if (lastCapture != null) 1 else 0,
                complete = session.isComplete,
                signal = CaptureSignal.Idle,
            )
        }
    }

    /** Classifies all faces and hands the result to the review screen. */
    fun finish(): Boolean = synchronized(lock) {
        if (!session.isComplete) return@synchronized false
        container.cubeSession.submitScan(session.result(), ScanOrigin(sessionId, mode, intervalMs))
        true
    }
}
