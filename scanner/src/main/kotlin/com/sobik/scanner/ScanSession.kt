package com.sobik.scanner

import com.sobik.model.ColorScheme
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.ScanResult
import com.sobik.model.StickerColor
import com.sobik.model.StickerScan

/** One step of the guided scan: which face to show and how to hold the cube. */
data class FaceStep(
    val face: Face,
    val title: String,
    val instruction: String,
    /** Center color when the cube is held as recommended with the standard color scheme. */
    val expectedColor: StickerColor,
)

object ScanPlan {
    /**
     * Scan order F, R, B, L, U, D: turn the cube to the left between the four side faces (top
     * stays up), then tilt for top and bottom. This yields exactly the sticker orientation of
     * [Face]'s documented layout, so no per-face rotation fix-up is needed.
     */
    fun steps(type: CubeType): List<FaceStep> {
        val s = ColorScheme.STANDARD
        val hold = if (type.hasFixedCenters) "Cầm cube với tâm trắng ở trên và tâm xanh lá hướng vào camera."
        else "Chọn một cách cầm (ví dụ góc trắng-xanh lá-đỏ ở trên-trước-phải) và giữ nguyên trong suốt quá trình quét."
        return listOf(
            FaceStep(Face.F, "Mặt trước (F)", "$hold Đưa mặt trước vừa khít khung.", s.colorOf(Face.F)),
            FaceStep(Face.R, "Mặt phải (R)", "Xoay cả khối sang trái 90° (mặt trên vẫn ở trên) để mặt phải hướng vào camera.", s.colorOf(Face.R)),
            FaceStep(Face.B, "Mặt sau (B)", "Tiếp tục xoay cả khối sang trái 90°.", s.colorOf(Face.B)),
            FaceStep(Face.L, "Mặt trái (L)", "Tiếp tục xoay cả khối sang trái 90°.", s.colorOf(Face.L)),
            FaceStep(Face.U, "Mặt trên (U)", "Xoay sang trái thêm 90° để mặt trước quay lại, rồi lật cube về phía bạn để mặt trên hướng vào camera (mặt trước nằm ở cạnh dưới khung).", s.colorOf(Face.U)),
            FaceStep(Face.D, "Mặt dưới (D)", "Lật cube 180° để mặt dưới hướng vào camera (mặt trước nằm ở cạnh trên khung).", s.colorOf(Face.D)),
        )
    }
}

/** A captured face: only small color samples are kept, never the camera image. */
data class FaceCapture(
    val face: Face,
    val samples: List<Lab>,
    val preview: List<StickerScan>,
    val warning: String? = null,
)

/**
 * State machine for scanning a cube face by face. UI-agnostic: feed it [FaceSamples] from any
 * camera pipeline (or tests) and read back instructions, previews and the final [ScanResult].
 */
class ScanSession(val type: CubeType, private val classifier: ColorClassifier = ColorClassifier()) {
    init {
        require(type.supportsScan) { "Scanning ${type.label} is not supported yet" }
    }

    val steps: List<FaceStep> = ScanPlan.steps(type)
    private val captures = arrayOfNulls<FaceCapture>(6)

    var stepIndex: Int = 0
        private set

    val currentStep: FaceStep? get() = steps.getOrNull(stepIndex)
    val isComplete: Boolean get() = captures.all { it != null }
    val capturedFaces: List<FaceCapture> get() = captures.filterNotNull()
    fun captureOf(face: Face): FaceCapture? = captures[face.ordinal]

    /** References for live preview: palette, recalibrated with centers already captured (3x3). */
    fun previewReferences(): Map<StickerColor, Lab> {
        val refs = ColorClassifier.DEFAULT_PALETTE.toMutableMap()
        if (type.hasFixedCenters) for (c in captures.filterNotNull()) {
            val center = type.stickersPerFace / 2
            refs[c.preview[center].color] = c.samples[center]
        }
        return refs
    }

    fun preview(samples: FaceSamples): List<StickerScan> =
        classifier.classifyPreview(samples.cells.map { it.lab }, previewReferences())

    /** Stores the samples for the current step and advances. */
    fun capture(samples: FaceSamples): FaceCapture {
        val step = requireNotNull(currentStep) { "All faces already captured" }
        require(samples.n == type.size)
        val labs = samples.cells.map { it.lab }
        val preview = classifier.classifyPreview(labs, previewReferences())
        var warning: String? = null
        if (type.hasFixedCenters) {
            val center = preview[type.stickersPerFace / 2].color
            val dup = captures.filterNotNull().firstOrNull { it.face != step.face && it.preview[type.stickersPerFace / 2].color == center }
            if (dup != null) warning = "Tâm mặt này giống mặt ${dup.face.symbol} (${center.displayName}) — có thể bạn đã quét trùng mặt."
            else if (center != step.expectedColor) warning = "Tâm đọc được là ${center.displayName} (thường là ${step.expectedColor.displayName}). Nếu bạn cầm cube khác hướng dẫn thì không sao."
        }
        val cap = FaceCapture(step.face, labs, preview, warning)
        captures[step.face.ordinal] = cap
        stepIndex = steps.indexOfFirst { captures[it.face.ordinal] == null }.let { if (it < 0) steps.size else it }
        return cap
    }

    /** Re-scan one face. */
    fun retake(face: Face) {
        captures[face.ordinal] = null
        stepIndex = steps.indexOfFirst { it.face == face }
    }

    fun reset() {
        captures.fill(null)
        stepIndex = 0
    }

    /** Final classification of all faces (global calibration). */
    fun result(): ScanResult {
        check(isComplete) { "Scan is not complete" }
        return classifier.classifyCube(type, Face.entries.map { captures[it.ordinal]!!.samples })
    }
}
