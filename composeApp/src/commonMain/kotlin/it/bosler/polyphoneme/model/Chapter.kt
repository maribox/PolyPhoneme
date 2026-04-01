package it.bosler.polyphoneme.model

data class Chapter(
    val index: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
)

data class Paragraph(
    val tokens: List<Token>,
)

data class Token(
    val word: String,
    val leadingPunctuation: String = "",
    val trailingPunctuation: String = "",
    val ipa: String? = null,
    val translation: String? = null,
    /** 0.0 = very rare word, 1.0 = very common (function word). Used by frequency slider. */
    val commonness: Float = 0.5f,
    val isDisambiguated: Boolean = false,
    val alternativePronunciations: List<String> = emptyList(),
)
