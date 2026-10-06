package com.sukisu.ultra.ui.component.folk

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/** Characters that read as sensible places to break. Whitespace is handled separately. */
private const val WrapOpportunity = "-_/\\.:,;"

/** Longest run without a break before one is forced in, so a digest cannot blow out a row. */
private const val LongTokenBreakInterval = 12

private const val ZeroWidthSpace = '\u200B'

/**
 * A [Text] that stays inside its box when handed one unbreakable token: a
 * fingerprint, a kernel release, a package name, a digest.
 *
 * The wrap opportunities are for the line breaker only - the semantics tree is
 * restored to the original string, so a screen reader reads the real value.
 */
@Composable
fun FolkWrapSafeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val wrapSafeText = remember(text) { text.withWrapOpportunities() }

    Text(
        text = wrapSafeText,
        // Screen readers read the rendered text, so hand them the original back.
        modifier = if (wrapSafeText == text) {
            modifier
        } else {
            modifier.clearAndSetSemantics { this.text = AnnotatedString(text) }
        },
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

private fun String.withWrapOpportunities(): String {
    if (length <= LongTokenBreakInterval) return this

    val safe = StringBuilder(length + length / LongTokenBreakInterval)
    var sinceLastBreak = 0

    for (character in this) {
        safe.append(character)

        if (character.isWrapOpportunity()) {
            // Whitespace already is one; for the rest, add one after the separator.
            if (!character.isWhitespace()) {
                safe.append(ZeroWidthSpace)
            }
            sinceLastBreak = 0
            continue
        }

        sinceLastBreak++
        if (sinceLastBreak >= LongTokenBreakInterval) {
            safe.append(ZeroWidthSpace)
            sinceLastBreak = 0
        }
    }

    return safe.toString()
}

private fun Char.isWrapOpportunity(): Boolean = isWhitespace() || this in WrapOpportunity
