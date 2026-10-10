package com.rohan.progresstracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rohan.progresstracker.R
import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.domain.formatDuration
import com.rohan.progresstracker.domain.value
import com.rohan.progresstracker.domain.weekStart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One task's history, shown inside its card on the Progress screen when expanded:
 * a header with the range picker, then a chart / grid / week list, then the statistics.
 */
@Composable
fun TaskHistoryPanel(history: TaskHistory, onSelectDays: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HistoryHeader(days = history.days, onSelectDays = onSelectDays)
        when {
            history.task.parameterType != ParameterType.NONE -> ValueChart(history)
            history.task.frequency == Frequency.WEEKLY -> WeeklyCompletionList(history)
            else -> DailyCompletionGrid(history)
        }
        StatsRow(history)
    }
}

@Composable
private fun HistoryHeader(days: Int, onSelectDays: (Int) -> Unit) {
    var rangeMenuOpen by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("History", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { rangeMenuOpen = true }) {
                Text("Last $days days")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Change history range")
            }
            DropdownMenu(expanded = rangeMenuOpen, onDismissRequest = { rangeMenuOpen = false }) {
                HISTORY_RANGES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("Last $option days") },
                        leadingIcon = {
                            if (option == days) Icon(Icons.Filled.Check, contentDescription = "Selected")
                            else Spacer(Modifier.width(24.dp))
                        },
                        onClick = {
                            rangeMenuOpen = false
                            onSelectDays(option)
                        },
                    )
                }
            }
        }
    }
}

/** Bar chart of minutes or quantity per day. */
@Composable
private fun ValueChart(history: TaskHistory) {
    val isTime = history.task.parameterType == ParameterType.TIME
    val valueByDate = history.records.filter { it.completed }.associate { it.date to (it.value() ?: 0) }
    val dates = datesBetween(history.rangeStart, history.today)
    val labelFormat = DateTimeFormatter.ofPattern("d MMM")
    val entries = dates.map { date ->
        BarEntry(
            label = if (history.days <= 7) date.shortDayName() else date.format(labelFormat),
            value = valueByDate[date] ?: 0,
        )
    }
    BarChart(
        entries = entries,
        formatValue = { if (isTime) formatDuration(it) else it.toString() },
        labelEvery = when {
            history.days <= 7 -> 1
            history.days <= 30 -> 7
            else -> 21
        },
        description = "${if (isTime) "Time" else "Quantity"} per day chart for ${history.task.name}",
    )
}

/** ✓ / × per day for completion-only daily tasks. */
@Composable
private fun DailyCompletionGrid(history: TaskHistory) {
    val doneDates = history.records.filter { it.completed }.map { it.date }.toSet()
    val dates = datesBetween(history.rangeStart, history.today)
    // Long ranges use small coloured squares (two weeks per row) so the card doesn't get too tall.
    val compact = history.days > 30
    val perRow = if (compact) 14 else 7
    val spacing = if (compact) 4.dp else 6.dp

    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        dates.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { date ->
                    val status = when {
                        date in doneDates -> DayStatus.DONE
                        date < history.task.createdDate || date == history.today -> DayStatus.NOT_TRACKED
                        else -> DayStatus.MISSED
                    }
                    if (compact) {
                        CompactDayCell(date, status, Modifier.weight(1f))
                    } else {
                        val label = if (history.days <= 7) date.shortDayName() else date.dayOfMonth.toString()
                        DayCell(label, status, Modifier.weight(1f))
                    }
                }
                // Keep cell sizes equal in a partial last row.
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** ✓ / × per week for completion-only weekly tasks. */
@Composable
private fun WeeklyCompletionList(history: TaskHistory) {
    val doneWeeks = history.records.filter { it.completed }.map { weekStart(it.date) }.toSet()
    val firstWeek = weekStart(history.rangeStart)
    val weeks = generateSequence(firstWeek) { it.plusWeeks(1) }.takeWhile { it <= history.today }.toList()
    val format = DateTimeFormatter.ofPattern("d MMM")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        weeks.forEach { monday ->
            val status = when {
                monday in doneWeeks -> DayStatus.DONE
                monday.plusDays(6) < history.task.createdDate || monday == weekStart(history.today) -> DayStatus.NOT_TRACKED
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

/** A small square coloured by status, with the day of the month inside. */
@Composable
private fun CompactDayCell(date: LocalDate, status: DayStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when (status) {
        DayStatus.DONE -> colors.primary to colors.onPrimary
        DayStatus.MISSED -> colors.errorContainer to colors.onErrorContainer
        DayStatus.NOT_TRACKED -> Color.Transparent to colors.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(4.dp))
            .clearAndSetSemantics {
                contentDescription = "${date.format(DateTimeFormatter.ofPattern("d MMM"))} ${status.spoken()}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = foreground)
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
