package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Note
import com.example.data.TaskItem
import com.example.data.TaskList
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.UiState
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileTab(
    user: FirebaseUser,
    notesState: UiState<List<Note>>,
    taskListsState: UiState<List<TaskList>>,
    tasksState: UiState<List<TaskItem>>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val lists = (taskListsState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val isSyncing = notesState is UiState.Loading || taskListsState is UiState.Loading || tasksState is UiState.Loading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.cloud_screen_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // User Account Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val photoUrl = user.photoUrl
                    val displayName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.cloud_account_guest_name)
                    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "F"

                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(R.string.cloud_avatar_cd),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!user.email.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Live Cloud Sync Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(
                                if (isSyncing) R.string.cloud_status_syncing else R.string.cloud_status_synced
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.cloud_status_offline_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    )
                }
            }

            // Synced Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricTile(
                    icon = Icons.Rounded.Description,
                    value = notes.size.toString(),
                    label = stringResource(R.string.cloud_stats_notes),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    icon = Icons.Rounded.PushPin,
                    value = notes.count { it.pinned }.toString(),
                    label = stringResource(R.string.cloud_stats_pinned),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricTile(
                    icon = Icons.Rounded.FolderCopy,
                    value = lists.size.toString(),
                    label = stringResource(R.string.cloud_stats_lists),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    icon = Icons.Rounded.Checklist,
                    value = tasks.count { !it.completed }.toString(),
                    label = stringResource(R.string.cloud_stats_active_tasks),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    icon = Icons.Rounded.TaskAlt,
                    value = tasks.count { it.completed }.toString(),
                    label = stringResource(R.string.cloud_stats_done_tasks),
                    modifier = Modifier.weight(1f),
                )
            }

            // Appearance Theme Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cloud_theme_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val options = listOf(
                            Triple(ThemeMode.SYSTEM, R.string.cloud_theme_system, Icons.Rounded.SettingsBrightness),
                            Triple(ThemeMode.LIGHT, R.string.cloud_theme_light, Icons.Rounded.LightMode),
                            Triple(ThemeMode.DARK, R.string.cloud_theme_dark, Icons.Rounded.DarkMode),
                        )
                        options.forEach { (mode, labelRes, icon) ->
                            val selected = themeMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onThemeModeChange(mode) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.key}"),
                            )
                        }
                    }
                }
            }

            // Sign Out Button
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button"),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.cloud_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(stringResource(R.string.cloud_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.cloud_sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOutClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.cloud_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
