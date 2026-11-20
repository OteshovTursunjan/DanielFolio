package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FieldLimits
import com.example.data.Note
import com.example.data.NoteCategory
import com.example.data.NoteColorKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (title: String, content: String, category: NoteCategory, colorKey: NoteColorKey, pinned: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    BackHandler(enabled = !isSaving, onBack = onBack)

    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var category by remember(note?.id) { mutableStateOf(note?.categoryEnum ?: NoteCategory.PERSONAL) }
    var colorKey by remember(note?.id) { mutableStateOf(note?.colorEnum ?: NoteColorKey.DEFAULT) }
    var pinned by remember(note?.id) { mutableStateOf(note?.pinned ?: false) }
    var validationError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pageColor = noteSurfaceColor(colorKey)

    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor,
                    scrolledContainerColor = pageColor,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("note_editor_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back_cd),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            if (note == null) R.string.editor_new_note else R.string.editor_edit_note
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { pinned = !pinned },
                        modifier = Modifier.testTag("note_editor_pin_button"),
                    ) {
                        Icon(
                            imageVector = if (pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = stringResource(
                                if (pinned) R.string.notes_unpin_cd else R.string.notes_pin_cd
                            ),
                            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSaving,
                            modifier = Modifier.testTag("note_editor_delete_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.editor_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            if (title.trim().isEmpty() && content.trim().isEmpty()) {
                                validationError = true
                            } else {
                                validationError = false
                                onSave(title, content, category, colorKey, pinned)
                            }
                        },
                        enabled = !isSaving,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("note_editor_save_button"),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.editor_save),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Color palette selector
            Text(
                text = stringResource(R.string.editor_color_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteColorKey.entries.forEach { option ->
                    val swatchColor = noteSurfaceColor(option)
                    val isSelected = option == colorKey
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            )
                            .clickable { colorKey = option }
                            .testTag("note_color_${option.key}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category chips
            Text(
                text = stringResource(R.string.editor_category_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NoteCategory.entries, key = { it.key }) { item ->
                    val selected = category == item
                    FilterChip(
                        selected = selected,
                        onClick = { category = item },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier.testTag("note_category_${item.key}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            if (validationError) {
                Text(
                    text = stringResource(R.string.editor_validation_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            // Title input
            Box(modifier = Modifier.fillMaxWidth()) {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_title_hint),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= FieldLimits.NOTE_TITLE_MAX) {
                            title = it
                            if (validationError) validationError = false
                        }
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_editor_title_input"),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
            ) {
                if (content.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_content_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
                BasicTextField(
                    value = content,
                    onValueChange = {
                        if (it.length <= FieldLimits.NOTE_CONTENT_MAX) {
                            content = it
                            if (validationError) validationError = false
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("note_editor_content_input"),
                )
            }
        }
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.editor_delete_confirm_title)) },
            text = { Text(stringResource(R.string.editor_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
