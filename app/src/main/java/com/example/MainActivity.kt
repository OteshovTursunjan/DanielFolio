package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.FolioRepository
import com.example.data.Note
import com.example.data.TaskItem
import com.example.data.TaskList
import com.example.ui.AuthScreen
import com.example.ui.NoteEditorScreen
import com.example.ui.NotesTab
import com.example.ui.ProfileTab
import com.example.ui.TaskEditorSheet
import com.example.ui.TaskListEditorSheet
import com.example.ui.TasksTab
import com.example.ui.theme.FolioTheme
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.FolioViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val PREFS_NAME = "folio_preferences"
private const val KEY_THEME_MODE = "theme_mode"

private enum class FolioTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    NOTES(R.string.nav_notes, Icons.Rounded.Description, Icons.Outlined.Description, "nav_notes"),
    TASKS(R.string.nav_tasks, Icons.Rounded.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    CLOUD(R.string.nav_cloud, Icons.Rounded.CloudDone, Icons.Outlined.CloudQueue, "nav_cloud"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialTheme = ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key))

        setContent {
            var themeMode by remember { mutableStateOf(initialTheme) }
            FolioTheme(themeMode = themeMode) {
                FolioAppNavigation(
                    themeMode = themeMode,
                    onThemeModeChange = { next ->
                        themeMode = next
                        prefs.edit().putString(KEY_THEME_MODE, next.key).apply()
                    },
                )
            }
        }
    }
}

@Composable
private fun FolioAppNavigation(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    auth: FirebaseAuth = Firebase.auth,
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            FolioWorkspace(
                user = user,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }
    }
}

@Composable
private fun FolioWorkspace(
    user: FirebaseUser,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: FolioViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FolioViewModel(FolioRepository(db), user.uid)
            }
        },
    )

    val notesState by viewModel.notesState.collectAsStateWithLifecycle()
    val taskListsState by viewModel.taskListsState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.noteSearchQuery.collectAsStateWithLifecycle()
    val noteFilter by viewModel.noteFilter.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val completionFilter by viewModel.taskCompletionFilter.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(FolioTab.NOTES) }
    var isNoteEditorOpen by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    var isListSheetOpen by remember { mutableStateOf(false) }
    var editingTaskList by remember { mutableStateOf<TaskList?>(null) }

    var isTaskSheetOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    val defaultListName = stringResource(R.string.tasks_default_list_name)

    if (isNoteEditorOpen) {
        NoteEditorScreen(
            note = editingNote,
            isSaving = isSaving,
            onBack = {
                isNoteEditorOpen = false
                editingNote = null
            },
            onSave = { title, content, category, colorKey, pinned ->
                viewModel.saveNote(
                    existingNoteId = editingNote?.id,
                    title = title,
                    content = content,
                    category = category,
                    colorKey = colorKey,
                    pinned = pinned,
                    onComplete = {
                        isNoteEditorOpen = false
                        editingNote = null
                    },
                )
            },
            onDelete = editingNote?.let { target ->
                {
                    viewModel.deleteNote(target.id) {
                        isNoteEditorOpen = false
                        editingNote = null
                    }
                }
            },
        )
        return
    }

    BackHandler(enabled = currentTab != FolioTab.NOTES) {
        currentTab = FolioTab.NOTES
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    FolioTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag(tab.tag),
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars,
                            tonalElevation = 2.dp,
                        ) {
                            FolioTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(tab.labelRes),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag(tab.tag),
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    when (currentTab) {
                        FolioTab.NOTES -> {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    editingNote = null
                                    isNoteEditorOpen = true
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(R.string.notes_fab_new)) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_new_note"),
                            )
                        }
                        FolioTab.TASKS -> {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    editingTask = null
                                    isTaskSheetOpen = true
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(R.string.tasks_fab_new)) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_new_task"),
                            )
                        }
                        FolioTab.CLOUD -> Unit
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when (currentTab) {
                        FolioTab.NOTES -> {
                            NotesTab(
                                notesState = notesState,
                                searchQuery = searchQuery,
                                activeFilter = noteFilter,
                                onSearchChange = viewModel::setNoteSearchQuery,
                                onFilterSelect = viewModel::setNoteFilter,
                                onNoteClick = { note ->
                                    editingNote = note
                                    isNoteEditorOpen = true
                                },
                                onTogglePin = viewModel::toggleNotePin,
                            )
                        }
                        FolioTab.TASKS -> {
                            TasksTab(
                                taskListsState = taskListsState,
                                tasksState = tasksState,
                                selectedListId = selectedListId,
                                completionFilter = completionFilter,
                                onSelectList = viewModel::selectTaskList,
                                onCompletionFilterSelect = viewModel::setTaskCompletionFilter,
                                onCreateListClick = {
                                    editingTaskList = null
                                    isListSheetOpen = true
                                },
                                onEditListClick = { list ->
                                    editingTaskList = list
                                    isListSheetOpen = true
                                },
                                onDeleteListConfirm = { list, allTasks ->
                                    viewModel.deleteTaskList(list.id, allTasks)
                                },
                                onTaskClick = { task ->
                                    editingTask = task
                                    isTaskSheetOpen = true
                                },
                                onToggleTask = viewModel::toggleTaskCompleted,
                            )
                        }
                        FolioTab.CLOUD -> {
                            ProfileTab(
                                user = user,
                                notesState = notesState,
                                taskListsState = taskListsState,
                                tasksState = tasksState,
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                onSignOutClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (isListSheetOpen) {
        TaskListEditorSheet(
            existingList = editingTaskList,
            isSaving = isSaving,
            onDismiss = {
                isListSheetOpen = false
                editingTaskList = null
            },
            onSave = { name, colorKey, iconKey ->
                viewModel.saveTaskList(
                    existingListId = editingTaskList?.id,
                    name = name,
                    colorKey = colorKey,
                    iconKey = iconKey,
                    onComplete = {
                        isListSheetOpen = false
                        editingTaskList = null
                    },
                )
            },
        )
    }

    if (isTaskSheetOpen) {
        val availableLists = (taskListsState as? UiState.Success)?.data.orEmpty()
        TaskEditorSheet(
            existingTask = editingTask,
            availableLists = availableLists,
            initialListId = selectedListId,
            isSaving = isSaving,
            onDismiss = {
                isTaskSheetOpen = false
                editingTask = null
            },
            onSave = { listId, title, details, priority, dueDateEpochDay ->
                viewModel.saveTask(
                    existingTask = editingTask,
                    listId = listId,
                    defaultListName = defaultListName,
                    title = title,
                    details = details,
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                    onComplete = {
                        isTaskSheetOpen = false
                        editingTask = null
                    },
                )
            },
            onDelete = editingTask?.let { target ->
                {
                    viewModel.deleteTask(target.id) {
                        isTaskSheetOpen = false
                        editingTask = null
                    }
                }
            },
        )
    }
}
