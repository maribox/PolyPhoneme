package it.bosler.polyphoneme.epub

import android.content.Context
import android.net.Uri
import it.bosler.polyphoneme.model.BookMeta
import java.io.File
import java.util.UUID

class EpubImporter(private val context: Context) {

    private val booksDir: File
        get() = File(context.filesDir, "books").also { it.mkdirs() }

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

        // Parse metadata
        val (_, parsed) = destFile.inputStream().use { EpubParser.parseMetadata(it) }

        return BookMeta(
            id = bookId,
            title = parsed.title,
            author = parsed.author,
            language = parsed.language,
            filePath = destFile.absolutePath,
            chapterCount = parsed.chapterCount,
            importedAt = System.currentTimeMillis(),
        )
    }

    fun deleteBookFile(filePath: String) {
        File(filePath).delete()
    }
}
