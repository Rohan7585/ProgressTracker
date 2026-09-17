package com.rohan.progresstracker.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.rohan.progresstracker.data.TodoTask
import com.rohan.progresstracker.ui.AppBottomBar
import com.rohan.progresstracker.ui.AppViewModelFactory
import com.rohan.progresstracker.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    navController: NavHostController,
    viewModel: TodoViewModel = viewModel(factory = AppViewModelFactory),
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Todo") }) },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = dropUnlessResumed { navController.navigate(Routes.editTask(Routes.KIND_TODO)) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add todo")
            }
        },
    ) { padding ->
        val list = todos
        when {
            list == null -> Unit
            list.isEmpty() -> EmptyMessage("No todos yet.\nTap + to add one.", Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(list, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = { viewModel.toggleCompleted(todo) },
                        onEdit = dropUnlessResumed { navController.navigate(Routes.editTask(Routes.KIND_TODO, todo.id)) },
                        onDelete = { viewModel.delete(todo) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TodoRow(todo: TodoTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.completed, onCheckedChange = { onToggle() })
        Text(
            text = todo.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (todo.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (todo.completed) TextDecoration.LineThrough else null,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
