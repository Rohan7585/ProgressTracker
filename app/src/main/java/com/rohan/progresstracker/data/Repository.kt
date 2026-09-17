package com.rohan.progresstracker.data

import com.rohan.progresstracker.domain.periodFor
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Single entry point the ViewModels use to read and write data. */
class Repository(db: AppDatabase) {
    private val recurringDao = db.recurringTaskDao()
    private val todoDao = db.todoDao()
    private val progressDao = db.progressDao()

    // ---- Recurring tasks ----

    fun observeRecurringTasks(): Flow<List<RecurringTask>> = recurringDao.observeAll()
    suspend fun getRecurringTask(id: Long) = recurringDao.get(id)
    suspend fun addRecurringTask(task: RecurringTask) = recurringDao.insert(task)
    suspend fun updateRecurringTask(task: RecurringTask) = recurringDao.update(task)

    /** Also deletes the task's progress records (cascade). */
    suspend fun deleteRecurringTask(task: RecurringTask) = recurringDao.delete(task)

    // ---- Todos ----

    fun observeTodos(): Flow<List<TodoTask>> = todoDao.observeAll()
    suspend fun getTodo(id: Long) = todoDao.get(id)
    suspend fun addTodo(title: String) = todoDao.insert(TodoTask(title = title))
    suspend fun updateTodo(task: TodoTask) = todoDao.update(task)
    suspend fun deleteTodo(task: TodoTask) = todoDao.delete(task)

    // ---- Progress ----

    fun observeProgressBetween(start: LocalDate, end: LocalDate): Flow<List<ProgressRecord>> =
        progressDao.observeBetween(start, end)

    /** The record for the task's current period (today for daily tasks, this week for weekly tasks). */
    suspend fun currentRecord(task: RecurringTask, today: LocalDate): ProgressRecord? {
        val (start, end) = periodFor(task.frequency, today)
        return progressDao.getForTaskBetween(task.id, start, end).firstOrNull()
    }

    /**
     * Saves progress for the task's current period. If a record already exists for this period it is
     * updated instead of creating a duplicate.
     */
    suspend fun saveProgress(
        task: RecurringTask,
        today: LocalDate,
        quantity: Int? = null,
        durationMinutes: Int? = null,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ) {
        val existing = currentRecord(task, today)
        progressDao.upsert(
            ProgressRecord(
                id = existing?.id ?: 0,
                taskId = task.id,
                date = existing?.date ?: today,
                completed = true,
                parameterType = task.parameterType,
                quantity = quantity,
                durationMinutes = durationMinutes,
                startTime = startTime,
                endTime = endTime,
            )
        )
    }

    /** Removes progress for the task's current period (marks it "not completed"). */
    suspend fun clearProgress(task: RecurringTask, today: LocalDate) {
        currentRecord(task, today)?.let { progressDao.delete(it) }
    }
}
