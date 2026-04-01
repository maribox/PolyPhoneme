package it.bosler.polyphoneme.data.ipa

/**
 * Explanations for the individual terms that make up phoneme descriptions.
 * E.g. "voiced alveolar plosive" → three terms, each with a category and explanation.
 */

enum class TermCategory { VOICING, PLACE, MANNER, VOWEL_HEIGHT, VOWEL_BACKNESS, VOWEL_ROUNDING, OTHER }

data class PhonemeTermInfo(
    val term: String,
    val category: TermCategory,
    val explanation: String,
)

object PhonemeTerms {

    private val terms = mapOf(
        // --- Voicing ---
        "voiced" to PhonemeTermInfo("voiced", TermCategory.VOICING,
            "Vocal cords vibrate during the sound. Put your hand on your throat and say \"zzz\" to feel it."),
        "voiceless" to PhonemeTermInfo("voiceless", TermCategory.VOICING,
            "Vocal cords don't vibrate. Say \"sss\" — you'll feel no vibration in your throat."),

        // --- Place of articulation ---
        "bilabial" to PhonemeTermInfo("bilabial", TermCategory.PLACE,
            "Made with both lips together, like the start of \"ball\" or \"mall\"."),
        "labiodental" to PhonemeTermInfo("labiodental", TermCategory.PLACE,
            "Lower lip touches upper teeth, like the start of \"fan\" or \"van\"."),
        "dental" to PhonemeTermInfo("dental", TermCategory.PLACE,
            "Tongue tip touches or is near the upper teeth, like the \"th\" in \"this\"."),
        "alveolar" to PhonemeTermInfo("alveolar", TermCategory.PLACE,
            "Tongue tip touches the ridge behind the upper front teeth, like \"t\", \"d\", \"n\", \"s\"."),
        "postalveolar" to PhonemeTermInfo("postalveolar", TermCategory.PLACE,
            "Tongue just behind the alveolar ridge, like \"sh\" in \"ship\" or \"zh\" in \"measure\"."),
        "retroflex" to PhonemeTermInfo("retroflex", TermCategory.PLACE,
            "Tongue tip curled back behind the alveolar ridge. Common in Hindi and some English dialects."),
        "palatal" to PhonemeTermInfo("palatal", TermCategory.PLACE,
            "Tongue body raised toward the hard palate (roof of mouth), like the \"y\" in \"yes\"."),
        "velar" to PhonemeTermInfo("velar", TermCategory.PLACE,
            "Back of tongue touches the soft palate (velum), like \"k\", \"g\", or \"ng\"."),
        "uvular" to PhonemeTermInfo("uvular", TermCategory.PLACE,
            "Back of tongue near the uvula (the dangling bit at the back). The French/German \"r\" sound."),
        "pharyngeal" to PhonemeTermInfo("pharyngeal", TermCategory.PLACE,
            "Constriction in the throat (pharynx). Common in Arabic."),
        "glottal" to PhonemeTermInfo("glottal", TermCategory.PLACE,
            "Made at the vocal cords themselves, like the \"h\" in \"hat\" or the catch in \"uh-oh\"."),

        // --- Manner of articulation ---
        "plosive" to PhonemeTermInfo("plosive", TermCategory.MANNER,
            "Airflow is completely blocked, then released in a burst — like \"p\", \"b\", \"t\", \"d\"."),
        "stop" to PhonemeTermInfo("stop", TermCategory.MANNER,
            "Airflow is completely blocked, then released in a burst — like \"p\", \"b\", \"t\", \"d\"."),
        "nasal" to PhonemeTermInfo("nasal", TermCategory.MANNER,
            "Air flows through the nose while the mouth is blocked — like \"m\", \"n\", or \"ng\"."),
        "trill" to PhonemeTermInfo("trill", TermCategory.MANNER,
            "A body part vibrates rapidly against another — like the rolled \"rr\" in Spanish \"perro\"."),
        "tap" to PhonemeTermInfo("tap", TermCategory.MANNER,
            "A single brief contact — like the quick \"t\" sound in American English \"butter\"."),
        "flap" to PhonemeTermInfo("flap", TermCategory.MANNER,
            "A single brief contact — like the quick \"d\" sound in American English \"ladder\"."),
        "fricative" to PhonemeTermInfo("fricative", TermCategory.MANNER,
            "Air is forced through a narrow gap, creating turbulence — like \"f\", \"s\", \"sh\", \"h\"."),
        "sibilant" to PhonemeTermInfo("sibilant", TermCategory.MANNER,
            "A fricative with a high-pitched hissing quality — like \"s\", \"z\", \"sh\", \"zh\"."),
        "approximant" to PhonemeTermInfo("approximant", TermCategory.MANNER,
            "Articulators approach each other but don't touch — like \"w\", \"r\", \"y\" in English."),
        "lateral" to PhonemeTermInfo("lateral", TermCategory.MANNER,
            "Air flows around the sides of the tongue — like \"l\" in \"lake\"."),
        "affricate" to PhonemeTermInfo("affricate", TermCategory.MANNER,
            "Starts as a stop, releases as a fricative — like \"ch\" in \"church\" or \"j\" in \"judge\"."),

        // --- Vowel height ---
        "close" to PhonemeTermInfo("close", TermCategory.VOWEL_HEIGHT,
            "Tongue is as high as possible without creating friction — like the vowel in \"see\" or \"too\"."),
        "near-close" to PhonemeTermInfo("near-close", TermCategory.VOWEL_HEIGHT,
            "Tongue slightly lower than close — like the vowel in \"bit\" or \"put\"."),
        "close-mid" to PhonemeTermInfo("close-mid", TermCategory.VOWEL_HEIGHT,
            "Tongue in the upper-middle area — like the vowel in French \"été\" or German \"See\"."),
        "mid" to PhonemeTermInfo("mid", TermCategory.VOWEL_HEIGHT,
            "Tongue at a neutral middle height — like the \"uh\" in \"about\" (schwa)."),
        "open-mid" to PhonemeTermInfo("open-mid", TermCategory.VOWEL_HEIGHT,
            "Tongue in the lower-middle area — like the vowel in \"bed\" or \"thought\"."),
        "near-open" to PhonemeTermInfo("near-open", TermCategory.VOWEL_HEIGHT,
            "Tongue slightly higher than fully open — like the vowel in \"cat\"."),
        "open" to PhonemeTermInfo("open", TermCategory.VOWEL_HEIGHT,
            "Tongue as low as possible, mouth wide open — like the vowel in \"father\"."),

        // --- Vowel backness ---
        "front" to PhonemeTermInfo("front", TermCategory.VOWEL_BACKNESS,
            "Tongue is pushed forward in the mouth — like the vowels in \"see\", \"say\", \"cat\"."),
        "central" to PhonemeTermInfo("central", TermCategory.VOWEL_BACKNESS,
            "Tongue is in the middle of the mouth — like the \"uh\" in \"about\"."),
        "near-front" to PhonemeTermInfo("near-front", TermCategory.VOWEL_BACKNESS,
            "Tongue slightly behind the front position — like the vowel in \"bit\"."),
        "near-back" to PhonemeTermInfo("near-back", TermCategory.VOWEL_BACKNESS,
            "Tongue slightly in front of the back position — like the vowel in \"put\"."),
        "back" to PhonemeTermInfo("back", TermCategory.VOWEL_BACKNESS,
            "Tongue is pulled back in the mouth — like the vowels in \"too\", \"go\", \"father\"."),

        // --- Vowel rounding ---
        "rounded" to PhonemeTermInfo("rounded", TermCategory.VOWEL_ROUNDING,
            "Lips are pursed/rounded — like the vowels in \"too\", \"go\", or French \"tu\"."),
        "unrounded" to PhonemeTermInfo("unrounded", TermCategory.VOWEL_ROUNDING,
            "Lips are spread or neutral — like the vowels in \"see\", \"bed\", \"cat\"."),

        // --- Other ---
        "vowel" to PhonemeTermInfo("vowel", TermCategory.OTHER,
            "A sound made with an open vocal tract — the voice flows freely without obstruction."),
        "consonant" to PhonemeTermInfo("consonant", TermCategory.OTHER,
            "A sound made by partially or fully obstructing airflow in the vocal tract."),
        "diphthong" to PhonemeTermInfo("diphthong", TermCategory.OTHER,
            "A vowel that glides from one position to another within the same syllable — like \"ow\" in \"cow\"."),
        "nasalized" to PhonemeTermInfo("nasalized", TermCategory.OTHER,
            "Air flows through the nose as well as the mouth — common in French vowels like \"on\", \"an\"."),
        "schwa" to PhonemeTermInfo("schwa", TermCategory.OTHER,
            "The most common vowel in English — a short, neutral \"uh\" sound, like the \"a\" in \"about\"."),
    )

    /**
     * Parse a phoneme description into its component terms and look up each one.
     * Returns terms in order, with known terms having explanations and unknown terms returned as-is.
     */
    fun parseDescription(description: String): List<PhonemeTermInfo> {
        if (description.isBlank()) return emptyList()

        val result = mutableListOf<PhonemeTermInfo>()
        val desc = description.lowercase()

        // Check for multi-word terms first (e.g. "near-close", "close-mid", "near-front")
        val remaining = mutableListOf<String>()
        val words = desc.split(" ", "(", ")")
            .map { it.trim(',', ' ') }
            .filter { it.isNotBlank() }

        var i = 0
        while (i < words.size) {
            // Try two-word hyphenated terms
            val word = words[i]
            val matched = terms[word]
            if (matched != null) {
                result.add(matched)
            }
            // Skip filler words
            i++
        }
        return result
    }

    fun lookup(term: String): PhonemeTermInfo? = terms[term.lowercase()]
}
