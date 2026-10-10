package com.rohan.progresstracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.domain.describe
import com.rohan.progresstracker.ui.AppBottomBar
import com.rohan.progresstracker.ui.AppViewModelFactory
import com.rohan.progresstracker.ui.Routes
import com.rohan.progresstracker.ui.edit.label
import com.rohan.progresstracker.ui.history.HistoryViewModel
import com.rohan.progresstracker.ui.history.progressHistorySection
import com.rohan.progresstracker.ui.todo.EmptyMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelFactory),
    historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val history by historyViewModel.uiState.collectAsStateWithLifecycle()
    var historyExpanded by rememberSaveable { mutableStateOf(false) }
    /** Id of the task whose Time/Quantity dialog is open (saved so it survives screen rotation). */
    var dialogTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        historyViewModel.refreshDate()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Tracker") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                    }
                },
            )
        },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = dropUnlessResumed { navController.navigate(Routes.editTask(Routes.KIND_RECURRING)) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        val list = tasks
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyMessage(
                "No daily or weekly tasks yet.\nTap + to add one.",
                Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.task.id }) { item ->
                    TaskCard(
                        item = item,
                        onOpen = dropUnlessResumed {
                            navController.navigate(Routes.editTask(Routes.KIND_RECURRING, item.task.id))
                        },
                        onAction = {
                            if (item.task.parameterType == ParameterType.NONE) viewModel.toggleDone(item)
                            else dialogTaskId = item.task.id
                        },
                    )
                }
                progressHistorySection(
                    state = history,
                    expanded = historyExpanded,
                    onToggle = { historyExpanded = !historyExpanded },
                    onSelectDays = historyViewModel::setDays,
                )
            }
        }
    }

    tasks?.firstOrNull { it.task.id == dialogTaskId }?.let { item ->
        val close = { dialogTaskId = null }
        when (item.task.parameterType) {
            ParameterType.TIME -> TimeProgressDialog(
                taskName = item.task.name,
                existing = item.record,
                onSave = { minutes, start, end -> viewModel.saveTime(item.task, minutes, start, end); close() },
                onClear = { viewModel.clearProgress(item.task); close() },
                onDismiss = close,
            )
            ParameterType.QUANTITY -> QuantityProgressDialog(
                taskName = item.task.name,
                existing = item.record,
                onSave = { quantity -> viewModel.saveQuantity(item.task, quantity); close() },
                onClear = { viewModel.clearProgress(item.task); close() },
                onDismiss = close,
            )
            ParameterType.NONE -> close()
        }
    }
}

@Composable
private fun TaskCard(item: TaskToday, onOpen: () -> Unit, onAction: () -> Unit) {
    val task = item.task
    val period = if (task.frequency == Frequency.DAILY) "Today" else "This week"
    val frequency = if (task.frequency == Frequency.DAILY) "Daily" else "Weekly"
    val parameter = if (task.parameterType == ParameterType.NONE) "" else " · ${task.parameterType.label()}"

    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$frequency$parameter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$period: ${describe(item.record)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val description = when {
                task.parameterType == ParameterType.NONE && item.isDone -> "Mark ${task.name} as not completed"
                task.parameterType == ParameterType.NONE -> "Mark ${task.name} as completed"
                item.isDone -> "Edit progress for ${task.name}"
                else -> "Record progress for ${task.name}"
            }
            if (item.isDone) {
                FilledIconButton(onClick = onAction) { Icon(Icons.Filled.Check, contentDescription = description) }
            } else {
                FilledTonalIconButton(onClick = onAction) {
                    Icon(
                        if (task.parameterType == ParameterType.NONE) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = description,
                    )
                }
            }
        }
    }
}
