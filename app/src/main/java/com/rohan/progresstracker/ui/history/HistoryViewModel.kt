package com.rohan.progresstracker.ui.history

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** The ranges offered in the history range picker; the first one is the default. */
val HISTORY_RANGES = listOf(7, 30, 90)

data class TaskHistory(
    val task: RecurringTask,
    /** This task's records from the start of the first week in the range up to today. */
    val records: List<ProgressRecord>,
    val stats: ProgressStats,
)

data class HistoryUiState(
    val days: Int,
    val rangeStart: LocalDate,
    val today: LocalDate,
    val tasks: List<TaskHistory>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val days = MutableStateFlow(savedStateHandle.get<Int>(DAYS_KEY) ?: HISTORY_RANGES.first())
    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HistoryUiState?> = combine(days, today) { d, t -> d to t }
        .flatMapLatest { (dayCount, date) ->
            val rangeStart = date.minusDays(dayCount - 1L)
            combine(
                repository.observeRecurringTasks(),
                // Load from the Monday of the first week so weekly tasks see their whole first week.
                repository.observeProgressBetween(weekStart(rangeStart), date),
            ) { tasks, records ->
                HistoryUiState(
                    days = dayCount,
                    rangeStart = rangeStart,
                    today = date,
                    tasks = tasks.map { task ->
                        val taskRecords = records.filter { it.taskId == task.id }
                        TaskHistory(task, taskRecords, computeStats(task, taskRecords, rangeStart, date))
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDays(value: Int) {
        days.value = value
        savedStateHandle[DAYS_KEY] = value
    }

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    private companion object {
        const val DAYS_KEY = "historyDays"
    }
}
