package com.sobik.scanner.camerax

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.sobik.scanner.FaceSampler
import com.sobik.scanner.FaceSamples
import com.sobik.scanner.GuideRegion
import com.sobik.scanner.Yuv420Source
import java.util.concurrent.atomic.AtomicReference

/** Size of the preview view and of the square guide drawn on it, in view pixels. */
data class GuideSpec(val viewWidth: Float, val viewHeight: Float, val guideSizePx: Float)

/**
 * CameraX analyzer: reads only the sticker sample points from the YUV planes (no Bitmap, no
 * full-frame conversion) and reports [FaceSamples] at most every [minIntervalMs].
 * Runs on the analysis executor; [onSamples] must be thread-safe.
 */
class CubeFrameAnalyzer(
    private val gridSize: () -> Int,
    private val onSamples: (FaceSamples) -> Unit,
    private val minIntervalMs: Long = 90,
) : ImageAnalysis.Analyzer {
    private val sampler = FaceSampler()
    private val guide = AtomicReference<GuideSpec?>(null)
    private var lastMs = 0L

    fun updateGuide(spec: GuideSpec) = guide.set(spec)

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            val spec = guide.get() ?: return
            if (now - lastMs < minIntervalMs) return
            lastMs = now
            val rotation = image.imageInfo.rotationDegrees
            val uprightW = if (rotation % 180 == 0) image.width else image.height
            val uprightH = if (rotation % 180 == 0) image.height else image.width
            val region = GuideRegion.fromPreview(spec.viewWidth, spec.viewHeight, uprightW, uprightH, spec.guideSizePx)
            val planes = image.planes
            val source = Yuv420Source(
                width = image.width,
                height = image.height,
                y = planes[0].buffer,
                yRowStride = planes[0].rowStride,
                u = planes[1].buffer,
                v = planes[2].buffer,
                uvRowStride = planes[1].rowStride,
                uvPixelStride = planes[1].pixelStride,
            )
            onSamples(sampler.sample(source, rotation, region, gridSize()))
        } finally {
            image.close() // release the buffer immediately so CameraX can reuse it
        }
    }
}
