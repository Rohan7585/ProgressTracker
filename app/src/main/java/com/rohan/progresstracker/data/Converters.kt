package com.rohan.progresstracker.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/** Tells Room how to store dates and times: dates as day numbers, times as minute-of-day. */
class Converters {
    @TypeConverter
    fun dateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun timeToMinuteOfDay(time: LocalTime?): Int? = time?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun minuteOfDayToTime(value: Int?): LocalTime? = value?.let { LocalTime.of(it / 60, it % 60) }
}
