package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FieldLimits
import com.example.data.ListColorKey
import com.example.data.ListIconKey
import com.example.data.TaskItem
import com.example.data.TaskList
import com.example.data.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListEditorSheet(
    existingList: TaskList?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, colorKey: ListColorKey, iconKey: ListIconKey) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(existingList?.id) { mutableStateOf(existingList?.name.orEmpty()) }
    var colorKey by remember(existingList?.id) { mutableStateOf(existingList?.colorEnum ?: ListColorKey.TERRACOTTA) }
    var iconKey by remember(existingList?.id) { mutableStateOf(existingList?.iconEnum ?: ListIconKey.CHECKLIST) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (existingList == null) R.string.dialog_new_list_title else R.string.dialog_edit_list_title
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= FieldLimits.LIST_NAME_MAX) name = it },
                label = { Text(stringResource(R.string.dialog_list_name_label)) },
                placeholder = { Text(stringResource(R.string.dialog_list_name_hint)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("list_name_input"),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dialog_list_color_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ListColorKey.entries.forEach { option ->
                        val color = listAccentColor(option)
                        val selected = option == colorKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { colorKey = option }
                                .testTag("list_color_${option.key}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dialog_list_icon_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ListIconKey.entries.forEach { option ->
                        val selected = option == iconKey
                        val accent = listAccentColor(colorKey)
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { iconKey = option }
                                .testTag("list_icon_${option.key}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = listIconVector(option),
                                    contentDescription = null,
                                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSave(name.trim(), colorKey, iconKey) },
                    enabled = !isSaving && name.trim().isNotEmpty(),
                    modifier = Modifier.testTag("list_save_button"),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (existingList == null) R.string.action_create else R.string.action_save
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    existingTask: TaskItem?,
    availableLists: List<TaskList>,
    initialListId: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        listId: String?,
        title: String,
        details: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
    ) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existingTask?.id) { mutableStateOf(existingTask?.title.orEmpty()) }
    var details by remember(existingTask?.id) { mutableStateOf(existingTask?.details.orEmpty()) }
    var priority by remember(existingTask?.id) {
        mutableStateOf(existingTask?.priorityEnum ?: TaskPriority.MEDIUM)
    }
    var selectedListId by remember(existingTask?.id, initialListId, availableLists) {
        mutableStateOf(
            existingTask?.listId
                ?: initialListId
                ?: availableLists.firstOrNull()?.id
        )
    }
    var dueDateEpochDay by remember(existingTask?.id) {
        mutableStateOf(existingTask?.dueDateEpochDay)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        if (existingTask == null) R.string.dialog_new_task_title else R.string.dialog_edit_task_title
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (onDelete != null && existingTask != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("task_delete_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= FieldLimits.TASK_TITLE_MAX) title = it },
                label = { Text(stringResource(R.string.dialog_task_title_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
            )

            OutlinedTextField(
                value = details,
                onValueChange = { if (it.length <= FieldLimits.TASK_DETAILS_MAX) details = it },
                label = { Text(stringResource(R.string.dialog_task_details_label)) },
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_details_input"),
            )

            if (availableLists.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_task_list_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableLists, key = { it.id }) { list ->
                            val selected = selectedListId == list.id
                            FilterChip(
                                selected = selected,
                                onClick = { selectedListId = list.id },
                                label = { Text(list.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = listAccentColor(list.colorEnum),
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }
            }

            // Priority selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.dialog_task_priority_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { option ->
                        val selected = priority == option
                        val color = priorityAccentColor(option)
                        FilterChip(
                            selected = selected,
                            onClick = { priority = option },
                            label = { Text(stringResource(option.labelRes)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                            ),
                            modifier = Modifier.testTag("task_priority_${option.key}"),
                        )
                    }
                }
            }

            // Due date row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.dialog_task_due_date_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val currentEpochDay = dueDateEpochDay
                    val dateLabel = if (currentEpochDay != null) {
                        formatDueDateLabel(currentEpochDay).first
                    } else {
                        stringResource(R.string.dialog_task_pick_date)
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("task_pick_date_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateLabel)
                    }
                    if (currentEpochDay != null) {
                        IconButton(
                            onClick = { dueDateEpochDay = null },
                            modifier = Modifier.testTag("task_clear_date_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.dialog_task_clear_date),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(selectedListId, title.trim(), details.trim(), priority, dueDateEpochDay)
                    },
                    enabled = !isSaving && title.trim().isNotEmpty(),
                    modifier = Modifier.testTag("task_save_button"),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (existingTask == null) R.string.action_create else R.string.action_save
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateEpochDay?.let { epochDayToUtcMillis(it) },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDateEpochDay = epochMillisToEpochDay(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm && existingTask != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_task_title)) },
            text = { Text(stringResource(R.string.dialog_delete_task_body, existingTask.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
