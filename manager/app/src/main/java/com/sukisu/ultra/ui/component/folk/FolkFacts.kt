package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.theme.tokens.FolkTheme

/**
 * A list of label/value facts inside one container.
 *
 * Rows are separated by a hairline rather than each becoming its own card, so a
 * block of facts reads as one object. Build rows with [fact]; use [row] for a
 * row that is not a plain label/value pair.
 */
class FolkFactsScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun fact(label: String, value: String, valueTone: FolkSeverity = FolkSeverity.Neutral) {
        items.add { FolkFactRow(label = label, value = value, valueTone = valueTone) }
    }

    fun row(content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
fun FolkFactsGroup(
    modifier: Modifier = Modifier,
    content: @Composable FolkFactsScope.() -> Unit,
) {
    val scope = FolkFactsScope()
    scope.content()
    if (scope.items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        scope.items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    color = FolkTheme.palette.separator,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item()
        }
    }
}

/**
 * One label/value line. The value takes its colour from a [FolkSeverity] so a
 * state value (a version that is behind, a service that is off) can be tinted
 * without the caller picking a colour.
 */
@Composable
fun FolkFactRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueTone: FolkSeverity = FolkSeverity.Neutral,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = folkPreferenceSummaryStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = folkPreferenceValueStyle(),
            color = folkSeverityColor(valueTone),
            textAlign = TextAlign.End,
        )
    }
}
