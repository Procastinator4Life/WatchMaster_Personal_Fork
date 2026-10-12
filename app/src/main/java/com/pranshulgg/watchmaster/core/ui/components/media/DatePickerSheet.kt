package com.pranshulgg.watchmaster.core.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.core.ui.components.ActionBottomSheet
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.pranshulgg.watchmaster.R
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.offset

enum class DateSelectionSource {
    CALENDAR,
    RELEASE_DATE,
    UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DatePickerSheet(
    show: Boolean,
    onDateSelected: (Long, Long?, DateSelectionSource) -> Unit,
    initialDate: Long? = null,
    releaseDate: Long? = null,
    id: Long = -1L,
    onDismiss: () -> Unit
) {
    if (show && id != -1L) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
        )

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val selected = datePickerState.selectedDateMillis
        val source = when {
            selected == null -> DateSelectionSource.UNKNOWN
            selected == releaseDate -> DateSelectionSource.RELEASE_DATE
            else -> DateSelectionSource.CALENDAR
        }
    
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )

        ActionBottomSheet(
            sheetState = sheetState,
            onCancel = onDismiss,
            onConfirm = { onDateSelected(id, selected, source) }
        ) {
            Column {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = source == DateSelectionSource.RELEASE_DATE,
                        onClick = { datePickerState.selectedDateMillis = releaseDate },
                        enabled = releaseDate != null,
                        colors = chipColors,
                        label = { Text("Release date") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.date_range_24px),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )

                    FilterChip(
                        selected = source == DateSelectionSource.UNKNOWN,
                        onClick = { datePickerState.selectedDateMillis = null },
                        colors = chipColors,
                        label = { Text("Unknown") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.schedule_24px),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    }
}