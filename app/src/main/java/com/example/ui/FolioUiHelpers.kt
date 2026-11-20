package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ListColorKey
import com.example.data.ListIconKey
import com.example.data.NoteColorKey
import com.example.data.TaskPriority
import com.example.ui.theme.LocalStationeryPalette
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun noteSurfaceColor(colorKey: NoteColorKey): Color {
    val palette = LocalStationeryPalette.current
    return when (colorKey) {
        NoteColorKey.DEFAULT -> palette.noteDefault
        NoteColorKey.SAND -> palette.noteSand
        NoteColorKey.SAGE -> palette.noteSage
        NoteColorKey.TERRACOTTA -> palette.noteTerracotta
        NoteColorKey.SLATE -> palette.noteSlate
        NoteColorKey.OCHRE -> palette.noteOchre
    }
}

@Composable
fun listAccentColor(colorKey: ListColorKey): Color {
    val palette = LocalStationeryPalette.current
    return when (colorKey) {
        ListColorKey.TERRACOTTA -> palette.accentTerracotta
        ListColorKey.SAGE -> palette.accentSage
        ListColorKey.OCHRE -> palette.accentOchre
        ListColorKey.SLATE -> palette.accentSlate
        ListColorKey.SAND -> palette.accentSand
    }
}

@Composable
fun priorityAccentColor(priority: TaskPriority): Color {
    val palette = LocalStationeryPalette.current
    return when (priority) {
        TaskPriority.HIGH -> palette.accentTerracotta
        TaskPriority.MEDIUM -> palette.accentOchre
        TaskPriority.LOW -> palette.accentSage
    }
}

fun listIconVector(iconKey: ListIconKey): ImageVector = when (iconKey) {
    ListIconKey.CHECKLIST -> Icons.Rounded.Checklist
    ListIconKey.WORK -> Icons.Rounded.WorkOutline
    ListIconKey.SHOPPING -> Icons.Rounded.ShoppingBag
    ListIconKey.HOME -> Icons.Rounded.Home
    ListIconKey.STAR -> Icons.Rounded.Star
}

fun formatNoteTimestamp(updatedAt: Timestamp?, createdAt: Timestamp?): String {
    val date = (updatedAt ?: createdAt ?: Timestamp.now()).toDate()
    val nowCal = Calendar.getInstance()
    val targetCal = Calendar.getInstance().apply { time = date }
    val ruLocale = Locale("ru", "RU")
    return if (
        nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("HH:mm", ruLocale).format(date)
    } else if (nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)) {
        SimpleDateFormat("d MMM", ruLocale).format(date)
    } else {
        SimpleDateFormat("d MMM yyyy", ruLocale).format(date)
    }
}

fun todayEpochDay(): Long {
    val cal = Calendar.getInstance()
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }
    return utcCal.timeInMillis / 86_400_000L
}

fun epochMillisToEpochDay(utcMillis: Long): Long = utcMillis / 86_400_000L

fun epochDayToUtcMillis(epochDay: Long): Long = epochDay * 86_400_000L

@Composable
fun formatDueDateLabel(epochDay: Long): Pair<String, Boolean> {
    val today = todayEpochDay()
    val ruLocale = Locale("ru", "RU")
    val formatter = SimpleDateFormat("d MMM", ruLocale).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val formatted = formatter.format(Date(epochDayToUtcMillis(epochDay)))
    return when {
        epochDay == today -> stringResource(R.string.tasks_due_today) to false
        epochDay == today + 1 -> stringResource(R.string.tasks_due_tomorrow) to false
        epochDay < today -> stringResource(R.string.tasks_due_overdue, formatted) to true
        else -> formatted to false
    }
}
