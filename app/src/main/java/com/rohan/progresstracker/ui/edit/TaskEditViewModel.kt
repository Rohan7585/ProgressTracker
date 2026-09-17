package com.rohan.progresstracker.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.progresstracker.data.Frequency
import com.rohan.progresstracker.data.ParameterType
import com.rohan.progresstracker.data.RecurringTask
import com.rohan.progresstracker.data.Repository
import com.rohan.progresstracker.data.TodoTask
import com.rohan.progresstracker.ui.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class FrequencyOption(val label: String) {
    ONE_TIME("One Time"), DAILY("Daily"), WEEKLY("Weekly")
}

data class TaskEditUiState(
    val name: String = "",
    val frequency: FrequencyOption = FrequencyOption.DAILY,
    val parameter: ParameterType = ParameterType.NONE,
    /** True when editing an existing task (vs. adding a new one). */
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val showNameError: Boolean = false,
    /** Set when the screen should close (after save or delete). */
    val finished: Boolean = false,
)

/** Add a new task (todo or recurring) or edit an existing one. */
class TaskEditViewModel(
    private val repository: Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val kind: String = savedStateHandle["kind"] ?: Routes.KIND_RECURRING
    private val id: Long = savedStateHandle["id"] ?: -1L
    private val isEditing = id != -1L

    private var existingTodo: TodoTask? = null
    private var existingRecurring: RecurringTask? = null

    private val _uiState = MutableStateFlow(
        TaskEditUiState(
            frequency = if (kind == Routes.KIND_TODO) FrequencyOption.ONE_TIME else FrequencyOption.DAILY,
            isEditing = isEditing,
            isLoading = isEditing,
        )
    )
    val uiState: StateFlow<TaskEditUiState> = _uiState

    init {
        if (isEditing) viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (kind == Routes.KIND_TODO) {
            val todo = repository.getTodo(id)
            existingTodo = todo
            _uiState.update { it.copy(name = todo?.title.orEmpty(), isLoading = false, finished = todo == null) }
        } else {
            val task = repository.getRecurringTask(id)
            existingRecurring = task
            _uiState.update {
                it.copy(
                    name = task?.name.orEmpty(),
                    frequency = if (task?.frequency == Frequency.WEEKLY) FrequencyOption.WEEKLY else FrequencyOption.DAILY,
                    parameter = task?.parameterType ?: ParameterType.NONE,
                    isLoading = false,
                    finished = task == null,
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, showNameError = false) }

    fun onFrequencyChange(option: FrequencyOption) = _uiState.update { it.copy(frequency = option) }

    fun onParameterChange(parameter: ParameterType) {
        // The parameter type can't change after creation, because existing history depends on it.
        if (!isEditing) _uiState.update { it.copy(parameter = parameter) }
    }

    /** Prevents a double tap from saving or deleting twice. */
    private var busy = false

    fun save() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        if (busy) return
        busy = true
        viewModelScope.launch {
            val todo = existingTodo
            val recurring = existingRecurring
            when {
                todo != null -> repository.updateTodo(todo.copy(title = name))
                recurring != null -> repository.updateRecurringTask(
                    recurring.copy(name = name, frequency = state.frequency.toFrequency())
                )
                state.frequency == FrequencyOption.ONE_TIME -> repository.addTodo(name)
                else -> repository.addRecurringTask(
                    RecurringTask(
                        name = name,
                        frequency = state.frequency.toFrequency(),
                        parameterType = state.parameter,
                        createdDate = LocalDate.now(),
                    )
                )
            }
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        if (busy) return
        busy = true
        viewModelScope.launch {
            existingTodo?.let { repository.deleteTodo(it) }
            existingRecurring?.let { repository.deleteRecurringTask(it) }
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun FrequencyOption.toFrequency() =
        if (this == FrequencyOption.WEEKLY) Frequency.WEEKLY else Frequency.DAILY
}
