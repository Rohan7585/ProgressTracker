package com.rohan.progresstracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurringTaskDao {
    @Query("SELECT * FROM recurring_tasks ORDER BY id")
    fun observeAll(): Flow<List<RecurringTask>>

    @Query("SELECT * FROM recurring_tasks WHERE id = :id")
    suspend fun get(id: Long): RecurringTask?

    @Insert
    suspend fun insert(task: RecurringTask): Long

    @Update
    suspend fun update(task: RecurringTask)

    @Delete
    suspend fun delete(task: RecurringTask)
}

@Dao
interface TodoDao {
    /** Open items first, then completed ones; oldest first within each group. */
    @Query("SELECT * FROM todo_tasks ORDER BY completed, createdAt")
    fun observeAll(): Flow<List<TodoTask>>

    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun get(id: Long): TodoTask?

    @Insert
    suspend fun insert(task: TodoTask): Long

    @Update
    suspend fun update(task: TodoTask)

    @Delete
    suspend fun delete(task: TodoTask)
}

@Dao
interface ProgressDao {
    /** All records with a date between [start] and [end], inclusive. */
    @Query("SELECT * FROM progress_records WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<ProgressRecord>>

    @Query("SELECT * FROM progress_records WHERE taskId = :taskId AND date BETWEEN :start AND :end ORDER BY date")
    suspend fun getForTaskBetween(taskId: Long, start: LocalDate, end: LocalDate): List<ProgressRecord>

    @Upsert
    suspend fun upsert(record: ProgressRecord)

    @Delete
    suspend fun delete(record: ProgressRecord)
}
