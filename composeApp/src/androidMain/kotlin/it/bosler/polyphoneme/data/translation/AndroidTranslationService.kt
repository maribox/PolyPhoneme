package it.bosler.polyphoneme.data.translation

import android.content.Context
import android.util.Log
import it.bosler.polyphoneme.model.Paragraph
import it.bosler.polyphoneme.model.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Normalizer

/**
 * On-device translation using bundled dictionaries with context-aware disambiguation.
 *
 * Dictionary format: word<TAB>pos1:trans1[|pos2:trans2|...]
 * Architecture: pivot through English (src→en→tgt).
 *
 * Context strategies:
 *   1. Hardcoded function word overrides (articles, prepositions, conjunctions)
 *   2. POS disambiguation from surrounding words (±2 word window)
 *   3. Prefer single-word translations over definitions
 *   4. Morphological fallback (suffix stripping for inflected forms)
 */
class AndroidTranslationService(private val context: Context) : TranslationService {

    data class Sense(val pos: String, val translation: String)

    // lang pair → Map<word, List<Sense>>
    private val dictionaries = mutableMapOf<String, Map<String, List<Sense>>>()
    private val mutex = Mutex()

    // ── Function word overrides — these common words have garbage dict entries ──
    // Maps: language → word → best translation
    private val FUNCTION_OVERRIDES: Map<String, Map<String, String>> = mapOf(
        "fr" to mapOf(
            "le" to "the", "la" to "the", "les" to "the", "l'" to "the",
            "un" to "a", "une" to "a", "des" to "some",
            "du" to "of the", "au" to "to the", "aux" to "to the",
            "de" to "of", "d'" to "of", "à" to "to", "en" to "in",
            "et" to "and", "ou" to "or", "mais" to "but", "ni" to "nor",
            "que" to "that", "qui" to "who", "quoi" to "what",
            "ce" to "this", "ces" to "these", "cette" to "this", "cet" to "this",
            "mon" to "my", "ma" to "my", "mes" to "my",
            "ton" to "your", "ta" to "your", "tes" to "your",
            "son" to "his/her", "sa" to "his/her", "ses" to "his/her",
            "notre" to "our", "nos" to "our", "votre" to "your", "vos" to "your",
            "leur" to "their", "leurs" to "their",
            "je" to "I", "tu" to "you", "il" to "he", "elle" to "she",
            "nous" to "we", "vous" to "you", "ils" to "they", "elles" to "they",
            "me" to "me", "te" to "you", "se" to "oneself",
            "ne" to "not", "pas" to "not", "plus" to "more", "jamais" to "never",
            "si" to "if", "quand" to "when", "comme" to "like",
            "sur" to "on", "sous" to "under", "dans" to "in",
            "par" to "by", "pour" to "for", "avec" to "with", "sans" to "without",
            "entre" to "between", "vers" to "toward", "chez" to "at",
            "avant" to "before", "après" to "after", "depuis" to "since",
            "pendant" to "during", "contre" to "against",
            "très" to "very", "bien" to "well", "mal" to "badly",
            "aussi" to "also", "encore" to "still", "déjà" to "already",
            "tout" to "all", "tous" to "all", "toute" to "all", "toutes" to "all",
            "même" to "same", "autre" to "other", "autres" to "others",
            "où" to "where", "dont" to "whose", "donc" to "so",
            "car" to "because", "puis" to "then", "alors" to "then",
            "ici" to "here", "là" to "there",
            "oui" to "yes", "non" to "no",
            "y" to "there", "en" to "of it",
            "c'est" to "it is", "j'ai" to "I have",
            "n" to "not", "n'" to "not",
            "c" to "it", "c'" to "it", "d" to "of", "d'" to "of",
            "l" to "the", "l'" to "the", "j" to "I", "j'" to "I",
            "s" to "oneself", "s'" to "oneself",
            "qu" to "that", "qu'" to "that",
            "m" to "me", "m'" to "me", "t" to "you", "t'" to "you",
            "n'est" to "is not", "n'a" to "has not",
            "qu'il" to "that he", "qu'elle" to "that she",
            "qu'on" to "that one", "qu'un" to "that a",
            "peut-être" to "maybe", "aujourd'hui" to "today",
            "beaucoup" to "a lot", "peu" to "little",
            "derrière" to "behind", "devant" to "in front",
            // Common verbs with garbage dict entries
            "ouvrir" to "to open", "trouver" to "to find", "penser" to "to think",
            "sentir" to "to feel", "entendre" to "to hear", "tomber" to "to fall",
            "rester" to "to stay", "monter" to "to climb", "descendre" to "to go down",
            "tourner" to "to turn", "porter" to "to carry", "regarder" to "to look at",
            "chercher" to "to look for", "laisser" to "to let", "parler" to "to speak",
            "aimer" to "to love", "manger" to "to eat", "boire" to "to drink",
            "dormir" to "to sleep", "courir" to "to run", "marcher" to "to walk",
            "écrire" to "to write", "lire" to "to read", "comprendre" to "to understand",
            "attendre" to "to wait", "perdre" to "to lose", "suivre" to "to follow",
            "vivre" to "to live", "mourir" to "to die", "naître" to "to be born",
            "connaître" to "to know", "paraître" to "to appear",
            "sembler" to "to seem", "passer" to "to pass",
            "commencer" to "to begin", "finir" to "to finish",
            "sortir" to "to go out", "entrer" to "to enter",
            "monter" to "to go up", "tomber" to "to fall",
            "arriver" to "to arrive", "partir" to "to leave",
            "revenir" to "to come back", "retourner" to "to return",
            "devenir" to "to become", "tenir" to "to hold",
            "recevoir" to "to receive", "envoyer" to "to send",
            "jeter" to "to throw", "appeler" to "to call",
            "essayer" to "to try", "montrer" to "to show",
            "demander" to "to ask", "répondre" to "to answer",
            "lever" to "to lift", "poser" to "to put down",
            "rappeler" to "to remind", "souvenir" to "to remember",
            // Common verb forms missing from dict
            "suis" to "am", "es" to "are", "est" to "is", "sont" to "are",
            "sommes" to "are", "êtes" to "are",
            "ai" to "have", "as" to "have", "a" to "has", "avons" to "have",
            "avez" to "have", "ont" to "have",
            "était" to "was", "avait" to "had", "étaient" to "were",
            "sera" to "will be", "seront" to "will be",
            "fait" to "does", "va" to "goes", "vais" to "go",
            "allons" to "go", "allez" to "go", "vont" to "go",
            "dit" to "says", "sait" to "knows", "voit" to "sees",
            "peut" to "can", "veut" to "wants", "doit" to "must",
            "faut" to "must", "vient" to "comes", "prend" to "takes",
            "met" to "puts", "tient" to "holds", "reste" to "stays",
            "vaut" to "is worth", "croit" to "believes",
            "cette" to "this", "ceux" to "those",
            "quel" to "which", "quelle" to "which",
            "chaque" to "each", "plusieurs" to "several",
            "rien" to "nothing", "personne" to "nobody",
            "jamais" to "never", "toujours" to "always",
            "lorsque" to "when", "puisque" to "since",
            "parce" to "because", "afin" to "in order",
            "cependant" to "however", "pourtant" to "however",
            "néanmoins" to "nevertheless", "toutefois" to "however",
            "ainsi" to "thus", "assez" to "enough",
            // Common words with bad/missing dict entries
            "avoir" to "to have", "être" to "to be", "faire" to "to do",
            "aller" to "to go", "venir" to "to come", "voir" to "to see",
            "savoir" to "to know", "pouvoir" to "can", "vouloir" to "to want",
            "devoir" to "must", "dire" to "to say", "prendre" to "to take",
            "mettre" to "to put", "donner" to "to give", "croire" to "to believe",
            "bonne" to "good", "bon" to "good", "bons" to "good", "bonnes" to "good",
            "belle" to "beautiful", "beau" to "beautiful", "beaux" to "beautiful",
            "grande" to "big", "grand" to "big", "grands" to "big", "grandes" to "big",
            "petit" to "small", "petite" to "small", "petits" to "small", "petites" to "small",
            "nouveau" to "new", "nouvelle" to "new", "nouveaux" to "new",
            "vieux" to "old", "vieille" to "old", "vieil" to "old",
            "jeune" to "young", "long" to "long", "longue" to "long",
            "yeux" to "eyes", "œil" to "eye",
            "homme" to "man", "femme" to "woman", "enfant" to "child",
            "temps" to "time", "jour" to "day", "nuit" to "night",
            "main" to "hand", "tête" to "head", "corps" to "body",
            "vie" to "life", "mort" to "death", "monde" to "world",
            "eau" to "water", "terre" to "earth", "ciel" to "sky",
            "maison" to "house", "chambre" to "room", "porte" to "door",
            "côté" to "side", "peine" to "trouble", "heure" to "hour",
            "fois" to "time", "chose" to "thing", "part" to "part",
            "lieu" to "place", "sorte" to "kind",
            "couché" to "lying", "assis" to "sitting", "debout" to "standing",
            // Verb forms: irregular/frequent conjugations
            "voulais" to "wanted", "croyais" to "believed",
            "savais" to "knew", "devais" to "had to", "voyais" to "saw",
            "faisait" to "was doing", "disait" to "was saying",
            "prenait" to "was taking", "donnait" to "was giving",
            "semblait" to "seemed", "trouvait" to "was finding",
            "pouvait" to "could", "devait" to "had to", "voulait" to "wanted",
            "savait" to "knew", "voyait" to "saw", "croyait" to "believed",
            "venait" to "was coming", "tenait" to "was holding",
            "mettait" to "was putting", "fallait" to "was necessary",
            "eu" to "had", "été" to "been", "vu" to "seen",
            "su" to "known", "pu" to "could", "voulu" to "wanted",
            "mis" to "put", "pris" to "taken",
            "avais" to "had", "avait" to "had", "avaient" to "had",
            "étais" to "was", "étaient" to "were",
            "faisais" to "was doing", "faisaient" to "were doing",
            "allais" to "was going", "allaient" to "were going",
            "disais" to "was saying", "disaient" to "were saying",
            "endors" to "fall asleep", "endort" to "falls asleep",
            "endormir" to "to fall asleep", "endormi" to "asleep", "endormie" to "asleep",
            "éteint" to "extinguished", "éteinte" to "extinguished",
            "éteints" to "extinguished", "éteintes" to "extinguished",
            "fermer" to "to close", "ferme" to "closes",
            "fermait" to "was closing", "fermaient" to "were closing",
            "fermé" to "closed", "fermée" to "closed",
            "ouvert" to "open", "ouverte" to "open",
            "assis" to "sitting", "assise" to "sitting",
            "debout" to "standing", "allongé" to "lying down",
            "tombé" to "fallen", "tombée" to "fallen",
            "parti" to "left", "partie" to "left",
            "arrivé" to "arrived", "arrivée" to "arrived",
            "entré" to "entered", "entrée" to "entrance",
            "sorti" to "gone out", "sortie" to "exit",
            "venu" to "came", "venue" to "came",
            "devenu" to "became", "devenue" to "became",
            "resté" to "stayed", "restée" to "stayed",
            "monté" to "climbed", "montée" to "climbed",
            "descendu" to "descended", "descendue" to "descended",
            // More common conjugated forms
            "trouvait" to "was finding", "trouvé" to "found", "trouvée" to "found",
            "pensait" to "was thinking", "pensé" to "thought",
            "sentait" to "was feeling", "senti" to "felt",
            "entendait" to "was hearing", "entendu" to "heard",
            "portait" to "was carrying", "porté" to "carried",
            "regardait" to "was looking", "regardé" to "looked",
            "cherchait" to "was looking for", "cherché" to "looked for",
            "laissait" to "was letting", "laissé" to "let",
            "parlait" to "was speaking", "parlé" to "spoken",
            "aimait" to "loved", "aimé" to "loved",
            "passait" to "was passing", "passé" to "passed",
            "commençait" to "was beginning", "commencé" to "begun",
            "comprenait" to "was understanding", "compris" to "understood",
            "attendait" to "was waiting", "attendu" to "waited",
            "vivait" to "was living", "vécu" to "lived",
            "connaissait" to "knew", "connu" to "known",
            "paraissait" to "appeared", "paru" to "appeared",
            "semblait" to "seemed", "semblé" to "seemed",
            "appelait" to "was calling", "appelé" to "called",
            "montrait" to "was showing", "montré" to "shown",
            "demandait" to "was asking", "demandé" to "asked",
            "répondait" to "was answering", "répondu" to "answered",
            "vécu" to "lived", "vécue" to "lived", "vécus" to "lived", "vécues" to "lived",
            "né" to "born", "née" to "born", "nés" to "born", "nées" to "born",
            "mort" to "dead", "morte" to "dead", "morts" to "dead", "mortes" to "dead",
            "écrit" to "written", "écrite" to "written",
            "lu" to "read", "lue" to "read",
            "dite" to "said",
            "faite" to "done",
            // Common nouns with bad/missing dict entries
            "chambre" to "room", "voix" to "voice", "maman" to "mom",
            "visage" to "face", "bras" to "arm", "doigt" to "finger",
            "pied" to "foot", "pieds" to "feet", "cœur" to "heart",
            "esprit" to "mind", "âme" to "soul", "rêve" to "dream",
            "pensée" to "thought", "idée" to "idea", "parole" to "word",
            "histoire" to "story", "livre" to "book", "lettre" to "letter",
            "pays" to "country", "ville" to "city", "rue" to "street",
            "chemin" to "path", "jardin" to "garden", "arbre" to "tree",
            "fleur" to "flower", "soleil" to "sun", "lune" to "moon",
            "lumière" to "light", "ombre" to "shadow",
            "ami" to "friend", "amie" to "friend",
            "père" to "father", "mère" to "mother", "frère" to "brother",
            "sœur" to "sister", "fils" to "son", "fille" to "girl",
            "famille" to "family", "enfants" to "children",
            "travail" to "work", "argent" to "money",
            "longtemps" to "long time", "bientôt" to "soon",
            "maintenant" to "now", "soudain" to "suddenly",
            // More adjectives
            "blanc" to "white", "blanche" to "white",
            "noir" to "black", "noire" to "black",
            "rouge" to "red", "bleu" to "blue", "vert" to "green",
            "haut" to "high", "haute" to "high", "bas" to "low",
            "plein" to "full", "vide" to "empty",
            "seul" to "alone", "seule" to "alone",
            "dernier" to "last", "dernière" to "last",
            "premier" to "first", "première" to "first",
            "certain" to "certain", "certaine" to "certain",
            "propre" to "own", "pauvre" to "poor",
            "fort" to "strong", "forte" to "strong",
            "vrai" to "true", "vraie" to "true",
        ),
        "de" to mapOf(
            "der" to "the", "die" to "the", "das" to "the",
            "des" to "of the", "dem" to "the", "den" to "the",
            "ein" to "a", "eine" to "a", "einer" to "a", "einem" to "a", "einen" to "a",
            "und" to "and", "oder" to "or", "aber" to "but",
            "ich" to "I", "du" to "you", "er" to "he", "sie" to "she/they", "es" to "it",
            "wir" to "we", "ihr" to "you", "Sie" to "you",
            "nicht" to "not", "kein" to "no", "keine" to "no",
            "von" to "from", "zu" to "to", "mit" to "with", "für" to "for",
            "auf" to "on", "in" to "in", "an" to "at", "um" to "around",
            "aus" to "from", "nach" to "after", "bei" to "at",
            "über" to "over", "unter" to "under", "vor" to "before",
            "hinter" to "behind", "neben" to "next to", "zwischen" to "between",
            "wenn" to "when/if", "dass" to "that", "weil" to "because",
            "als" to "as/when", "ob" to "whether", "wie" to "how",
            "auch" to "also", "noch" to "still", "schon" to "already",
            "sehr" to "very", "nur" to "only", "hier" to "here",
            "da" to "there", "wo" to "where", "was" to "what",
            "wer" to "who", "ja" to "yes", "nein" to "no",
            "dann" to "then", "also" to "so", "denn" to "because",
            "doch" to "yet", "mal" to "once", "ganz" to "quite",
            "immer" to "always", "nie" to "never",
            "mein" to "my", "dein" to "your", "sein" to "his",
            "ihr" to "her/their", "unser" to "our", "euer" to "your",
            "dieser" to "this", "jeder" to "every", "alle" to "all",
            // Common verbs with garbage dict entries
            "haben" to "to have", "sein" to "to be", "werden" to "to become",
            "können" to "can", "müssen" to "must", "sollen" to "should",
            "wollen" to "to want", "dürfen" to "may", "mögen" to "to like",
            "machen" to "to do", "gehen" to "to go", "kommen" to "to come",
            "sehen" to "to see", "geben" to "to give", "nehmen" to "to take",
            "finden" to "to find", "sagen" to "to say", "wissen" to "to know",
            "denken" to "to think", "lassen" to "to let", "stehen" to "to stand",
            "bringen" to "to bring", "halten" to "to hold", "legen" to "to lay",
            "setzen" to "to set", "spielen" to "to play", "lesen" to "to read",
            "sprechen" to "to speak", "fahren" to "to drive", "schreiben" to "to write",
            "ziehen" to "to pull", "leben" to "to live", "arbeiten" to "to work",
            "öffnen" to "to open", "finden" to "to find", "denken" to "to think",
            "fühlen" to "to feel", "hören" to "to hear", "fallen" to "to fall",
            "bleiben" to "to stay", "steigen" to "to climb", "drehen" to "to turn",
            "tragen" to "to carry", "schauen" to "to look", "suchen" to "to search",
            "essen" to "to eat", "trinken" to "to drink", "schlafen" to "to sleep",
            "laufen" to "to run", "lieben" to "to love", "warten" to "to wait",
            "beginnen" to "to begin", "verstehen" to "to understand",
            "vergessen" to "to forget", "erinnern" to "to remember",
            "helfen" to "to help", "bitten" to "to ask", "antworten" to "to answer",
            "rufen" to "to call", "zeigen" to "to show", "schließen" to "to close",
            "werfen" to "to throw", "fangen" to "to catch",
            "sterben" to "to die", "kennen" to "to know",
            "glauben" to "to believe", "hoffen" to "to hope",
            "versuchen" to "to try", "brauchen" to "to need",
            "legen" to "to lay", "stellen" to "to place", "sitzen" to "to sit",
            "liegen" to "to lie", "hängen" to "to hang",
            "schlagen" to "to hit", "schneiden" to "to cut",
            // Conjugated forms
            "hat" to "has", "ist" to "is", "bin" to "am", "bist" to "are",
            "sind" to "are", "war" to "was", "waren" to "were", "hatte" to "had",
            "hatten" to "had", "wird" to "becomes", "wurde" to "became",
            "kann" to "can", "konnte" to "could", "muss" to "must",
            "musste" to "had to", "soll" to "should", "sollte" to "should",
            "will" to "wants", "wollte" to "wanted", "darf" to "may",
            "mag" to "likes", "macht" to "does", "geht" to "goes",
            "kommt" to "comes", "sieht" to "sees", "gibt" to "gives",
            "nimmt" to "takes", "findet" to "finds", "sagt" to "says",
            "weiß" to "knows", "denkt" to "thinks", "steht" to "stands",
            "hält" to "holds", "liest" to "reads", "spricht" to "speaks",
            "fährt" to "drives", "schreibt" to "writes", "zieht" to "pulls",
            // Common nouns/adjectives
            "Mann" to "man", "Frau" to "woman", "Kind" to "child",
            "Haus" to "house", "Tag" to "day", "Nacht" to "night",
            "Zeit" to "time", "Jahr" to "year", "Welt" to "world",
            "Hand" to "hand", "Kopf" to "head", "Auge" to "eye", "Augen" to "eyes",
            "Wasser" to "water", "Weg" to "way", "Stadt" to "city",
            "groß" to "big", "klein" to "small", "alt" to "old",
            "neu" to "new", "gut" to "good", "schlecht" to "bad",
            "schön" to "beautiful", "lang" to "long", "kurz" to "short",
            // Common nouns with bad/missing dict entries
            "Zimmer" to "room", "Stimme" to "voice", "Wort" to "word",
            "Leben" to "life", "Mensch" to "person", "Freundin" to "friend",
            "Liebe" to "love", "Herz" to "heart", "Seele" to "soul",
            "Traum" to "dream", "Gedanke" to "thought", "Idee" to "idea",
            "Bild" to "picture", "Licht" to "light", "Schatten" to "shadow",
            "Himmel" to "sky", "Sonne" to "sun", "Mond" to "moon",
            "Baum" to "tree", "Blume" to "flower", "Garten" to "garden",
            "Straße" to "street", "Brücke" to "bridge", "Tür" to "door",
            "Fenster" to "window", "Wand" to "wall", "Boden" to "floor",
            "Vater" to "father", "Mutter" to "mother", "Bruder" to "brother",
            "Schwester" to "sister", "Sohn" to "son", "Tochter" to "daughter",
            "Familie" to "family", "Kinder" to "children",
            "Arbeit" to "work", "Geld" to "money", "Buch" to "book",
            "Brief" to "letter", "Geschichte" to "story",
            "Freude" to "joy", "Angst" to "fear", "Hoffnung" to "hope",
            "Wasser" to "water", "Feuer" to "fire", "Erde" to "earth",
            "weiß" to "white", "schwarz" to "black", "rot" to "red",
            "blau" to "blue", "grün" to "green",
            "stark" to "strong", "schwach" to "weak",
            "schnell" to "fast", "langsam" to "slow",
            "richtig" to "correct", "falsch" to "wrong",
            "allein" to "alone", "zusammen" to "together",
            "letzt" to "last", "nächst" to "next",
        ),
        "es" to mapOf(
            "el" to "the", "la" to "the", "los" to "the", "las" to "the",
            "un" to "a", "una" to "a", "unos" to "some", "unas" to "some",
            "y" to "and", "o" to "or", "pero" to "but", "sino" to "but",
            "que" to "that", "quien" to "who", "cual" to "which",
            "yo" to "I", "tú" to "you", "él" to "he", "ella" to "she",
            "nosotros" to "we", "ellos" to "they", "ellas" to "they",
            "de" to "of", "en" to "in", "a" to "to", "con" to "with",
            "por" to "by", "para" to "for", "sin" to "without",
            "sobre" to "on", "entre" to "between", "hasta" to "until",
            "desde" to "since", "hacia" to "toward",
            "no" to "not", "sí" to "yes", "muy" to "very",
            "también" to "also", "ya" to "already", "más" to "more",
            "donde" to "where", "cuando" to "when", "como" to "like",
            "si" to "if", "porque" to "because", "aunque" to "although",
            "este" to "this", "ese" to "that", "aquel" to "that",
            "mi" to "my", "tu" to "your", "su" to "his/her",
            "todo" to "all", "otro" to "other", "cada" to "each",
            // Common verbs
            "ser" to "to be", "estar" to "to be", "tener" to "to have",
            "hacer" to "to do", "ir" to "to go", "venir" to "to come",
            "poder" to "can", "querer" to "to want", "deber" to "must",
            "saber" to "to know", "decir" to "to say", "ver" to "to see",
            "dar" to "to give", "poner" to "to put", "salir" to "to leave",
            // Conjugated
            "es" to "is", "está" to "is", "son" to "are", "era" to "was",
            "tiene" to "has", "hace" to "does", "va" to "goes",
            "puede" to "can", "quiere" to "wants", "debe" to "must",
            "sabe" to "knows", "dice" to "says", "ve" to "sees",
            "hay" to "there is",
            // Adjectives/nouns
            "bueno" to "good", "buena" to "good", "malo" to "bad", "mala" to "bad",
            "grande" to "big", "pequeño" to "small", "pequeña" to "small",
            "hombre" to "man", "mujer" to "woman", "niño" to "child",
            "casa" to "house", "tiempo" to "time", "vida" to "life",
            "mundo" to "world", "día" to "day", "noche" to "night",
            "ojos" to "eyes", "ojo" to "eye", "mano" to "hand",
            // More verbs with garbage dict entries
            "abrir" to "to open", "encontrar" to "to find", "pensar" to "to think",
            "sentir" to "to feel", "oír" to "to hear", "caer" to "to fall",
            "quedarse" to "to stay", "quedar" to "to stay", "subir" to "to go up",
            "bajar" to "to go down", "llevar" to "to carry", "mirar" to "to look",
            "buscar" to "to look for", "dejar" to "to leave", "hablar" to "to speak",
            "amar" to "to love", "comer" to "to eat", "beber" to "to drink",
            "correr" to "to run", "caminar" to "to walk", "escribir" to "to write",
            "leer" to "to read", "entender" to "to understand",
            "vivir" to "to live", "morir" to "to die", "nacer" to "to be born",
            "conocer" to "to know", "creer" to "to believe", "esperar" to "to wait",
            "llamar" to "to call", "mostrar" to "to show", "cerrar" to "to close",
            "comenzar" to "to begin", "terminar" to "to end",
            "tomar" to "to take", "pagar" to "to pay", "jugar" to "to play",
            "dormir" to "to sleep", "tocar" to "to touch", "casar" to "to marry",
            "ayudar" to "to help", "necesitar" to "to need", "parecer" to "to seem",
            "recordar" to "to remember", "olvidar" to "to forget",
            // Common conjugated forms
            "abrió" to "opened", "encontró" to "found", "pensó" to "thought",
            "sintió" to "felt", "cayó" to "fell", "quedó" to "stayed",
            "llevó" to "carried", "miró" to "looked", "buscó" to "looked for",
            "dejó" to "left", "habló" to "spoke", "comió" to "ate",
            "corrió" to "ran", "escribió" to "wrote", "leyó" to "read",
            "vivió" to "lived", "murió" to "died", "nació" to "was born",
            "llamó" to "called", "cerró" to "closed",
        ),
        "it" to mapOf(
            "il" to "the", "lo" to "the", "la" to "the", "i" to "the",
            "gli" to "the", "le" to "the", "un" to "a", "uno" to "a", "una" to "a",
            "e" to "and", "o" to "or", "ma" to "but", "che" to "that",
            "di" to "of", "a" to "to", "da" to "from", "in" to "in",
            "con" to "with", "su" to "on", "per" to "for", "tra" to "between",
            "io" to "I", "tu" to "you", "lui" to "he", "lei" to "she",
            "noi" to "we", "voi" to "you", "loro" to "they",
            "non" to "not", "sì" to "yes", "no" to "no",
            "questo" to "this", "quello" to "that",
            "come" to "like", "dove" to "where", "quando" to "when",
            "se" to "if", "perché" to "because", "anche" to "also",
            "molto" to "very", "più" to "more", "già" to "already",
            "mio" to "my", "tuo" to "your", "suo" to "his/her",
            "tutto" to "all", "altro" to "other", "ogni" to "every",
            // Common verbs
            "essere" to "to be", "avere" to "to have", "fare" to "to do",
            "andare" to "to go", "venire" to "to come", "dire" to "to say",
            "potere" to "can", "volere" to "to want", "dovere" to "must",
            "sapere" to "to know", "vedere" to "to see", "dare" to "to give",
            "stare" to "to stay", "prendere" to "to take",
            // Conjugated
            "ha" to "has", "sono" to "are", "era" to "was",
            "fa" to "does", "dice" to "says", "vede" to "sees",
            "può" to "can", "vuole" to "wants", "deve" to "must",
            "sa" to "knows", "va" to "goes", "viene" to "comes",
            // Common nouns/adj
            "buono" to "good", "buona" to "good", "cattivo" to "bad",
            "grande" to "big", "piccolo" to "small", "piccola" to "small",
            "uomo" to "man", "donna" to "woman", "bambino" to "child",
            "casa" to "house", "tempo" to "time", "vita" to "life",
            "mondo" to "world", "giorno" to "day", "notte" to "night",
            "occhi" to "eyes", "occhio" to "eye", "mano" to "hand",
            // More verbs with garbage dict entries
            "aprire" to "to open", "trovare" to "to find", "pensare" to "to think",
            "sentire" to "to feel", "cadere" to "to fall", "restare" to "to stay",
            "salire" to "to go up", "scendere" to "to go down",
            "portare" to "to carry", "guardare" to "to look at",
            "cercare" to "to look for", "lasciare" to "to let",
            "parlare" to "to speak", "amare" to "to love",
            "mangiare" to "to eat", "bere" to "to drink",
            "correre" to "to run", "camminare" to "to walk",
            "scrivere" to "to write", "leggere" to "to read",
            "capire" to "to understand", "vivere" to "to live",
            "morire" to "to die", "nascere" to "to be born",
            "conoscere" to "to know", "credere" to "to believe",
            "aspettare" to "to wait", "chiamare" to "to call",
            "mostrare" to "to show", "chiudere" to "to close",
            "cominciare" to "to begin", "finire" to "to finish",
            "dormire" to "to sleep", "toccare" to "to touch",
            "aiutare" to "to help", "sembrare" to "to seem",
            "ricordare" to "to remember", "dimenticare" to "to forget",
        ),
        "pt" to mapOf(
            "o" to "the", "a" to "the", "os" to "the", "as" to "the",
            "um" to "a", "uma" to "a", "uns" to "some", "umas" to "some",
            "e" to "and", "ou" to "or", "mas" to "but", "que" to "that",
            "de" to "of", "em" to "in", "para" to "for", "com" to "with",
            "por" to "by", "sem" to "without", "sobre" to "on",
            "eu" to "I", "tu" to "you", "ele" to "he", "ela" to "she",
            "nós" to "we", "eles" to "they", "elas" to "they",
            "não" to "not", "sim" to "yes", "muito" to "very",
            "também" to "also", "já" to "already", "mais" to "more",
            "onde" to "where", "quando" to "when", "como" to "like",
            "se" to "if", "porque" to "because",
            "meu" to "my", "seu" to "your/his", "nosso" to "our",
            "este" to "this", "esse" to "that", "todo" to "all",
            // Common verbs
            "ser" to "to be", "estar" to "to be", "ter" to "to have",
            "fazer" to "to do", "ir" to "to go", "vir" to "to come",
            "poder" to "can", "querer" to "to want", "dever" to "must",
            "saber" to "to know", "dizer" to "to say", "ver" to "to see",
            "dar" to "to give", "pôr" to "to put",
            // Conjugated
            "tem" to "has", "faz" to "does", "vai" to "goes",
            "pode" to "can", "quer" to "wants", "deve" to "must",
            "sabe" to "knows", "diz" to "says", "vê" to "sees",
            "há" to "there is",
            // Common nouns/adj
            "bom" to "good", "boa" to "good", "mau" to "bad",
            "grande" to "big", "pequeno" to "small", "pequena" to "small",
            "homem" to "man", "mulher" to "woman", "criança" to "child",
            "casa" to "house", "tempo" to "time", "vida" to "life",
            "mundo" to "world", "dia" to "day", "noite" to "night",
            "olhos" to "eyes", "olho" to "eye", "mão" to "hand",
            // More verbs with garbage dict entries
            "abrir" to "to open", "encontrar" to "to find", "pensar" to "to think",
            "sentir" to "to feel", "ouvir" to "to hear", "cair" to "to fall",
            "ficar" to "to stay", "subir" to "to climb", "descer" to "to go down",
            "levar" to "to take", "olhar" to "to look", "procurar" to "to look for",
            "deixar" to "to leave", "falar" to "to speak", "amar" to "to love",
            "comer" to "to eat", "beber" to "to drink", "correr" to "to run",
            "andar" to "to walk", "escrever" to "to write", "ler" to "to read",
            "entender" to "to understand", "viver" to "to live", "morrer" to "to die",
            "nascer" to "to be born", "conhecer" to "to know",
            "acreditar" to "to believe", "esperar" to "to wait",
            "chamar" to "to call", "mostrar" to "to show",
            "fechar" to "to close", "começar" to "to begin",
            "acabar" to "to finish", "dormir" to "to sleep",
            "ajudar" to "to help", "parecer" to "to seem",
            "lembrar" to "to remember", "esquecer" to "to forget",
            "quarto" to "room", "coração" to "heart", "alma" to "soul",
            "amigo" to "friend", "amiga" to "friend",
            "pai" to "father", "mãe" to "mother",
            "irmão" to "brother", "irmã" to "sister",
            "filho" to "son", "filha" to "daughter",
            "nome" to "name", "rua" to "street",
        ),
        "nl" to mapOf(
            "de" to "the", "het" to "the", "een" to "a",
            "en" to "and", "of" to "or", "maar" to "but", "dat" to "that",
            "van" to "of", "in" to "in", "op" to "on", "met" to "with",
            "voor" to "for", "aan" to "to", "uit" to "from", "door" to "through",
            "ik" to "I", "jij" to "you", "hij" to "he", "zij" to "she/they",
            "wij" to "we", "jullie" to "you",
            "niet" to "not", "ja" to "yes", "nee" to "no",
            "waar" to "where", "wanneer" to "when", "hoe" to "how",
            "als" to "if/when", "omdat" to "because", "ook" to "also",
            "zeer" to "very", "meer" to "more", "al" to "already",
            "mijn" to "my", "jouw" to "your", "zijn" to "his",
            "dit" to "this", "alle" to "all", "ander" to "other",
            // Common verbs
            "hebben" to "to have", "zijn" to "to be", "worden" to "to become",
            "kunnen" to "can", "moeten" to "must", "willen" to "to want",
            "zullen" to "will", "mogen" to "may", "gaan" to "to go",
            "komen" to "to come", "zien" to "to see", "doen" to "to do",
            "maken" to "to make", "geven" to "to give", "nemen" to "to take",
            // Conjugated
            "heeft" to "has", "gaat" to "goes", "komt" to "comes",
            "ziet" to "sees", "doet" to "does", "geeft" to "gives",
            "kan" to "can", "moet" to "must", "wil" to "wants",
            "mag" to "may", "zal" to "will",
            // Common nouns with bad dict entries
            "kamer" to "room", "stem" to "voice", "woord" to "word",
            "hart" to "heart", "ziel" to "soul", "vriend" to "friend",
            "vriendin" to "friend", "leven" to "life", "wereld" to "world",
            "moeder" to "mother", "vader" to "father", "broer" to "brother",
            "zus" to "sister", "kind" to "child", "kinderen" to "children",
            "werk" to "work", "geld" to "money", "huis" to "house",
            "man" to "man", "vrouw" to "woman", "oog" to "eye", "ogen" to "eyes",
            // More verbs with garbage dict entries
            "openen" to "to open", "vinden" to "to find", "denken" to "to think",
            "voelen" to "to feel", "horen" to "to hear", "vallen" to "to fall",
            "blijven" to "to stay", "dragen" to "to carry", "kijken" to "to look",
            "zoeken" to "to look for", "laten" to "to let", "spreken" to "to speak",
            "eten" to "to eat", "drinken" to "to drink", "rennen" to "to run",
            "lopen" to "to walk", "schrijven" to "to write", "lezen" to "to read",
            "begrijpen" to "to understand", "leven" to "to live", "sterven" to "to die",
            "kennen" to "to know", "geloven" to "to believe", "wachten" to "to wait",
            "bellen" to "to call", "tonen" to "to show", "sluiten" to "to close",
            "beginnen" to "to begin", "eindigen" to "to end",
            "slapen" to "to sleep", "helpen" to "to help",
            "lijken" to "to seem", "herinneren" to "to remember",
            "vergeten" to "to forget",
        ),
        "ru" to mapOf(
            // Russian function words
            "и" to "and", "в" to "in", "не" to "not", "на" to "on",
            "я" to "I", "он" to "he", "она" to "she", "они" to "they",
            "мы" to "we", "вы" to "you", "ты" to "you",
            "что" to "that", "как" to "how", "это" to "this",
            "с" to "with", "но" to "but", "а" to "and/but",
            "все" to "all", "за" to "for", "к" to "to",
            "по" to "along", "из" to "from", "о" to "about",
            "от" to "from", "до" to "to/until", "у" to "at",
            "да" to "yes", "нет" to "no", "еще" to "still",
            "уже" to "already", "так" to "so", "тоже" to "also",
            "только" to "only", "очень" to "very", "когда" to "when",
            "где" to "where", "если" to "if", "потому" to "because",
            "чтобы" to "in order to", "здесь" to "here", "там" to "there",
            // Common verbs
            "быть" to "to be", "есть" to "is", "был" to "was", "была" to "was",
            "было" to "was", "были" to "were", "будет" to "will be",
            "иметь" to "to have", "делать" to "to do", "идти" to "to go",
            "знать" to "to know", "хотеть" to "to want", "мочь" to "can",
            "говорить" to "to speak", "видеть" to "to see",
            "сказать" to "to say", "думать" to "to think",
            // More verbs with garbage dict entries
            "открыть" to "to open", "найти" to "to find",
            "чувствовать" to "to feel", "слышать" to "to hear",
            "упасть" to "to fall", "остаться" to "to stay",
            "нести" to "to carry", "смотреть" to "to look",
            "искать" to "to look for", "оставить" to "to leave",
            "любить" to "to love", "есть" to "to eat",
            "пить" to "to drink", "бежать" to "to run",
            "ходить" to "to walk", "писать" to "to write",
            "читать" to "to read", "понимать" to "to understand",
            "жить" to "to live", "умереть" to "to die",
            "родиться" to "to be born", "помочь" to "to help",
            "ждать" to "to wait", "показать" to "to show",
            "закрыть" to "to close", "начать" to "to begin",
            "кончить" to "to finish", "спать" to "to sleep",
            "верить" to "to believe", "помнить" to "to remember",
            "забыть" to "to forget", "звать" to "to call",
            // Common nouns
            "комната" to "room", "голос" to "voice", "слово" to "word",
            "сердце" to "heart", "душа" to "soul", "друг" to "friend",
            "жизнь" to "life", "мир" to "world", "мать" to "mother",
            "отец" to "father", "брат" to "brother", "сестра" to "sister",
            "работа" to "work", "деньги" to "money", "дом" to "house",
            "человек" to "person", "женщина" to "woman", "мужчина" to "man",
            "глаз" to "eye", "глаза" to "eyes", "рука" to "hand",
        ),
    )

