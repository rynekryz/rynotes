package com.rynekryz.rynotes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color

private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
private val italicRegex = Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")
private val underlineRegex = Regex("__(.+?)__")
private val strikeRegex = Regex("~~(.+?)~~")
private val codeRegex = Regex("`(.+?)`")

data class MarkdownLine(
    val raw: String,
    val kind: LineKind,
    val checked: Boolean = false,
    val content: String = raw,
)

enum class LineKind { HEADING1, HEADING2, HEADING3, BULLET, NUMBERED, CHECKBOX, QUOTE, DIVIDER, PLAIN, BLANK }

fun parseMarkdownLine(raw: String): MarkdownLine {
    val trimmed = raw.trimStart()
    return when {
        raw.isBlank() -> MarkdownLine(raw, LineKind.BLANK, content = "")
        trimmed.startsWith("### ") -> MarkdownLine(raw, LineKind.HEADING3, content = trimmed.removePrefix("### "))
        trimmed.startsWith("## ") -> MarkdownLine(raw, LineKind.HEADING2, content = trimmed.removePrefix("## "))
        trimmed.startsWith("# ") -> MarkdownLine(raw, LineKind.HEADING1, content = trimmed.removePrefix("# "))
        trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
            MarkdownLine(raw, LineKind.CHECKBOX, checked = true, content = trimmed.drop(6))
        trimmed.startsWith("- [ ] ") ->
            MarkdownLine(raw, LineKind.CHECKBOX, checked = false, content = trimmed.drop(6))
        trimmed.startsWith("- ") || trimmed.startsWith("• ") ->
            MarkdownLine(raw, LineKind.BULLET, content = trimmed.drop(2))
        trimmed.startsWith("> ") ->
            MarkdownLine(raw, LineKind.QUOTE, content = trimmed.removePrefix("> "))
        (trimmed == "---") || (trimmed == "***") ->
            MarkdownLine(raw, LineKind.DIVIDER, content = "")
        Regex("^\\d+\\.\\s").containsMatchIn(trimmed) ->
            MarkdownLine(raw, LineKind.NUMBERED, content = trimmed.replaceFirst(Regex("^\\d+\\.\\s"), ""))
        else -> MarkdownLine(raw, LineKind.PLAIN, content = raw)
    }
}

fun inlineMarkdownToAnnotatedString(text: String, baseColor: Color): AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val remaining = text.substring(i)
            val boldMatch = boldRegex.find(remaining)
            val underlineMatch = underlineRegex.find(remaining)
            val strikeMatch = strikeRegex.find(remaining)
            val codeMatch = codeRegex.find(remaining)
            val italicMatch = italicRegex.find(remaining)

            val candidates = listOfNotNull(
                boldMatch?.let { it.range.first to Pair("bold", it) },
                underlineMatch?.let { it.range.first to Pair("underline", it) },
                strikeMatch?.let { it.range.first to Pair("strike", it) },
                codeMatch?.let { it.range.first to Pair("code", it) },
                italicMatch?.let { it.range.first to Pair("italic", it) }
            )

            val next = candidates.minByOrNull { it.first }
            if (next == null || next.first != 0) {
                val nextIndex = next?.first ?: remaining.length
                append(remaining.substring(0, nextIndex))
                i += nextIndex
                continue
            }

            val (type, match) = next.second
            val inner = match.groupValues[1]
            when (type) {
                "bold" -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                "italic" -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                "underline" -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(inner)
                    pop()
                }
                "strike" -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                }
                "code" -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = baseColor.copy(alpha = 0.12f)
                        )
                    )
                    append(inner)
                    pop()
                }
            }
            i += match.range.last - match.range.first + 1
        }
    }
}
