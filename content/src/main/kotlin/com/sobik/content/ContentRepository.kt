package com.sobik.content

import com.sobik.model.Algorithm
import com.sobik.model.AlgorithmCategory
import com.sobik.model.BeginnerLesson
import com.sobik.model.CubeGuide
import com.sobik.model.PuzzleBrand
import com.sobik.model.PuzzleProduct
import com.sobik.model.SourceLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AlgorithmFile(val version: Int, val algorithms: List<Algorithm>)

@Serializable
internal data class LessonFile(val version: Int, val title: String, val lessons: List<BeginnerLesson>)

@Serializable
internal data class PuzzleFile(
    val version: Int,
    val disclaimer: String = "",
    val sources: List<SourceLink> = emptyList(),
    val aliases: Map<String, String> = emptyMap(),
    val brands: List<PuzzleBrand>,
    val puzzles: List<PuzzleProduct>,
)

@Serializable
internal data class GuideFile(val version: Int, val guides: List<CubeGuide>, val algorithms: List<Algorithm> = emptyList())

/**
 * Read-only access to bundled learning content (JSON files under `/content` on the classpath).
 * Content is data, not code: editing the JSON (or later downloading a newer version) changes
 * guides and the algorithm library without touching UI or solvers.
 */
class ContentRepository(private val source: ContentSource = ClasspathContentSource) {
    private val json = Json { ignoreUnknownKeys = true }

    val algorithms3x3: List<Algorithm> by lazy {
        json.decodeFromString<AlgorithmFile>(source.read("algorithms_3x3.json")).algorithms
    }

    private val lessonFile: LessonFile by lazy { json.decodeFromString(source.read("beginner_3x3.json")) }
    val beginnerTitle: String get() = lessonFile.title
    val beginnerLessons: List<BeginnerLesson> by lazy { lessonFile.lessons.sortedBy { it.order } }

    private val guideFile: GuideFile by lazy { json.decodeFromString(source.read("big_cubes.json")) }
    val bigCubeGuides: List<CubeGuide> get() = guideFile.guides
    val bigCubeAlgorithms: List<Algorithm> get() = guideFile.algorithms

    private val puzzleFile: PuzzleFile by lazy { json.decodeFromString(source.read("puzzles.json")) }
    val puzzleBrands: List<PuzzleBrand> get() = puzzleFile.brands
    val puzzles: List<PuzzleProduct> get() = puzzleFile.puzzles
    val puzzleDisclaimer: String get() = puzzleFile.disclaimer
    val puzzleSources: List<SourceLink> get() = puzzleFile.sources
    fun brand(id: String): PuzzleBrand? = puzzleBrands.firstOrNull { it.id == id }

    /** Finds a brand by name, id or alias ("xman", "yongjun", "rubik's cube"...). */
    fun findBrand(name: String): PuzzleBrand? {
        val key = name.trim().lowercase()
        val id = puzzleFile.aliases[key] ?: key
        return puzzleBrands.firstOrNull { it.id == id || it.name.equals(name.trim(), ignoreCase = true) }
    }

    val allAlgorithms: List<Algorithm> get() = algorithms3x3 + bigCubeAlgorithms

    fun algorithm(id: String): Algorithm? = allAlgorithms.firstOrNull { it.id == id }
    fun algorithms(category: AlgorithmCategory): List<Algorithm> = allAlgorithms.filter { it.category == category }
    fun lesson(id: String): BeginnerLesson? = beginnerLessons.firstOrNull { it.id == id }
    fun guide(cubeSize: Int): CubeGuide? = bigCubeGuides.firstOrNull { it.cubeSize == cubeSize }
}

fun interface ContentSource {
    fun read(name: String): String
}

object ClasspathContentSource : ContentSource {
    override fun read(name: String): String =
        requireNotNull(ContentRepository::class.java.getResourceAsStream("/content/$name")) { "Missing content file $name" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
