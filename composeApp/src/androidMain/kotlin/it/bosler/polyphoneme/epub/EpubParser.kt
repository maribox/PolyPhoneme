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

    /** Returns the index of the first chapter with substantial content (>= 300 words). */
    fun findFirstContentChapter(book: Book): Int {
        val spineRefs = book.spine.spineReferences
        for (i in spineRefs.indices) {
            val html = try { String(spineRefs[i].resource.data) } catch (_: Exception) { continue }
            val doc = Jsoup.parse(html)
            // Remove style/script noise
            doc.select("style, script").remove()
            val wordCount = doc.body()?.text()?.split(Regex("\\s+"))?.count { it.length > 2 } ?: 0
            if (wordCount >= 300) return i
        }
        return 0
    }

    /**
     * Detects the language of [text] from the set of supported languages.
     * Uses script detection for Russian/Japanese/Chinese, stop-word frequency for Latin languages.
     */
    fun detectLanguage(text: String): String? {
        if (text.length < 100) return null
        val sample = text.take(3000).lowercase()

        // Script detection — fast and reliable
        val cyrillic = sample.count { it in '\u0400'..'\u04FF' }
        val hiragana = sample.count { it in '\u3040'..'\u309F' }
        val katakana = sample.count { it in '\u30A0'..'\u30FF' }
        val cjk     = sample.count { it in '\u4E00'..'\u9FFF' || it in '\u3400'..'\u4DBF' }
        val total   = sample.count { !it.isWhitespace() }.coerceAtLeast(1)
        if (cyrillic.toFloat() / total > 0.2f) return "ru"
        if ((hiragana + katakana).toFloat() / total > 0.04f) return "ja"
        if (cjk.toFloat() / total > 0.15f) return "zh"

        // Latin — score by language-discriminative stop words + diacritics
        fun hits(vararg words: String) = words.sumOf { w ->
            Regex("(?<![a-zA-ZÀ-ÿ])${Regex.escape(w)}(?![a-zA-ZÀ-ÿ])").findAll(sample).count()
        }
        val scores = mapOf(
            "de" to (hits("der", "die", "das", "ein", "eine", "nicht", "ist", "und", "auch", "aber", "werden", "haben")
                    + sample.count { it in "äöüß" } * 3),
            "fr" to (hits("le", "la", "les", "des", "une", "est", "que", "dans", "cette", "mais", "leur", "aussi", "même")
                    + sample.count { it in "éèêëàâùûôœç" } * 2),
            "es" to (hits("los", "las", "una", "que", "con", "por", "para", "pero", "como", "son", "está", "hay")
                    + sample.count { it in "ñáéíóúü" } * 4),
            "it" to (hits("gli", "una", "che", "con", "per", "sono", "questo", "della", "anche", "come", "però", "quando")
                    + sample.count { it in "àèéìòù" } * 2),
            "pt" to (hits("uma", "que", "com", "por", "para", "mas", "não", "são", "isso", "este", "ela", "pelo")
                    + sample.count { it in "ãõâêôáéíóú" } * 3),
            "nl" to (hits("het", "van", "een", "maar", "zijn", "deze", "ook", "werd", "door", "naar", "over", "worden")
                    + sample.count { it in "ij" } * 2),
            "en" to hits("the", "and", "that", "this", "with", "was", "have", "from", "they", "would", "been", "were"),
        )
        return scores.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
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
