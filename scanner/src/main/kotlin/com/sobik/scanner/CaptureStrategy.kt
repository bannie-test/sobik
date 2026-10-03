package com.sobik.scanner

/** Available scan modes. New modes only need a new [CaptureStrategy]. */
enum class ScanMode(val displayName: String, val description: String) {
    MANUAL("Thủ công", "Bấm nút chụp cho từng mặt."),
    TIMED("Hẹn giờ", "Tự chụp sau mỗi vài giây — chỉ cần giữ đúng mặt trước camera."),
    AUTO("Tự động", "Tự chụp khi cube đứng yên và đã chuyển sang mặt mới."),
}

sealed interface CaptureSignal {
    /** Waiting for the user (manual mode) — nothing to show. */
    data object Idle : CaptureSignal
    data class Countdown(val remainingMs: Long) : CaptureSignal
    data class Waiting(val reason: String) : CaptureSignal
    data object Capture : CaptureSignal
}

/** Decides, frame by frame, when a face should be captured. */
interface CaptureStrategy {
    fun onFrame(frame: FaceSamples, nowMs: Long): CaptureSignal
    fun onCaptured(frame: FaceSamples, nowMs: Long)
    fun reset(nowMs: Long)
}

fun ScanMode.createStrategy(timedIntervalMs: Long = 3000): CaptureStrategy = when (this) {
    ScanMode.MANUAL -> ManualCaptureStrategy()
    ScanMode.TIMED -> TimedCaptureStrategy(timedIntervalMs)
    ScanMode.AUTO -> AutoCaptureStrategy()
}

class ManualCaptureStrategy : CaptureStrategy {
    override fun onFrame(frame: FaceSamples, nowMs: Long): CaptureSignal =
        frame.quality.hint?.let { CaptureSignal.Waiting(it) } ?: CaptureSignal.Idle
    override fun onCaptured(frame: FaceSamples, nowMs: Long) {}
    override fun reset(nowMs: Long) {}
}

class TimedCaptureStrategy(private val intervalMs: Long = 3000) : CaptureStrategy {
    private var startMs = -1L

    override fun onFrame(frame: FaceSamples, nowMs: Long): CaptureSignal {
        if (startMs < 0) startMs = nowMs
        val remaining = intervalMs - (nowMs - startMs)
        if (remaining > 0) return CaptureSignal.Countdown(remaining)
        return if (frame.quality.isGood) CaptureSignal.Capture else CaptureSignal.Waiting(frame.quality.hint ?: "Đang chờ ảnh rõ hơn…")
    }

    override fun onCaptured(frame: FaceSamples, nowMs: Long) { startMs = nowMs }
    override fun reset(nowMs: Long) { startMs = nowMs }
}

/**
 * Captures when the grid colors have been stable for [stableMs] and differ from the previously
 * captured face (so the same face is not captured twice while the user is turning the cube).
 */
class AutoCaptureStrategy(
    private val stableMs: Long = 700,
    private val stabilityThreshold: Float = 6f,
    private val changeThreshold: Float = 15f,
) : CaptureStrategy {
    private var previous: FaceSamples? = null
    private var lastCaptured: FaceSamples? = null
    private var stableSince = -1L

    override fun onFrame(frame: FaceSamples, nowMs: Long): CaptureSignal {
        val prev = previous
        previous = frame
        if (!frame.quality.isGood || frame.quality.isBlurry) {
            stableSince = -1
            return CaptureSignal.Waiting(frame.quality.hint ?: "Đưa mặt cube vào khung.")
        }
        lastCaptured?.let {
            if (FaceSampler.difference(frame, it) < changeThreshold) {
                stableSince = -1
                return CaptureSignal.Waiting("Xoay cube sang mặt tiếp theo.")
            }
        }
        if (prev == null || FaceSampler.difference(frame, prev) > stabilityThreshold) {
            stableSince = -1
            return CaptureSignal.Waiting("Giữ yên cube…")
        }
        if (stableSince < 0) stableSince = nowMs
        val remaining = stableMs - (nowMs - stableSince)
        return if (remaining <= 0) CaptureSignal.Capture else CaptureSignal.Countdown(remaining)
    }

    override fun onCaptured(frame: FaceSamples, nowMs: Long) {
        lastCaptured = frame
        stableSince = -1
    }

    override fun reset(nowMs: Long) {
        previous = null
        lastCaptured = null
        stableSince = -1
    }
}
