package com.sobik.content

import com.sobik.model.PuzzleBrand
import com.sobik.model.PuzzleProduct
import com.sobik.model.PuzzleShape
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Minimal read-only file tree (Android assets in the app, a directory in tests and tools). */
interface CatalogFileSystem {
    /** Names of the entries directly inside [path] (empty if none or not a directory). */
    fun list(path: String): List<String>
    /** File content, or null if [path] is not a file. */
    fun read(path: String): String?
}

class DirectoryCatalogFileSystem(private val root: File) : CatalogFileSystem {
    override fun list(path: String): List<String> = File(root, path).list()?.sorted().orEmpty()
    override fun read(path: String): String? = File(root, path).takeIf { it.isFile }?.readText()
}

/** Optional per-model information file: `<model folder>/info.json`. Every field is optional. */
@Serializable
internal data class ModelInfo(
    val name: String? = null,
    val shape: PuzzleShape? = null,
    val size: Int? = null,
    val tags: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val reviews: List<String> = emptyList(),
    val audience: String = "",
    /** Image file name inside the model folder; by default the first `image.*` found. */
    val image: String? = null,
)

/**
 * Folder-driven puzzle catalog:
 *
 * ```
 * rubik/brands/<brand-id>/<category>/<model-id>/info.json   (optional)
 * rubik/brands/<brand-id>/<category>/<model-id>/image.webp  (or .png / .jpg, optional)
 * ```
 *
 * Adding a model means adding a folder; images and information can be filled in later. Models
 * without an image show a placeholder; models without info.json are named after their folder.
 */
class RubikCatalog(private val fs: CatalogFileSystem, private val root: String = ROOT) {
    private val json = Json { ignoreUnknownKeys = true }

    fun products(): List<PuzzleProduct> = fs.list(root).filterNot(::hidden).flatMap { brand ->
        fs.list("$root/$brand").filterNot(::hidden).flatMap { category ->
            fs.list("$root/$brand/$category").filterNot(::hidden).mapNotNull { model ->
                load(brand, category, model)
            }
        }
    }

    /** Ids of the brand folders present (including empty ones waiting for content). */
    fun brandFolders(): List<String> = fs.list(root).filterNot(::hidden)

    private fun load(brand: String, category: String, model: String): PuzzleProduct? {
        val dir = "$root/$brand/$category/$model"
        val files = fs.list(dir)
        if (files.isEmpty()) return null // not a folder
        val info = fs.read("$dir/info.json")?.let { runCatching { json.decodeFromString<ModelInfo>(it) }.getOrNull() } ?: ModelInfo()
        val image = (info.image ?: files.firstOrNull { f -> IMAGE_EXTENSIONS.any { f.lowercase().endsWith(it) } && f.startsWith("image") })
            ?.takeIf { it in files }
        val (shape, size) = shapeOf(category)
        return PuzzleProduct(
            id = model,
            name = info.name ?: titleFromFolder(model),
            brandId = brand,
            category = CATEGORY_LABELS[category] ?: category,
            shape = info.shape ?: shape,
            size = info.size ?: size,
            tags = info.tags,
            features = info.features,
            reviews = info.reviews,
            audience = info.audience,
            image = image?.let { "$dir/$it" },
        )
    }

    private fun hidden(name: String) = name.startsWith(".") || name.endsWith(".md")

    companion object {
        const val ROOT = "rubik/brands"
        private val IMAGE_EXTENSIONS = listOf(".webp", ".png", ".jpg", ".jpeg")

        /** Folder name -> label shown in the app. Folder names are lowercase. */
        val CATEGORY_LABELS = linkedMapOf(
            "2x2" to "2x2", "3x3" to "3x3", "4x4" to "4x4", "5x5" to "5x5", "6x6" to "6x6", "7x7" to "7x7",
            "pyraminx" to "Pyraminx", "megaminx" to "Megaminx", "skewb" to "Skewb", "square-1" to "Square-1", "clock" to "Clock",
        )

        fun folderOf(categoryLabel: String): String =
            CATEGORY_LABELS.entries.firstOrNull { it.value == categoryLabel }?.key ?: categoryLabel.lowercase()

        private fun shapeOf(category: String): Pair<PuzzleShape, Int> = when (category) {
            "pyraminx" -> PuzzleShape.PYRAMINX to 3
            "megaminx" -> PuzzleShape.MEGAMINX to 3
            "skewb" -> PuzzleShape.SKEWB to 3
            "square-1" -> PuzzleShape.SQUARE1 to 3
            "clock" -> PuzzleShape.CLOCK to 3
            else -> PuzzleShape.CUBE to (category.substringBefore('x').toIntOrNull()?.coerceIn(2, 7) ?: 3)
        }

        fun titleFromFolder(folder: String) =
            folder.split('-').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }
}

/** Ids of [id] and all of its sub-brands. */
fun brandFamily(brands: List<PuzzleBrand>, id: String): Set<String> =
    setOf(id) + brands.filter { it.parentId == id }.flatMap { brandFamily(brands, it.id) }
