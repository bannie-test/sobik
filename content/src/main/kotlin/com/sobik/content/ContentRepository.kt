package com.sobik.content

import com.sobik.model.Algorithm
import com.sobik.model.AlgorithmCategory
import com.sobik.model.BeginnerLesson
import com.sobik.model.CubeGuide
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class AlgorithmFile(val version: Int, val algorithms: List<Algorithm>)

@Serializable
internal data class LessonFile(val version: Int, val title: String, val lessons: List<BeginnerLesson>)

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
