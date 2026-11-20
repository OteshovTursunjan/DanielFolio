package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TaskItem
import com.example.data.TaskList
import com.example.viewmodel.TaskCompletionFilter
import com.example.viewmodel.UiState

@Composable
fun TasksTab(
    taskListsState: UiState<List<TaskList>>,
    tasksState: UiState<List<TaskItem>>,
    selectedListId: String?,
    completionFilter: TaskCompletionFilter,
    onSelectList: (String?) -> Unit,
    onCompletionFilterSelect: (TaskCompletionFilter) -> Unit,
    onCreateListClick: () -> Unit,
    onEditListClick: (TaskList) -> Unit,
    onDeleteListConfirm: (TaskList, List<TaskItem>) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var listPendingDelete by remember { mutableStateOf<TaskList?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Editorial Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.tasks_screen_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.nav_cloud),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        val isLoading = taskListsState is UiState.Loading || tasksState is UiState.Loading
        val errorMessage = (taskListsState as? UiState.Error)?.message
            ?: (tasksState as? UiState.Error)?.message

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(18.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                val lists = (taskListsState as UiState.Success).data
                val allTasks = (tasksState as UiState.Success).data
                val listMap = remember(lists) { lists.associateBy { it.id } }
                val activeList = remember(lists, selectedListId) {
                    lists.firstOrNull { it.id == selectedListId }
                }

                // Task Lists Horizontal Strip
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "all_lists") {
                        val isSelected = activeList == null
                        val activeCount = allTasks.count { !it.completed }
                        TaskListPill(
                            label = stringResource(R.string.tasks_all_lists),
                            count = activeCount,
                            selected = isSelected,
                            accentColor = MaterialTheme.colorScheme.primary,
                            icon = Icons.Rounded.FolderOpen,
                            onClick = { onSelectList(null) },
                            tag = "task_list_all",
                        )
                    }

                    items(lists, key = { it.id }) { taskList ->
                        val isSelected = activeList?.id == taskList.id
                        val count = allTasks.count { it.listId == taskList.id && !it.completed }
                        TaskListPill(
                            label = taskList.name,
                            count = count,
                            selected = isSelected,
                            accentColor = listAccentColor(taskList.colorEnum),
                            icon = listIconVector(taskList.iconEnum),
                            onClick = { onSelectList(taskList.id) },
                            tag = "task_list_${taskList.id}",
                        )
                    }

                    item(key = "add_list") {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onCreateListClick)
                                .testTag("create_task_list_chip"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.tasks_new_list_chip),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val scopedTasks = remember(allTasks, activeList) {
                    if (activeList == null) allTasks else allTasks.filter { it.listId == activeList.id }
                }

                val filteredTasks = remember(scopedTasks, completionFilter) {
                    when (completionFilter) {
                        TaskCompletionFilter.ALL -> scopedTasks
                        TaskCompletionFilter.ACTIVE -> scopedTasks.filter { !it.completed }
                        TaskCompletionFilter.COMPLETED -> scopedTasks.filter { it.completed }
                    }
                }

                if (lists.isEmpty() && allTasks.isEmpty()) {
                    TasksEmptyListsState(onCreateListClick = onCreateListClick)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Progress & Selected List Header Card
                        item(key = "progress_card") {
                            val totalCount = scopedTasks.size
                            val doneCount = scopedTasks.count { it.completed }
                            val progressTarget = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressTarget,
                                animationSpec = spring(),
                                label = "task_progress",
                            )
                            val accent = activeList?.let { listAccentColor(it.colorEnum) }
                                ?: MaterialTheme.colorScheme.secondary

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeList?.name ?: stringResource(R.string.tasks_progress_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(
                                                    R.string.tasks_progress_summary,
                                                    doneCount,
                                                    totalCount,
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        if (activeList != null) {
                                            Row {
                                                IconButton(
                                                    onClick = { onEditListClick(activeList) },
                                                    modifier = Modifier.testTag("edit_selected_list_button"),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Edit,
                                                        contentDescription = stringResource(R.string.tasks_edit_list_cd),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { listPendingDelete = activeList },
                                                    modifier = Modifier.testTag("delete_selected_list_button"),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.DeleteOutline,
                                                        contentDescription = stringResource(R.string.tasks_delete_list_cd),
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(100.dp)),
                                        color = accent,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        strokeCap = StrokeCap.Round,
                                    )
                                }
                            }
                        }

                        // Completion Filter Chips
                        item(key = "completion_filters") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                            ) {
                                val filters = listOf(
                                    TaskCompletionFilter.ALL to R.string.tasks_filter_all,
                                    TaskCompletionFilter.ACTIVE to R.string.tasks_filter_active,
                                    TaskCompletionFilter.COMPLETED to R.string.tasks_filter_completed,
                                )
                                filters.forEach { (filter, labelRes) ->
                                    val selected = completionFilter == filter
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onCompletionFilterSelect(filter) },
                                        label = {
                                            Text(
                                                text = stringResource(labelRes),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        ),
                                        modifier = Modifier.testTag("tasks_filter_${filter.name.lowercase()}"),
                                    )
                                }
                            }
                        }

                        if (filteredTasks.isEmpty()) {
                            item(key = "empty_tasks") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(R.string.tasks_empty_items_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.tasks_empty_items_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else {
                            items(filteredTasks, key = { it.id }) { task ->
                                val parentList = listMap[task.listId]
                                TaskRowCard(
                                    task = task,
                                    listName = if (activeList == null) parentList?.name else null,
                                    onToggle = { onToggleTask(task) },
                                    onClick = { onTaskClick(task) },
                                )
                            }
                        }
                    }
                }

                listPendingDelete?.let { targetList ->
                    val affectedCount = allTasks.count { it.listId == targetList.id }
                    AlertDialog(
                        onDismissRequest = { listPendingDelete = null },
                        title = { Text(stringResource(R.string.dialog_delete_list_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.dialog_delete_list_body,
                                    targetList.name,
                                    affectedCount,
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onDeleteListConfirm(targetList, allTasks)
                                    listPendingDelete = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(stringResource(R.string.action_delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { listPendingDelete = null }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListPill(
    label: String,
    count: Int,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.surface,
        label = "list_pill_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "list_pill_fg",
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(
            1.dp,
            if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) contentColor else accentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    task: TaskItem,
    listName: String?,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val priorityColor = priorityAccentColor(task.priorityEnum)
    val checkBg by animateColorAsState(
        targetValue = if (task.completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        label = "check_bg",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("task_row_${task.id}"),
        shape = MaterialTheme.shapes.large,
        color = if (task.completed) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 48dp accessible touch target around circular checkbox
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Checkbox,
                        onClick = onToggle,
                    )
                    .testTag("task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(checkBg)
                        .border(
                            width = 2.dp,
                            color = if (task.completed) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                priorityColor
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.tasks_toggle_cd),
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, bottom = 4.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (task.details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Priority badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = stringResource(task.priorityEnum.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }

                    if (!listName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = listName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }

                    task.dueDateEpochDay?.let { epochDay ->
                        val (label, isOverdue) = formatDueDateLabel(epochDay)
                        val dateColor = if (isOverdue && !task.completed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = dateColor,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = dateColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksEmptyListsState(onCreateListClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(170.dp),
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_workspace),
                contentDescription = stringResource(R.string.empty_workspace_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.tasks_empty_lists_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tasks_empty_lists_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateListClick,
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_create_list_button"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.tasks_create_list_cta))
        }
    }
}
