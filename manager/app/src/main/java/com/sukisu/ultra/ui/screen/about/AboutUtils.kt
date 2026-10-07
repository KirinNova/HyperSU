package com.sukisu.ultra.ui.screen.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.runtime.Immutable

@Immutable
data class LinkInfo(
    val fullText: String,
    val url: String
)

/** The QQ group behind [about_source_code]'s share link. */
private const val QQ_GROUP_UIN = "1121908641"

/**
 * Opens one of the About page's links.
 *
 * The QQ group's share link is an https://qun.qq.com address, which a browser claims before QQ
 * does. QQ's own group-card scheme jumps straight into the app, so it is tried first and the
 * share link remains the fallback for a device without QQ.
 */
fun openAboutLink(context: Context, url: String) {
    if (url.startsWith("https://qun.qq.com/")) {
        val groupCard =
            "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$QQ_GROUP_UIN" +
                "&card_type=group&source=qrcode"
        val opened = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(groupCard))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        if (opened) return
    }
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun extractLinks(html: String): List<LinkInfo> {
    val lines = html.split("<br/>", "<br>", "\n")
    val result = mutableListOf<LinkInfo>()

    for (line in lines) {
        val spanned: Spanned = Html.fromHtml(line, Html.FROM_HTML_MODE_LEGACY)
        val spans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        val text = spanned.toString().trim()

        for (span in spans) {
            val url = span.url
            result.add(LinkInfo(text, url))
        }
    }
    return result
}

