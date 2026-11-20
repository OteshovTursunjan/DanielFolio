package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FolioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    private fun notesCollection(userId: String) =
        db.collection("users").document(userId).collection("notes")

    private fun taskListsCollection(userId: String) =
        db.collection("users").document(userId).collection("taskLists")

    private fun tasksCollection(userId: String) =
        db.collection("users").document(userId).collection("tasks")

    fun observeNotes(userId: String): Flow<List<Note>> {
        if (userId.isBlank()) return emptyFlow()
        val path = "users/$userId/notes"
        return notesCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Note::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<Note> { it.pinned }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeTaskLists(userId: String): Flow<List<TaskList>> {
        if (userId.isBlank()) return emptyFlow()
        val path = "users/$userId/taskLists"
        return taskListsCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TaskList::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeTasks(userId: String): Flow<List<TaskItem>> {
        if (userId.isBlank()) return emptyFlow()
        val path = "users/$userId/tasks"
        return tasksCollection(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<TaskItem> { it.completed }
                            .thenByDescending { it.priorityEnum.weight }
                            .thenBy { it.dueDateEpochDay ?: Long.MAX_VALUE }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun addNote(
        userId: String,
        title: String,
        content: String,
        category: NoteCategory,
        colorKey: NoteColorKey,
        pinned: Boolean,
    ): Result<String> {
        var docPath = "users/$userId/notes"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim().take(FieldLimits.NOTE_TITLE_MAX)
            val cleanContent = content.trim().take(FieldLimits.NOTE_CONTENT_MAX)
            require(cleanTitle.isNotEmpty() || cleanContent.isNotEmpty()) {
                "Note must have a title or content."
            }
            val docRef = notesCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "content" to cleanContent,
                "category" to category.key,
                "colorKey" to colorKey.key,
                "pinned" to pinned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNote(
        userId: String,
        noteId: String,
        title: String,
        content: String,
        category: NoteCategory,
        colorKey: NoteColorKey,
        pinned: Boolean,
    ): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank." }
            val cleanTitle = title.trim().take(FieldLimits.NOTE_TITLE_MAX)
            val cleanContent = content.trim().take(FieldLimits.NOTE_CONTENT_MAX)
            require(cleanTitle.isNotEmpty() || cleanContent.isNotEmpty()) {
                "Note must have a title or content."
            }
            val updates = mapOf(
                "title" to cleanTitle,
                "content" to cleanContent,
                "category" to category.key,
                "colorKey" to colorKey.key,
                "pinned" to pinned,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            notesCollection(uid).document(noteId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleNotePin(userId: String, noteId: String, pinned: Boolean): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank." }
            notesCollection(uid).document(noteId).update(
                mapOf(
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val docPath = "users/$userId/notes/$noteId"
        return try {
            val uid = requireUserId(userId)
            require(noteId.isNotBlank()) { "Note ID must not be blank." }
            notesCollection(uid).document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTaskList(
        userId: String,
        name: String,
        colorKey: ListColorKey,
        iconKey: ListIconKey,
    ): Result<String> {
        var docPath = "users/$userId/taskLists"
        return try {
            val uid = requireUserId(userId)
            val cleanName = name.trim().take(FieldLimits.LIST_NAME_MAX)
            require(cleanName.isNotEmpty()) { "Task list name must not be empty." }
            val docRef = taskListsCollection(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "colorKey" to colorKey.key,
                "iconKey" to iconKey.key,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTaskList(
        userId: String,
        listId: String,
        name: String,
        colorKey: ListColorKey,
        iconKey: ListIconKey,
    ): Result<Unit> {
        val docPath = "users/$userId/taskLists/$listId"
        return try {
            val uid = requireUserId(userId)
            require(listId.isNotBlank()) { "Task list ID must not be blank." }
            val cleanName = name.trim().take(FieldLimits.LIST_NAME_MAX)
            require(cleanName.isNotEmpty()) { "Task list name must not be empty." }
            taskListsCollection(uid).document(listId).update(
                mapOf(
                    "name" to cleanName,
                    "colorKey" to colorKey.key,
                    "iconKey" to iconKey.key,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTaskList(userId: String, listId: String, taskIdsInList: List<String>): Result<Unit> {
        val docPath = "users/$userId/taskLists/$listId"
        return try {
            val uid = requireUserId(userId)
            require(listId.isNotBlank()) { "Task list ID must not be blank." }
            val batch = db.batch()
            taskIdsInList.filter { it.isNotBlank() }.forEach { taskId ->
                batch.delete(tasksCollection(uid).document(taskId))
            }
            batch.delete(taskListsCollection(uid).document(listId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addTask(
        userId: String,
        listId: String,
        title: String,
        details: String,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
    ): Result<String> {
        var docPath = "users/$userId/tasks"
        return try {
            val uid = requireUserId(userId)
            require(listId.isNotBlank()) { "List ID must not be blank." }
            val cleanTitle = title.trim().take(FieldLimits.TASK_TITLE_MAX)
            require(cleanTitle.isNotEmpty()) { "Task title must not be empty." }
            val cleanDetails = details.trim().take(FieldLimits.TASK_DETAILS_MAX)
            val safeDueDay = dueDateEpochDay?.takeIf { it in 0L..100000L }

            val docRef = tasksCollection(uid).document()
            docPath = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "listId" to listId,
                "title" to cleanTitle,
                "details" to cleanDetails,
                "completed" to false,
                "priority" to priority.key,
                "dueDateEpochDay" to safeDueDay,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTask(
        userId: String,
        taskId: String,
        listId: String,
        title: String,
        details: String,
        completed: Boolean,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
    ): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            require(listId.isNotBlank()) { "List ID must not be blank." }
            val cleanTitle = title.trim().take(FieldLimits.TASK_TITLE_MAX)
            require(cleanTitle.isNotEmpty()) { "Task title must not be empty." }
            val cleanDetails = details.trim().take(FieldLimits.TASK_DETAILS_MAX)
            val safeDueDay = dueDateEpochDay?.takeIf { it in 0L..100000L }

            val updates = mapOf<String, Any>(
                "listId" to listId,
                "title" to cleanTitle,
                "details" to cleanDetails,
                "completed" to completed,
                "priority" to priority.key,
                "dueDateEpochDay" to (safeDueDay ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            tasksCollection(uid).document(taskId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(userId: String, taskId: String, completed: Boolean): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            tasksCollection(uid).document(taskId).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docPath = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "Task ID must not be blank." }
            tasksCollection(uid).document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
