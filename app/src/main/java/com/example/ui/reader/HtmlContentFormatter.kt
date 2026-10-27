package com.example.ui.reader

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.core.text.HtmlCompat

object HtmlContentFormatter {

    /**
     * Removes <img>, <iframe>, <picture>, and common media wrappers from HTML for reading display only.
     * The original content in the database remains untouched.
     */
    fun cleanHtmlForDisplay(rawHtml: String): String {
        var text = rawHtml

        // 1. Remove <img> tags
        text = text.replace(Regex("""<img[^>]*>""", RegexOption.IGNORE_CASE), "")

        // 2. Remove <iframe>...</iframe> tags
        text = text.replace(Regex("""<iframe[^>]*>.*?</iframe>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<iframe[^>]*/>""", RegexOption.IGNORE_CASE), "")

        // 3. Remove <picture>...</picture>
        text = text.replace(Regex("""<picture[^>]*>.*?</picture>""", RegexOption.IGNORE_CASE), "")

        // 4. Remove WordPress caption / embed shortcodes
        text = text.replace(Regex("""\[caption[^\]]*\][\s\S]*?\[/caption\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\[embed[^\]]*\][\s\S]*?\[/embed\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\[gallery[^\]]*\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\[video[^\]]*\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\[audio[^\]]*\]""", RegexOption.IGNORE_CASE), "")

        // 5. Clean up redundant empty tags like <p></p>
        text = text.replace(Regex("""<p>\s*</p>""", RegexOption.IGNORE_CASE), "")

        return text.trim()
    }

    /**
     * Converts cleaned HTML into a styled AnnotatedString for Jetpack Compose.
     */
    fun htmlToAnnotatedString(html: String, defaultTextColor: Color): AnnotatedString {
        val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return spannedToAnnotatedString(spanned, defaultTextColor)
    }

    private fun spannedToAnnotatedString(spanned: Spanned, defaultTextColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(spanned.toString())

            val spans = spanned.getSpans(0, spanned.length, Any::class.java)
            for (span in spans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                if (start >= end || start < 0 || end > length) continue

                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            Typeface.BOLD -> addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                start,
                                end
                            )
                            Typeface.ITALIC -> addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                start,
                                end
                            )
                            Typeface.BOLD_ITALIC -> addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                                start,
                                end
                            )
                        }
                    }
                    is UnderlineSpan -> {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start,
                            end
                        )
                    }
                    is RelativeSizeSpan -> {
                        addStyle(
                            SpanStyle(fontSize = span.sizeChange.em),
                            start,
                            end
                        )
                    }
                    is ForegroundColorSpan -> {
                        addStyle(
                            SpanStyle(color = Color(span.foregroundColor)),
                            start,
                            end
                        )
                    }
                }
            }
        }
    }
}
