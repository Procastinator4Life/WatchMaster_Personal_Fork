package com.pranshulgg.watchmaster.feature.shared.media.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import androidx.compose.material3.SheetValue

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesEditorSheet(
    show: Boolean,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!show) return

    var isEditing by remember { mutableStateOf(initialNote.isBlank()) }
    var textValue by remember {
        mutableStateOf(TextFieldValue(initialNote, TextRange(initialNote.length)))
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            !(isEditing && targetValue == SheetValue.Hidden)
        }
    )

    val scope = rememberCoroutineScope()

    fun animatedDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    var undoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    var redoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }

    // Synchronize initial text when opening with fresh content
    LaunchedEffect(initialNote) {
        if (textValue.text != initialNote && undoStack.isEmpty()) {
            textValue = TextFieldValue(initialNote, TextRange(initialNote.length))
            isEditing = initialNote.isBlank()
        }
    }

    fun updateText(new: TextFieldValue) {
        if (new.text != textValue.text) {
            undoStack = (undoStack + textValue).takeLast(200)
            redoStack = emptyList()
        }
        textValue = new
    }

    fun undo() {
        val last = undoStack.lastOrNull() ?: return
        redoStack = redoStack + textValue
        undoStack = undoStack.dropLast(1)
        textValue = last
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = undoStack + textValue
        redoStack = redoStack.dropLast(1)
        textValue = next
    }

    fun commitIfNeeded() {
        if (textValue.text != initialNote) onSave(textValue.text)
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isEditing) {
                commitIfNeeded()
                onDismiss()
            }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {

        BackHandler {
            if (isEditing) {
                commitIfNeeded()
                isEditing = false
            } else {
                commitIfNeeded()
                animatedDismiss()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )

                FilledIconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = { if (isEditing) commitIfNeeded(); isEditing = !isEditing },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Symbol(
                        icon = if (isEditing) R.drawable.check_24px else R.drawable.edit_24px,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                thickness = 3.dp,
                color = MaterialTheme.colorScheme.outline
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isEditing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { newValue -> updateText(applySmartListContinuation(textValue, newValue)) },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        visualTransformation = NoteEditorVisualTransformation(
                            mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            normalColor = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = "Write your note in Markdown…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (textValue.text.isBlank()) {
                            Text(
                                text = "No note yet. Tap the pencil to add one.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            NoteContentRenderer(
                                text = textValue.text,
                                onTextChanged = { updatedText ->
                                    updateText(
                                        TextFieldValue(
                                            text = updatedText,
                                            selection = TextRange(updatedText.length)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isEditing) 8.dp else 16.dp))

            if (isEditing) {
                NotesFormattingToolbar(
                    onQuote = { updateText(prefixCurrentLine(textValue, "> ")) },
                    onCheckbox = { updateText(prefixCurrentLine(textValue, "- [ ] ")) },
                    onCallout = { updateText(prefixCurrentLine(textValue, "> [!NOTE] ")) },
                    onHeading = { level -> updateText(setHeadingLevel(textValue, level)) },
                    onBold = { updateText(wrapSelection(textValue, "**")) },
                    onItalic = { updateText(wrapSelection(textValue, "*")) },
                    onStrikethrough = { updateText(wrapSelection(textValue, "~~")) },
                    onDivider = { updateText(insertDivider(textValue)) },
                    onUndo = { undo() },
                    onRedo = { redo() },
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}