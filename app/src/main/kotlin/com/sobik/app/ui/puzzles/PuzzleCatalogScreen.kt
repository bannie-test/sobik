package com.sobik.app.ui.puzzles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sobik.app.AppContainer
import com.sobik.app.ui.Navigator
import com.sobik.app.ui.Screen
import com.sobik.app.ui.common.AppScaffold
import com.sobik.app.ui.common.SectionCard
import com.sobik.model.PuzzleBrand
import com.sobik.model.PuzzleProduct
import com.sobik.visualization.BrandLogoView
import com.sobik.visualization.PuzzleImage

private val CATEGORY_ORDER = listOf("3x3", "2x2", "4x4", "5x5", "6x6", "7x7", "Pyraminx", "Megaminx", "Skewb", "Square-1", "Clock")

fun brandColor(brand: PuzzleBrand?): Color =
    brand?.color?.toLongOrNull(16)?.let { Color(it.toInt()) } ?: Color.DarkGray

/** Ids of a brand and all of its sub-brands. */
private fun family(brands: List<PuzzleBrand>, id: String): Set<String> =
    setOf(id) + brands.filter { it.parentId == id }.flatMap { family(brands, it.id) }

/** Showcase ("kệ trưng bày", no prices): brand shelf, category filter and a product grid. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuzzleCatalogScreen(container: AppContainer, nav: Navigator) {
    val content = container.content
    val brands = remember { content.puzzleBrands }
    val puzzles = remember { content.puzzles }
    var brandFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val brandsWithProducts = remember { brands.filter { b -> puzzles.any { it.brandId in family(brands, b.id) } } }
    val shown = remember(brandFilter, category) {
        val allowed = brandFilter?.let { family(brands, it) }
        puzzles.filter { (allowed == null || it.brandId in allowed) && (category == null || it.category == category) }
            .sortedWith(compareBy({ CATEGORY_ORDER.indexOf(it.category).let { i -> if (i < 0) 99 else i } }, { if ("Mới" in it.tags) 0 else 1 }))
    }
    val categories = remember { puzzles.map { it.category }.distinct().sortedBy { CATEGORY_ORDER.indexOf(it).let { i -> if (i < 0) 99 else i } } }

    AppScaffold("Kệ Rubik", onBack = { nav.pop() }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hãng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(brandsWithProducts, key = { it.id }) { b ->
                            val selected = brandFilter == b.id
                            Box(
                                Modifier.width(96.dp).height(48.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .border(if (selected) 3.dp else 1.dp, if (selected) brandColor(b) else Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                                    .clickable { brandFilter = if (selected) null else b.id }
                                    .padding(8.dp),
                            ) { BrandLogoView(b.logo, brandColor(b), Modifier.fillMaxSize(), b.mark) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Tất cả") })
                        for (c in categories) FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c) })
                    }
                    brandFilter?.let { id ->
                        val b = brands.first { it.id == id }
                        if (b.summary.isNotBlank()) Text(b.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${shown.size} mẫu", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(shown, key = { it.id }) { p -> ProductCard(p, content.brand(p.brandId)) { nav.push(Screen.PuzzleDetail(p.id)) } }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(content.puzzleDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ProductCard(p: PuzzleProduct, brand: PuzzleBrand?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White, RoundedCornerShape(12.dp))) {
                PuzzleImage(p.shape, p.size, brand?.mark ?: "", brandColor(brand), Modifier.fillMaxSize().padding(6.dp), logo = brand?.logo)
                if ("Mới" in p.tags) Badge("MỚI", Color(0xFFD32F2F), Modifier.align(Alignment.TopStart).padding(6.dp))
            }
            BrandLogoView(brand?.logo, brandColor(brand), Modifier.width(72.dp).height(18.dp), brand?.name ?: "")
            Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(p.category, style = MaterialTheme.typography.labelSmall, color = brandColor(brand))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                p.tags.filter { it != "Mới" }.take(1).forEach { Badge(it, Color(0xFF455A64)) }
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.background(color, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
    )
}

/** Product page: large picture, brand, badges, features, community comments, sources. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuzzleDetailScreen(container: AppContainer, nav: Navigator, id: String) {
    val content = container.content
    val p = remember(id) { content.puzzles.firstOrNull { it.id == id } } ?: return
    val brand = content.brand(p.brandId)
    val parent = brand?.parentId?.let { content.brand(it) }
    AppScaffold(p.name, onBack = { nav.pop() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(Color.White, RoundedCornerShape(16.dp))) {
                PuzzleImage(p.shape, p.size, brand?.mark ?: "", brandColor(brand), Modifier.fillMaxSize().padding(16.dp), logo = brand?.logo)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogoView(brand?.logo, brandColor(brand), Modifier.width(120.dp).height(32.dp), brand?.name ?: "")
                if (parent != null) Text("thuộc ${parent.name}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(p.category, brandColor(brand))
                p.tags.forEach { Badge(it, if (it == "Mới") Color(0xFFD32F2F) else Color(0xFF455A64)) }
            }
            if (p.features.isNotEmpty()) SectionCard("Đặc điểm nổi bật") { p.features.forEach { Text("• $it") } }
            if (p.reviews.isNotEmpty()) SectionCard("Nhận xét phổ biến") { p.reviews.forEach { Text("• $it") } }
            if (p.audience.isNotBlank()) SectionCard("Phù hợp với") { Text(p.audience) }
            if (brand != null && brand.summary.isNotBlank()) SectionCard("Về ${brand.name}") {
                if (brand.country.isNotBlank()) Text(brand.country, style = MaterialTheme.typography.labelMedium)
                Text(brand.summary)
            }
            SectionCard("Nguồn tham khảo") {
                content.puzzleSources.forEach { Text("${it.name}: ${it.url}", style = MaterialTheme.typography.labelSmall) }
                Text(content.puzzleDisclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
