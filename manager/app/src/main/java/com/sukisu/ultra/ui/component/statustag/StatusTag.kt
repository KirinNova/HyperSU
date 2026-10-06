package com.sukisu.ultra.ui.component.statustag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.ultra.ui.theme.tokens.FolkShape

/**
 * A small inline pill for a short status word, in the FolkPatch design: a
 * capsule block placed next to a title.
 *
 * The caller supplies both colours because the caller knows the meaning being
 * tagged; the shape and the type are fixed here so the tag reads the same
 * wherever it appears.
 */
@Composable
fun StatusTag(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .background(color = backgroundColor, shape = FolkShape.CornerFull),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            fontSize = 10.sp,
        )
    }
}
