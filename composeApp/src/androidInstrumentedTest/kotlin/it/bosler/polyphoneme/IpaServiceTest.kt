package it.bosler.polyphoneme

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.bosler.polyphoneme.data.ipa.DictionaryIpaService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IpaServiceTest {

    @Test
    fun testGermanIpaForCommonWords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = DictionaryIpaService(context)

        assertTrue("German should be supported", service.isLanguageSupported("de"))

        val words = listOf("die", "der", "und", "ist", "nicht", "himmlischen", "Sonne", "Erde")
        val result = service.transcribe(words, "de")

        // Log all results
        for ((word, ipa) in result) {
            val codepoints = ipa.map { "U+${it.code.toString(16).padStart(4, '0')}" }
            Log.d("IpaTest", "$word → $ipa (codepoints: $codepoints)")
        }

        // "die" in German should be "diː" not "daɪ" (English)
        val dieIpa = result["die"]
        assertNotNull("'die' should have IPA", dieIpa)
        Log.d("IpaTest", "'die' IPA: '$dieIpa' codepoints: ${dieIpa!!.map { "U+${it.code.toString(16).padStart(4, '0')}" }}")

        // Must contain 'i' and 'ː' (U+02D0), NOT 'a' and 'ɪ'
        assertTrue("'die' IPA should start with 'd'", dieIpa.startsWith("d"))
        assertTrue("'die' IPA should contain 'ː' (U+02D0)", dieIpa.contains('\u02D0'))
        assertFalse("'die' IPA should NOT contain 'ɪ' (English)", dieIpa.contains('\u026A'))

        // "der" should be German
        val derIpa = result["der"]
        assertNotNull("'der' should have IPA", derIpa)
        Log.d("IpaTest", "'der' IPA: '$derIpa'")

        // "und" should be German "ʊnt"
        val undIpa = result["und"]
        assertNotNull("'und' should have IPA", undIpa)
        Log.d("IpaTest", "'und' IPA: '$undIpa'")
        assertTrue("'und' should contain 'ʊ'", undIpa!!.contains('ʊ'))
    }
}
