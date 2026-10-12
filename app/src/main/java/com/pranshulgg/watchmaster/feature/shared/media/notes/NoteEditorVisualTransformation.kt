package com.pranshulgg.watchmaster.feature.shared.media.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
private val ITALIC_REGEX = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
private val STRIKE_REGEX = Regex("~~(.+?)~~")
private val HEADING_REGEX = Regex("^(#{1,6})\\s")

class NoteEditorVisualTransformation(
    private val mutedColor: Color,
    private val normalColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val lines = text.text.split("\n")
        var offset = 0

        for (line in lines) {
            val lineStart = offset
            val headingMatch = HEADING_REGEX.find(line)

            when {
                headingMatch != null -> {
                    val markerEnd = lineStart + headingMatch.value.length
                    builder.addStyle(SpanStyle(color = mutedColor), lineStart, markerEnd)
                    val headingLevel = headingMatch.groupValues[1].length

                    val fontSize = when (headingLevel) {
                        1 -> 28.sp
                        2 -> 24.sp
                        3 -> 21.sp
                        4 -> 18.sp
                        5 -> 16.sp
                        6 -> 15.sp
                        else -> 16.sp
                    }

                    val fontWeight = when (headingLevel) {
                        1, 2, 3 -> FontWeight.SemiBold
                        4, 5, 6 -> FontWeight.Medium
                        else -> FontWeight.Medium
                    }

                    builder.addStyle(
                        SpanStyle(
                            fontWeight = fontWeight,
                            fontSize = fontSize
                        ),
                        markerEnd,
                        lineStart + line.length
                    )
                }
                line.startsWith("> ") -> {
                    builder.addStyle(SpanStyle(color = mutedColor), lineStart, lineStart + 2)
                    builder.addStyle(
                        SpanStyle(color = normalColor, fontStyle = FontStyle.Italic),
                        lineStart + 2, lineStart + line.length
                    )
                }
                line.startsWith("- ") -> {
                    builder.addStyle(SpanStyle(color = mutedColor), lineStart, lineStart + 2)
                }
                line.trim() == "---" -> {
                    builder.addStyle(SpanStyle(color = mutedColor), lineStart, lineStart + line.length)
                }
                else -> applyInlineStyles(builder, line, lineStart)
            }

            offset += line.length + 1
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun applyInlineStyles(builder: AnnotatedString.Builder, line: String, lineStart: Int) {
        for (match in BOLD_REGEX.findAll(line)) {
            val start = lineStart + match.range.first
            val end = lineStart + match.range.last + 1
            builder.addStyle(SpanStyle(color = mutedColor), start, start + 2)
            builder.addStyle(SpanStyle(color = mutedColor), end - 2, end)
            builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start + 2, end - 2)
        }
        for (match in STRIKE_REGEX.findAll(line)) {
            val start = lineStart + match.range.first
            val end = lineStart + match.range.last + 1
            builder.addStyle(SpanStyle(color = mutedColor), start, start + 2)
            builder.addStyle(SpanStyle(color = mutedColor), end - 2, end)
            builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start + 2, end - 2)
        }
        for (match in ITALIC_REGEX.findAll(line)) {
            val start = lineStart + match.range.first
            val end = lineStart + match.range.last + 1
            builder.addStyle(SpanStyle(color = mutedColor), start, start + 1)
            builder.addStyle(SpanStyle(color = mutedColor), end - 1, end)
            builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start + 1, end - 1)
        }
    }
}