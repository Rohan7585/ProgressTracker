package com.rohan.progresstracker.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.ui.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    navController: NavHostController,
    viewModel: TaskEditViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    // Only leave the screen while it is the active one, so a double tap can't pop two screens.
    val close = dropUnlessResumed { navController.popBackStack() }

    LaunchedEffect(state.finished) {
        if (state.finished) close()
    }
    LaunchedEffect(Unit) {
        if (!state.isEditing) focusRequester.requestFocus()
    }

    val isTodo = state.frequency == FrequencyOption.ONE_TIME
    val title = when {
        !state.isEditing -> "Add task"
        isTodo -> "Edit todo"
        else -> "Edit task"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = close) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Task name") },
                singleLine = true,
                isError = state.showNameError,
                supportingText = if (state.showNameError) ({ Text("Please enter a name") }) else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (isTodo) viewModel.save() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            // A todo can't become a recurring task (and vice versa) after creation, so hide the choice when editing a todo.
            if (!(state.isEditing && isTodo)) {
                SectionLabel("Frequency")
                ChoiceRow(
                    options = FrequencyOption.entries,
                    selected = state.frequency,
                    label = { it.label },
                    enabled = { !state.isEditing || it != FrequencyOption.ONE_TIME },
                    onSelect = viewModel::onFrequencyChange,
                )
            }

            if (!isTodo) {
                SectionLabel("Progress parameter")
                ChoiceRow(
                    options = ParameterType.entries,
                    selected = state.parameter,
                    label = { it.label() },
                    enabled = { !state.isEditing || it == state.parameter },
                    onSelect = viewModel::onParameterChange,
                )
                Text(
                    text = if (state.isEditing) "The parameter can't be changed after a task is created."
                    else state.parameter.description(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Save")
            }

            if (state.isEditing) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${state.name.trim()}\"?") },
            text = { if (!isTodo) Text("All progress history for this task will also be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = enabled(option),
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

fun ParameterType.label() = when (this) {
    ParameterType.NONE -> "None"
    ParameterType.TIME -> "Time"
    ParameterType.QUANTITY -> "Quantity"
}

private fun ParameterType.description() = when (this) {
    ParameterType.NONE -> "Just mark it as done (e.g. iron clothes, clean room)."
    ParameterType.TIME -> "Record time spent (e.g. study, exercise, reading)."
    ParameterType.QUANTITY -> "Record a number (e.g. push-ups, pages read)."
}
