package it.bosler.polyphoneme.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.bosler.polyphoneme.data.library.TocEntry
import it.bosler.polyphoneme.di.AppDependencies
import it.bosler.polyphoneme.model.AppSettings
import it.bosler.polyphoneme.model.AppTheme
import it.bosler.polyphoneme.model.Chapter
import it.bosler.polyphoneme.model.DarkModePreference
import it.bosler.polyphoneme.model.ReaderBackground
import it.bosler.polyphoneme.model.ReaderFont
import it.bosler.polyphoneme.model.Token
import kotlinx.coroutines.async
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

    private val _firstContentChapterIndex = MutableStateFlow(0)
    val firstContentChapterIndex: StateFlow<Int> = _firstContentChapterIndex

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _bookLanguage = MutableStateFlow<String>("en")
    val bookLanguage: StateFlow<String> = _bookLanguage

    private val _bookTranslationFrequency = MutableStateFlow<Float?>(null)
    val bookTranslationFrequency: StateFlow<Float?> = _bookTranslationFrequency

    private val _selectedWord = MutableStateFlow<Token?>(null)
    val selectedWord: StateFlow<Token?> = _selectedWord

    /** Selected phrase: (paragraphIndex, startTokenIndex, endTokenIndex exclusive) */
    private val _selectedPhrase = MutableStateFlow<Triple<Int, Int, Int>?>(null)
    val selectedPhrase: StateFlow<Triple<Int, Int, Int>?> = _selectedPhrase

    private val _phraseTranslation = MutableStateFlow<String?>(null)
    val phraseTranslation: StateFlow<String?> = _phraseTranslation

    private var currentBookId: String? = null

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun loadBook(bookId: String, chapterIndex: Int) {
        if (currentBookId == bookId) return
        currentBookId = bookId
        chapterCache.clear()
        viewModelScope.launch {
            _isLoading.value = true
            val metaDeferred = async { bookRepo.getBookMeta(bookId) }
            val countDeferred = async { bookRepo.getChapterCount(bookId) }
            val tocDeferred = async { bookRepo.getTableOfContents(bookId) }
            val meta = metaDeferred.await()
            _bookLanguage.value = meta?.language ?: "en"
            _bookTranslationFrequency.value = meta?.translationFrequency
            _chapterCount.value = countDeferred.await()
            val (tocEntries, firstContent) = tocDeferred.await()
            _toc.value = tocEntries
            _firstContentChapterIndex.value = firstContent
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
        // Clear phrase selection when selecting a single word
        _selectedPhrase.value = null
        _phraseTranslation.value = null
    }

    /**
     * Select a range of words within a paragraph for phrase translation.
     * If a word is already selected, tapping another extends to a phrase.
     */
    fun selectWordInParagraph(token: Token, paragraphIndex: Int, tokenIndex: Int) {
        val currentPhrase = _selectedPhrase.value
        val currentWord = _selectedWord.value

        if (currentWord != null && currentPhrase == null) {
            // A single word is selected — find it in the same paragraph to extend
            val chapter = _chapter.value ?: return
            if (paragraphIndex >= chapter.paragraphs.size) return
            val para = chapter.paragraphs[paragraphIndex]

            // Find the previously selected word's index
            val prevIdx = para.tokens.indexOfFirst { it === currentWord }
            if (prevIdx >= 0 && prevIdx != tokenIndex) {
                val start = minOf(prevIdx, tokenIndex)
                val end = maxOf(prevIdx, tokenIndex) + 1
                _selectedPhrase.value = Triple(paragraphIndex, start, end)
                _selectedWord.value = null
                computePhraseTranslation(paragraphIndex, start, end)
                return
            }
        }

        if (currentPhrase != null && currentPhrase.first == paragraphIndex) {
            // Extend or contract existing phrase selection
            val start = minOf(currentPhrase.second, tokenIndex)
            val end = maxOf(currentPhrase.third - 1, tokenIndex) + 1
            _selectedPhrase.value = Triple(paragraphIndex, start, end)
            computePhraseTranslation(paragraphIndex, start, end)
            return
        }

        // First tap — just select the single word
        _selectedWord.value = token
        _selectedPhrase.value = null
        _phraseTranslation.value = null
    }

    private fun computePhraseTranslation(paragraphIndex: Int, start: Int, end: Int) {
        val chapter = _chapter.value ?: return
        if (paragraphIndex >= chapter.paragraphs.size) return
        val para = chapter.paragraphs[paragraphIndex]
        val tokens = para.tokens.subList(start, end.coerceAtMost(para.tokens.size))
        val lang = _bookLanguage.value
        val nativeLang = settings.value.nativeLanguage

        viewModelScope.launch {
            val svc = AppDependencies.translationService
            val translation = svc?.translatePhrase(tokens, para.tokens, start, lang, nativeLang)
            _phraseTranslation.value = translation
        }
    }

    fun clearSelection() {
        _selectedWord.value = null
        _selectedPhrase.value = null
        _phraseTranslation.value = null
    }

    fun speak(text: String, language: String) {
        AppDependencies.audioPlayer.speak(text, language)
    }

    fun updateBookTranslationFrequency(freq: Float?) {
        _bookTranslationFrequency.value = freq
        val bookId = currentBookId ?: return
        viewModelScope.launch {
            bookRepo.updateBookTranslationFrequency(bookId, freq)
        }
    }

    fun updateDarkModePreference(pref: DarkModePreference) {
        viewModelScope.launch { settingsRepo.updateDarkModePreference(pref) }
    }

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepo.updateAppTheme(theme) }
    }

    fun updateReaderBackground(bg: ReaderBackground) {
        viewModelScope.launch { settingsRepo.updateReaderBackground(bg) }
    }

    fun updateReaderFont(font: ReaderFont) {
        viewModelScope.launch { settingsRepo.updateReaderFont(font) }
    }

    fun updateTranslationFrequency(freq: Float) {
        viewModelScope.launch { settingsRepo.updateTranslationFrequency(freq) }
    }

    fun updateIpaEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.updateIpaEnabled(enabled) }
    }

    fun updateIpaPosition(position: it.bosler.polyphoneme.model.IpaPosition) {
        viewModelScope.launch { settingsRepo.updateIpaPosition(position) }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepo.updateLineSpacing(spacing) }
    }

    fun updateLetterSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepo.updateLetterSpacing(spacing) }
    }

    fun updateWordSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepo.updateWordSpacing(spacing) }
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
            try {
                val count = _chapterCount.value
                var idx = index.coerceIn(0, count - 1)
                val nativeLang = settings.value.nativeLanguage

                // Check if we have a preloaded enriched chapter
                val cached = chapterCache.remove(idx)
                if (cached != null && cached.paragraphs.isNotEmpty()) {
                    _chapterIndex.value = idx
                    _chapter.value = cached
                    _isLoading.value = false
                    bookRepo.updateLastRead(bookId, idx)
                    preloadAdjacentChapters(bookId, idx, count, nativeLang)
                    return@launch
                }

                // Skip empty chapters
                val direction = if (forward) 1 else -1
                var rawChapter = bookRepo.loadChapterRaw(bookId, idx)
                while (rawChapter.paragraphs.isEmpty() && idx + direction in 0 until count) {
                    idx += direction
                    rawChapter = bookRepo.loadChapterRaw(bookId, idx)
                }

                _chapterIndex.value = idx
                bookRepo.updateLastRead(bookId, idx)

                // Load enriched chapter (IPA + translations) — show loading until ready
                val enrichedChapter = bookRepo.loadChapter(bookId, idx, nativeLang)
                if (_chapterIndex.value == idx && currentBookId == bookId) {
                    _chapter.value = enrichedChapter
                }

                // Preload adjacent chapters in background
                preloadAdjacentChapters(bookId, idx, count, nativeLang)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val chapterCache = mutableMapOf<Int, Chapter>()

    private fun preloadAdjacentChapters(bookId: String, currentIdx: Int, count: Int, nativeLang: String) {
        viewModelScope.launch {
            // Preload next chapter
            if (currentIdx + 1 < count) {
                try {
                    chapterCache[currentIdx + 1] = bookRepo.loadChapter(bookId, currentIdx + 1, nativeLang)
                } catch (_: Exception) {}
            }
            // Preload previous chapter
            if (currentIdx - 1 >= 0) {
                try {
                    chapterCache[currentIdx - 1] = bookRepo.loadChapter(bookId, currentIdx - 1, nativeLang)
                } catch (_: Exception) {}
            }
        }
    }
}
