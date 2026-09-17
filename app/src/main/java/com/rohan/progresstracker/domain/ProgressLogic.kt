package com.rohan.progresstracker.domain

import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.data.ProgressRecord
import com.rohan.progresstracker.data.RecurringTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

// Plain Kotlin rules for dates, durations and statistics. No Android code here, so it is unit-testable.

/** Weeks run Monday to Sunday. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** The period (first day, last day) in which [date] falls for a task of the given frequency. */
fun periodFor(frequency: Frequency, date: LocalDate): Pair<LocalDate, LocalDate> = when (frequency) {
    Frequency.DAILY -> date to date
    Frequency.WEEKLY -> weekStart(date).let { it to it.plusDays(6) }
}

/** Minutes from [start] to [end]. If end is earlier than start, the activity is assumed to cross midnight. */
fun minutesBetween(start: LocalTime, end: LocalTime): Int {
    val s = start.hour * 60 + start.minute
    val e = end.hour * 60 + end.minute
    return if (e >= s) e - s else e + 24 * 60 - s
}

private val clockFormat = Regex("""^(\d{1,2}):(\d{2})$""")
private val durationPart = Regex("""(\d+(?:\.\d+)?)\s*(hours|hour|hrs|hr|h|minutes|minute|mins|min|m)""")

/**
 * Parses user-typed durations into minutes. Accepts e.g. "90", "90m", "90 min", "1h 30m", "1h30m",
 * "1:30", "1.5h", "1 hour 30 minutes". Returns null if the text is not understood or is not positive.
 */
fun parseDurationMinutes(input: String): Int? {
    val text = input.trim().lowercase()
    if (text.isEmpty()) return null

    val minutes: Int? = when {
        text.all { it.isDigit() } -> text.toIntOrNull()
        clockFormat.matches(text) -> {
            val (h, m) = clockFormat.find(text)!!.destructured
            if (m.toInt() < 60) h.toInt() * 60 + m.toInt() else null
        }
        else -> {
            // The whole text must be made of "<number><unit>" parts.
            val leftover = durationPart.replace(text, "").replace(" ", "")
            if (leftover.isNotEmpty()) null
            else durationPart.findAll(text).sumOf { match ->
                val value = match.groupValues[1].toDouble()
                if (match.groupValues[2].startsWith("h")) value * 60 else value
            }.roundToInt()
        }
    }
    return minutes?.takeIf { it > 0 }
}

/** 45 -> "45 min", 90 -> "1h 30m", 120 -> "2h". */
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** The number a record contributes to graphs and totals (null for completion-only tasks). */
fun ProgressRecord.value(): Int? = when (parameterType) {
    ParameterType.NONE -> null
    ParameterType.TIME -> durationMinutes
    ParameterType.QUANTITY -> quantity
}

/** Short text describing a record, e.g. "Completed", "45 min", "80". */
fun describe(record: ProgressRecord?): String = when {
    record == null || !record.completed -> "Not completed"
    record.parameterType == ParameterType.TIME -> formatDuration(record.durationMinutes ?: 0)
    record.parameterType == ParameterType.QUANTITY -> (record.quantity ?: 0).toString()
    else -> "Completed"
}

data class ProgressStats(
    /** Days (daily tasks) or weeks (weekly tasks) with progress. */
    val completedPeriods: Int,
    /** Days or weeks in the selected range since the task was created (the current one only if completed). */
    val totalPeriods: Int,
    /** Sum of minutes or quantity; null for completion-only tasks. */
    val total: Int?,
    /** Average per completed day/week; null for completion-only tasks or no progress. */
    val average: Double?,
) {
    val completionPercent: Int
        get() = if (totalPeriods == 0) 0 else (completedPeriods * 100.0 / totalPeriods).roundToInt()
}

/** Statistics for [task] over [rangeStart]..[today], given the task's records in that range. */
fun computeStats(
    task: RecurringTask,
    records: List<ProgressRecord>,
    rangeStart: LocalDate,
    today: LocalDate,
): ProgressStats {
    val start = maxOf(rangeStart, task.createdDate)
    // For weekly tasks the first week counts in full, even if it began before the range.
    val firstDay = if (task.frequency == Frequency.WEEKLY) weekStart(start) else start
    val completed = records.filter { it.completed && it.date in firstDay..today }

    val (completedPeriods, totalPeriods) = when (task.frequency) {
        Frequency.DAILY -> {
            val days = if (start > today) 0 else (today.toEpochDay() - start.toEpochDay() + 1).toInt()
            completed.map { it.date }.distinct().size to days
        }
        Frequency.WEEKLY -> {
            val weeks = if (start > today) 0 else
                ((weekStart(today).toEpochDay() - weekStart(start).toEpochDay()) / 7 + 1).toInt()
            completed.map { weekStart(it.date) }.distinct().size to weeks
        }
    }

    // Today (or this week) is still in progress: only count it once it is completed.
    val currentPeriodStart = periodFor(task.frequency, today).first
    val currentDone = completed.any { it.date >= currentPeriodStart }
    val countedPeriods = if (totalPeriods > 0 && !currentDone) totalPeriods - 1 else totalPeriods

    val values = completed.mapNotNull { it.value() }
    val hasValues = task.parameterType != ParameterType.NONE
    return ProgressStats(
        completedPeriods = completedPeriods,
        totalPeriods = countedPeriods,
        total = if (hasValues) values.sum() else null,
        average = if (hasValues && values.isNotEmpty()) values.average() else null,
    )
}
