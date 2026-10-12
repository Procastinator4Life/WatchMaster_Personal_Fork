package com.pranshulgg.watchmaster.feature.shared.media.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.media.MediaSectionCard
import com.pranshulgg.watchmaster.feature.shared.media.notes.stripMarkdownForPreview

@Composable
fun NotesSection(isNoteNull: Boolean = true, action: () -> Unit, noteText: String) {
    MediaSectionCard(
        title = "Notes",
        titleIcon = R.drawable.sticky_note_2_24px,
        showAction = true,
        actionText = "Edit note",
        onClick = { action() } // Card-wide tap area with clipped rounded ripple
    ) {
        if (!isNoteNull) {
            Text(
                text = stripMarkdownForPreview(noteText, maxLength = 160),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}