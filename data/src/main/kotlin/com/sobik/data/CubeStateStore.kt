package com.sobik.data

import com.sobik.model.CubeState
import java.io.File

data class SavedCube(val state: CubeState, val savedAtMillis: Long)

/** Persists validated cube states (latest first). Only validated states should be saved. */
interface CubeStateStore {
    fun save(state: CubeState, nowMillis: Long = System.currentTimeMillis())
    fun latest(): SavedCube?
    fun history(): List<SavedCube>
    fun clear()
}

/**
 * Tiny line-based file store ("timestamp|3:WWW..."). A 3x3 entry is ~70 bytes, so even the full
 * history loads instantly without a database.
 */
class FileCubeStateStore(dir: File, private val maxEntries: Int = 30) : CubeStateStore {
    private val file = File(dir, "cube_states.txt")
    private val lock = Any()

    override fun save(state: CubeState, nowMillis: Long) = synchronized(lock) {
        val entries = (listOf(SavedCube(state, nowMillis)) + readAll().filter { it.state != state }).take(maxEntries)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(entries.joinToString("\n") { "${it.savedAtMillis}|${it.state.serialize()}" })
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        Unit
    }

    override fun latest(): SavedCube? = synchronized(lock) { readAll().firstOrNull() }
    override fun history(): List<SavedCube> = synchronized(lock) { readAll() }
    override fun clear() = synchronized(lock) { file.delete(); Unit }

    private fun readAll(): List<SavedCube> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val sep = line.indexOf('|')
            if (sep <= 0) return@mapNotNull null
            runCatching { SavedCube(CubeState.deserialize(line.substring(sep + 1)), line.substring(0, sep).toLong()) }.getOrNull()
        }
    }
}

/** In-memory store for previews and tests. */
class InMemoryCubeStateStore : CubeStateStore {
    private val items = ArrayList<SavedCube>()
    override fun save(state: CubeState, nowMillis: Long) { items.removeAll { it.state == state }; items.add(0, SavedCube(state, nowMillis)) }
    override fun latest() = items.firstOrNull()
    override fun history() = items.toList()
    override fun clear() = items.clear()
}