    // ── Compound phrase overrides — multi-word expressions ─────────────────
    private val PHRASE_OVERRIDES: Map<String, Map<String, String>> = mapOf(
        "fr" to mapOf(
            "à peine" to "barely", "peut-être" to "maybe",
            "tout à fait" to "completely", "en train de" to "in the process of",
            "à cause de" to "because of", "grâce à" to "thanks to",
            "en face de" to "in front of", "au lieu de" to "instead of",
            "de temps en temps" to "from time to time",
            "tout de suite" to "right away", "tout à coup" to "suddenly",
            "à côté de" to "next to", "au-dessus de" to "above",
            "au-dessous de" to "below", "en dehors de" to "outside of",
            "il y a" to "there is", "il n'y a" to "there is no",
            "bien sûr" to "of course", "par exemple" to "for example",
            "c'est-à-dire" to "that is to say",
            "de nouveau" to "again", "en effet" to "indeed",
            "en même temps" to "at the same time",
            "de plus en plus" to "more and more",
            "de bonne heure" to "early",
            "n'avais pas" to "didn't have",
            "n'avait pas" to "didn't have",
            "n'est pas" to "is not",
            "n'a pas" to "doesn't have",
            "ne pas" to "not",
            "si vite" to "so fast",
            "pas encore" to "not yet",
            "pas du tout" to "not at all",
            "tous les" to "every",
            "toutes les" to "every",
            "de plus" to "moreover",
        ),
        "de" to mapOf(
            "auf einmal" to "suddenly", "zum Beispiel" to "for example",
            "in der Nähe" to "nearby", "auf jeden Fall" to "in any case",
            "zum ersten Mal" to "for the first time",
            "hin und her" to "back and forth",
            "ab und zu" to "now and then",
            "so dass" to "so that", "obwohl" to "although",
        ),
        "es" to mapOf(
            "sin embargo" to "however", "por ejemplo" to "for example",
            "a pesar de" to "despite", "en vez de" to "instead of",
            "de repente" to "suddenly", "a menudo" to "often",
            "tal vez" to "maybe", "por supuesto" to "of course",
            "en seguida" to "right away",
        ),
        "it" to mapOf(
            "per esempio" to "for example", "a causa di" to "because of",
            "invece di" to "instead of", "di solito" to "usually",
            "tutto sommato" to "all in all", "ad un tratto" to "suddenly",
            "per favore" to "please",
        ),
        "pt" to mapOf(
            "por exemplo" to "for example", "por causa de" to "because of",
            "em vez de" to "instead of", "de repente" to "suddenly",
            "com certeza" to "certainly", "ao mesmo tempo" to "at the same time",
        ),
    )

