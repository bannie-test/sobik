package com.sobik.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sobik.model.Move
import com.sobik.model.Notation
import com.sobik.model.StickerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun MovesText(moves: List<Move>, modifier: Modifier = Modifier) {
    Text(
        if (moves.isEmpty()) "(không cần bước nào)" else Notation.format(moves),
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun NotationText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
}

/** Six color buttons; used by the correction screen and manual input. */
@Composable
fun ColorPicker(selected: StickerColor?, onPick: (StickerColor) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        for (c in StickerColor.entries) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(Color(c.argb), CircleShape)
                    .border(
                        if (c == selected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.4f)),
                        CircleShape,
                    )
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun BigActionButton(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, accent: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier.fillMaxWidth().height(88.dp).clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (enabled) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (enabled) accent else Color.Gray, CircleShape))
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
