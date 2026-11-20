package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Note
import com.example.data.NoteCategory
import com.example.viewmodel.NoteFilter
import com.example.viewmodel.UiState

@Composable
fun NotesTab(
    notesState: UiState<List<Note>>,
    searchQuery: String,
    activeFilter: NoteFilter,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (NoteFilter) -> Unit,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Editorial Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.notes_screen_title),
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

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.notes_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("notes_clear_search_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.notes_clear_search_cd),
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("notes_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        val filterOptions = remember {
            listOf(
                NoteFilter.ALL to R.string.notes_filter_all,
                NoteFilter.PINNED to R.string.notes_filter_pinned,
                NoteFilter.PERSONAL to R.string.category_personal,
                NoteFilter.WORK to R.string.category_work,
                NoteFilter.IDEAS to R.string.category_ideas,
                NoteFilter.STUDY to R.string.category_study,
                NoteFilter.OTHER to R.string.category_other,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filterOptions, key = { it.first.name }) { (filter, labelRes) ->
                val selected = activeFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelect(filter) },
                    label = {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.testTag("notes_filter_${filter.name.lowercase()}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (notesState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Error -> {
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
                            text = notesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(18.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            is UiState.Success -> {
                val allNotes = notesState.data
                val filteredNotes = remember(allNotes, searchQuery, activeFilter) {
                    val query = searchQuery.trim().lowercase()
                    allNotes.filter { note ->
                        val matchesFilter = when (activeFilter) {
                            NoteFilter.ALL -> true
                            NoteFilter.PINNED -> note.pinned
                            NoteFilter.PERSONAL -> note.categoryEnum == NoteCategory.PERSONAL
                            NoteFilter.WORK -> note.categoryEnum == NoteCategory.WORK
                            NoteFilter.IDEAS -> note.categoryEnum == NoteCategory.IDEAS
                            NoteFilter.STUDY -> note.categoryEnum == NoteCategory.STUDY
                            NoteFilter.OTHER -> note.categoryEnum == NoteCategory.OTHER
                        }
                        val matchesQuery = query.isEmpty() ||
                            note.title.lowercase().contains(query) ||
                            note.content.lowercase().contains(query)
                        matchesFilter && matchesQuery
                    }
                }

                if (filteredNotes.isEmpty()) {
                    NotesEmptyState(
                        isFiltered = searchQuery.isNotBlank() || activeFilter != NoteFilter.ALL,
                    )
                } else {
                    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.pinned } }
                    val regularNotes = remember(filteredNotes) { filteredNotes.filter { !it.pinned } }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = when {
                            maxWidth >= 840.dp -> 3
                            maxWidth >= 560.dp -> 2
                            else -> 2
                        }
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 6.dp,
                                bottom = 96.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (pinnedNotes.isNotEmpty() && activeFilter != NoteFilter.PINNED) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = stringResource(R.string.notes_section_pinned),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                                    )
                                }
                            }

                            items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note) },
                                    onTogglePin = { onTogglePin(note) },
                                )
                            }

                            if (pinnedNotes.isNotEmpty() && regularNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Text(
                                        text = stringResource(R.string.notes_section_others),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                    )
                                }
                            }

                            items(regularNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note) },
                                    onTogglePin = { onTogglePin(note) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val cardColor = noteSurfaceColor(note.colorEnum)
    val timeLabel = remember(note.updatedAt, note.createdAt) {
        formatNoteTimestamp(note.updatedAt, note.createdAt)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}"),
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("note_pin_${note.id}"),
                ) {
                    Icon(
                        imageVector = if (note.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(
                            if (note.pinned) R.string.notes_unpin_cd else R.string.notes_pin_cd
                        ),
                        tint = if (note.pinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = stringResource(note.categoryEnum.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(isFiltered: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
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
            text = stringResource(
                if (isFiltered) R.string.notes_empty_search_title else R.string.notes_empty_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isFiltered) R.string.notes_empty_search_subtitle else R.string.notes_empty_subtitle
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