    // ── Articles (next word likely noun) ──────────────────────────────────────
    private val ARTICLES = mapOf(
        "fr" to setOf("le", "la", "les", "l'", "l\u2019", "un", "une", "des", "du", "d'", "d\u2019", "au", "aux"),
        "de" to setOf("der", "die", "das", "des", "dem", "den", "ein", "eine", "einer", "einem", "einen", "eines"),
        "es" to setOf("el", "la", "los", "las", "un", "una", "unos", "unas", "del"),
        "it" to setOf("il", "lo", "la", "i", "gli", "le", "l'", "l\u2019", "un", "uno", "una", "del", "della", "dei", "degli", "delle"),
        "pt" to setOf("o", "a", "os", "as", "um", "uma", "uns", "umas"),
        "nl" to setOf("de", "het", "een"),
        "ru" to emptySet(),
    )

    // ── Copula verbs (next word likely adjective) ────────────────────────────
    private val COPULAS = mapOf(
        "fr" to setOf("est", "sont", "était", "étaient", "sera", "seront", "semble", "paraît", "devient", "reste", "restent", "suis", "êtes", "sommes", "fût", "serait", "seraient"),
        "de" to setOf("ist", "sind", "war", "waren", "bleibt", "scheint", "wirkt", "wird", "bin", "bist", "seid", "wäre", "wären"),
        "es" to setOf("es", "son", "era", "eran", "parece", "está", "están", "soy", "eres", "somos", "estoy", "estás", "estamos"),
        "it" to setOf("è", "sono", "era", "erano", "sembra", "resta", "rimane", "sei", "siamo", "siete"),
        "pt" to setOf("é", "são", "era", "eram", "parece", "fica", "ficam", "sou", "estou", "está", "estão"),
        "nl" to setOf("is", "zijn", "was", "waren", "lijkt", "wordt", "ben", "bent"),
        "ru" to emptySet(),
    )

