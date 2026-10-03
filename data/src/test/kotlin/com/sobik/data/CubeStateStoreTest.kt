package com.sobik.data

import com.sobik.model.CubeState
import com.sobik.model.CubeType
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CubeStateStoreTest {
    @Test
    fun `saves and reloads states newest first`() {
        val dir = Files.createTempDirectory("store").toFile()
        val store = FileCubeStateStore(dir)
        assertNull(store.latest())
        val a = CubeState.solved(CubeType.CUBE_3X3)
        val b = CubeState.solved(CubeType.CUBE_2X2)
        store.save(a, 1); store.save(b, 2); store.save(a, 3)
        val reloaded = FileCubeStateStore(dir)
        assertEquals(listOf(a, b), reloaded.history().map { it.state })
        assertEquals(3L, reloaded.latest()!!.savedAtMillis)
        reloaded.clear()
        assertNull(FileCubeStateStore(dir).latest())
    }
}
