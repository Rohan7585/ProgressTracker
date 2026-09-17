package com.rohan.progresstracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RecurringTask::class, TodoTask::class, ProgressRecord::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recurringTaskDao(): RecurringTaskDao
    abstract fun todoDao(): TodoDao
    abstract fun progressDao(): ProgressDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "progress_tracker.db").build()
    }
}
