package com.rohan.progresstracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.progresstracker.data.ProgressRecord
import com.rohan.progresstracker.data.RecurringTask
import com.rohan.progresstracker.data.Repository
import com.rohan.progresstracker.domain.periodFor
import com.rohan.progresstracker.domain.weekStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** A recurring task plus its progress for the current day (daily) or week (weekly), if any. */
data class TaskToday(val task: RecurringTask, val record: ProgressRecord?) {
    val isDone: Boolean get() = record?.completed == true
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val repository: Repository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    /** null while loading. */
    val tasks: StateFlow<List<TaskToday>?> = today
        .flatMapLatest { date ->
            val weekStart = weekStart(date)
            combine(
                repository.observeRecurringTasks(),
                repository.observeProgressBetween(weekStart, weekStart.plusDays(6)),
            ) { tasks, records ->
                tasks.map { task ->
                    val (start, end) = periodFor(task.frequency, date)
                    TaskToday(task, records.firstOrNull { it.taskId == task.id && it.date in start..end })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Call when the screen becomes visible, so a new day is picked up if the app stayed open overnight. */
    fun refreshDate() {
        today.value = LocalDate.now()
    }

    /** For completion-only tasks: tick / untick today. */
    fun toggleDone(item: TaskToday) = viewModelScope.launch {
        refreshDate()
        if (item.isDone) repository.clearProgress(item.task, today.value)
        else repository.saveProgress(item.task, today.value)
    }

    fun saveTime(task: RecurringTask, minutes: Int, start: LocalTime?, end: LocalTime?) = viewModelScope.launch {
        refreshDate()
        repository.saveProgress(task, today.value, durationMinutes = minutes, startTime = start, endTime = end)
    }

    fun saveQuantity(task: RecurringTask, quantity: Int) = viewModelScope.launch {
        refreshDate()
        repository.saveProgress(task, today.value, quantity = quantity)
    }

    fun clearProgress(task: RecurringTask) = viewModelScope.launch {
        refreshDate()
        repository.clearProgress(task, today.value)
    }
}
