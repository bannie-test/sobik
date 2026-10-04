package com.sobik.app.platform

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.sobik.app.ui.common.CatalogImageLoader
import com.sobik.content.CatalogFileSystem

/** The catalog folder tree read from the APK's assets. */
class AssetCatalogFileSystem(private val assets: AssetManager) : CatalogFileSystem {
    override fun list(path: String): List<String> = runCatching { assets.list(path)?.sorted() }.getOrNull().orEmpty()
    override fun read(path: String): String? =
        runCatching { assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()
}

/**
 * Decodes catalog images from assets, downsampled to the size actually shown, with a small
 * memory cache (about 8 MB) so scrolling the grid doesn't decode the same photo twice.
 */
class AssetImageLoader(private val assets: AssetManager) : CatalogImageLoader {
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    override fun load(path: String, maxSidePx: Int): ImageBitmap? {
        val key = "$path@$maxSidePx"
        cache.get(key)?.let { return it }
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSidePx) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
        }.getOrNull()?.also { cache.put(key, it) }
    }
}
