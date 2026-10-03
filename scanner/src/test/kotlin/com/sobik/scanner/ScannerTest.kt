package com.sobik.scanner

import com.sobik.engine.CubeValidator
import com.sobik.engine.applyMoves
import com.sobik.model.CubeState
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.model.StickerColor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScannerTest {
    private val rgb = mapOf(
        StickerColor.WHITE to intArrayOf(235, 235, 230),
        StickerColor.YELLOW to intArrayOf(250, 215, 10),
        StickerColor.GREEN to intArrayOf(0, 155, 72),
        StickerColor.BLUE to intArrayOf(0, 70, 173),
        StickerColor.RED to intArrayOf(196, 30, 45),
        StickerColor.ORANGE to intArrayOf(255, 100, 0),
    )

    private fun shade(c: StickerColor, light: Float, rnd: Random): Int {
        val v = rgb.getValue(c)
        fun ch(x: Int) = (x * light + rnd.nextInt(-8, 9)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(v[0]) shl 16) or (ch(v[1]) shl 8) or ch(v[2])
    }

    /** Renders an upright face (stickers with black gaps on gray) and returns a 90°-rotated sensor image. */
    private fun sensorImage(colors: List<StickerColor>, n: Int, light: Float, rnd: Random): IntArraySource {
        val uw = 480; val uh = 640
        val upright = IntArray(uw * uh) { 0xFF808080.toInt() }
        val side = 0.6f * uw; val left = (uw - side) / 2; val top = (uh - side) / 2; val cell = side / n
        for (y in 0 until uh) for (x in 0 until uw) {
            val fx = (x - left) / cell; val fy = (y - top) / cell
            if (fx < 0 || fy < 0 || fx >= n || fy >= n) continue
            val inner = fx - fx.toInt() in 0.08f..0.92f && fy - fy.toInt() in 0.08f..0.92f
            upright[y * uw + x] = if (inner) shade(colors[fy.toInt() * n + fx.toInt()], light, rnd) else 0xFF101010.toInt()
        }
        val w = uh; val h = uw // sensor is landscape
        val sensor = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) sensor[y * w + x] = upright[x * uw + (h - 1 - y)]
        return IntArraySource(w, h, sensor)
    }

    @Test
    fun `sampler reads a rotated sensor frame in display orientation`() {
        val rnd = Random(1)
        val colors = listOf(
            StickerColor.RED, StickerColor.ORANGE, StickerColor.WHITE,
            StickerColor.YELLOW, StickerColor.GREEN, StickerColor.BLUE,
            StickerColor.ORANGE, StickerColor.RED, StickerColor.YELLOW,
        )
        val img = sensorImage(colors, 3, 1f, rnd)
        val samples = FaceSampler().sample(img, 90, GuideRegion(size = 0.6f), 3)
        val preview = ColorClassifier().classifyPreview(samples.cells.map { it.lab })
        assertEquals(colors, preview.map { it.color })
        assertTrue(samples.quality.isGood, samples.quality.toString())
        assertTrue(!samples.quality.isBlurry)
    }

    private fun scanCube(state: CubeState, rnd: Random, lights: List<Float>): ScanSession {
        val session = ScanSession(state.type)
        val sampler = FaceSampler()
        while (!session.isComplete) {
            val face = session.currentStep!!.face
            val img = sensorImage(state.faceColors(face), state.size, lights[face.ordinal], rnd)
            session.capture(sampler.sample(img, 90, GuideRegion(size = 0.6f), state.size))
        }
        return session
    }

    @Test
    fun `full 3x3 scan under uneven light is recognized and valid`() {
        val rnd = Random(3)
        repeat(5) { k ->
            val state = CubeState.solved(CubeType.CUBE_3X3).applyMoves("R U2 F' L D2 B R' U F2 L' D B2 U' R2 F" + if (k % 2 == 0) " M x" else "")
            val result = scanCube(state, rnd, listOf(1f, 0.85f, 0.95f, 0.7f, 0.8f, 0.9f)).result()
            assertEquals(state, result.state)
            assertTrue(CubeValidator.validate(result.state).isValid)
        }
    }

    @Test
    fun `full 2x2 scan is recognized without centers`() {
        val rnd = Random(4)
        repeat(5) {
            val state = CubeState.solved(CubeType.CUBE_2X2).applyMoves("R U' F2 R' U F' R2 U2 y")
            val result = scanCube(state, rnd, listOf(0.9f, 0.8f, 1f, 0.75f, 0.85f, 0.95f)).result()
            assertEquals(state, result.state)
        }
    }

    @Test
    fun `ambiguous samples get low confidence`() {
        val labs = Face.entries.map { f -> List(9) { ColorMath.rgbToLab(rgb.getValue(StickerColor.entries[f.ordinal])[0], rgb.getValue(StickerColor.entries[f.ordinal])[1], rgb.getValue(StickerColor.entries[f.ordinal])[2]) } }.toMutableList()
        // one sticker exactly between red and orange
        val between = ColorMath.rgbToLab(226, 65, 22)
        labs[0] = labs[0].toMutableList().also { it[0] = between }
        val r = ColorClassifier().classifyCube(CubeType.CUBE_3X3, labs)
        assertTrue(r.stickers[0].confidence < 0.6f, "confidence ${r.stickers[0].confidence}")
        assertTrue(r.stickers.drop(1).filter { it.confidence < 0.45f }.size <= 2)
    }

    @Test
    fun `capture strategies`() {
        val rnd = Random(5)
        val sampler = FaceSampler()
        val a = sampler.sample(sensorImage(List(9) { StickerColor.GREEN }, 3, 1f, rnd), 90, GuideRegion(size = 0.6f), 3)
        val b = sampler.sample(sensorImage(List(9) { StickerColor.RED }, 3, 1f, rnd), 90, GuideRegion(size = 0.6f), 3)

        val timed = TimedCaptureStrategy(3000)
        assertIs<CaptureSignal.Countdown>(timed.onFrame(a, 0))
        assertIs<CaptureSignal.Capture>(timed.onFrame(a, 3100))
        timed.onCaptured(a, 3100)
        assertIs<CaptureSignal.Countdown>(timed.onFrame(a, 3200))

        val auto = AutoCaptureStrategy(stableMs = 500)
        assertIs<CaptureSignal.Waiting>(auto.onFrame(a, 0))
        assertIs<CaptureSignal.Countdown>(auto.onFrame(a, 100))
        assertIs<CaptureSignal.Capture>(auto.onFrame(a, 700))
        auto.onCaptured(a, 700)
        assertIs<CaptureSignal.Waiting>(auto.onFrame(a, 800)) // same face: wait for a new one
        auto.onFrame(b, 900)
        assertIs<CaptureSignal.Countdown>(auto.onFrame(b, 1000))
        assertIs<CaptureSignal.Capture>(auto.onFrame(b, 1600))
    }

    @Test
    fun `dark frame is reported`() {
        val rnd = Random(6)
        val dark = FaceSampler().sample(sensorImage(List(9) { StickerColor.BLUE }, 3, 0.15f, rnd), 90, GuideRegion(size = 0.6f), 3)
        assertTrue(dark.quality.isTooDark || dark.quality.hint != null)
    }

    @Test
    fun `guide mapping from preview`() {
        val g = GuideRegion.fromPreview(1080f, 1920f, 480, 640, 756f)
        assertEquals(0.525f, g.size, 0.001f)
    }
}

class AutoCaptureResetTest {
    @Test
    fun `retaking the face just captured is possible after reset`() {
        val cell = CellSample(Lab(50f, 40f, 30f), 1f)
        val face = FaceSamples(3, List(9) { cell }, FrameQuality(50f, 1f, 20f, 0f))
        val auto = AutoCaptureStrategy(stableMs = 100)
        auto.onFrame(face, 0); auto.onFrame(face, 10)
        assertIs<CaptureSignal.Capture>(auto.onFrame(face, 200))
        auto.onCaptured(face, 200)
        assertIs<CaptureSignal.Waiting>(auto.onFrame(face, 300))
        auto.reset(300)
        auto.onFrame(face, 310); auto.onFrame(face, 320)
        assertIs<CaptureSignal.Capture>(auto.onFrame(face, 500))
    }
}
