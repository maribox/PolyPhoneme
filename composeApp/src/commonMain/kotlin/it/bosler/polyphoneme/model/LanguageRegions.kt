package it.bosler.polyphoneme.model

/**
 * Centralized, data-driven configuration for regional pronunciation variants.
 * Both IPA transcription and TTS use this to stay consistent.
 * To add a new language/region, just add an entry to [config].
 */
object LanguageRegions {

    data class Region(
        val code: String,
        val displayName: String,
        /** IPA character replacements applied when this region is active. */
        val ipaTransformations: List<Pair<String, String>> = emptyList(),
        /** ISO 3166-1 country code for TTS locale (e.g., "ES", "MX", "BR"). */
        val ttsCountryCode: String = "",
    )

    /**
     * All supported regional variants, keyed by language code.
     * The first region in each list is the default.
     */
    val config: Map<String, List<Region>> = mapOf(
        "es" to listOf(
            Region(
                code = "es",
                displayName = "Spain (Castilian)",
                ttsCountryCode = "ES",
            ),
            Region(
                code = "latam",
                displayName = "Latin America",
                ipaTransformations = listOf(
                    "θ" to "s",   // seseo: /θ/ → /s/
                    "ʎ" to "ʝ",   // yeísmo: /ʎ/ → /ʝ/
                ),
                ttsCountryCode = "MX",
            ),
        ),
        "pt" to listOf(
            Region(
                code = "pt",
                displayName = "Portugal",
                ttsCountryCode = "PT",
            ),
            Region(
                code = "br",
                displayName = "Brazil",
                ipaTransformations = listOf(
                    "ɐ" to "a",
                    "ɨ" to "i",
                ),
                ttsCountryCode = "BR",
            ),
        ),
    )

    /** Languages that have regional variants. */
    val supportedLanguages: Set<String> get() = config.keys

    fun getDefault(lang: String): String =
        config[lang]?.firstOrNull()?.code ?: ""

    fun getRegion(lang: String, regionCode: String): Region? =
        config[lang]?.find { it.code == regionCode }

    fun getRegions(lang: String): List<Region> =
        config[lang] ?: emptyList()

    /** Apply all IPA transformations for the given language and region. */
    fun applyIpaTransformations(ipa: String, lang: String, regionCode: String): String {
        val region = getRegion(lang, regionCode) ?: return ipa
        var result = ipa
        for ((from, to) in region.ipaTransformations) {
            result = result.replace(from, to)
        }
        return result
    }

    /** Get the TTS country code for a language + region combination. */
    fun getTtsCountryCode(lang: String, regionCode: String): String =
        getRegion(lang, regionCode)?.ttsCountryCode ?: ""
}
