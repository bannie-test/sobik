package com.sobik.app.ui.puzzles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.common.AppScaffold
import com.sobik.model.PuzzleBrand
import com.sobik.model.PuzzleProduct
import com.sobik.visualization.PuzzleImage

private val CATEGORY_ORDER = listOf("2x2", "3x3", "4x4", "5x5", "6x6", "7x7", "Pyraminx", "Megaminx", "Skewb", "Square-1", "Clock")

fun brandColor(brand: PuzzleBrand?): Color =
    brand?.color?.toLongOrNull(16)?.let { Color(it.toInt()) } ?: Color.DarkGray

/** Catalog of puzzle types and manufacturers, grouped by brand or by puzzle type. */
@Composable
fun PuzzleCatalogScreen(container: AppContainer, nav: Navigator) {
    var byBrand by rememberSaveable { mutableStateOf(true) }
    val brands = remember { container.content.puzzleBrands }
    val puzzles = remember { container.content.puzzles }
    val groups: List<Pair<String, List<PuzzleProduct>>> = remember(byBrand) {
        if (byBrand) brands.map { b -> b.name to puzzles.filter { it.brandId == b.id } }.filter { it.second.isNotEmpty() }
        else puzzles.groupBy { it.category }.toList().sortedBy { (c, _) -> CATEGORY_ORDER.indexOf(c).let { if (it < 0) 99 else it } }
    }
    AppScaffold("Các loại Rubik", onBack = { nav.pop() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = byBrand, onClick = { byBrand = true }, label = { Text("Theo hãng") })
                    FilterChip(selected = !byBrand, onClick = { byBrand = false }, label = { Text("Theo loại") })
                }
            }
            for ((title, items) in groups) {
                item(key = "h-$title-$byBrand") {
                    val brand = if (byBrand) brands.first { it.name == title } else null
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (brand != null) brandColor(brand) else MaterialTheme.colorScheme.primary)
                        if (brand != null) Text("${brand.country} · ${brand.summary}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(items, key = { "${it.id}-$byBrand" }) { p -> PuzzleCard(p, container.content.brand(p.brandId)) }
            }
            item {
                Text(container.content.puzzleDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun PuzzleCard(p: PuzzleProduct, brand: PuzzleBrand?) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            PuzzleImage(p.shape, p.size, brand?.mark ?: "", brandColor(brand), Modifier.size(96.dp))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${brand?.name ?: p.brandId} · ${p.category}", style = MaterialTheme.typography.labelMedium, color = brandColor(brand))
                if (p.features.isNotEmpty()) Text("Đặc điểm: " + p.features.joinToString("; "), style = MaterialTheme.typography.bodySmall)
                if (p.reviews.isNotEmpty()) Text("Nhận xét phổ biến: " + p.reviews.joinToString("; "), style = MaterialTheme.typography.bodySmall)
                if (p.audience.isNotBlank()) Text("Phù hợp: ${p.audience}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
