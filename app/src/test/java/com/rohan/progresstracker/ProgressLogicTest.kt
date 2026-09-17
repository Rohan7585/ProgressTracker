package com.rohan.progresstracker

import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.data.ProgressRecord
import com.rohan.progresstracker.data.RecurringTask
import com.rohan.progresstracker.domain.computeStats
import com.rohan.progresstracker.domain.formatDuration
import com.rohan.progresstracker.domain.minutesBetween
import com.rohan.progresstracker.domain.parseDurationMinutes
import com.rohan.progresstracker.domain.periodFor
import com.rohan.progresstracker.domain.weekStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ProgressLogicTest {

    @Test
    fun parsesDurations() {
        assertEquals(90, parseDurationMinutes("90"))
        assertEquals(90, parseDurationMinutes("90m"))
        assertEquals(90, parseDurationMinutes("90 min"))
        assertEquals(90, parseDurationMinutes("1h 30m"))
        assertEquals(90, parseDurationMinutes("1h30m"))
        assertEquals(90, parseDurationMinutes("1:30"))
        assertEquals(90, parseDurationMinutes("1.5h"))
        assertEquals(90, parseDurationMinutes(" 1 Hour 30 Minutes "))
        assertEquals(120, parseDurationMinutes("2 hrs"))
    }

    @Test
    fun rejectsBadDurations() {
        assertNull(parseDurationMinutes(""))
        assertNull(parseDurationMinutes("0"))
        assertNull(parseDurationMinutes("abc"))
        assertNull(parseDurationMinutes("1h abc"))
        assertNull(parseDurationMinutes("1:75"))
        assertNull(parseDurationMinutes("-5"))
    }

    @Test
    fun formatsDurations() {
        assertEquals("45 min", formatDuration(45))
        assertEquals("1h 30m", formatDuration(90))
        assertEquals("2h", formatDuration(120))
    }

    @Test
    fun calculatesMinutesBetweenTimes() {
        assertEquals(90, minutesBetween(LocalTime.of(19, 0), LocalTime.of(20, 30)))
        // Crosses midnight
        assertEquals(90, minutesBetween(LocalTime.of(23, 30), LocalTime.of(1, 0)))
        assertEquals(0, minutesBetween(LocalTime.of(10, 0), LocalTime.of(10, 0)))
    }

    @Test
    fun weeksStartOnMonday() {
        val wednesday = LocalDate.of(2026, 9, 16)
        assertEquals(LocalDate.of(2026, 9, 14), weekStart(wednesday))
        assertEquals(LocalDate.of(2026, 9, 14), weekStart(LocalDate.of(2026, 9, 14)))
        assertEquals(LocalDate.of(2026, 9, 14), weekStart(LocalDate.of(2026, 9, 20)))
        assertEquals(
            LocalDate.of(2026, 9, 14) to LocalDate.of(2026, 9, 20),
            periodFor(Frequency.WEEKLY, wednesday),
        )
    }

    @Test
    fun dailyStatsIgnoreDaysBeforeTaskWasCreated() {
        val today = LocalDate.of(2026, 9, 16)
        val task = RecurringTask(1, "Study", Frequency.DAILY, ParameterType.TIME, createdDate = today.minusDays(3))
        val records = listOf(
            ProgressRecord(taskId = 1, date = today.minusDays(3), parameterType = ParameterType.TIME, durationMinutes = 60),
            ProgressRecord(taskId = 1, date = today, parameterType = ParameterType.TIME, durationMinutes = 30),
        )
        val stats = computeStats(task, records, rangeStart = today.minusDays(6), today = today)
        assertEquals(2, stats.completedPeriods)
        assertEquals(4, stats.totalPeriods)
        assertEquals(50, stats.completionPercent)
        assertEquals(90, stats.total)
        assertEquals(45.0, stats.average!!, 0.001)
    }

    @Test
    fun weeklyStatsCountWeeks() {
        val today = LocalDate.of(2026, 9, 16) // Wednesday
        val task = RecurringTask(1, "Gym", Frequency.WEEKLY, ParameterType.NONE, createdDate = LocalDate.of(2026, 1, 1))
        val records = listOf(
            ProgressRecord(taskId = 1, date = LocalDate.of(2026, 9, 1), parameterType = ParameterType.NONE),
            ProgressRecord(taskId = 1, date = LocalDate.of(2026, 9, 15), parameterType = ParameterType.NONE),
        )
        // Last 30 days: Aug 18 .. Sep 16 touches weeks starting Aug 17, 24, 31, Sep 7, 14 = 5 weeks.
        val stats = computeStats(task, records, rangeStart = today.minusDays(29), today = today)
        assertEquals(2, stats.completedPeriods)
        assertEquals(5, stats.totalPeriods)
        assertNull(stats.total)
    }

    @Test
    fun currentPeriodNotCountedUntilCompleted() {
        val today = LocalDate.of(2026, 9, 16)
        val daily = RecurringTask(1, "Iron", Frequency.DAILY, ParameterType.NONE, createdDate = LocalDate.of(2026, 1, 1))
        val yesterday = listOf(ProgressRecord(taskId = 1, date = today.minusDays(1), parameterType = ParameterType.NONE))
        val stats = computeStats(daily, yesterday, rangeStart = today.minusDays(6), today = today)
        assertEquals(1, stats.completedPeriods)
        assertEquals(6, stats.totalPeriods) // today is still pending

        val weekly = daily.copy(frequency = Frequency.WEEKLY)
        val lastWeek = listOf(ProgressRecord(taskId = 1, date = LocalDate.of(2026, 9, 9), parameterType = ParameterType.NONE))
        val weeklyStats = computeStats(weekly, lastWeek, rangeStart = today.minusDays(6), today = today)
        assertEquals(1, weeklyStats.completedPeriods)
        assertEquals(1, weeklyStats.totalPeriods) // this week is still pending

        // A task created today with nothing done yet has 0 of 0.
        val fresh = daily.copy(createdDate = today)
        assertEquals(0, computeStats(fresh, emptyList(), today.minusDays(6), today).completionPercent)
    }
}
