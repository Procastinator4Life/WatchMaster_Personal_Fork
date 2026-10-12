package com.pranshulgg.watchmaster.feature.shared.media.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun wrapSelection(value: TextFieldValue, marker: String): TextFieldValue {
    val sel = value.selection
    val text = value.text
    return if (sel.collapsed) {
        val newText = text.substring(0, sel.start) + marker + marker + text.substring(sel.start)
        value.copy(text = newText, selection = TextRange(sel.start + marker.length))
    } else {
        val selected = text.substring(sel.min, sel.max)
        val newText = text.substring(0, sel.min) + marker + selected + marker + text.substring(sel.max)
        value.copy(
            text = newText,
            selection = TextRange(sel.min + marker.length, sel.max + marker.length)
        )
    }
}

private fun lineStartFor(text: String, cursor: Int): Int {
    val idx = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
    return if (idx == -1) 0 else idx + 1
}

private fun lineEndFor(text: String, from: Int): Int {
    val idx = text.indexOf('\n', from)
    return if (idx == -1) text.length else idx
}

fun prefixCurrentLine(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start
    val lineStart = lineStartFor(text, cursor)
    val availableLength = (text.length - lineStart).coerceAtLeast(0)
    val already = availableLength >= prefix.length &&
        text.regionMatches(lineStart, prefix, 0, prefix.length)

    return if (already) {
        val newText = text.removeRange(lineStart, lineStart + prefix.length)
        value.copy(text = newText, selection = TextRange((cursor - prefix.length).coerceAtLeast(lineStart)))
    } else {
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        value.copy(text = newText, selection = TextRange(cursor + prefix.length))
    }
}

fun setHeadingLevel(value: TextFieldValue, level: Int): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start
    val lineStart = lineStartFor(text, cursor)
    val lineEnd = lineEndFor(text, lineStart)
    val line = text.substring(lineStart, lineEnd)
    val stripped = line.trimStart('#').trimStart(' ')
    val newPrefix = "#".repeat(level) + " "
    val newLine = newPrefix + stripped
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val delta = newLine.length - line.length

    return value.copy(
        text = newText,
        selection = TextRange((cursor + delta).coerceIn(lineStart, lineStart + newLine.length))
    )
}

fun insertDivider(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start
    val needsLeadingNewline = cursor > 0 && text.getOrNull(cursor - 1) != '\n'
    val insertText = (if (needsLeadingNewline) "\n" else "") + "\n---\n\n"
    val newText = text.substring(0, cursor) + insertText + text.substring(cursor)
    return value.copy(text = newText, selection = TextRange(cursor + insertText.length))
}

/**
 * When the user presses Enter right after a bullet ("- ") or quote ("> ") line,
 * continue the same marker on the new line.
 */
fun applySmartListContinuation(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val isSingleNewlineInsert = new.text.length == old.text.length + 1 &&
        new.selection.collapsed &&
        new.text.getOrNull(new.selection.start - 1) == '\n'

    if (!isSingleNewlineInsert) return new

    val cursor = new.selection.start
    val prevLineEnd = cursor - 1
    val prevLineStart = lineStartFor(new.text, prevLineEnd)
    val prevLine = new.text.substring(prevLineStart, prevLineEnd)

    val marker = when {
        prevLine.startsWith("- ") -> "- "
        prevLine.startsWith("> ") -> "> "
        else -> null
    } ?: return new

    return if (prevLine.trim() == marker.trim()) {
        val newText = new.text.removeRange(prevLineStart, prevLineEnd)
        new.copy(text = newText, selection = TextRange(prevLineStart))
    } else {
        val newText = new.text.substring(0, cursor) + marker + new.text.substring(cursor)
        new.copy(text = newText, selection = TextRange(cursor + marker.length))
    }
}

fun stripMarkdownForPreview(markdown: String, maxLength: Int = 150): String {
    val stripped = markdown
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^>\\s?\\[!NOTE\\]\\s?", RegexOption.MULTILINE), "")
        .replace(Regex("^>\\s?", RegexOption.MULTILINE), "")
        .replace(Regex("^-\\s?\\[[ xX]?\\]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("~~(.*?)~~"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("^-{3,}$", RegexOption.MULTILINE), "")
        // ❌ Removed: .replace("\n", " ") -> This preserves line breaks!
        .trim()

    return if (stripped.length > maxLength) stripped.take(maxLength).trimEnd() + "…" else stripped
}

/**
 * CommonMark treats a single newline inside a paragraph as a soft break.
 * This appends two spaces ("  ") ONLY to non-empty lines inside a paragraph
 * so single Enters wrap, while leaving blank lines clean so double Enters 
 * create real empty line spacing between paragraphs.
 */
fun normalizeMarkdownForRender(source: String): String {
    val collapsed = source.replace(Regex("\n{3,}"), "\n\n")
    val lines = collapsed.split("\n")
    val result = StringBuilder()

    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trim()
        
        // Always ensure a newline before horizontal dividers so Markdown doesn't parse them as header underlines
        if (trimmed == "---" && result.isNotEmpty() && !result.endsWith("\n\n")) {
            if (!result.endsWith("\n")) result.append("\n")
        }

        result.append(line)

        val isLastLine = index == lines.lastIndex
        if (!isLastLine) {
            val nextLine = lines[index + 1]
            val isCurrentBlank = line.isBlank()
            val isNextBlank = nextLine.isBlank()

            // Only add hard-break spaces if BOTH current and next lines contain actual text
            if (!isCurrentBlank && !isNextBlank) {
                result.append("  ")
            }
            result.append("\n")
        }
    }

    return result.toString()
}