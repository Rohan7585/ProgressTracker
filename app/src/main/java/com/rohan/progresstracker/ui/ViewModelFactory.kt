package com.rohan.progresstracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rohan.progresstracker.ProgressTrackerApp
import com.rohan.progresstracker.ui.edit.TaskEditViewModel
import com.rohan.progresstracker.ui.history.HistoryViewModel
import com.rohan.progresstracker.ui.progress.ProgressViewModel
import com.rohan.progresstracker.ui.todo.TodoViewModel

/** Creates every ViewModel with the app's shared repository. */
val AppViewModelFactory = viewModelFactory {
    initializer { ProgressViewModel(app().repository) }
    initializer { TodoViewModel(app().repository) }
    initializer { HistoryViewModel(app().repository, createSavedStateHandle()) }
    initializer { TaskEditViewModel(app().repository, createSavedStateHandle()) }
}

private fun CreationExtras.app() = this[APPLICATION_KEY] as ProgressTrackerApp
