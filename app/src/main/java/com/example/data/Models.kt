package com.example.data

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class NoteCategory(val key: String, @StringRes val labelRes: Int) {
    PERSONAL("personal", R.string.category_personal),
    WORK("work", R.string.category_work),
    IDEAS("ideas", R.string.category_ideas),
    STUDY("study", R.string.category_study),
    OTHER("other", R.string.category_other);

    companion object {
        fun fromRaw(raw: String?): NoteCategory =
            entries.firstOrNull { it.key == raw } ?: PERSONAL
    }
}

enum class NoteColorKey(val key: String) {
    DEFAULT("default"),
    SAND("sand"),
    SAGE("sage"),
    TERRACOTTA("terracotta"),
    SLATE("slate"),
    OCHRE("ochre");

    companion object {
        fun fromRaw(raw: String?): NoteColorKey =
            entries.firstOrNull { it.key == raw } ?: DEFAULT
    }
}

enum class ListColorKey(val key: String) {
    TERRACOTTA("terracotta"),
    SAGE("sage"),
    OCHRE("ochre"),
    SLATE("slate"),
    SAND("sand");

    companion object {
        fun fromRaw(raw: String?): ListColorKey =
            entries.firstOrNull { it.key == raw } ?: TERRACOTTA
    }
}

enum class ListIconKey(val key: String) {
    CHECKLIST("checklist"),
    WORK("work"),
    SHOPPING("shopping"),
    HOME("home"),
    STAR("star");

    companion object {
        fun fromRaw(raw: String?): ListIconKey =
            entries.firstOrNull { it.key == raw } ?: CHECKLIST
    }
}

enum class TaskPriority(val key: String, @StringRes val labelRes: Int, val weight: Int) {
    HIGH("high", R.string.priority_high, 3),
    MEDIUM("medium", R.string.priority_medium, 2),
    LOW("low", R.string.priority_low, 1);

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { it.key == raw } ?: MEDIUM
    }
}

@Keep
@IgnoreExtraProperties
data class Note(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = NoteCategory.PERSONAL.key,
    val colorKey: String = NoteColorKey.DEFAULT.key,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val categoryEnum: NoteCategory get() = NoteCategory.fromRaw(category)
    val colorEnum: NoteColorKey get() = NoteColorKey.fromRaw(colorKey)
}

@Keep
@IgnoreExtraProperties
data class TaskList(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorKey: String = ListColorKey.TERRACOTTA.key,
    val iconKey: String = ListIconKey.CHECKLIST.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val colorEnum: ListColorKey get() = ListColorKey.fromRaw(colorKey)
    val iconEnum: ListIconKey get() = ListIconKey.fromRaw(iconKey)
}

@Keep
@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val listId: String = "",
    val title: String = "",
    val details: String = "",
    val completed: Boolean = false,
    val priority: String = TaskPriority.MEDIUM.key,
    val dueDateEpochDay: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val priorityEnum: TaskPriority get() = TaskPriority.fromRaw(priority)
}

object FieldLimits {
    const val NOTE_TITLE_MAX = 120
    const val NOTE_CONTENT_MAX = 10000
    const val LIST_NAME_MAX = 60
    const val TASK_TITLE_MAX = 200
    const val TASK_DETAILS_MAX = 2000
}
