package it.bosler.polyphoneme.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.polyphoneme.data.library.TocEntry
import it.bosler.polyphoneme.di.AppDependencies
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.Chapter
import it.bosler.polyphoneme.model.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {

    private val bookRepo get() = AppDependencies.bookRepository
    private val settingsRepo get() = AppDependencies.settingsRepository

    private val _chapter = MutableStateFlow<Chapter?>(null)
    val chapter: StateFlow<Chapter?> = _chapter

    private val _chapterIndex = MutableStateFlow(0)
    val chapterIndex: StateFlow<Int> = _chapterIndex

    private val _chapterCount = MutableStateFlow(0)
    val chapterCount: StateFlow<Int> = _chapterCount

    private val _toc = MutableStateFlow<List<TocEntry>>(emptyList())
    val toc: StateFlow<List<TocEntry>> = _toc

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _bookLanguage = MutableStateFlow<String>("en")
    val bookLanguage: StateFlow<String> = _bookLanguage

    private val _selectedWord = MutableStateFlow<Token?>(null)
    val selectedWord: StateFlow<Token?> = _selectedWord

    private var currentBookId: String? = null

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun loadBook(bookId: String, chapterIndex: Int) {
        if (currentBookId == bookId) return
        currentBookId = bookId
        viewModelScope.launch {
            val meta = bookRepo.getBookMeta(bookId)
            _bookLanguage.value = meta?.language ?: "en"
            _chapterCount.value = bookRepo.getChapterCount(bookId)
            _toc.value = bookRepo.getTableOfContents(bookId)
            loadChapter(chapterIndex, forward = true)
        }
    }

    fun goToChapter(index: Int) {
        loadChapter(index, forward = index >= _chapterIndex.value)
    }

    fun nextChapter() {
        if (_chapterIndex.value < _chapterCount.value - 1) {
            loadChapter(_chapterIndex.value + 1, forward = true)
        }
    }

    fun prevChapter() {
        if (_chapterIndex.value > 0) {
            loadChapter(_chapterIndex.value - 1, forward = false)
        }
    }

    fun updateFontSize(delta: Int) {
        viewModelScope.launch {
            val current = settings.value.fontSize
            settingsRepo.updateFontSize(current + delta)
        }
    }

    fun selectWord(token: Token?) {
        _selectedWord.value = token
    }

    fun speak(text: String, language: String) {
        AppDependencies.audioPlayer.speak(text, language)
    }

    fun dismissPageModeTutorial() {
        viewModelScope.launch {
            settingsRepo.updateHasSeenPageModeTutorial(true)
        }
    }

    private fun loadChapter(index: Int, forward: Boolean) {
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val count = _chapterCount.value
            var idx = index.coerceIn(0, count - 1)

            // Skip empty chapters
            val direction = if (forward) 1 else -1
            var chapter = bookRepo.loadChapter(bookId, idx)
            while (chapter.paragraphs.isEmpty() && idx + direction in 0 until count) {
                idx += direction
                chapter = bookRepo.loadChapter(bookId, idx)
            }

            _chapterIndex.value = idx
            _chapter.value = chapter
            bookRepo.updateLastRead(bookId, idx)
            _isLoading.value = false
        }
    }
}
