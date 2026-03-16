package it.bosler.polyphoneme.epub

import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.epub.EpubReader
import it.bosler.polyphoneme.data.library.TocEntry
import it.bosler.polyphoneme.model.Chapter
import it.bosler.polyphoneme.model.Paragraph
import org.jsoup.Jsoup
import java.io.InputStream

data class ParsedBook(
    val title: String,
    val author: String,
    val language: String?,
    val chapterCount: Int,
)

object EpubParser {

    fun parseMetadata(inputStream: InputStream): Pair<Book, ParsedBook> {
        val book = EpubReader().readEpub(inputStream)
        val metadata = book.metadata
        val title = metadata.firstTitle ?: "Untitled"
        val author = metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}".trim() } ?: "Unknown"
        val language = metadata.language?.takeIf { it.isNotBlank() }
        val chapterCount = book.spine.spineReferences.size
        return book to ParsedBook(title, author, language, chapterCount)
    }

    fun parseChapter(book: Book, chapterIndex: Int): Chapter {
        val spineRefs = book.spine.spineReferences
        if (chapterIndex !in spineRefs.indices) {
            return Chapter(index = chapterIndex, title = "Chapter ${chapterIndex + 1}", paragraphs = emptyList())
        }

        val resource = spineRefs[chapterIndex].resource
        val html = String(resource.data)
        val doc = Jsoup.parse(html)

        val title = doc.selectFirst("h1, h2, h3")?.text() ?: "Chapter ${chapterIndex + 1}"

        val blockElements = doc.select("p, h1, h2, h3, h4, h5, h6, li, blockquote, dd, dt")
        val paragraphs = blockElements.mapNotNull { element ->
            val text = element.text().trim()
            if (text.isEmpty()) return@mapNotNull null
            val tokens = Tokenizer.tokenize(text)
            if (tokens.isEmpty()) return@mapNotNull null
            Paragraph(tokens)
        }

        return Chapter(index = chapterIndex, title = title, paragraphs = paragraphs)
    }

    fun extractTableOfContents(book: Book): List<TocEntry> {
        val toc = book.tableOfContents
        if (toc != null && toc.tocReferences.isNotEmpty()) {
            val spineRefs = book.spine.spineReferences
            return toc.tocReferences.mapNotNull { ref ->
                val spineIndex = spineRefs.indexOfFirst { it.resource.href == ref.resource?.href }
                if (spineIndex >= 0) {
                    TocEntry(index = spineIndex, title = ref.title ?: "Chapter ${spineIndex + 1}")
                } else null
            }
        }
        // Fallback: generate TOC from spine
        return book.spine.spineReferences.mapIndexed { index, ref ->
            val html = try { String(ref.resource.data) } catch (_: Exception) { "" }
            val title = if (html.isNotEmpty()) {
                Jsoup.parse(html).selectFirst("h1, h2, h3, title")?.text()
            } else null
            TocEntry(index = index, title = title ?: "Chapter ${index + 1}")
        }
    }
}
