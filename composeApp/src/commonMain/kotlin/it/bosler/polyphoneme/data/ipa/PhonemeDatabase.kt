package it.bosler.polyphoneme.data.ipa

data class PhonemeInfo(
    val symbol: String,
    val name: String,
    val examples: Map<String, String> = emptyMap(), // lang code -> "word"
)

object PhonemeDatabase {

    fun tokenize(ipa: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < ipa.length) {
            // Skip spaces and syllable breaks
            if (ipa[i] == ' ' || ipa[i] == '.') { i++; continue }

            // Try known digraphs
            if (i + 1 < ipa.length) {
                val two = ipa.substring(i, i + 2)
                if (two in digraphs) {
                    var end = i + 2
                    while (end < ipa.length && isCombining(ipa[end])) end++
                    result.add(ipa.substring(i, end))
                    i = end
                    continue
                }
            }
            // Single char + combining marks
            var end = i + 1
            while (end < ipa.length && isCombining(ipa[end])) end++
            result.add(ipa.substring(i, end))
            i = end
        }
        return result
    }

    fun lookup(symbol: String): PhonemeInfo? = phonemes[symbol] ?: phonemes[symbol.first().toString()]

    private fun isCombining(c: Char): Boolean {
        val type = Character.getType(c).toByte()
        return type == Character.NON_SPACING_MARK.toByte()
            || type == Character.COMBINING_SPACING_MARK.toByte()
            || c == 'ː' || c == 'ʰ' || c == 'ʷ' || c == 'ʲ' || c == 'ˠ' || c == 'ˤ'
    }

    private val digraphs = setOf(
        "tʃ", "dʒ", "ts", "dz", "pf",
        "aɪ", "aʊ", "ɔɪ", "eɪ", "oʊ", "ɪə", "ɛə", "ʊə",
        "aɪ", "ɑʊ", "ɔʏ",
    )

    private val phonemes: Map<String, PhonemeInfo> = buildMap {
        // --- Plosives ---
        put("p", PhonemeInfo("p", "voiceless bilabial plosive", mapOf("en" to "pin", "de" to "Paar", "fr" to "pas", "es" to "padre")))
        put("b", PhonemeInfo("b", "voiced bilabial plosive", mapOf("en" to "bat", "de" to "Ball", "fr" to "bon", "es" to "bien")))
        put("t", PhonemeInfo("t", "voiceless alveolar plosive", mapOf("en" to "top", "de" to "Tag", "fr" to "tout", "es" to "tomar")))
        put("d", PhonemeInfo("d", "voiced alveolar plosive", mapOf("en" to "dog", "de" to "Dach", "fr" to "dans", "es" to "dar")))
        put("k", PhonemeInfo("k", "voiceless velar plosive", mapOf("en" to "cat", "de" to "kalt", "fr" to "car", "es" to "casa")))
        put("ɡ", PhonemeInfo("ɡ", "voiced velar plosive", mapOf("en" to "go", "de" to "gut", "fr" to "gare", "es" to "gato")))
        put("g", PhonemeInfo("g", "voiced velar plosive", mapOf("en" to "go", "de" to "gut", "fr" to "gare", "es" to "gato")))
        put("ʔ", PhonemeInfo("ʔ", "glottal stop", mapOf("en" to "uh-oh", "de" to "Eis")))

        // --- Nasals ---
        put("m", PhonemeInfo("m", "voiced bilabial nasal", mapOf("en" to "man", "de" to "Maus", "fr" to "main", "es" to "malo")))
        put("n", PhonemeInfo("n", "voiced alveolar nasal", mapOf("en" to "net", "de" to "Name", "fr" to "nous", "es" to "nada")))
        put("ŋ", PhonemeInfo("ŋ", "voiced velar nasal", mapOf("en" to "sing", "de" to "lang")))
        put("ɲ", PhonemeInfo("ɲ", "voiced palatal nasal", mapOf("fr" to "agneau", "es" to "año")))
        put("ɱ", PhonemeInfo("ɱ", "voiced labiodental nasal", mapOf("en" to "symphony")))

        // --- Fricatives ---
        put("f", PhonemeInfo("f", "voiceless labiodental fricative", mapOf("en" to "fish", "de" to "Fisch", "fr" to "fait", "es" to "fuego")))
        put("v", PhonemeInfo("v", "voiced labiodental fricative", mapOf("en" to "vine", "de" to "was", "fr" to "vous")))
        put("s", PhonemeInfo("s", "voiceless alveolar fricative", mapOf("en" to "sun", "de" to "See", "fr" to "sans", "es" to "sol")))
        put("z", PhonemeInfo("z", "voiced alveolar fricative", mapOf("en" to "zoo", "de" to "See", "fr" to "zone")))
        put("ʃ", PhonemeInfo("ʃ", "voiceless postalveolar fricative", mapOf("en" to "ship", "de" to "Schule", "fr" to "chat")))
        put("ʒ", PhonemeInfo("ʒ", "voiced postalveolar fricative", mapOf("en" to "measure", "fr" to "jour")))
        put("θ", PhonemeInfo("θ", "voiceless dental fricative", mapOf("en" to "think", "es" to "cero")))
        put("ð", PhonemeInfo("ð", "voiced dental fricative", mapOf("en" to "this", "es" to "nada")))
        put("ç", PhonemeInfo("ç", "voiceless palatal fricative", mapOf("de" to "ich")))
        put("x", PhonemeInfo("x", "voiceless velar fricative", mapOf("de" to "Bach", "es" to "ojo")))
        put("χ", PhonemeInfo("χ", "voiceless uvular fricative", mapOf("de" to "ach")))
        put("ʁ", PhonemeInfo("ʁ", "voiced uvular fricative", mapOf("de" to "rot", "fr" to "rouge")))
        put("h", PhonemeInfo("h", "voiceless glottal fricative", mapOf("en" to "hat", "de" to "Haus")))
        put("ɦ", PhonemeInfo("ɦ", "voiced glottal fricative", mapOf("en" to "ahead")))
        put("ɸ", PhonemeInfo("ɸ", "voiceless bilabial fricative", mapOf("es" to "fuego")))
        put("β", PhonemeInfo("β", "voiced bilabial fricative", mapOf("es" to "lobo")))
        put("ɣ", PhonemeInfo("ɣ", "voiced velar fricative", mapOf("es" to "luego")))

        // --- Affricates ---
        put("tʃ", PhonemeInfo("tʃ", "voiceless postalveolar affricate", mapOf("en" to "church", "de" to "deutsch", "es" to "mucho")))
        put("dʒ", PhonemeInfo("dʒ", "voiced postalveolar affricate", mapOf("en" to "judge")))
        put("ts", PhonemeInfo("ts", "voiceless alveolar affricate", mapOf("de" to "Zahl", "es" to "pizza")))
        put("pf", PhonemeInfo("pf", "voiceless labiodental affricate", mapOf("de" to "Pferd")))

        // --- Approximants ---
        put("j", PhonemeInfo("j", "voiced palatal approximant", mapOf("en" to "yes", "de" to "ja", "fr" to "yeux", "es" to "yo")))
        put("w", PhonemeInfo("w", "voiced labio-velar approximant", mapOf("en" to "wet", "fr" to "oui")))
        put("ʋ", PhonemeInfo("ʋ", "voiced labiodental approximant", mapOf("de" to "was")))
        put("ɹ", PhonemeInfo("ɹ", "voiced alveolar approximant", mapOf("en" to "red")))
        put("ɻ", PhonemeInfo("ɻ", "voiced retroflex approximant", mapOf("en" to "red")))
        put("ɥ", PhonemeInfo("ɥ", "voiced labio-palatal approximant", mapOf("fr" to "huit")))

        // --- Laterals ---
        put("l", PhonemeInfo("l", "voiced alveolar lateral", mapOf("en" to "love", "de" to "laut", "fr" to "lune", "es" to "luna")))
        put("ʎ", PhonemeInfo("ʎ", "voiced palatal lateral", mapOf("es" to "calle")))
        put("ɫ", PhonemeInfo("ɫ", "velarized alveolar lateral (dark L)", mapOf("en" to "full")))

        // --- Trills & Taps ---
        put("r", PhonemeInfo("r", "voiced alveolar trill", mapOf("es" to "perro", "de" to "Rat")))
        put("ʀ", PhonemeInfo("ʀ", "voiced uvular trill", mapOf("de" to "rot", "fr" to "rouge")))
        put("ɾ", PhonemeInfo("ɾ", "voiced alveolar tap", mapOf("es" to "pero", "en" to "butter")))
        put("ɽ", PhonemeInfo("ɽ", "voiced retroflex flap", mapOf()))

        // --- Monophthong vowels ---
        put("i", PhonemeInfo("i", "close front unrounded vowel", mapOf("en" to "see", "de" to "Stil", "fr" to "si", "es" to "sí")))
        put("ɪ", PhonemeInfo("ɪ", "near-close near-front unrounded vowel", mapOf("en" to "bit", "de" to "mit")))
        put("e", PhonemeInfo("e", "close-mid front unrounded vowel", mapOf("de" to "See", "fr" to "été", "es" to "tres")))
        put("ɛ", PhonemeInfo("ɛ", "open-mid front unrounded vowel", mapOf("en" to "bed", "de" to "Bett", "fr" to "fête")))
        put("æ", PhonemeInfo("æ", "near-open front unrounded vowel", mapOf("en" to "cat")))
        put("a", PhonemeInfo("a", "open front unrounded vowel", mapOf("de" to "Mann", "fr" to "la", "es" to "casa")))
        put("ɑ", PhonemeInfo("ɑ", "open back unrounded vowel", mapOf("en" to "father")))
        put("ɒ", PhonemeInfo("ɒ", "open back rounded vowel", mapOf("en" to "lot (British)")))
        put("ɔ", PhonemeInfo("ɔ", "open-mid back rounded vowel", mapOf("en" to "thought", "de" to "voll", "fr" to "sort")))
        put("o", PhonemeInfo("o", "close-mid back rounded vowel", mapOf("de" to "Boot", "fr" to "beau", "es" to "todo")))
        put("ʊ", PhonemeInfo("ʊ", "near-close near-back rounded vowel", mapOf("en" to "put", "de" to "Mutter")))
        put("u", PhonemeInfo("u", "close back rounded vowel", mapOf("en" to "boot", "de" to "Mut", "fr" to "tout", "es" to "tu")))
        put("ə", PhonemeInfo("ə", "mid central vowel (schwa)", mapOf("en" to "about", "de" to "bitte", "fr" to "le")))
        put("ɜ", PhonemeInfo("ɜ", "open-mid central unrounded vowel", mapOf("en" to "bird")))
        put("ʌ", PhonemeInfo("ʌ", "open-mid back unrounded vowel", mapOf("en" to "cup")))
        put("ɐ", PhonemeInfo("ɐ", "near-open central vowel", mapOf("de" to "besser")))
        put("ɤ", PhonemeInfo("ɤ", "close-mid back unrounded vowel", mapOf()))

        // --- Front rounded vowels (French/German) ---
        put("y", PhonemeInfo("y", "close front rounded vowel", mapOf("de" to "über", "fr" to "tu")))
        put("ʏ", PhonemeInfo("ʏ", "near-close near-front rounded vowel", mapOf("de" to "hübsch")))
        put("ø", PhonemeInfo("ø", "close-mid front rounded vowel", mapOf("de" to "schön", "fr" to "feu")))
        put("œ", PhonemeInfo("œ", "open-mid front rounded vowel", mapOf("de" to "zwölf", "fr" to "neuf")))

        // --- Nasalized vowels (French) ---
        put("ɑ̃", PhonemeInfo("ɑ̃", "nasalized open back vowel", mapOf("fr" to "an")))
        put("ɛ̃", PhonemeInfo("ɛ̃", "nasalized open-mid front vowel", mapOf("fr" to "vin")))
        put("ɔ̃", PhonemeInfo("ɔ̃", "nasalized open-mid back vowel", mapOf("fr" to "bon")))
        put("œ̃", PhonemeInfo("œ̃", "nasalized open-mid front rounded vowel", mapOf("fr" to "un")))

        // --- Diphthongs ---
        put("aɪ", PhonemeInfo("aɪ", "diphthong", mapOf("en" to "price", "de" to "mein")))
        put("aʊ", PhonemeInfo("aʊ", "diphthong", mapOf("en" to "mouth", "de" to "Haus")))
        put("ɔɪ", PhonemeInfo("ɔɪ", "diphthong", mapOf("en" to "choice")))
        put("eɪ", PhonemeInfo("eɪ", "diphthong", mapOf("en" to "face")))
        put("oʊ", PhonemeInfo("oʊ", "diphthong", mapOf("en" to "goat")))
        put("ɔʏ", PhonemeInfo("ɔʏ", "diphthong", mapOf("de" to "neu")))
        put("ɪə", PhonemeInfo("ɪə", "diphthong", mapOf("en" to "near")))
        put("ɛə", PhonemeInfo("ɛə", "diphthong", mapOf("en" to "square")))
        put("ʊə", PhonemeInfo("ʊə", "diphthong", mapOf("en" to "cure")))
        put("ɑʊ", PhonemeInfo("ɑʊ", "diphthong", mapOf("en" to "now")))

        // --- Suprasegmentals ---
        put("ˈ", PhonemeInfo("ˈ", "primary stress (follows this mark)"))
        put("ˌ", PhonemeInfo("ˌ", "secondary stress (follows this mark)"))
        put("ː", PhonemeInfo("ː", "long vowel (preceding vowel is lengthened)"))
    }
}
