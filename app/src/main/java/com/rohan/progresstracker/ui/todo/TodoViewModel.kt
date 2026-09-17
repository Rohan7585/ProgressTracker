package com.rohan.progresstracker.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.progresstracker.data.Repository
import com.rohan.progresstracker.data.TodoTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(private val repository: Repository) : ViewModel() {

    /** null while the first load is in progress. */
    val todos: StateFlow<List<TodoTask>?> = repository.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleCompleted(task: TodoTask) = viewModelScope.launch {
        repository.updateTodo(task.copy(completed = !task.completed))
    }

    fun delete(task: TodoTask) = viewModelScope.launch {
        repository.deleteTodo(task)
    }
}
