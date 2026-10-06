package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.theme.tokens.FolkShape
import com.sukisu.ultra.ui.theme.tokens.FolkTheme

/**
 * How good or bad a state is, so a screen declares the meaning once instead of
 * picking a colour per call site (the scattered `if (ok) primary else error`
 * this replaces). Colours come from [FolkTheme.palette], so every theme source
 * keeps working.
 */
enum class FolkSeverity { Positive, Caution, Critical, Neutral, Info }

@Composable
fun folkSeverityColor(severity: FolkSeverity): Color = when (severity) {
    FolkSeverity.Positive -> FolkTheme.palette.positive
    FolkSeverity.Caution -> FolkTheme.palette.caution
    FolkSeverity.Critical -> FolkTheme.palette.critical
    FolkSeverity.Neutral -> FolkTheme.palette.neutral
    FolkSeverity.Info -> MaterialTheme.colorScheme.secondary
}

/** A small coloured dot for a status, for places too tight for a full badge. */
@Composable
fun FolkStatusDot(severity: FolkSeverity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(folkSeverityColor(severity)),
    )
}

/**
 * A pill that says what a state is: a severity dot, a label, and an optional
 * muted metadata value.
 *
 * The fill is the severity colour at a low alpha while the text stays
 * [MaterialTheme.colorScheme.onSurface], so the label keeps its contrast on
 * every theme instead of turning into light-on-light.
 */
@Composable
fun FolkStatusBadge(
    severity: FolkSeverity,
    label: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val tint = folkSeverityColor(severity)
    Row(
        modifier = modifier
            .clip(FolkShape.CornerFull)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolkStatusDot(severity)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (meta != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
