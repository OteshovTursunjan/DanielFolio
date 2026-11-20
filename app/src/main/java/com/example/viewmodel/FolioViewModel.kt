package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.FolioRepository
import com.example.data.ListColorKey
import com.example.data.ListIconKey
import com.example.data.Note
import com.example.data.NoteCategory
import com.example.data.NoteColorKey
import com.example.data.TaskItem
import com.example.data.TaskList
import com.example.data.TaskPriority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class NoteFilter {
    ALL,
    PINNED,
    PERSONAL,
    WORK,
    IDEAS,
    STUDY,
    OTHER,
}

enum class TaskCompletionFilter {
    ALL,
    ACTIVE,
    COMPLETED,
}

data class SnackbarEvent(val messageRes: Int)

class FolioViewModel(
    private val repository: FolioRepository,
    private val currentUserId: String,
) : ViewModel() {

    val notesState: StateFlow<UiState<List<Note>>> = repository.observeNotes(currentUserId)
        .map<List<Note>, UiState<List<Note>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing notes", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load notes"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val taskListsState: StateFlow<UiState<List<TaskList>>> = repository.observeTaskLists(currentUserId)
        .map<List<TaskList>, UiState<List<TaskList>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing task lists", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load task lists"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val tasksState: StateFlow<UiState<List<TaskItem>>> = repository.observeTasks(currentUserId)
        .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to load tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _noteFilter = MutableStateFlow(NoteFilter.ALL)
    val noteFilter: StateFlow<NoteFilter> = _noteFilter.asStateFlow()

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId: StateFlow<String?> = _selectedListId.asStateFlow()

    private val _taskCompletionFilter = MutableStateFlow(TaskCompletionFilter.ALL)
    val taskCompletionFilter: StateFlow<TaskCompletionFilter> = _taskCompletionFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun setNoteFilter(filter: NoteFilter) {
        _noteFilter.value = filter
    }

    fun selectTaskList(listId: String?) {
        _selectedListId.value = listId
    }

    fun setTaskCompletionFilter(filter: TaskCompletionFilter) {
        _taskCompletionFilter.value = filter
    }

    fun saveNote(
        existingNoteId: String?,
        title: String,
        content: String,
        category: NoteCategory,
        colorKey: NoteColorKey,
        pinned: Boolean,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (existingNoteId.isNullOrBlank()) {
                repository.addNote(
                    userId = currentUserId,
                    title = title,
                    content = content,
                    category = category,
                    colorKey = colorKey,
                    pinned = pinned,
                ).map { Unit }
            } else {
                repository.updateNote(
                    userId = currentUserId,
                    noteId = existingNoteId,
                    title = title,
                    content = content,
                    category = category,
                    colorKey = colorKey,
                    pinned = pinned,
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_note_saved))
                onComplete()
            }.onFailure {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
            }
        }
    }

    fun toggleNotePin(note: Note) {
        viewModelScope.launch {
            repository.toggleNotePin(currentUserId, note.id, !note.pinned)
                .onFailure {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                }
        }
    }

    fun deleteNote(noteId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId)
                .onSuccess {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_note_deleted))
                    onComplete()
                }
                .onFailure {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                }
        }
    }

    fun saveTaskList(
        existingListId: String?,
        name: String,
        colorKey: ListColorKey,
        iconKey: ListIconKey,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = if (existingListId.isNullOrBlank()) {
                repository.addTaskList(currentUserId, name, colorKey, iconKey)
                    .onSuccess { createdId ->
                        _selectedListId.value = createdId
                    }
                    .map { Unit }
            } else {
                repository.updateTaskList(currentUserId, existingListId, name, colorKey, iconKey)
            }
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_list_saved))
                onComplete()
            }.onFailure {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
            }
        }
    }

    fun deleteTaskList(listId: String, allTasks: List<TaskItem>) {
        viewModelScope.launch {
            val taskIds = allTasks.filter { it.listId == listId }.map { it.id }
            repository.deleteTaskList(currentUserId, listId, taskIds)
                .onSuccess {
                    if (_selectedListId.value == listId) {
                        _selectedListId.value = null
                    }
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_list_deleted))
                }
                .onFailure {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                }
        }
    }

    fun saveTask(
        existingTask: TaskItem?,
        listId: String?,
        defaultListName: String,
        title: String,
        details: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val resolvedListId = if (!listId.isNullOrBlank()) {
                listId
            } else {
                val created = repository.addTaskList(
                    userId = currentUserId,
                    name = defaultListName,
                    colorKey = ListColorKey.TERRACOTTA,
                    iconKey = ListIconKey.CHECKLIST,
                )
                created.getOrElse {
                    _isSaving.value = false
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                    return@launch
                }
            }

            val result = if (existingTask == null) {
                repository.addTask(
                    userId = currentUserId,
                    listId = resolvedListId,
                    title = title,
                    details = details,
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                ).map { Unit }
            } else {
                repository.updateTask(
                    userId = currentUserId,
                    taskId = existingTask.id,
                    listId = resolvedListId,
                    title = title,
                    details = details,
                    completed = existingTask.completed,
                    priority = priority,
                    dueDateEpochDay = dueDateEpochDay,
                )
            }
            _isSaving.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_task_saved))
                onComplete()
            }.onFailure {
                _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
            }
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(currentUserId, task.id, !task.completed)
                .onFailure {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                }
        }
    }

    fun deleteTask(taskId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTask(currentUserId, taskId)
                .onSuccess {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_task_deleted))
                    onComplete()
                }
                .onFailure {
                    _snackbarEvents.tryEmit(SnackbarEvent(R.string.msg_sync_error))
                }
        }
    }

    private companion object {
        const val TAG = "FolioVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
