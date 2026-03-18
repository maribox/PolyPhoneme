package it.bosler.polyphoneme.data.library

import android.content.Context
import io.documentnode.epub4j.epub.EpubReader
import it.bosler.polyphoneme.data.ipa.IpaService
import it.bosler.polyphoneme.epub.EpubImporter
import it.bosler.polyphoneme.epub.EpubParser
import it.bosler.polyphoneme.model.BookMeta
import it.bosler.polyphoneme.model.Chapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class AndroidBookRepository(
    private val context: Context,
    private val ipaService: IpaService? = null,
) : BookRepository {

    private val importer = EpubImporter(context)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val libraryFile: File
        get() = File(context.filesDir, "library.json")

    override suspend fun importBook(uri: String): BookMeta {
        val bookMeta = importer.importBook(uri)
        val library = loadLibrary().toMutableList()
        library.add(bookMeta)
        saveLibrary(library)
        return bookMeta
    }

    override suspend fun getLibrary(): List<BookMeta> {
        val library = loadLibrary()
        // Back-fill covers for books imported before cover extraction was added
        var changed = false
        val updated = library.map { book ->
            if (book.coverPath == null) {
                val coverPath = extractCoverForExisting(book)
                if (coverPath != null) {
                    changed = true
                    book.copy(coverPath = coverPath)
                } else book
            } else book
        }
        if (changed) saveLibrary(updated)
        return updated
    }

    private fun extractCoverForExisting(book: BookMeta): String? {
        return try {
            val epubFile = File(book.filePath)
            if (!epubFile.exists()) return null
            val epubBook = epubFile.inputStream().use { EpubReader().readEpub(it) }
            val coverImage = epubBook.coverImage ?: return null
            val coversDir = File(context.filesDir, "covers").also { it.mkdirs() }
            val ext = when {
                coverImage.mediaType?.name?.contains("png") == true -> "png"
                else -> "jpg"
            }
            val coverFile = File(coversDir, "${book.id}.$ext")
            coverFile.outputStream().use { out -> out.write(coverImage.data) }
            coverFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getBookMeta(bookId: String): BookMeta? =
        loadLibrary().find { it.id == bookId }

    override suspend fun deleteBook(id: String) {
        val library = loadLibrary().toMutableList()
        val book = library.find { it.id == id } ?: return
        importer.deleteBookFile(book.filePath)
        book.coverPath?.let { File(it).delete() }
        library.removeAll { it.id == id }
        saveLibrary(library)
    }

    override suspend fun loadChapter(bookId: String, chapterIndex: Int): Chapter {
        val book = loadLibrary().find { it.id == bookId }
            ?: throw IllegalArgumentException("Book not found: $bookId")
        val epubFile = File(book.filePath)
        val epubBook = epubFile.inputStream().use { EpubReader().readEpub(it) }
        val chapter = EpubParser.parseChapter(epubBook, chapterIndex)

        // Detect language from EPUB metadata if not stored, and persist it
        val language = book.language ?: run {
            val detected = epubBook.metadata.language?.takeIf { it.isNotBlank() } ?: "en"
            updateLanguage(bookId, detected)
            detected
        }

        // Apply IPA transcription with context-aware disambiguation
        val service = ipaService ?: return chapter
        val transcribedParagraphs = service.transcribeInContext(chapter.paragraphs, language)
        return chapter.copy(paragraphs = transcribedParagraphs)
    }

    override suspend fun getChapterCount(bookId: String): Int {
        return loadLibrary().find { it.id == bookId }?.chapterCount ?: 0
    }

    override suspend fun updateLastRead(bookId: String, chapterIndex: Int) {
        val library = loadLibrary().toMutableList()
        val index = library.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            library[index] = library[index].copy(lastReadChapter = chapterIndex)
            saveLibrary(library)
        }
    }

    override suspend fun getTableOfContents(bookId: String): List<TocEntry> {
        val book = loadLibrary().find { it.id == bookId }
            ?: throw IllegalArgumentException("Book not found: $bookId")
        val epubFile = File(book.filePath)
        val epubBook = epubFile.inputStream().use { EpubReader().readEpub(it) }
        return EpubParser.extractTableOfContents(epubBook)
    }

    private fun updateLanguage(bookId: String, language: String) {
        val library = loadLibrary().toMutableList()
        val index = library.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            library[index] = library[index].copy(language = language)
            saveLibrary(library)
        }
    }

    private fun loadLibrary(): List<BookMeta> {
        if (!libraryFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<BookMeta>>(libraryFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLibrary(library: List<BookMeta>) {
        libraryFile.writeText(json.encodeToString(library))
    }
}
