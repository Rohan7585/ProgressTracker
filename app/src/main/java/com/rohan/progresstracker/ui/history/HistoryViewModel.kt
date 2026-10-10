package com.rohan.progresstracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.progresstracker.data.ProgressRecord
import com.rohan.progresstracker.data.RecurringTask
import com.rohan.progresstracker.data.Repository
import com.rohan.progresstracker.domain.ProgressStats
import com.rohan.progresstracker.domain.computeStats
import com.rohan.progresstracker.domain.weekStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** The ranges offered in each task's history range picker; the first one is the default. */
val HISTORY_RANGES = listOf(7, 30, 90)

/** One task's history over its selected range. */
data class TaskHistory(
    val task: RecurringTask,
    val days: Int,
    val rangeStart: LocalDate,
    val today: LocalDate,
    /** This task's records from the start of the first week in the range up to today. */
    val records: List<ProgressRecord>,
    val stats: ProgressStats,
)

/** Per-task history shown when a task card on the Progress screen is expanded. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repository: Repository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    /** Task ids whose history is open. */
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()
    /** Selected range per task id; tasks not in the map use the default range. */
    private val daysByTask = MutableStateFlow<Map<Long, Int>>(emptyMap())

    /** History for every task, by task id (computed for all so expanding is instant). */
    val histories: StateFlow<Map<Long, TaskHistory>> = today
        .flatMapLatest { date ->
            // Load enough for the longest range, from the Monday of its first week.
            val earliest = weekStart(date.minusDays(HISTORY_RANGES.max() - 1L))
            combine(
                repository.observeRecurringTasks(),
                repository.observeProgressBetween(earliest, date),
                daysByTask,
            ) { tasks, records, selectedDays ->
                val recordsByTask = records.groupBy { it.taskId }
                tasks.associate { task ->
                    val days = selectedDays[task.id] ?: HISTORY_RANGES.first()
                    val rangeStart = date.minusDays(days - 1L)
                    val firstDay = weekStart(rangeStart)
                    val taskRecords = recordsByTask[task.id].orEmpty().filter { it.date >= firstDay }
                    task.id to TaskHistory(
                        task = task,
                        days = days,
                        rangeStart = rangeStart,
                        today = date,
                        records = taskRecords,
                        stats = computeStats(task, taskRecords, rangeStart, date),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun toggleExpanded(taskId: Long) {
        _expandedIds.update { if (taskId in it) it - taskId else it + taskId }
    }

    fun setDays(taskId: Long, days: Int) {
        daysByTask.update { it + (taskId to days) }
    }

    fun refreshDate() {
        today.value = LocalDate.now()
    }
}
