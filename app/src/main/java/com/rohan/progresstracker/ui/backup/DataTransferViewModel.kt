package com.rohan.progresstracker.ui.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohan.progresstracker.BuildConfig
import com.rohan.progresstracker.data.BackupData
import com.rohan.progresstracker.data.BackupFormatException
import com.rohan.progresstracker.data.BackupJson
import com.rohan.progresstracker.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class DataTransferUiState(
    val busy: Boolean = false,
    /** A file that was read successfully and is waiting for the user to confirm the import. */
    val pendingImport: BackupData? = null,
)

class DataTransferViewModel(
    private val repository: Repository,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(DataTransferUiState())
    val state: StateFlow<DataTransferUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off messages to show in a snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun suggestedFileName() = "progress-tracker-${LocalDate.now()}.json"

    /** Writes all data to the file the user just created. */
    fun export(uri: Uri) = runBusy {
        val data = repository.exportData()
        val json = BackupJson.encode(data, BuildConfig.VERSION_NAME)
        try {
            withContext(Dispatchers.IO) {
                val stream = contentResolver.openOutputStream(uri, "w") ?: error("No output stream")
                stream.use { it.write(json.toByteArray()) }
            }
            "Exported ${summary(data)}."
        } catch (e: Exception) {
            "Couldn't save the file. Please try another location."
        }
    }

    /** Reads and checks the chosen file; the import itself happens after [confirmImport]. */
    fun readImportFile(uri: Uri) = runBusy {
        try {
            val text = withContext(Dispatchers.IO) {
                val stream = contentResolver.openInputStream(uri) ?: error("No input stream")
                stream.use { it.readBytes().decodeToString() }
            }
            val data = withContext(Dispatchers.Default) { BackupJson.decode(text) }
            _state.update { it.copy(pendingImport = data) }
            null
        } catch (e: BackupFormatException) {
            e.message
        } catch (e: Exception) {
            "Couldn't read the file."
        }
    }

    fun confirmImport() {
        val data = _state.value.pendingImport ?: return
        _state.update { it.copy(pendingImport = null) }
        runBusy {
            try {
                repository.replaceAllData(data)
                "Imported ${summary(data)}."
            } catch (e: Exception) {
                "Import failed. Your data was not changed."
            }
        }
    }

    fun cancelImport() {
        _state.update { it.copy(pendingImport = null) }
    }

    /** Runs [block] with the busy indicator shown, then shows the message it returns (if any). */
    private fun runBusy(block: suspend () -> String?) = viewModelScope.launch {
        _state.update { it.copy(busy = true) }
        val message = block()
        _state.update { it.copy(busy = false) }
        message?.let { _messages.send(it) }
    }
}

/** e.g. "3 tasks, 1 todo and 42 progress entries". */
fun summary(data: BackupData): String {
    fun count(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"
    return "${count(data.tasks.size, "task")}, ${count(data.todos.size, "todo")} and " +
        "${data.records.size} progress ${if (data.records.size == 1) "entry" else "entries"}"
}
