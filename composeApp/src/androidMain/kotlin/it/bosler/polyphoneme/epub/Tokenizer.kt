package it.bosler.polyphoneme.epub

import it.bosler.polyphoneme.model.Token

object Tokenizer {

    private val TOKEN_PATTERN = Regex("""(\p{P}*)([\p{L}\p{M}\p{N}][\p{L}\p{M}\p{N}'''\u2019-]*)(\p{P}*)""")

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        for (match in TOKEN_PATTERN.findAll(text)) {
            val leading = match.groupValues[1]
            val word = match.groupValues[2]
            val trailing = match.groupValues[3]
            tokens.add(Token(word = word, leadingPunctuation = leading, trailingPunctuation = trailing))
        }
        return tokens
    }
}
