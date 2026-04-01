package it.bosler.polyphoneme.data.library

import it.bosler.polyphoneme.model.BookMeta
import it.bosler.polyphoneme.model.Chapter

data class TocEntry(val index: Int, val title: String)

interface BookRepository {
    suspend fun importBook(uri: String): BookMeta
    suspend fun getLibrary(): List<BookMeta>
    suspend fun getBookMeta(bookId: String): BookMeta?
    suspend fun deleteBook(id: String)
    suspend fun loadChapter(bookId: String, chapterIndex: Int, nativeLanguage: String = "en"): Chapter
    /** Fast: parse EPUB chapter text only, no IPA or translation. */
    suspend fun loadChapterRaw(bookId: String, chapterIndex: Int): Chapter
    suspend fun getChapterCount(bookId: String): Int
    suspend fun updateLastRead(bookId: String, chapterIndex: Int)
    suspend fun updateBookLanguage(bookId: String, language: String)
    suspend fun updateBookTranslationFrequency(bookId: String, frequency: Float?)
    /** Returns all TOC entries and the index of the first content chapter. */
    suspend fun getTableOfContents(bookId: String): Pair<List<TocEntry>, Int>
}
