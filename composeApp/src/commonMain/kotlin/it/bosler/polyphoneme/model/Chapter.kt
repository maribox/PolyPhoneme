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
)
