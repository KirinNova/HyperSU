package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.theme.tokens.FolkShape
import com.sukisu.ultra.ui.theme.tokens.FolkTheme

/**
 * The square an icon sits in: a grouped inset fill with the 12dp continuous
 * corner. One definition so the home grid, settings rows and module cards do not
 * each pick their own size and background.
 */
@Composable
fun FolkIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(FolkShape.Corner12)
            .background(FolkTheme.palette.groupedInset),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}
