package it.bosler.polyphoneme.data.library

import android.content.Context
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.epub.EpubReader
import it.bosler.polyphoneme.data.ipa.IpaService
import it.bosler.polyphoneme.data.translation.AndroidTranslationService
import it.bosler.polyphoneme.epub.EpubImporter
import it.bosler.polyphoneme.epub.EpubParser
import kotlinx.coroutines.async
import it.bosler.polyphoneme.model.BookMeta
import it.bosler.polyphoneme.model.Chapter
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class AndroidBookRepository(
    private val context: Context,
    private val ipaService: IpaService? = null,
    private val translationService: AndroidTranslationService? = null,
) : BookRepository {

    private val importer = EpubImporter(context)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // In-memory cache to avoid re-parsing library.json on every operation
    private var libraryCache: List<BookMeta>? = null

    // Cache the parsed epub Book to avoid re-reading all resources on every chapter navigation
    private var epubCache: Pair<String, Book>? = null // bookId -> Book

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

    override suspend fun loadChapter(bookId: String, chapterIndex: Int, nativeLanguage: String): Chapter {
        val book = loadLibrary().find { it.id == bookId }
            ?: throw IllegalArgumentException("Book not found: $bookId")
        val epubFile = File(book.filePath)
        val epubBook = epubCache?.takeIf { it.first == bookId }?.second
            ?: epubFile.inputStream().use { EpubReader().readEpub(it) }.also { epubCache = bookId to it }
        val chapter = EpubParser.parseChapter(epubBook, chapterIndex)

        // Detect language from actual chapter text — more reliable than EPUB metadata or import-time sampling
        val chapterText = chapter.paragraphs.joinToString(" ") { para ->
            para.tokens.joinToString(" ") { it.word }
        }
        val detected = if (chapterText.length >= 100) EpubParser.detectLanguage(chapterText) else null
        val language = detected ?: book.language ?: "en"
        if (detected != null && detected != book.language) {
            updateLanguage(bookId, detected)
        }

        // Run IPA transcription and translation in parallel
        val translationSvc = translationService
        return kotlinx.coroutines.coroutineScope {
            val ipaDeferred = if (ipaService != null) {
                async(kotlinx.coroutines.Dispatchers.Default) {
                    ipaService.transcribeInContext(chapter.paragraphs, language)
                }
            } else null

            val translationDeferred = if (translationSvc != null) {
                async(kotlinx.coroutines.Dispatchers.Default) {
                    translationSvc.translate(chapter.paragraphs, language, nativeLanguage)
                }
            } else null

            val ipaParagraphs = ipaDeferred?.await() ?: chapter.paragraphs
            val translatedParagraphs = translationDeferred?.await()

            // Merge: IPA from ipaParagraphs, translation from translatedParagraphs
            val merged = if (translatedParagraphs != null) {
                ipaParagraphs.mapIndexed { pIdx, para ->
                    val transPara = translatedParagraphs.getOrNull(pIdx) ?: para
                    para.copy(tokens = para.tokens.mapIndexed { tIdx, token ->
                        val transToken = transPara.tokens.getOrNull(tIdx)
                        if (transToken != null) {
                            token.copy(translation = transToken.translation, commonness = transToken.commonness)
                        } else token
                    })
                }
            } else ipaParagraphs

            chapter.copy(paragraphs = merged)
        }
    }

    override suspend fun loadChapterRaw(bookId: String, chapterIndex: Int): Chapter {
        val book = loadLibrary().find { it.id == bookId }
            ?: throw IllegalArgumentException("Book not found: $bookId")
        val epubFile = File(book.filePath)
        val epubBook = epubCache?.takeIf { it.first == bookId }?.second
            ?: epubFile.inputStream().use { EpubReader().readEpub(it) }.also { epubCache = bookId to it }
        return EpubParser.parseChapter(epubBook, chapterIndex)
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

    override suspend fun getTableOfContents(bookId: String): Pair<List<TocEntry>, Int> {
        val book = loadLibrary().find { it.id == bookId }
            ?: throw IllegalArgumentException("Book not found: $bookId")
        val epubFile = File(book.filePath)
        val epubBook = epubCache?.takeIf { it.first == bookId }?.second
            ?: epubFile.inputStream().use { EpubReader().readEpub(it) }.also { epubCache = bookId to it }
        return EpubParser.extractTableOfContents(epubBook)
    }

    override suspend fun updateBookLanguage(bookId: String, language: String) =
        updateLanguage(bookId, language)

    override suspend fun updateBookTranslationFrequency(bookId: String, frequency: Float?) {
        val library = loadLibrary().toMutableList()
        val index = library.indexOfFirst { it.id == bookId }
        if (index >= 0) {
            library[index] = library[index].copy(translationFrequency = frequency)
            saveLibrary(library)
        }
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
        libraryCache?.let { return it }
        if (!libraryFile.exists()) return emptyList<BookMeta>().also { libraryCache = it }
        return try {
            json.decodeFromString<List<BookMeta>>(libraryFile.readText()).also { libraryCache = it }
        } catch (_: Exception) {
            emptyList<BookMeta>().also { libraryCache = it }
        }
    }

    private fun saveLibrary(library: List<BookMeta>) {
        libraryCache = library
        libraryFile.writeText(json.encodeToString(library))
    }
}
