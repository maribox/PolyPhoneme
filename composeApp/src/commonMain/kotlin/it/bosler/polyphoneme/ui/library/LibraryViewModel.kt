package it.bosler.polyphoneme.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.polyphoneme.di.AppDependencies
import it.bosler.polyphoneme.model.BookMeta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {

    private val repo get() = AppDependencies.bookRepository

    private val _books = MutableStateFlow<List<BookMeta>>(emptyList())
    val books: StateFlow<List<BookMeta>> = _books

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _importedBook = MutableSharedFlow<BookMeta>()
    val importedBook: SharedFlow<BookMeta> = _importedBook

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            _books.value = repo.getLibrary()
        }
    }

    fun importBook(uri: String, autoOpen: Boolean = false) {
        viewModelScope.launch {
            _isImporting.value = true
            _error.value = null
            try {
                val book = repo.importBook(uri)
                _books.value = repo.getLibrary()
                if (autoOpen) {
                    _importedBook.emit(book)
                }
            } catch (e: Exception) {
                _error.value = "Failed to import book: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteBook(id: String) {
        viewModelScope.launch {
            repo.deleteBook(id)
            _books.value = repo.getLibrary()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
