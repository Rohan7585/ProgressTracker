package com.rohan.progresstracker.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Everything stored in the app, as written to / read from an export file. */
data class BackupData(
    val tasks: List<RecurringTask>,
    val todos: List<TodoTask>,
    val records: List<ProgressRecord>,
)

/** Thrown when an import file can't be used. The message is shown to the user. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Converts [BackupData] to and from a readable JSON file. Ids are kept so progress records stay linked
 * to their tasks. Dates are "2026-10-10", times are "07:30".
 */
object BackupJson {
    private const val APP_ID = "com.rohan.progresstracker"
    private const val FORMAT_VERSION = 1

    fun encode(data: BackupData, appVersion: String): String {
        val root = JSONObject()
            .put("app", APP_ID)
            .put("formatVersion", FORMAT_VERSION)
            .put("appVersion", appVersion)
            .put("exportedAt", OffsetDateTime.now().toString())
            .put("recurringTasks", JSONArray(data.tasks.map { it.toJson() }))
            .put("todos", JSONArray(data.todos.map { it.toJson() }))
            .put("progressRecords", JSONArray(data.records.map { it.toJson() }))
        return root.toString(2)
    }

    /** Parses and checks an export file. Throws [BackupFormatException] if it is not usable. */
    fun decode(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupFormatException("This file is not a Progress Tracker export.")
        }
        if (root.optString("app") != APP_ID) throw BackupFormatException("This file is not a Progress Tracker export.")
        val version = root.optInt("formatVersion", -1)
        if (version > FORMAT_VERSION) {
            throw BackupFormatException("This file was made by a newer version of the app. Please update the app first.")
        }
        if (version < 1) throw BackupFormatException("This export file is damaged.")

        val data = try {
            BackupData(
                tasks = root.getJSONArray("recurringTasks").objects().map { it.toRecurringTask() },
                todos = root.getJSONArray("todos").objects().map { it.toTodo() },
                records = root.getJSONArray("progressRecords").objects().map { it.toRecord() },
            )
        } catch (e: JSONException) {
            throw BackupFormatException("This export file is damaged.")
        } catch (e: IllegalArgumentException) { // unknown enum value
            throw BackupFormatException("This export file is damaged.")
        } catch (e: DateTimeParseException) {
            throw BackupFormatException("This export file is damaged.")
        }

        val taskIds = data.tasks.map { it.id }.toSet()
        val consistent = taskIds.size == data.tasks.size &&
            data.todos.map { it.id }.distinct().size == data.todos.size &&
            data.records.all { it.taskId in taskIds } &&
            data.records.map { it.taskId to it.date }.distinct().size == data.records.size
        if (!consistent) throw BackupFormatException("This export file is damaged.")
        return data
    }

    // ---- Writing ----

    private fun RecurringTask.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("frequency", frequency.name)
        .put("parameterType", parameterType.name)
        .put("createdDate", createdDate.toString())

    private fun TodoTask.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("completed", completed)
        .put("createdAt", createdAt)

    private fun ProgressRecord.toJson() = JSONObject()
        .put("id", id)
        .put("taskId", taskId)
        .put("date", date.toString())
        .put("completed", completed)
        .put("parameterType", parameterType.name)
        .put("quantity", quantity ?: JSONObject.NULL)
        .put("durationMinutes", durationMinutes ?: JSONObject.NULL)
        .put("startTime", startTime?.toString() ?: JSONObject.NULL)
        .put("endTime", endTime?.toString() ?: JSONObject.NULL)

    // ---- Reading ----

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.toRecurringTask() = RecurringTask(
        id = getLong("id"),
        name = getString("name").also { if (it.isBlank()) throw JSONException("blank name") },
        frequency = Frequency.valueOf(getString("frequency")),
        parameterType = ParameterType.valueOf(getString("parameterType")),
        createdDate = LocalDate.parse(getString("createdDate")),
    )

    private fun JSONObject.toTodo() = TodoTask(
        id = getLong("id"),
        title = getString("title"),
        completed = getBoolean("completed"),
        createdAt = getLong("createdAt"),
    )

    private fun JSONObject.toRecord() = ProgressRecord(
        id = getLong("id"),
        taskId = getLong("taskId"),
        date = LocalDate.parse(getString("date")),
        completed = getBoolean("completed"),
        parameterType = ParameterType.valueOf(getString("parameterType")),
        quantity = optIntOrNull("quantity"),
        durationMinutes = optIntOrNull("durationMinutes"),
        startTime = optStringOrNull("startTime")?.let(LocalTime::parse),
        endTime = optStringOrNull("endTime")?.let(LocalTime::parse),
    )

    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
}
