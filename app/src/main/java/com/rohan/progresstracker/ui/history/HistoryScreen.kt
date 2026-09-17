package com.rohan.progresstracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.domain.formatDuration
import com.rohan.progresstracker.domain.value
import com.rohan.progresstracker.domain.weekStart
import com.rohan.progresstracker.ui.AppViewModelFactory
import com.rohan.progresstracker.ui.edit.label
import com.rohan.progresstracker.ui.todo.EmptyMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress history") },
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val ui = state ?: return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                listOf(7, 30).forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = ui.days == days,
                        onClick = { viewModel.setDays(days) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text("Last $days days") }
                }
            }

            if (ui.tasks.isEmpty()) {
                EmptyMessage("No daily or weekly tasks yet.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.tasks, key = { it.task.id }) { history ->
                        TaskHistoryCard(history, ui)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskHistoryCard(history: TaskHistory, ui: HistoryUiState) {
    val task = history.task
    val frequency = if (task.frequency == Frequency.DAILY) "Daily" else "Weekly"
    val parameter = if (task.parameterType == ParameterType.NONE) "" else " · ${task.parameterType.label()}"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(task.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$frequency$parameter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                task.parameterType != ParameterType.NONE -> ValueChart(history, ui)
                task.frequency == Frequency.WEEKLY -> WeeklyCompletionList(history, ui)
                else -> DailyCompletionGrid(history, ui)
            }

            StatsRow(history)
        }
    }
}

/** Bar chart of minutes or quantity per day. */
@Composable
private fun ValueChart(history: TaskHistory, ui: HistoryUiState) {
    val isTime = history.task.parameterType == ParameterType.TIME
    val valueByDate = history.records.filter { it.completed }.associate { it.date to (it.value() ?: 0) }
    val dates = datesBetween(ui.rangeStart, ui.today)
    val labelFormat = DateTimeFormatter.ofPattern("d MMM")
    val entries = dates.map { date ->
        BarEntry(
            label = if (ui.days <= 7) date.shortDayName() else date.format(labelFormat),
            value = valueByDate[date] ?: 0,
        )
    }
    BarChart(
        entries = entries,
        formatValue = { if (isTime) formatDuration(it) else it.toString() },
        labelEvery = if (ui.days <= 7) 1 else 7,
        description = "${if (isTime) "Time" else "Quantity"} per day chart for ${history.task.name}",
    )
}

/** ✓ / × per day for completion-only daily tasks. */
@Composable
private fun DailyCompletionGrid(history: TaskHistory, ui: HistoryUiState) {
    val doneDates = history.records.filter { it.completed }.map { it.date }.toSet()
    val dates = datesBetween(ui.rangeStart, ui.today)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        dates.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    val label = if (ui.days <= 7) date.shortDayName() else date.dayOfMonth.toString()
                    val status = when {
                        date in doneDates -> DayStatus.DONE
                        date < history.task.createdDate || date == ui.today -> DayStatus.NOT_TRACKED
                        else -> DayStatus.MISSED
                    }
                    DayCell(label, status, Modifier.weight(1f))
                }
                // Keep cell sizes equal in a partial last row.
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** ✓ / × per week for completion-only weekly tasks. */
@Composable
private fun WeeklyCompletionList(history: TaskHistory, ui: HistoryUiState) {
    val doneWeeks = history.records.filter { it.completed }.map { weekStart(it.date) }.toSet()
    val firstWeek = weekStart(ui.rangeStart)
    val weeks = generateSequence(firstWeek) { it.plusWeeks(1) }.takeWhile { it <= ui.today }.toList()
    val format = DateTimeFormatter.ofPattern("d MMM")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        weeks.forEach { monday ->
            val status = when {
                monday in doneWeeks -> DayStatus.DONE
                monday.plusDays(6) < history.task.createdDate || monday == weekStart(ui.today) -> DayStatus.NOT_TRACKED
                else -> DayStatus.MISSED
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Week of ${monday.format(format)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status)
            }
        }
    }
}

private enum class DayStatus { DONE, MISSED, NOT_TRACKED }

@Composable
private fun DayCell(label: String, status: DayStatus, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label ${status.spoken()}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusBadge(status, Modifier.fillMaxWidth().aspectRatio(1f))
    }
}

@Composable
private fun StatusBadge(status: DayStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (symbol, background, foreground) = when (status) {
        DayStatus.DONE -> Triple("✓", colors.primary, colors.onPrimary)
        DayStatus.MISSED -> Triple("×", colors.errorContainer, colors.onErrorContainer)
        DayStatus.NOT_TRACKED -> Triple("–", Color.Transparent, colors.onSurfaceVariant)
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = foreground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatsRow(history: TaskHistory) {
    val stats = history.stats
    val isTime = history.task.parameterType == ParameterType.TIME
    val unit = if (history.task.frequency == Frequency.DAILY) "Days" else "Weeks"
    val format: (Int) -> String = { if (isTime) formatDuration(it) else it.toString() }

    Row(modifier = Modifier.fillMaxWidth()) {
        stats.total?.let { Stat("Total", format(it), Modifier.weight(1f)) }
        if (history.task.parameterType != ParameterType.NONE) {
            Stat("Average", stats.average?.let { format(it.roundToInt()) } ?: "–", Modifier.weight(1f))
        }
        Stat(unit, "${stats.completedPeriods}/${stats.totalPeriods}", Modifier.weight(1f))
        Stat("Completion", "${stats.completionPercent}%", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun DayStatus.spoken() = when (this) {
    DayStatus.DONE -> "completed"
    DayStatus.MISSED -> "not completed"
    DayStatus.NOT_TRACKED -> "not tracked yet"
}

private fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> =
    generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.toList()

private fun LocalDate.shortDayName(): String = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
