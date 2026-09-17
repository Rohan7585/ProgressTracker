package com.rohan.progresstracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class Frequency { DAILY, WEEKLY }

enum class ParameterType { NONE, TIME, QUANTITY }

/** A Daily or Weekly activity shown on the Progress screen. */
@Entity(tableName = "recurring_tasks")
data class RecurringTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val frequency: Frequency,
    val parameterType: ParameterType,
    /** Used so completion percentages don't count days before the task existed. */
    val createdDate: LocalDate,
)

/** A normal one-time Todo item. */
@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Progress for one recurring task on one date.
 * The unique (taskId, date) index guarantees at most one record per task per day.
 * Records are deleted automatically when their task is deleted.
 */
@Entity(
    tableName = "progress_records",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTask::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["taskId", "date"], unique = true), Index("date")],
)
data class ProgressRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val date: LocalDate,
    val completed: Boolean = true,
    val parameterType: ParameterType,
    val quantity: Int? = null,
    val durationMinutes: Int? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
)
