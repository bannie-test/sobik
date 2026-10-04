package com.sobik.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads catalog images (from the app's assets on Android). */
fun interface CatalogImageLoader {
    /** Decoded image no larger than [maxSidePx] on its longer side, or null if missing/unreadable. */
    fun load(path: String, maxSidePx: Int): ImageBitmap?

    companion object {
        val NONE = CatalogImageLoader { _, _ -> null }
    }
}

/** Recommended image size for catalog photos (4:3). */
const val CATALOG_IMAGE_WIDTH = 800
const val CATALOG_IMAGE_HEIGHT = 600

/** A model's photo in a 4:3 frame, or a placeholder when it has none yet. Decoding runs off the main thread. */
@Composable
fun CatalogImage(path: String?, loader: CatalogImageLoader, label: String, modifier: Modifier = Modifier, maxSidePx: Int = 600) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = path?.let { withContext(Dispatchers.IO) { loader.load(it, maxSidePx) } }
    }
    val frame = modifier.aspectRatio(CATALOG_IMAGE_WIDTH.toFloat() / CATALOG_IMAGE_HEIGHT)
    val image = bitmap
    if (image != null) Image(image, contentDescription = label, modifier = frame, contentScale = ContentScale.Fit)
    else PlaceholderImage(label, frame)
}

/** placehold.co-style placeholder: gray box, the model name and the expected image size. */
@Composable
fun PlaceholderImage(label: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawRect(Color(0xFFDDDDDD))
        val big = measurer.measure(
            label,
            TextStyle(color = Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = (size.height / 9f / density).sp, textAlign = TextAlign.Center),
            constraints = Constraints(maxWidth = (size.width * 0.9f).toInt()),
            maxLines = 3,
        )
        val small = measurer.measure("${CATALOG_IMAGE_WIDTH} × ${CATALOG_IMAGE_HEIGHT}", TextStyle(color = Color(0xFF9E9E9E), fontSize = (size.height / 14f / density).sp))
        val total = big.size.height + small.size.height
        drawText(big, topLeft = Offset((size.width - big.size.width) / 2f, (size.height - total) / 2f))
        drawText(small, topLeft = Offset((size.width - small.size.width) / 2f, (size.height - total) / 2f + big.size.height))
    }
}
