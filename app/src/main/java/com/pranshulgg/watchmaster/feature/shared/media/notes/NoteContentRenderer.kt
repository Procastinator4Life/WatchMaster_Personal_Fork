package com.pranshulgg.watchmaster.feature.shared.media.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownTypography
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.Symbol

private const val CALLOUT_MARKER = "[!NOTE]"
private const val PARAGRAPH_SPACING = 14

private sealed class NoteBlock {
    data class Regular(val text: String) : NoteBlock()
    data class Callout(val text: String) : NoteBlock()
}

private data class CheckboxLine(
    val lineIndex: Int,
    val checked: Boolean,
    val text: String
)

private fun parseNoteBlocks(source: String): List<NoteBlock> {
    val lines = source.lines()
    val blocks = mutableListOf<NoteBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        if (line.startsWith("> $CALLOUT_MARKER")) {
            val calloutLines = mutableListOf(
                line.removePrefix("> $CALLOUT_MARKER").trim()
            )
            i++

            while (i < lines.size && lines[i].startsWith("> ")) {
                calloutLines += lines[i].removePrefix("> ")
                i++
            }

            blocks += NoteBlock.Callout(
                calloutLines.joinToString("\n").trim()
            )
        } else {
            val regularLines = mutableListOf(line)
            i++

            while (
                i < lines.size &&
                !lines[i].startsWith("> $CALLOUT_MARKER")
            ) {
                regularLines += lines[i]
                i++
            }

            blocks += NoteBlock.Regular(
                regularLines.joinToString("\n")
            )
        }
    }

    return blocks
}

/**
 * Splits Markdown into visual groups wherever the user
 * inserted a blank line.
 */
private fun splitIntoMarkdownGroups(text: String): List<String> {
    return text
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

/**
 * Finds a checkbox Markdown line.
 *
 * Supports:
 * - [ ] unchecked
 * - [x] checked
 * - [X] checked
 *
 * Also supports indentation before the checkbox.
 */
private fun parseCheckboxLine(line: String): CheckboxLine? {
    val match = Regex("""^(\s*)-\s*\[([ xX])\]\s?(.*)$""")
        .matchEntire(line)
        ?: return null

    val indentation = match.groupValues[1]
    val state = match.groupValues[2]
    val text = match.groupValues[3]

    return CheckboxLine(
        lineIndex = -1,
        checked = state.equals("x", ignoreCase = true),
        text = indentation + text
    )
}

private fun toggleCheckbox(
    source: String,
    lineIndex: Int
): String {
    val lines = source.lines().toMutableList()

    if (lineIndex !in lines.indices) return source

    val line = lines[lineIndex]

    val regex = Regex("""^(\s*-\s*\[)([ xX])(\]\s?.*)$""")
    val match = regex.matchEntire(line) ?: return source

    val current = match.groupValues[2]
    val newState = if (current.equals("x", ignoreCase = true)) {
        " "
    } else {
        "x"
    }

    lines[lineIndex] =
        match.groupValues[1] +
            newState +
            match.groupValues[3]

    return lines.joinToString("\n")
}

@Composable
private fun MarkdownGroup(
    text: String,
    originalText: String,
    onCheckboxChanged: (Int) -> Unit,
    typography: MarkdownTypography
) {
    val lines = text.lines()

    var markdownLines = mutableListOf<String>()

    @Composable
    fun flushMarkdown() {
        if (markdownLines.isEmpty()) return

        Markdown(
            content = normalizeMarkdownForRender(
                markdownLines.joinToString("\n")
            ),
            typography = typography
        )

        markdownLines = mutableListOf()
    }

    lines.forEach { line ->
        val checkbox = parseCheckboxLine(line)

        if (checkbox == null) {
            markdownLines += line
        } else {
            flushMarkdown()

            val sourceLineIndex = originalText
                .lines()
                .indexOfFirst { it == line }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = checkbox.checked,
                    onCheckedChange = {
                        if (sourceLineIndex >= 0) {
                            onCheckboxChanged(sourceLineIndex)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(8.dp))

                Markdown(
                    content = normalizeMarkdownForRender(
                        checkbox.text.trimStart()
                    ),
                    typography = typography
                )
            }
        }
    }

    flushMarkdown()
}

@Composable
fun NoteContentRenderer(
    text: String,
    onTextChanged: (String) -> Unit
) {
    val customTypography = markdownTypography(
        h1 = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp
        ),
        h2 = MaterialTheme.typography.titleLarge.copy(
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 30.sp
        ),
        h3 = MaterialTheme.typography.titleMedium.copy(
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 27.sp
        ),
        h4 = MaterialTheme.typography.titleSmall.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp
        ),
        h5 = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp
        ),
        h6 = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp
        ),
        paragraph = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            lineHeight = 25.sp
        )
    )

    Column {
        parseNoteBlocks(text).forEach { block ->
            when (block) {
                is NoteBlock.Regular -> {
                    val groups = splitIntoMarkdownGroups(block.text)

                    groups.forEachIndexed { index, group ->
                        MarkdownGroup(
                            text = group,
                            originalText = text,
                            onCheckboxChanged = { lineIndex ->
                                onTextChanged(
                                    toggleCheckbox(text, lineIndex)
                                )
                            },
                            typography = customTypography
                        )

                        if (index < groups.lastIndex) {
                            Spacer(
                                Modifier.height(PARAGRAPH_SPACING.dp)
                            )
                        }
                    }
                }

                is NoteBlock.Callout -> {
                    CalloutBox(
                        text = block.text,
                        typography = customTypography
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CalloutBox(
    text: String,
    typography: MarkdownTypography
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp)
        ) {
            Symbol(
                R.drawable.lightbulb_24px,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(Modifier.width(10.dp))

            Markdown(
                content = normalizeMarkdownForRender(
                    text.ifBlank { " " }
                ),
                colors = markdownColor(
                    text = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                typography = typography
            )
        }
    }
}