package it.bosler.polyphoneme.epub

import android.content.Context
import android.net.Uri
import it.bosler.polyphoneme.model.BookMeta
import java.io.File
import java.util.UUID

class EpubImporter(private val context: Context) {

    private val booksDir: File
        get() = File(context.filesDir, "books").also { it.mkdirs() }

    private val coversDir: File
        get() = File(context.filesDir, "covers").also { it.mkdirs() }

    fun importBook(uriString: String): BookMeta {
        val uri = Uri.parse(uriString)
        val bookId = UUID.randomUUID().toString()
        val destFile = File(booksDir, "$bookId.epub")

        // Copy EPUB to internal storage
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open URI: $uriString")

        // Parse metadata and extract cover
        val (book, parsed) = destFile.inputStream().use { EpubParser.parseMetadata(it) }
        val coverPath = extractCover(book, bookId)

        return BookMeta(
            id = bookId,
            title = parsed.title,
            author = parsed.author,
            language = parsed.language,
            filePath = destFile.absolutePath,
            coverPath = coverPath,
            chapterCount = parsed.chapterCount,
            importedAt = System.currentTimeMillis(),
        )
    }

    private fun extractCover(book: io.documentnode.epub4j.domain.Book, bookId: String): String? {
        val coverImage = book.coverImage ?: return null
        return try {
            val ext = when {
                coverImage.mediaType?.name?.contains("png") == true -> "png"
                coverImage.mediaType?.name?.contains("gif") == true -> "gif"
                else -> "jpg"
            }
            val coverFile = File(coversDir, "$bookId.$ext")
            coverFile.outputStream().use { out ->
                out.write(coverImage.data)
            }
            coverFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun deleteBookFile(filePath: String) {
        File(filePath).delete()
    }
}
