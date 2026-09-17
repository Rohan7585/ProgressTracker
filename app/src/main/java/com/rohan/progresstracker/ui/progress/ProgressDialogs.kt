package com.rohan.progresstracker.ui.progress

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rohan.progresstracker.data.ProgressRecord
import com.rohan.progresstracker.domain.formatDuration
import com.rohan.progresstracker.domain.minutesBetween
import com.rohan.progresstracker.domain.parseDurationMinutes
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class TimeMode { START_END, DURATION }

/** Record time spent: either start + end time, or a typed duration. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeProgressDialog(
    taskName: String,
    existing: ProgressRecord?,
    onSave: (minutes: Int, start: LocalTime?, end: LocalTime?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { LocalTime.now().withSecond(0).withNano(0) }
    var mode by rememberSaveable {
        mutableStateOf(if (existing != null && existing.startTime == null) TimeMode.DURATION else TimeMode.START_END)
    }
    var start by rememberSaveable { mutableStateOf(existing?.startTime ?: now.minusHours(1)) }
    var end by rememberSaveable { mutableStateOf(existing?.endTime ?: now) }
    var durationText by rememberSaveable {
        mutableStateOf(existing?.takeIf { it.startTime == null }?.durationMinutes?.let(::formatDuration).orEmpty())
    }
    /** Which time is being picked: "start", "end" or null. */
    var picking by rememberSaveable { mutableStateOf<String?>(null) }

    val minutes: Int? = when (mode) {
        TimeMode.START_END -> minutesBetween(start, end).takeIf { it > 0 }
        TimeMode.DURATION -> parseDurationMinutes(durationText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(taskName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == TimeMode.START_END,
                        onClick = { mode = TimeMode.START_END },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Start – End", maxLines = 1) }
                    SegmentedButton(
                        selected = mode == TimeMode.DURATION,
                        onClick = { mode = TimeMode.DURATION },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Duration", maxLines = 1) }
                }

                when (mode) {
                    TimeMode.START_END -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeField("Start", start, onClick = { picking = "start" }, modifier = Modifier.weight(1f))
                            TimeField("End", end, onClick = { picking = "end" }, modifier = Modifier.weight(1f))
                        }
                        Text(
                            text = when {
                                minutes == null -> "End time must be different from start time"
                                end < start -> "Duration: ${formatDuration(minutes)} (past midnight)"
                                else -> "Duration: ${formatDuration(minutes)}"
                            },
                            color = if (minutes == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    TimeMode.DURATION -> {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it },
                            label = { Text("Duration") },
                            placeholder = { Text("e.g. 90 or 1h 30m") },
                            singleLine = true,
                            isError = durationText.isNotBlank() && minutes == null,
                            supportingText = {
                                Text(
                                    when {
                                        durationText.isBlank() -> "Minutes, or formats like 1h 30m, 1:30"
                                        minutes == null -> "Not understood. Try 90, 1h 30m or 1:30"
                                        else -> "= ${formatDuration(minutes)}"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = minutes != null,
                onClick = {
                    if (minutes == null) return@TextButton
                    if (mode == TimeMode.START_END) onSave(minutes, start, end) else onSave(minutes, null, null)
                },
            ) { Text("Save") }
        },
        dismissButton = {
            DialogSecondaryButtons(showClear = existing != null, onClear = onClear, onDismiss = onDismiss)
        },
    )

    picking?.let { which ->
        TimePickerDialog(
            initial = if (which == "start") start else end,
            onConfirm = { time ->
                if (which == "start") start = time else end = time
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

/** Record a whole number, e.g. push-ups done. */
@Composable
fun QuantityProgressDialog(
    taskName: String,
    existing: ProgressRecord?,
    onSave: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(existing?.quantity?.toString().orEmpty()) }
    val quantity = text.trim().toIntOrNull()?.takeIf { it > 0 }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(taskName) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(9) },
                label = { Text("Quantity") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(enabled = quantity != null, onClick = { quantity?.let(onSave) }) { Text("Save") }
        },
        dismissButton = {
            DialogSecondaryButtons(showClear = existing != null, onClear = onClear, onDismiss = onDismiss)
        },
    )
}

@Composable
private fun TimeField(label: String, time: LocalTime, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label time" }) {
            Text(formatTime(time), maxLines = 1)
        }
    }
}

@Composable
private fun DialogSecondaryButtons(showClear: Boolean, onClear: () -> Unit, onDismiss: () -> Unit) {
    Row {
        if (showClear) {
            TextButton(onClick = onClear) { Text("Clear", color = MaterialTheme.colorScheme.error) }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state, modifier = Modifier.padding(top = 8.dp)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun formatTime(time: LocalTime): String = time.format(timeFormatter)