    // ── Prepositions (next word likely noun) ──────────────────────────────────
    private val PREPOSITIONS = mapOf(
        "fr" to setOf("de", "à", "en", "dans", "sur", "sous", "par", "pour", "avec", "sans", "entre", "vers", "chez", "avant", "après", "depuis", "pendant", "contre", "devant", "derrière"),
        "de" to setOf("von", "zu", "mit", "für", "auf", "in", "an", "um", "aus", "nach", "bei", "über", "unter", "vor", "hinter", "neben", "zwischen", "durch", "gegen", "ohne"),
        "es" to setOf("de", "a", "en", "con", "por", "para", "sin", "sobre", "entre", "hasta", "desde", "hacia", "contra"),
        "it" to setOf("di", "a", "da", "in", "con", "su", "per", "tra", "fra", "senza", "verso", "contro"),
        "pt" to setOf("de", "a", "em", "com", "por", "para", "sem", "sobre", "entre", "até", "desde", "contra"),
        "nl" to setOf("van", "in", "op", "met", "voor", "aan", "uit", "door", "naar", "over", "onder", "bij", "zonder", "tegen", "tussen"),
        "ru" to emptySet(),
    )

    // ── Pronouns / subjects (next word likely verb) ──────────────────────────
    private val PRONOUNS = mapOf(
        "fr" to setOf("je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "j'"),
        "de" to setOf("ich", "du", "er", "sie", "es", "wir", "ihr"),
        "es" to setOf("yo", "tú", "él", "ella", "nosotros", "ellos", "ellas", "usted", "ustedes"),
        "it" to setOf("io", "tu", "lui", "lei", "noi", "voi", "loro"),
        "pt" to setOf("eu", "tu", "ele", "ela", "nós", "eles", "elas", "você", "vocês"),
        "nl" to setOf("ik", "jij", "hij", "zij", "wij", "jullie", "u"),
        "ru" to emptySet(),
    )

    /**
     * Find all phrase override matches in a token list, returning (IntRange, translation) pairs.
     * Greedy: longer matches take priority.
     */
    private fun findPhraseSpans(
        tokens: List<Token>,
        phraseOverrides: Map<String, String>,
        tgt: String,
        enToTgt: Map<String, List<Sense>>?,
    ): List<Pair<IntRange, String>> {
        if (phraseOverrides.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<IntRange, String>>()
        val used = BooleanArray(tokens.size)

        // Try longest phrases first (up to 5 tokens)
        for (len in minOf(tokens.size, 5) downTo 2) {
            for (start in 0..(tokens.size - len)) {
                if (used.slice(start until start + len).any { it }) continue
                val phrase = (start until start + len).joinToString(" ") { tokens[it].word.lowercase() }
                phraseOverrides[phrase]?.let { enTrans ->
                    var translation = enTrans
                    if (tgt != "en" && enToTgt != null) {
                        val tgtSenses = lookupSenses(enTrans.substringBefore('/'), enToTgt)
                        translation = tgtSenses?.let { pickTranslation(it, null) } ?: enTrans
                    }
                    result.add(start until start + len to translation)
                    for (j in start until start + len) used[j] = true
                }
            }
        }
        return result
    }

    private fun dictKey(src: String, tgt: String) = "$src-$tgt"

    private suspend fun loadDictionary(src: String, tgt: String): Map<String, List<Sense>>? {
        val key = dictKey(src, tgt)
        mutex.withLock { dictionaries[key]?.let { return it } }

        val fileName = "dict-$src-$tgt.txt"
        val result = withContext(Dispatchers.IO) {
            try {
                val map = HashMap<String, List<Sense>>(80_000)
                context.assets.open(fileName).bufferedReader().useLines { lines ->
                    for (line in lines) {
                        val tab = line.indexOf('\t')
                        if (tab <= 0 || tab >= line.length - 1) continue
                        val word = line.substring(0, tab)
                        val sensesStr = line.substring(tab + 1).trim()
                        val senses = sensesStr.split("|").mapNotNull { part ->
                            val colon = part.indexOf(':')
                            if (colon > 0 && colon < part.length - 1) {
                                val trans = part.substring(colon + 1)
                                // Filter garbage: skip definitions > 4 words or starting uppercase
                                if (isGarbageTranslation(trans)) null
                                else Sense(part.substring(0, colon), trans)
                            } else if (colon < 0) {
                                if (isGarbageTranslation(part)) null
                                else Sense("?", part)
                            } else null
                        }
                        if (senses.isNotEmpty()) map[word] = senses
                    }
                }
                Log.d("TranslationService", "Loaded dict-$src-$tgt: ${map.size} entries")
                map as Map<String, List<Sense>>
            } catch (e: Exception) {
                Log.w("TranslationService", "No dictionary for $src→$tgt: ${e.message}")
                null
            }
        } ?: return null

        mutex.withLock { dictionaries[key] = result }
        return result
    }

    /** Filter garbage entries at load time */
    private fun isGarbageTranslation(t: String): Boolean {
        // Skip empty translations
        if (t.isBlank()) return true
        val words = t.split(' ')
        // Skip entries with >4 words — these are definitions, not translations
        if (words.size > 4) return true
        // Skip entries starting with uppercase that look like meta-descriptions
        if (words.size > 1 && t.first().isUpperCase()) return true
        // Skip entries that are entirely within parentheses
        if (t.startsWith('(') && t.endsWith(')')) return true
        // Skip entries containing parenthetical explanations > 2 words
        if (t.contains('(') && t.contains(')') && words.size > 2) return true
        // Skip transliteration-only entries like "(dva):"
        if (t.endsWith(':') && t.length > 1 && t.count { it == ':' } <= 1) return true
        // Skip "a surname" / "a name" type entries
        if (t == "a surname" || t == "a name" || t == "a given name" || t == "a place name") return true
        // Skip entries with "as an" or "as a" — these are definitions
        if (words.size > 2 && (t.contains("as an ") || t.contains("as a "))) return true
        // Skip entries that are grammar notes rather than translations
        if (t == "imperative" || t == "singular imperative" || t == "plural imperative") return true
        if (t.contains("past participle") || t.contains("present participle")) return true
        return false
    }

    /**
     * Translate all tokens in paragraphs from [bookLanguage] to [nativeLanguage].
     * Uses ±2 word window for context-aware POS disambiguation.
     */
    suspend fun translate(
        paragraphs: List<Paragraph>,
        bookLanguage: String,
        nativeLanguage: String,
    ): List<Paragraph> {
        val src = normalizeLanguage(bookLanguage)
        val tgt = normalizeLanguage(nativeLanguage)
        if (src == tgt) return paragraphs

        val srcToEn: Map<String, List<Sense>>? = if (src != "en") loadDictionary(src, "en") else null
        val enToTgt: Map<String, List<Sense>>? = if (tgt != "en") loadDictionary("en", tgt) else null

        if (srcToEn == null && src != "en") return paragraphs
        if (enToTgt == null && tgt != "en") return paragraphs

        val articles = ARTICLES[src] ?: emptySet()
        val copulas = COPULAS[src] ?: emptySet()
        val prepositions = PREPOSITIONS[src] ?: emptySet()
        val pronouns = PRONOUNS[src] ?: emptySet()
        val overrides = FUNCTION_OVERRIDES[src] ?: emptyMap()
        val isGerman = src == "de"

        val phraseOverrides = PHRASE_OVERRIDES[src] ?: emptyMap()

        return paragraphs.map { paragraph ->
            val tokens = paragraph.tokens
            // Pre-compute phrase override spans (bigrams, trigrams, etc.)
            val phraseSpans = findPhraseSpans(tokens, phraseOverrides, tgt, enToTgt)
            paragraph.copy(
                tokens = tokens.mapIndexed { i, token ->
                    // Check if this token is part of a phrase override
                    val phraseMatch = phraseSpans.firstOrNull { i in it.first }
                    if (phraseMatch != null) {
                        // First token of phrase gets translation, others get empty
                        // Phrase overrides are common expressions → high commonness
                        if (i == phraseMatch.first.first) {
                            token.copy(translation = phraseMatch.second, commonness = 0.9f)
                        } else {
                            token.copy(translation = "", commonness = 1.0f)
                        }
                    } else {
                        translateToken(
                            token, i, tokens,
                            srcToEn, enToTgt,
                            articles, copulas, prepositions, pronouns, overrides,
                            isGerman, src, tgt,
                        )
                    }
                }
            )
        }
    }

    private fun translateToken(
        token: Token, index: Int, tokens: List<Token>,
        srcToEn: Map<String, List<Sense>>?, enToTgt: Map<String, List<Sense>>?,
        articles: Set<String>, copulas: Set<String>,
        prepositions: Set<String>, pronouns: Set<String>,
        overrides: Map<String, String>,
        isGerman: Boolean, src: String, tgt: String,
    ): Token {
        val wordLower = token.word.lowercase()

        // 1. Check function word overrides first (highest quality)
        val override = overrides[wordLower]
        if (override != null) {
            var translation = override
            // Pivot to target language if needed
            if (tgt != "en" && enToTgt != null) {
                val enSenses = lookupSenses(override.substringBefore('/'), enToTgt)
                translation = enSenses?.let { pickTranslation(it, null) } ?: override
            }
            // Override words are very common — low priority for translation display
            val commonness = if (wordLower.length <= 3) 1.0f
                else if (wordLower in articles || wordLower in prepositions || wordLower in pronouns) 1.0f
                else 0.85f // common verb/noun overrides
            return token.copy(translation = translation, commonness = commonness)
        }

        // 2. Dictionary lookup
        val dict = when {
            src == "en" && enToTgt != null -> enToTgt
            srcToEn != null -> srcToEn
            else -> return token
        }
        val senses = lookupSenses(token.word, dict) ?: return token

        // 3. Context-aware POS disambiguation using ±2 window
        val prev1 = if (index > 0) tokens[index - 1].word.lowercase() else null
        val prev2 = if (index > 1) tokens[index - 2].word.lowercase() else null
        val next1 = if (index < tokens.size - 1) tokens[index + 1].word.lowercase() else null
        val next2 = if (index < tokens.size - 2) tokens[index + 2].word.lowercase() else null

        val nextIsNoun = next1 != null && srcToEn != null &&
            lookupSenses(next1, srcToEn)?.any { it.pos == "n" } == true
        val prevIsPrep = prev1 != null && prev1 in prepositions

        val preferredPos: String? = when {
            // Article + X → X is noun
            prev1 != null && prev1 in articles -> "n"
            // Preposition + X → X is noun
            prevIsPrep -> "n"
            // Article + adj + X (e.g., "le grand homme") → X is noun
            prev2 != null && prev2 in articles && prev1 != null -> "n"
            // Copula + X → X is adjective
            prev1 != null && prev1 in copulas -> "adj"
            // Pronoun + X → X is verb
            prev1 != null && prev1 in pronouns -> "v"
            // German: capitalized → noun
            isGerman && token.word.first().isUpperCase() -> "n"
            // X + noun → X is likely adjective
            nextIsNoun && senses.any { it.pos == "adj" } -> "adj"
            // If word has verb sense and is preceded by "ne"/"not" patterns → verb
            prev1 == "ne" || prev1 == "n'" || prev1 == "nicht" || prev1 == "no" -> "v"
            else -> null
        }

        var translation = pickTranslation(senses, preferredPos)

        // 4. Pivot through English if needed
        if (src != "en" && tgt != "en" && enToTgt != null && translation != null) {
            // Try to translate the English word to target language
            // Keep English if: target lookup fails, or gives a worse translation
            val enWord = translation
            val enSenses = lookupSenses(enWord, enToTgt)
            val targetTranslation = enSenses?.let { pickTranslation(it, null) }
            // Only use target translation if it's a real word (not a transliteration or worse)
            translation = if (targetTranslation != null && targetTranslation.length > 1) {
                targetTranslation
            } else {
                enWord // Keep the English translation as fallback
            }
        }

        if (translation == null) return token

        // Estimate commonness from word length (rough proxy for frequency)
        // Short words are almost always common; long words are rare
        val commonness = when {
            wordLower.length <= 2 -> 0.95f
            wordLower.length == 3 -> 0.85f
            wordLower.length == 4 -> 0.7f
            wordLower.length == 5 -> 0.55f
            wordLower.length == 6 -> 0.4f
            wordLower.length == 7 -> 0.3f
            wordLower.length == 8 -> 0.2f
            else -> 0.1f  // long words are rare
        }
        return token.copy(translation = translation, commonness = commonness)
    }

    /**
     * Translate a phrase (multiple tokens) with context.
     * Tries phrase lookup first, then falls back to compositional translation.
     */
    override suspend fun translatePhrase(
        tokens: List<Token>,
        allTokens: List<Token>,
        startIndex: Int,
        bookLanguage: String,
        nativeLanguage: String,
    ): String? {
        val src = normalizeLanguage(bookLanguage)
        val tgt = normalizeLanguage(nativeLanguage)
        if (src == tgt) return null

        val srcToEn: Map<String, List<Sense>>? = if (src != "en") loadDictionary(src, "en") else null
        val enToTgt: Map<String, List<Sense>>? = if (tgt != "en") loadDictionary("en", tgt) else null

        // 1. Check phrase overrides first (highest quality)
        val phraseOverrides = PHRASE_OVERRIDES[src] ?: emptyMap()
        val phraseKey = tokens.joinToString(" ") { it.word.lowercase() }
        phraseOverrides[phraseKey]?.let { override ->
            if (tgt == "en") return override
            // Pivot to target language
            if (enToTgt != null) {
                val enSenses = lookupSenses(override.substringBefore('/'), enToTgt)
                return enSenses?.let { pickTranslation(it, null) } ?: override
            }
            return override
        }
        // Also check all sub-phrases (sliding window) for partial matches
        for (len in minOf(tokens.size, 5) downTo 2) {
            for (start in 0..(tokens.size - len)) {
                val subPhrase = tokens.subList(start, start + len).joinToString(" ") { it.word.lowercase() }
                phraseOverrides[subPhrase]?.let { /* found sub-phrase match, will use in composition */ }
            }
        }

        // 2. Dictionary phrase lookup
        val dict = when {
            src == "en" && enToTgt != null -> enToTgt
            srcToEn != null -> srcToEn
            else -> return null
        }
        lookupSenses(phraseKey, dict)?.let { senses ->
            var translation = pickTranslation(senses, null)
            if (translation != null && src != "en" && tgt != "en" && enToTgt != null) {
                val enSenses = lookupSenses(translation, enToTgt)
                translation = enSenses?.let { pickTranslation(it, null) } ?: translation
            }
            if (translation != null) return translation
        }

        // Compositional: translate each word with context, join intelligently
        val articles = ARTICLES[src] ?: emptySet()
        val copulas = COPULAS[src] ?: emptySet()
        val prepositions = PREPOSITIONS[src] ?: emptySet()
        val pronouns = PRONOUNS[src] ?: emptySet()
        val overrides = FUNCTION_OVERRIDES[src] ?: emptyMap()
        val isGerman = src == "de"

        val translatedWords = tokens.mapIndexed { localIdx, token ->
            val globalIdx = startIndex + localIdx
            val translated = translateToken(
                token, globalIdx, allTokens,
                srcToEn, enToTgt,
                articles, copulas, prepositions, pronouns, overrides,
                isGerman, src, tgt,
            )
            translated.translation ?: token.word
        }

        return translatedWords.joinToString(" ")
    }

    /**
     * Look up senses for [word] with multiple fallbacks:
     * 1. Exact lowercase
     * 2. Accent-stripped
     * 3. Lemma-stripped suffixes
     */
    private fun lookupSenses(word: String, dict: Map<String, List<Sense>>): List<Sense>? {
        val lower = word.lowercase()
        dict[lower]?.let { return it }

        // Accent-stripped fallback
        val stripped = stripAccents(lower)
        if (stripped != lower) dict[stripped]?.let { return it }

        // Morphological fallbacks
        for (lemma in lemmatize(lower)) {
            dict[lemma]?.let { return it }
            val strippedLemma = stripAccents(lemma)
            if (strippedLemma != lemma) dict[strippedLemma]?.let { return it }
        }

        return null
    }

    private fun pickTranslation(senses: List<Sense>, preferredPos: String?): String? {
        // 1. Preferred POS with single-word translation
        if (preferredPos != null) {
            senses.firstOrNull { it.pos == preferredPos && ' ' !in it.translation }
                ?.translation?.let { return it }
        }
        // 2. Preferred POS, multi-word "to X" verbs — keep the whole thing
        if (preferredPos != null) {
            senses.firstOrNull { it.pos == preferredPos && isCleanMultiWord(it.translation) }
                ?.translation?.let { return it }
        }
        // 3. Any POS, single-word
        senses.firstOrNull { ' ' !in it.translation }?.translation?.let { return it }
        // 4. Any POS, clean multi-word (e.g., "to have", "in front")
        senses.firstOrNull { isCleanMultiWord(it.translation) }?.translation?.let { return it }
        // 5. Preferred POS, shortest multi-word (take first word)
        if (preferredPos != null) {
            senses.filter { it.pos == preferredPos }
                .minByOrNull { it.translation.length }?.translation
                ?.let { return firstWordOrNull(it) }
        }
        // 6. Any sense, first word of shortest
        return senses.minByOrNull { it.translation.length }?.translation?.let { firstWordOrNull(it) }
    }

    /** Returns true for short, clean multi-word translations like "to have", "in bed" */
    private fun isCleanMultiWord(t: String): Boolean {
        val words = t.split(' ')
        if (words.size > 3) return false
        if (words.any { it.isNotEmpty() && it.first().isUpperCase() }) return false
        // "to X" verb infinitives are good
        if (words.size == 2 && words[0] == "to") return true
        // 2-word preposition phrases are ok
        if (words.size == 2) return true
        return false
    }

    private fun firstWordOrNull(t: String): String? {
        if (' ' !in t) return t
        val words = t.split(' ')
        // Skip capitalised descriptions ("Substitutes for another")
        if (words.first().first().isUpperCase()) return null
        // Skip leading articles — use the next word instead
        val skipArticles = setOf("a", "an", "the")
        val meaningful = words.firstOrNull { it.lowercase() !in skipArticles && it.isNotEmpty() }
        return meaningful
    }

    private fun stripAccents(s: String): String {
        val normalized = Normalizer.normalize(s, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{M}"), "")
    }

    /**
     * Extended lemmatization for inflected forms across supported languages.
     */
    private fun lemmatize(word: String): List<String> {
        if (word.length < 4) return emptyList()
        val c = mutableListOf<String>()
        // French verb conjugation
        if (word.endsWith("aient")) c += listOf(word.dropLast(5) + "er", word.dropLast(5) + "ir", word.dropLast(5) + "re", word.dropLast(5) + "oir")
        if (word.endsWith("erait")) c += listOf(word.dropLast(5) + "er")
        if (word.endsWith("erais")) c += listOf(word.dropLast(5) + "er")
        if (word.endsWith("ions")) c += listOf(word.dropLast(4) + "er", word.dropLast(4) + "ir")
        if (word.endsWith("iez")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "ir")
        if (word.endsWith("ant")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "ir")
        if (word.endsWith("ait")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "ir", word.dropLast(3) + "re")
        if (word.endsWith("ais")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "re", word.dropLast(3) + "oir", word.dropLast(3) + "ire")
        if (word.endsWith("ent")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "re")
        if (word.endsWith("ons")) c += listOf(word.dropLast(3) + "er", word.dropLast(3) + "ir")
        // French past participles
        if (word.endsWith("é")) c += listOf(word.dropLast(1) + "er")
        if (word.endsWith("ée")) c += listOf(word.dropLast(2) + "er")
        if (word.endsWith("és")) c += listOf(word.dropLast(2) + "er")
        if (word.endsWith("ées")) c += listOf(word.dropLast(3) + "er")
        if (word.endsWith("i")) c += listOf(word.dropLast(1) + "ir")
        if (word.endsWith("is")) c += listOf(word.dropLast(2) + "ir", word.dropLast(2) + "re")
        if (word.endsWith("it")) c += listOf(word.dropLast(2) + "ir", word.dropLast(2) + "re")
        if (word.endsWith("u")) c += listOf(word.dropLast(1) + "oir", word.dropLast(1) + "re")
        // French adjective/noun morphology
        if (word.endsWith("aux")) c += listOf(word.dropLast(3) + "al")
        if (word.endsWith("eaux")) c += listOf(word.dropLast(4) + "eau")
        // German inflections
        if (word.endsWith("en")) c += listOf(word.dropLast(2), word.dropLast(2) + "e")
        if (word.endsWith("er")) c += listOf(word.dropLast(2), word.dropLast(2) + "e")
        if (word.endsWith("em")) c += listOf(word.dropLast(2), word.dropLast(2) + "e")
        if (word.endsWith("st")) c += listOf(word.dropLast(2), word.dropLast(2) + "en")
        if (word.endsWith("te")) c += listOf(word.dropLast(2) + "en")
        if (word.endsWith("ung")) c += listOf(word.dropLast(3) + "en") // Wanderung→wandern
        // Spanish/Portuguese conjugation
        if (word.endsWith("ando")) c += listOf(word.dropLast(4) + "ar")
        if (word.endsWith("endo")) c += listOf(word.dropLast(4) + "er")
        if (word.endsWith("ado")) c += listOf(word.dropLast(3) + "ar")
        if (word.endsWith("ido")) c += listOf(word.dropLast(3) + "ir", word.dropLast(3) + "er")
        if (word.endsWith("aba")) c += listOf(word.dropLast(3) + "ar")
        if (word.endsWith("aban")) c += listOf(word.dropLast(4) + "ar")
        // Italian conjugation
        if (word.endsWith("ando")) c += listOf(word.dropLast(4) + "are")
        if (word.endsWith("endo")) c += listOf(word.dropLast(4) + "ere")
        if (word.endsWith("ato")) c += listOf(word.dropLast(3) + "are")
        if (word.endsWith("uto")) c += listOf(word.dropLast(3) + "ere")
        if (word.endsWith("ito")) c += listOf(word.dropLast(3) + "ire")
        // Common plurals (all Romance languages)
        if (word.endsWith("es") && word.length > 4) c += listOf(word.dropLast(2), word.dropLast(2) + "e")
        if (word.endsWith("s") && word.length > 3) c += listOf(word.dropLast(1))
        return c.distinct()
    }

    private fun normalizeLanguage(lang: String) =
        lang.lowercase().split("-", "_").first()
}
