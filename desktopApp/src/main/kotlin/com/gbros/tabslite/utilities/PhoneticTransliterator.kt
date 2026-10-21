package com.gbros.tabslite.utilities

/**
 * High-performance, pure Kotlin phonetic transliterator for converting
 * Indic scripts (Devanagari, Bengali, Gurmukhi, Tamil) into readable Romanized
 * English text for hybrid sing-along chord charts (Hinglish, Benglish, Pinglish, Tanglish).
 */
object PhoneticTransliterator {

    // ==========================================
    // DEVANAGARI (HINDI / MARATHI / NEPALI)
    // ==========================================

    private val DEVANAGARI_VOWELS = mapOf(
        '\u0905' to "a", '\u0906' to "aa", '\u0907' to "i", '\u0908' to "ee",
        '\u0909' to "u", '\u090A' to "oo", '\u090B' to "ri", '\u090F' to "e",
        '\u0910' to "ai", '\u0913' to "o", '\u0914' to "au"
    )

    private val DEVANAGARI_MATRAS = mapOf(
        '\u093E' to "aa", '\u093F' to "i", '\u0940' to "ee", '\u0941' to "u",
        '\u0942' to "oo", '\u0943' to "ri", '\u0947' to "e", '\u0948' to "ai",
        '\u094B' to "o", '\u094C' to "au"
    )

    private val DEVANAGARI_CONSONANTS = mapOf(
        '\u0915' to "k", '\u0916' to "kh", '\u0917' to "g", '\u0918' to "gh", '\u0919' to "ng",
        '\u091A' to "ch", '\u091B' to "chh", '\u091C' to "j", '\u091D' to "jh", '\u091E' to "ny",
        '\u091F' to "t", '\u0920' to "th", '\u0921' to "d", '\u0922' to "dh", '\u0923' to "n",
        '\u0924' to "t", '\u0925' to "th", '\u0926' to "d", '\u0927' to "dh", '\u0928' to "n",
        '\u092A' to "p", '\u092B' to "ph", '\u092C' to "b", '\u092D' to "bh", '\u092E' to "m",
        '\u092F' to "y", '\u0930' to "r", '\u0932' to "l", '\u0935' to "v",
        '\u0936' to "sh", '\u0937' to "sh", '\u0938' to "s", '\u0939' to "h",
        '\u0958' to "q", '\u0959' to "kh", '\u095A' to "g", '\u095B' to "z",
        '\u095C' to "r", '\u095D' to "rh", '\u095E' to "f", '\u095F' to "y"
    )

    // ==========================================
    // BENGALI (BANGLA)
    // ==========================================

    private val BENGALI_VOWELS = mapOf(
        '\u0985' to "o", '\u0986' to "aa", '\u0987' to "i", '\u0988' to "ee",
        '\u0989' to "u", '\u098A' to "oo", '\u098B' to "ri", '\u098F' to "e",
        '\u0990' to "oi", '\u0993' to "o", '\u0994' to "ou"
    )

    private val BENGALI_MATRAS = mapOf(
        '\u09BE' to "aa", '\u09BF' to "i", '\u09C0' to "ee", '\u09C1' to "u",
        '\u09C2' to "oo", '\u09C3' to "ri", '\u09C7' to "e", '\u09C8' to "oi",
        '\u09CB' to "o", '\u09CC' to "ou"
    )

    private val BENGALI_CONSONANTS = mapOf(
        '\u0995' to "k", '\u0996' to "kh", '\u0997' to "g", '\u0998' to "gh", '\u0999' to "ng",
        '\u099A' to "ch", '\u099B' to "chh", '\u099C' to "j", '\u099D' to "jh", '\u099E' to "ny",
        '\u099F' to "t", '\u09A0' to "th", '\u09A1' to "d", '\u09A2' to "dh", '\u09A3' to "n",
        '\u09A4' to "t", '\u09A5' to "th", '\u09A6' to "d", '\u09A7' to "dh", '\u09A8' to "n",
        '\u09AA' to "p", '\u09AB' to "ph", '\u09AC' to "b", '\u09AD' to "bh", '\u09AE' to "m",
        '\u09AF' to "j", '\u09B0' to "r", '\u09B2' to "l",
        '\u09B6' to "sh", '\u09B7' to "sh", '\u09B8' to "s", '\u09B9' to "h",
        '\u09DC' to "r", '\u09DD' to "rh", '\u09DF' to "y", '\u09CE' to "t"
    )

    /**
     * Transliterates Devanagari Hindi text to Romanized Hinglish.
     */
    fun transliterateDevanagari(text: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            when {
                DEVANAGARI_VOWELS.containsKey(c) -> {
                    sb.append(DEVANAGARI_VOWELS[c])
                }
                DEVANAGARI_CONSONANTS.containsKey(c) -> {
                    val cons = DEVANAGARI_CONSONANTS[c]!!
                    val next = if (i + 1 < len) text[i + 1] else null

                    when {
                        next == '\u094D' -> {
                            // Halant (virama) - suppress inherent vowel
                            sb.append(cons)
                            i++ // skip virama
                        }
                        next != null && DEVANAGARI_MATRAS.containsKey(next) -> {
                            sb.append(cons)
                            sb.append(DEVANAGARI_MATRAS[next])
                            i++ // skip matra
                        }
                        else -> {
                            // Inherent vowel 'a' (omit at end of word or before space/punctuation for natural Hindi phonetic flow)
                            val isEndOfWord = (i + 1 >= len) || !text[i + 1].isLetter()
                            if (isEndOfWord) {
                                sb.append(cons)
                            } else {
                                sb.append(cons).append("a")
                            }
                        }
                    }
                }
                c == '\u0902' -> sb.append("n") // Anusvara
                c == '\u0901' -> sb.append("n") // Chandrabindu
                c == '\u0903' -> sb.append("h") // Visarga
                c == '\u0964' -> sb.append(".") // Danda
                c == '\u0965' -> sb.append(".") // Double danda
                c in '\u0966'..'\u096F' -> sb.append(c - '\u0966') // Devanagari digits 0-9
                else -> sb.append(c)
            }
            i++
        }

        return cleanRomanizedSpacing(sb.toString())
    }

    /**
     * Transliterates Bengali Bangla text to Romanized Benglish / Banglish.
     */
    fun transliterateBengali(text: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            when {
                BENGALI_VOWELS.containsKey(c) -> {
                    sb.append(BENGALI_VOWELS[c])
                }
                BENGALI_CONSONANTS.containsKey(c) -> {
                    val cons = BENGALI_CONSONANTS[c]!!
                    val next = if (i + 1 < len) text[i + 1] else null

                    when {
                        next == '\u09CD' -> {
                            // Hasanta (virama)
                            sb.append(cons)
                            i++
                        }
                        next != null && BENGALI_MATRAS.containsKey(next) -> {
                            sb.append(cons)
                            sb.append(BENGALI_MATRAS[next])
                            i++
                        }
                        else -> {
                            val isEndOfWord = (i + 1 >= len) || !text[i + 1].isLetter()
                            if (isEndOfWord) {
                                sb.append(cons)
                            } else {
                                sb.append(cons).append("o")
                            }
                        }
                    }
                }
                c == '\u0982' -> sb.append("ng") // Anusvara
                c == '\u0981' -> sb.append("n")  // Chandrabindu
                c == '\u0983' -> sb.append("h")  // Visarga
                c == '\u0964' -> sb.append(".")  // Danda
                c in '\u09E6'..'\u09EF' -> sb.append(c - '\u09E6') // Bengali digits 0-9
                else -> sb.append(c)
            }
            i++
        }

        return cleanRomanizedSpacing(sb.toString())
    }

    // ==========================================
    // GURMUKHI (PUNJABI)
    // ==========================================

    private val GURMUKHI_VOWELS = mapOf(
        '\u0A05' to "a", '\u0A06' to "aa", '\u0A07' to "i", '\u0A08' to "ee",
        '\u0A09' to "u", '\u0A0A' to "oo", '\u0A0F' to "e", '\u0A10' to "ai",
        '\u0A13' to "o", '\u0A14' to "au"
    )

    private val GURMUKHI_MATRAS = mapOf(
        '\u0A3E' to "aa", '\u0A3F' to "i", '\u0A40' to "ee", '\u0A41' to "u",
        '\u0A42' to "oo", '\u0A47' to "e", '\u0A48' to "ai", '\u0A4B' to "o",
        '\u0A4C' to "au"
    )

    private val GURMUKHI_CONSONANTS = mapOf(
        '\u0A15' to "k", '\u0A16' to "kh", '\u0A17' to "g", '\u0A18' to "gh", '\u0A19' to "ng",
        '\u0A1A' to "ch", '\u0A1B' to "chh", '\u0A1C' to "j", '\u0A1D' to "jh", '\u0A1E' to "ny",
        '\u0A1F' to "t", '\u0A20' to "th", '\u0A21' to "d", '\u0A22' to "dh", '\u0A23' to "n",
        '\u0A24' to "t", '\u0A25' to "th", '\u0A26' to "d", '\u0A27' to "dh", '\u0A28' to "n",
        '\u0A2A' to "p", '\u0A2B' to "ph", '\u0A2C' to "b", '\u0A2D' to "bh", '\u0A2E' to "m",
        '\u0A2F' to "y", '\u0A30' to "r", '\u0A32' to "l", '\u0A35' to "v", '\u0A38' to "s", '\u0A39' to "h",
        '\u0A59' to "kh", '\u0A5A' to "g", '\u0A5B' to "z", '\u0A5C' to "r", '\u0A5E' to "f"
    )

    /**
     * Transliterates Gurmukhi Punjabi text to Romanized Pinglish.
     */
    fun transliterateGurmukhi(text: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            when {
                GURMUKHI_VOWELS.containsKey(c) -> sb.append(GURMUKHI_VOWELS[c])
                GURMUKHI_CONSONANTS.containsKey(c) -> {
                    val cons = GURMUKHI_CONSONANTS[c]!!
                    val next = if (i + 1 < len) text[i + 1] else null

                    when {
                        next == '\u0A4D' -> {
                            sb.append(cons)
                            i++ // skip virama
                        }
                        next != null && GURMUKHI_MATRAS.containsKey(next) -> {
                            sb.append(cons)
                            sb.append(GURMUKHI_MATRAS[next])
                            i++ // skip matra
                        }
                        else -> {
                            val isEndOfWord = (i + 1 >= len) || !text[i + 1].isLetter()
                            if (isEndOfWord) sb.append(cons) else sb.append(cons).append("a")
                        }
                    }
                }
                c == '\u0A02' -> sb.append("n") // Bindi
                c == '\u0A70' -> sb.append("n") // Tippi
                c == '\u0A71' -> {
                    // Addak (doubles next consonant)
                    val next = if (i + 1 < len) text[i + 1] else null
                    if (next != null && GURMUKHI_CONSONANTS.containsKey(next)) {
                        sb.append(GURMUKHI_CONSONANTS[next]?.take(1) ?: "")
                    }
                }
                c == '\u0964' -> sb.append(".")
                c in '\u0A66'..'\u0A6F' -> sb.append(c - '\u0A66') // Gurmukhi digits 0-9
                else -> sb.append(c)
            }
            i++
        }
        return cleanRomanizedSpacing(sb.toString())
    }

    // ==========================================
    // TAMIL
    // ==========================================

    private val TAMIL_VOWELS = mapOf(
        '\u0B85' to "a", '\u0B86' to "aa", '\u0B87' to "i", '\u0B88' to "ee",
        '\u0B89' to "u", '\u0B8A' to "oo", '\u0B8E' to "e", '\u0B8F' to "ee",
        '\u0B90' to "ai", '\u0B92' to "o", '\u0B93' to "oo", '\u0B94' to "au"
    )

    private val TAMIL_MATRAS = mapOf(
        '\u0BBE' to "aa", '\u0BBF' to "i", '\u0BC0' to "ee", '\u0BC1' to "u",
        '\u0BC2' to "oo", '\u0BC6' to "e", '\u0BC7' to "ee", '\u0BC8' to "ai",
        '\u0BCA' to "o", '\u0BCB' to "oo", '\u0BCC' to "au"
    )

    private val TAMIL_CONSONANTS = mapOf(
        '\u0B95' to "k", '\u0B99' to "ng", '\u0B9A' to "ch", '\u0B9E' to "ny",
        '\u0B9F' to "t", '\u0BA3' to "n", '\u0BA4' to "th", '\u0BA8' to "n",
        '\u0BAA' to "p", '\u0BAE' to "m", '\u0BAF' to "y", '\u0BB0' to "r",
        '\u0BB2' to "l", '\u0BB5' to "v", '\u0BB4' to "zh", '\u0BB3' to "l",
        '\u0BB1' to "r", '\u0BA9' to "n", '\u0BB7' to "sh", '\u0BB8' to "s", '\u0BB9' to "h"
    )

    /**
     * Transliterates Tamil text to Romanized Tanglish.
     */
    fun transliterateTamil(text: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            when {
                TAMIL_VOWELS.containsKey(c) -> sb.append(TAMIL_VOWELS[c])
                TAMIL_CONSONANTS.containsKey(c) -> {
                    val cons = TAMIL_CONSONANTS[c]!!
                    val next = if (i + 1 < len) text[i + 1] else null

                    when {
                        next == '\u0BCD' -> {
                            sb.append(cons)
                            i++ // skip pulli
                        }
                        next != null && TAMIL_MATRAS.containsKey(next) -> {
                            sb.append(cons)
                            sb.append(TAMIL_MATRAS[next])
                            i++ // skip matra
                        }
                        else -> {
                            val isEndOfWord = (i + 1 >= len) || !text[i + 1].isLetter()
                            if (isEndOfWord) sb.append(cons) else sb.append(cons).append("a")
                        }
                    }
                }
                c == '\u0964' -> sb.append(".")
                c in '\u0BE6'..'\u0BEF' -> sb.append(c - '\u0BE6') // Tamil digits 0-9
                else -> sb.append(c)
            }
            i++
        }
        return cleanRomanizedSpacing(sb.toString())
    }

    /**
     * Checks if a string contains any non-Latin Indic or Arabic/Urdu scripts.
     */
    fun hasTransliterableScript(text: String): Boolean {
        return text.any {
            it in '\u0900'..'\u097F' || // Devanagari (Hindi, Marathi, Nepali)
            it in '\u0980'..'\u09FF' || // Bengali
            it in '\u0A00'..'\u0A7F' || // Gurmukhi (Punjabi)
            it in '\u0A80'..'\u0AFF' || // Gujarati
            it in '\u0B80'..'\u0BFF' || // Tamil
            it in '\u0C00'..'\u0C7F' || // Telugu
            it in '\u0600'..'\u06FF'    // Urdu / Arabic
        }
    }

    /**
     * Converts any non-Latin Indic/Urdu scripts in the input text to clean Romanized sing-along text.
     */
    fun processHybridText(text: String, hybridLangCode: String = ""): String {
        if (text.isBlank()) return text
        var result = text
        if (result.any { it in '\u0900'..'\u097F' }) {
            result = transliterateDevanagari(result)
        }
        if (result.any { it in '\u0980'..'\u09FF' }) {
            result = transliterateBengali(result)
        }
        if (result.any { it in '\u0A00'..'\u0A7F' }) {
            result = transliterateGurmukhi(result)
        }
        if (result.any { it in '\u0B80'..'\u0BFF' }) {
            result = transliterateTamil(result)
        }
        if (result.any { it in '\u0600'..'\u06FF' }) {
            result = transliterateDevanagari(urduToDevanagari(result))
        }
        return cleanRomanizedSpacing(result)
    }

    /**
     * Universal Indic Script Validator & Sanitizer.
     * Repairs broken vowel combinations, eliminates dotted circles (◌),
     * fixes consecutive matras, resolves orphan combining marks, and cures invalid nuktas.
     */
    fun sanitizeIndicScript(text: String): String {
        if (text.isEmpty()) return text
        var res = text

        // 1. Devanagari repairs (eliminate dotted circle combinations)
        res = res.replace("\u0906\u0940", "आई") // आ + ी -> आई
        res = res.replace("\u0906\u093F", "आइ") // आ + ि -> आइ
        res = res.replace("\u0906\u0947", "एक") // आ + े -> एक
        res = res.replace("\u0906\u0948", "ऐ")  // आ + ै -> ऐ
        res = res.replace("\u0906\u094B", "ओ")  // आ + ो -> ओ
        res = res.replace("\u0906\u094C", "औ")  // आ + ौ -> औ
        res = res.replace("\u0906\u093E", "आ")  // आ + ा -> आ
        res = res.replace("\u0905\u093E", "आ")  // अ + ा -> आ
        res = res.replace("\u0905\u0947", "ए")  // अ + े -> ए
        res = res.replace("\u0905\u0948", "ऐ")  // अ + ै -> ऐ
        res = res.replace("\u0905\u094B", "ओ")  // अ + ो -> ओ
        res = res.replace("\u0905\u094C", "औ")  // अ + ौ -> औ

        // Two consecutive matras in Devanagari
        res = res.replace("\u093E\u0940", "ाई")  // ा + ी -> ाई (e.g. रा◌ीस -> राईस)
        res = res.replace("\u093E\u093F", "ाइ")  // ा + ि -> ाइ
        res = res.replace("\u093E\u0947", "ाए")  // ा + े -> ाए
        res = res.replace("\u093E\u094B", "ाओ")  // ा + ो -> ाओ
        res = res.replace("\u0940\u093E", "िया") // ी + ा -> िया (e.g. ली◌ा -> लिया)
        res = res.replace("\u093F\u093E", "िया") // ि + ा -> िया
        res = res.replace("\u0940\u0947", "िए")  // ी + े -> िए (e.g. ली◌े -> लिए)
        res = res.replace("\u0947\u093E", "या")  // े + ा -> या

        // Invalid nukta combinations in Devanagari (like द़ causing dotted circle)
        res = res.replace("\u0926\u093C", "\u0926") // द + nukta -> द (e.g. बोद्र◌ा / बोद़ा -> बोदा)
        res = res.replace("\u0924\u093C", "\u0924") // त + nukta -> त
        res = res.replace("\u0928\u093C", "\u0928") // न + nukta -> न
        res = res.replace("\u092E\u093C", "\u092E") // म + nukta -> म
        res = res.replace("\u092F\u093C", "\u092F") // य + nukta -> य

        // 2. Bengali repairs
        res = res.replace("\u0985\u09BE", "আ")  // অ + া -> আ
        res = res.replace("\u0986\u09C0", "আই") // আ + ী -> আই
        res = res.replace("\u0986\u09BF", "আই") // আ + ি -> আই
        res = res.replace("\u0986\u09C7", "এ")  // আ + ে -> এ
        res = res.replace("\u0986\u09CB", "ও")  // আ + ো -> ও
        res = res.replace("\u0986\u09BE", "আ")  // আ + া -> আ
        res = res.replace("\u09BE\u09C0", "াই")  // া + ী -> াই
        res = res.replace("\u09BE\u09BF", "াই")  // া + ি -> াই
        res = res.replace("\u09BE\u09C7", "ায়ে") // া + ে -> ায়ে
        res = res.replace("\u09C0\u09BE", "িয়া") // ী + া -> িয়া
        res = res.replace("\u09BF\u09BE", "িয়া") // ি + া -> িয়া
        res = res.replace("\u09C7\u09BE", "েয়া") // ে + া -> েয়া
        res = res.replace("\u09A6\u09BC", "\u09A6") // দ + nukta (দ়) -> দ (e.g. বোদ়◌া -> বোদা)
        res = res.replace("\u09A4\u09BC", "\u09A4") // ত + nukta -> ত
        res = res.replace("\u09A8\u09BC", "\u09A8") // ন + nukta -> ন
        res = res.replace("\u09AE\u09BC", "\u09AE") // ম + nukta -> ম

        // 3. Gurmukhi repairs
        res = res.replace("\u0A05\u0A3E", "ਆ")  // ਅ + ਾ -> ਆ
        res = res.replace("\u0A06\u0A40", "ਆਈ") // ਆ + ੀ -> ਆਈ
        res = res.replace("\u0A06\u0A47", "ਏ")  // ਆ + ੇ -> ਏ
        res = res.replace("\u0A06\u0A4B", "ਓ")  // ਆ + ੋ -> ਓ
        res = res.replace("\u0A06\u0A3E", "ਆ")  // ਆ + ਾ -> ਆ
        res = res.replace("\u0A3E\u0A40", "ਾਈ")  // ਾ + ੀ -> ਾਈ
        res = res.replace("\u0A40\u0A3E", "ੀਆ") // ੀ + ਾ -> ੀਆ
        res = res.replace("\u0A26\u0A3C", "\u0A26") // ਦ + nukta (ਦ਼) -> ਦ (e.g. ਬੋਦ਼◌ਾ -> ਬੋਦਾ)
        res = res.replace("\u0A24\u0A3C", "\u0A24") // ਤ + nukta -> ਤ
        res = res.replace("\u0A28\u0A3C", "\u0A28") // ਨ + nukta -> ਨ
        res = res.replace("\u0A2E\u0A3C", "\u0A2E") // ਮ + nukta -> ਮ

        // 4. Orphan matras at start of words (e.g. after space/punctuation or line start)
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u093E"""), "$1आ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u093F\u0940]"""), "$1ई")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u0941\u0942]"""), "$1ऊ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0947"""), "$1ए")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0948"""), "$1ऐ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u094B"""), "$1ओ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u094C"""), "$1औ")

        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u09BE"""), "$1আ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u09BF\u09C0]"""), "$1ই")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u09C1\u09C2]"""), "$1উ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u09C7"""), "$1এ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u09C8"""), "$1ঐ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u09CB"""), "$1ও")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u09CC"""), "$1ঔ")

        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0A3E"""), "$1ਆ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u0A3F\u0A40]"""), "$1ਈ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])[\u0A41\u0A42]"""), "$1ਊ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0A47"""), "$1ਏ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0A48"""), "$1ਐ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0A4B"""), "$1ਓ")
        res = res.replace(Regex("""(^|[\s,।!?.–—"'\(\)\[\]])\u0A4C"""), "$1ਔ")

        return res
    }

    /**
     * Converts Devanagari script text into authentic Bengali script text (Bangla lipi).
     * Follows the direct ISCII / Unicode parallel alignment between Devanagari (0x0900-0x097F)
     * and Bengali (0x0980-0x09FF) with proper handling of language-specific letters.
     */
    fun devanagariToBengali(text: String): String {
        val sanitized = sanitizeIndicScript(text)
        val sb = StringBuilder(sanitized.length)
        var i = 0
        val len = sanitized.length
        while (i < len) {
            val c = sanitized[i]
            when {
                // Chandrabindu, Anusvara, Visarga
                c in '\u0901'..'\u0903' -> sb.append((c.code + 0x80).toChar())
                // Independent Vowels
                c in '\u0905'..'\u090C' -> sb.append((c.code + 0x80).toChar())
                c == '\u090F' -> sb.append('\u098F') // e
                c == '\u0910' -> sb.append('\u0990') // ai
                c == '\u0913' -> sb.append('\u0993') // o
                c == '\u0914' -> sb.append('\u0994') // au
                // Consonants ka through na
                c in '\u0915'..'\u0928' -> sb.append((c.code + 0x80).toChar())
                // Consonants pa through ma
                c in '\u092A'..'\u092E' -> sb.append((c.code + 0x80).toChar())
                // Consonant ya (य):
                // In Bengali, initial ya is য (\u09AF). But after a vowel or matra (e.g. তোমায়, আমায়, যায়),
                // it represents the semi-vowel antastha ya (য়, \u09DF).
                c == '\u092F' -> {
                    val prev = if (i > 0) sanitized[i - 1] else null
                    val isAfterVowel = prev != null && (prev in '\u093E'..'\u094C' || prev in '\u0905'..'\u0914')
                    if (isAfterVowel) {
                        sb.append('\u09DF') // য়
                    } else {
                        sb.append('\u09AF') // য
                    }
                }
                c == '\u0930' -> sb.append('\u09B0') // ra
                c == '\u0932' -> sb.append('\u09B2') // la
                c == '\u0935' -> sb.append('\u09AC') // va -> ba (Bengali uses ba for both)
                c == '\u0936' -> sb.append('\u09B6') // sha
                c == '\u0937' -> sb.append('\u09B7') // ssa
                c == '\u0938' -> sb.append('\u09B8') // sa
                c == '\u0939' -> sb.append('\u09B9') // ha
                // Matras (vowel signs)
                c in '\u093E'..'\u0944' -> sb.append((c.code + 0x80).toChar())
                c in '\u0947'..'\u0948' -> sb.append((c.code + 0x80).toChar())
                c in '\u094B'..'\u094C' -> sb.append((c.code + 0x80).toChar())
                // Virama / Halant
                c == '\u094D' -> sb.append('\u09CD')
                // Nukta consonants
                c == '\u0958' -> sb.append('\u0995') // qa -> ka
                c == '\u0959' -> sb.append('\u0996') // khha -> kha
                c == '\u095A' -> sb.append('\u0997') // ghha -> gha
                c == '\u095B' -> sb.append('\u099C') // za -> ja
                c == '\u095C' -> sb.append('\u09DC') // rra (ড়)
                c == '\u095D' -> sb.append('\u09DD') // rha (ঢ়)
                c == '\u095E' -> sb.append('\u09AB') // fa -> pha
                c == '\u095F' -> sb.append('\u09DF') // yya (য়)
                // Digits
                c in '\u0966'..'\u096F' -> sb.append((c.code + 0x80).toChar())
                else -> sb.append(c)
            }
            i++
        }
        return sanitizeIndicScript(sb.toString())
    }

    /**
     * Checks if a Devanagari text string is actually Bengali song lyrics
     * mistakenly transcribed in Devanagari (Hindi) script by Whisper.
     */
    fun isBengaliInDevanagari(text: String): Boolean {
        if (text.none { it in '\u0900'..'\u097F' }) return false
        val words = text.split(Regex("[\\s,।!?.–—\"'()\\[\\]{}]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val bengaliDevanagariMarkers = setOf(
            // Pronouns & Possessives
            "आमी", "आमि", "अमी", "अमि", "तुमी", "तुमि", "तोरा", "तुइ", "तोर", "तोमार", "तोमाय", "तोमाए", "तोमाइ", "तोमाके",
            "आमार", "अमार", "आमाय", "आमाए", "आमाइ", "आमाके", "आमरा", "अमरा", "आमादेर", "तोमादेर", "तादेर", "निजेर", "काके", "ताके",
            "एई", "एइ", "ओई", "ओइ", "जे", "से", "के", "जार", "तार", "जारा", "तारा",
            // Emotions & Love
            "भालोबाशी", "भालोबासी", "भालोबाशि", "भालोबासि", "भालोबाशा", "भालोबासा", "भालो", "भोलो", "भाबते", "भाबा",
            // Mind, Heart, Music & Nature
            "मोन", "मन", "मोने", "मने", "मोनर", "मोनेर", "प्राण", "प्राणेर", "हृदोय", "गान", "गाने", "गानेर", "गॉनेर", "सुर", "छन्द",
            "मेघ", "पाखी", "नोदी", "चोख", "चोखे", "चोखेर", "आकाश", "हावा", "रात", "राते", "रातेर", "दिन", "दिने",
            // Common Verbs & Auxiliaries
            "छिलो", "चिलो", "छिल", "चिल", "छिलाम", "चिलाम", "छिले", "चिली",
            "होबे", "हबे", "होयेछे", "होयेचे", "होयेछिलो", "होयेचिलो", "होये", "होते", "होबो",
            "आचे", "आछे", "आचेन", "आछेन", "आच", "आछ",
            "बोले", "बोलेछे", "बोलेचे", "बोलेछिलो", "बोलते", "बोलबो",
            "शुने", "शुनते", "शुनबो", "शोनो", "देखे", "देखते", "देखबो", "देखो",
            "कोरबो", "कोरब", "कोरची", "कोरछी", "कोरते", "कोरे",
            "जाबो", "जाते", "जावा", "गेछे", "गेछिलो", "खाबो", "थाकबो", "थाकते", "थाका",
            "दाओ", "नाओ", "पाओ", "पावा", "फेले", "फिरे", "पालिये",
            // Questions, Conjunctions & Particles
            "कोथा", "कोथाय", "कोथाइ", "केनो", "कैनो", "कतो", "एतो", "आर", "ताय",
            "किन्तु", "जौदि", "जोदि", "तोबे", "ताहले", "ताहोले", "एखोन", "तोखोन",
            "नेई", "नाई", "नेइ", "नाइ", "ना", "शोब", "शब", "शोबाई", "शबाई", "एक्ला", "चोलो", "चोल", "एबार", "बोंधु", "बंधु",
            "एक्टा", "एक्टु", "गुलो", "गुलि", "खूब", "पाग्लू", "बोझेना", "बोझे", "शेडिन", "सेदिन", "दुग्गा", "कोलकाता", "ढाका"
        )

        var count = 0
        for (w in words) {
            if (w in bengaliDevanagariMarkers) {
                count++
            } else if (w.length >= 3) {
                // Morphological suffixes uniquely characteristic of Bengali verbs & inflections in Devanagari:
                val isBengaliSuffix = w.endsWith("बो") || // 1st person future: korbo, jabo, khabo, bolbo, thakbo
                        w.endsWith("ते") ||               // Infinitive/locative: thakte, bolte, dekhte, shunte
                        w.endsWith("ेछे") || w.endsWith("ेचे") ||       // Perfect 3rd person: hoyechhe, bolechhe
                        w.endsWith("ेछिलो") || w.endsWith("ेचिलो") ||   // Past perfect: hoyechhilo, bolechhilo
                        w.endsWith("छिलाम") || w.endsWith("चिलाम") ||   // Past 1st person: bolchhiliam, chilam
                        w.endsWith("गुलो") || w.endsWith("गुलि") ||     // Plural noun marker: gulo, guli
                        w.endsWith("देर") || w.endsWith("देरके") ||     // Plural genitive/accusative: tader, amader
                        w.endsWith("टा") || w.endsWith("टि") ||         // Definite article: ekta, sheta
                        w.endsWith("ेर")                                // Genitive: moner, gaaner, chokher
                if (isBengaliSuffix) {
                    count++
                }
            }
        }

        return (count >= 2 || (count >= 1 && words.size <= 12))
    }

    /**
     * Converts Bengali script text into Devanagari (Hindi) script.
     * Follows the direct ISCII / Unicode parallel alignment between Bengali (0x0980-0x09FF)
     * and Devanagari (0x0900-0x097F).
     */
    fun bengaliToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            when {
                // Chandrabindu, Anusvara, Visarga
                c in '\u0981'..'\u0983' -> sb.append((c.code - 0x80).toChar())
                // Independent Vowels
                c in '\u0985'..'\u098C' -> sb.append((c.code - 0x80).toChar())
                c == '\u098F' -> sb.append('\u090F') // e
                c == '\u0990' -> sb.append('\u0910') // ai
                c == '\u0993' -> sb.append('\u0913') // o
                c == '\u0994' -> sb.append('\u0914') // au
                // Consonants ka through na
                c in '\u0995'..'\u09A8' -> sb.append((c.code - 0x80).toChar())
                // Consonants pa through ra
                c in '\u09AA'..'\u09B0' -> sb.append((c.code - 0x80).toChar())
                c == '\u09B2' -> sb.append('\u0932') // la
                c == '\u09B6' -> sb.append('\u0936') // sha
                c == '\u09B7' -> sb.append('\u0937') // ssa
                c == '\u09B8' -> sb.append('\u0938') // sa
                c == '\u09B9' -> sb.append('\u0939') // ha
                // Matras (vowel signs)
                c in '\u09BE'..'\u09C4' -> sb.append((c.code - 0x80).toChar())
                c in '\u09C7'..'\u09C8' -> sb.append((c.code - 0x80).toChar())
                c in '\u09CB'..'\u09CC' -> sb.append((c.code - 0x80).toChar())
                // Virama / Hasanta
                c == '\u09CD' -> sb.append('\u094D')
                // Nukta consonants
                c == '\u09DC' -> sb.append('\u095C') // ড় -> ड़
                c == '\u09DD' -> sb.append('\u095D') // ঢ় -> ढ़
                c == '\u09DF' -> sb.append('\u092F') // য় -> य
                c == '\u09CE' -> sb.append("त्")    // ৎ (khandata) -> त्
                // Digits
                c in '\u09E6'..'\u09EF' -> sb.append((c.code - 0x80).toChar())
                else -> sb.append(c)
            }
            i++
        }
        return sanitizeIndicScript(sb.toString())
    }

    /**
     * Converts Devanagari script to Gurmukhi (Punjabi) script.
     */
    fun devanagariToGurmukhi(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when {
                c in '\u0905'..'\u090A' -> sb.append((c.code + 0x100).toChar())
                c == '\u090F' -> sb.append('\u0A0F')
                c == '\u0910' -> sb.append('\u0A10')
                c == '\u0913' -> sb.append('\u0A13')
                c == '\u0914' -> sb.append('\u0A14')
                c in '\u0915'..'\u0928' -> sb.append((c.code + 0x100).toChar())
                c in '\u092A'..'\u0930' -> sb.append((c.code + 0x100).toChar())
                c == '\u0932' -> sb.append('\u0A32')
                c == '\u0935' -> sb.append('\u0A35')
                c == '\u0936' -> sb.append('\u0A36')
                c == '\u0937' || c == '\u0938' -> sb.append('\u0A38')
                c == '\u0939' -> sb.append('\u0A39')
                c in '\u093E'..'\u0942' -> sb.append((c.code + 0x100).toChar())
                c == '\u0947' -> sb.append('\u0A47')
                c == '\u0948' -> sb.append('\u0A48')
                c == '\u094B' -> sb.append('\u0A4B')
                c == '\u094C' -> sb.append('\u0A4C')
                c == '\u094D' -> sb.append('\u0A4D')
                c == '\u0902' -> sb.append('\u0A02')
                c in '\u0966'..'\u096F' -> sb.append((c.code + 0x100).toChar())
                c == '\u0959' -> sb.append('\u0A59') // khha ਖ਼
                c == '\u095A' -> sb.append('\u0A5A') // ghha ਗ਼
                c == '\u095B' -> sb.append('\u0A5B') // za ਜ਼
                c == '\u095C' -> sb.append('\u0A5C') // rra ੜ
                c == '\u095E' -> sb.append('\u0A5E') // fa ਫ਼
                c == '\u093C' -> sb.append('\u0A3C') // nukta
                else -> sb.append(c)
            }
        }
        return sanitizeIndicScript(sb.toString())
    }

    /**
     * Converts Gurmukhi (Punjabi) script to Devanagari script.
     */
    fun gurmukhiToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when {
                c in '\u0A05'..'\u0A0A' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A0F'..'\u0A10' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A13'..'\u0A14' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A15'..'\u0A28' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A2A'..'\u0A30' -> sb.append((c.code - 0x100).toChar())
                c == '\u0A32' -> sb.append('\u0932')
                c == '\u0A35' -> sb.append('\u0935')
                c == '\u0A36' -> sb.append('\u0936')
                c == '\u0A38' -> sb.append('\u0938')
                c == '\u0A39' -> sb.append('\u0939')
                c in '\u0A3E'..'\u0A42' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A47'..'\u0A48' -> sb.append((c.code - 0x100).toChar())
                c in '\u0A4B'..'\u0A4C' -> sb.append((c.code - 0x100).toChar())
                c == '\u0A4D' -> sb.append('\u094D')
                c == '\u0A02' || c == '\u0A70' -> sb.append('\u0902')
                c in '\u0A66'..'\u0A6F' -> sb.append((c.code - 0x100).toChar())
                c == '\u0A59' -> sb.append('\u0959')
                c == '\u0A5A' -> sb.append('\u095A')
                c == '\u0A5B' -> sb.append('\u095B')
                c == '\u0A5C' -> sb.append('\u095C')
                c == '\u0A5E' -> sb.append('\u095E')
                c == '\u0A3C' -> sb.append('\u093C')
                else -> sb.append(c)
            }
        }
        return sanitizeIndicScript(sb.toString())
    }

    /**
     * Converts Devanagari script to Gujarati script.
     */
    fun devanagariToGujarati(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                in '\u0901'..'\u0903', in '\u0905'..'\u090B', in '\u090F'..'\u0914',
                in '\u0915'..'\u0928', in '\u092A'..'\u0930', in '\u0932'..'\u0933',
                in '\u0935'..'\u0939', in '\u093E'..'\u0944', in '\u0947'..'\u094C',
                '\u094D', in '\u0966'..'\u096F' -> sb.append((c.code + 0x180).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Gujarati script to Devanagari script.
     */
    fun gujaratiToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                in '\u0A81'..'\u0A83', in '\u0A85'..'\u0A8B', in '\u0A8F'..'\u0A94',
                in '\u0A95'..'\u0AA8', in '\u0AAA'..'\u0AB0', in '\u0AB2'..'\u0AB3',
                in '\u0AB5'..'\u0AB9', in '\u0ABE'..'\u0AC4', in '\u0AC7'..'\u0ACC',
                '\u0ACD', in '\u0AE6'..'\u0AEF' -> sb.append((c.code - 0x180).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Devanagari script to Telugu script.
     */
    fun devanagariToTelugu(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                in '\u0901'..'\u0903', in '\u0905'..'\u090C', in '\u090F'..'\u0914',
                in '\u0915'..'\u0928', in '\u092A'..'\u0930', in '\u0932'..'\u0939',
                in '\u093E'..'\u0944', in '\u0947'..'\u094C', '\u094D', in '\u0966'..'\u096F' -> sb.append((c.code + 0x300).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Telugu script to Devanagari script.
     */
    fun teluguToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                in '\u0C01'..'\u0C03', in '\u0C05'..'\u0C0C', in '\u0C0F'..'\u0C14',
                in '\u0C15'..'\u0C28', in '\u0C2A'..'\u0C30', in '\u0C32'..'\u0C39',
                in '\u0C3E'..'\u0C44', in '\u0C47'..'\u0C4C', '\u0C4D', in '\u0C66'..'\u0C6F' -> sb.append((c.code - 0x300).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Devanagari script to Tamil script.
     */
    fun devanagariToTamil(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '\u0902' -> sb.append('\u0B82')
                in '\u0905'..'\u090A' -> sb.append((c.code + 0x280).toChar())
                '\u090F', '\u0910' -> sb.append((c.code + 0x280).toChar())
                '\u0913', '\u0914' -> sb.append((c.code + 0x280).toChar())
                in '\u0915'..'\u0918' -> sb.append('\u0B95') // k, kh, g, gh -> க
                '\u0919' -> sb.append('\u0B99') // ng -> ங
                in '\u091A'..'\u091D' -> sb.append('\u0B9A') // ch, chh, j, jh -> ச
                '\u091E' -> sb.append('\u0B9E') // ny -> ஞ
                in '\u091F'..'\u0922' -> sb.append('\u0B9F') // t, th, d, dh -> ட
                '\u0923' -> sb.append('\u0BA3') // n -> ண
                in '\u0924'..'\u0927' -> sb.append('\u0BA4') // t, th, d, dh -> த
                '\u0928' -> sb.append('\u0BA8') // n -> ந
                in '\u092A'..'\u092D' -> sb.append('\u0BAA') // p, ph, b, bh -> ப
                '\u092E' -> sb.append('\u0BAE') // m -> ம
                '\u092F' -> sb.append('\u0BAF') // y -> ய
                '\u0930' -> sb.append('\u0BB0') // r -> ர
                '\u0932' -> sb.append('\u0BB2') // l -> ல
                '\u0935' -> sb.append('\u0BB5') // v -> வ
                '\u0936' -> sb.append('\u0BB6') // sh -> ஶ
                '\u0937' -> sb.append('\u0BB7') // ss -> ஷ
                '\u0938' -> sb.append('\u0BB8') // s -> ஸ
                '\u0939' -> sb.append('\u0BB9') // h -> ஹ
                in '\u093E'..'\u0940' -> sb.append((c.code + 0x280).toChar())
                in '\u0941'..'\u0942' -> sb.append((c.code + 0x280).toChar())
                '\u0947', '\u0948' -> sb.append((c.code + 0x280).toChar())
                '\u094B', '\u094C' -> sb.append((c.code + 0x280).toChar())
                '\u094D' -> sb.append('\u0BCD')
                in '\u0966'..'\u096F' -> sb.append((c.code + 0x280).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Tamil script to Devanagari script.
     */
    fun tamilToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '\u0B82' -> sb.append('\u0902')
                in '\u0B85'..'\u0B8A' -> sb.append((c.code - 0x280).toChar())
                '\u0B8E', '\u0B8F' -> sb.append('\u090F')
                '\u0B90' -> sb.append('\u0910')
                '\u0B92', '\u0B93' -> sb.append('\u0913')
                '\u0B94' -> sb.append('\u0914')
                '\u0B95' -> sb.append('\u0915')
                '\u0B99' -> sb.append('\u0919')
                '\u0B9A' -> sb.append('\u091A')
                '\u0B9E' -> sb.append('\u091E')
                '\u0B9F' -> sb.append('\u091F')
                '\u0BA3' -> sb.append('\u0923')
                '\u0BA4' -> sb.append('\u0924')
                '\u0BA8', '\u0BA9' -> sb.append('\u0928')
                '\u0BAA' -> sb.append('\u092A')
                '\u0BAE' -> sb.append('\u092E')
                '\u0BAF' -> sb.append('\u092F')
                '\u0BB0', '\u0BB1' -> sb.append('\u0930')
                '\u0BB2', '\u0BB3', '\u0BB4' -> sb.append('\u0932')
                '\u0BB5' -> sb.append('\u0935')
                '\u0BB6' -> sb.append('\u0936')
                '\u0BB7' -> sb.append('\u0937')
                '\u0BB8' -> sb.append('\u0938')
                '\u0BB9' -> sb.append('\u0939')
                in '\u0BBE'..'\u0BC0' -> sb.append((c.code - 0x280).toChar())
                in '\u0BC1'..'\u0BC2' -> sb.append((c.code - 0x280).toChar())
                '\u0BC6', '\u0BC7' -> sb.append('\u0947')
                '\u0BC8' -> sb.append('\u0948')
                '\u0BCA', '\u0BCB' -> sb.append('\u094B')
                '\u0BCC' -> sb.append('\u094C')
                '\u0BCD' -> sb.append('\u094D')
                in '\u0BE6'..'\u0BEF' -> sb.append((c.code - 0x280).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Converts Devanagari script to Urdu (Perso-Arabic) script.
     */
    fun devanagariToUrdu(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            when (c) {
                // Consonants
                '\u0915' -> sb.append('\u06A9') // k -> ک
                '\u0916' -> sb.append("\u06A9\u06BE") // kh -> کھ
                '\u0917' -> sb.append('\u06AF') // g -> گ
                '\u0918' -> sb.append("\u06AF\u06BE") // gh -> گھ
                '\u0919' -> sb.append('\u0646') // ng -> ن
                '\u091A' -> sb.append('\u0686') // ch -> چ
                '\u091B' -> sb.append("\u0686\u06BE") // chh -> چھ
                '\u091C' -> sb.append('\u062C') // j -> ج
                '\u091D' -> sb.append("\u062C\u06BE") // jh -> جھ
                '\u091E' -> sb.append('\u0646') // ny -> ن
                '\u091F' -> sb.append('\u0679') // t -> ٹ
                '\u0920' -> sb.append("\u0679\u06BE") // th -> ٹھ
                '\u0921' -> sb.append('\u0688') // d -> ڈ
                '\u0922' -> sb.append("\u0688\u06BE") // dh -> ڈھ
                '\u0923' -> sb.append('\u0646') // n -> ن
                '\u0924' -> sb.append('\u062A') // t -> ت
                '\u0925' -> sb.append("\u062A\u06BE") // th -> تھ
                '\u0926' -> sb.append('\u062F') // d -> د
                '\u0927' -> sb.append("\u062F\u06BE") // dh -> دھ
                '\u0928' -> sb.append('\u0646') // n -> ن
                '\u092A' -> sb.append('\u067E') // p -> پ
                '\u092B' -> sb.append("\u067E\u06BE") // ph -> پھ
                '\u092C' -> sb.append('\u0628') // b -> ب
                '\u092D' -> sb.append("\u0628\u06BE") // bh -> بھ
                '\u092E' -> sb.append('\u0645') // m -> م
                '\u092F' -> sb.append('\u06CC') // y -> ی
                '\u0930' -> sb.append('\u0631') // r -> ر
                '\u0932' -> sb.append('\u0644') // l -> ل
                '\u0935' -> sb.append('\u0648') // v -> و
                '\u0936' -> sb.append('\u0634') // sh -> ش
                '\u0937' -> sb.append('\u0634') // ss -> ش
                '\u0938' -> sb.append('\u0633') // s -> س
                '\u0939' -> sb.append('\u06C1') // h -> ہ
                '\u0958' -> sb.append('\u0642') // q -> ق
                '\u0959' -> sb.append('\u062E') // kh -> خ
                '\u095A' -> sb.append('\u063A') // gh -> غ
                '\u095B' -> sb.append('\u0632') // z -> ز
                '\u095C' -> sb.append('\u0691') // rr -> ڑ
                '\u095D' -> sb.append("\u0691\u06BE") // rrh -> ڑھ
                '\u095E' -> sb.append('\u0641') // f -> ف
                '\u095F' -> sb.append('\u06CC') // y -> ی
                // Independent Vowels
                '\u0905' -> sb.append('\u0627') // a -> ا
                '\u0906' -> sb.append('\u0622') // aa -> آ
                '\u0907', '\u0908' -> sb.append("\u0627\u06CC") // i, ee -> ای
                '\u0909', '\u090A' -> sb.append("\u0627\u0648") // u, oo -> او
                '\u090F', '\u0910' -> sb.append("\u0627\u06D2") // e, ai -> اے
                '\u0913', '\u0914' -> sb.append("\u0627\u0648") // o, au -> او
                // Matras
                '\u093E' -> sb.append('\u0627') // aa matra -> ا
                '\u093F', '\u0940' -> sb.append('\u06CC') // i, ee matra -> ی
                '\u0941', '\u0942' -> sb.append('\u0648') // u, oo matra -> و
                '\u0947', '\u0948' -> sb.append('\u06D2') // e, ai matra -> ے
                '\u094B', '\u094C' -> sb.append('\u0648') // o, au matra -> و
                // Anusvara
                '\u0901', '\u0902' -> sb.append('\u06BA') // ں
                // Virama / Halant
                '\u094D' -> { /* omit */ }
                // Punctuation
                '\u0964' -> sb.append('\u06D4') // Urdu full stop ۔
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Converts Urdu (Perso-Arabic) script to Devanagari script.
     */
    fun urduToDevanagari(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        val len = text.length
        while (i < len) {
            // Check 2-char combinations
            if (i + 1 < len) {
                // Aspirated consonants (with do-chashmi he \u06BE)
                if (text[i + 1] == '\u06BE') {
                    val matched = when (text[i]) {
                        '\u0628' -> "भ" // bh
                        '\u067E' -> "फ" // ph
                        '\u062A' -> "थ" // th
                        '\u0679' -> "ठ" // tth
                        '\u062C' -> "झ" // jh
                        '\u0686' -> "छ" // chh
                        '\u062F' -> "ध" // dh
                        '\u0688' -> "ढ" // ddh
                        '\u06A9', '\u0643' -> "ख" // kh
                        '\u06AF' -> "घ" // gh
                        '\u0691' -> "ढ़" // rrh
                        else -> null
                    }
                    if (matched != null) {
                        sb.append(matched)
                        i += 2
                        continue
                    }
                }

                // Alif Madda diphthongs
                if (text[i] == '\u0622') {
                    if (text[i + 1] == '\u06D2') {
                        sb.append("आए")
                        i += 2; continue
                    } else if (text[i + 1] == '\u06CC' || text[i + 1] == '\u064A') {
                        sb.append("आई")
                        i += 2; continue
                    }
                }

                // Initial Alif diphthongs (e.g. اس -> इस, ایک -> एक, اور -> और/ओ)
                if (text[i] == '\u0627') {
                    val isStart = (i == 0 || text[i - 1].isWhitespace())
                    if (isStart) {
                        if (text[i + 1] == '\u06CC' || text[i + 1] == '\u064A') {
                            sb.append("ई")
                            i += 2; continue
                        } else if (text[i + 1] == '\u06D2') {
                            sb.append("एक")
                            i += 2; continue
                        } else if (text[i + 1] == '\u0648') {
                            sb.append("ओ")
                            i += 2; continue
                        }
                    } else {
                        // After consonant: Alif + Yeh / Bari Yeh / Waw
                        if (text[i + 1] == '\u06CC' || text[i + 1] == '\u064A') {
                            sb.append("ाई")
                            i += 2; continue
                        } else if (text[i + 1] == '\u06D2') {
                            sb.append("ाए")
                            i += 2; continue
                        } else if (text[i + 1] == '\u0648') {
                            sb.append("ाओ")
                            i += 2; continue
                        }
                    }
                }

                // Hamza combinations (e.g. لئے -> लिए, گئی -> गई, کوئی -> कोई)
                if (text[i] == '\u0626') {
                    if (text[i + 1] == '\u06D2') {
                        if (sb.isNotEmpty() && (sb.last() == 'ी' || sb.last() == 'ि' || sb.last() == 'ा')) sb.append("ए")
                        else sb.append("िए")
                        i += 2; continue
                    } else if (text[i + 1] == '\u06CC' || text[i + 1] == '\u064A') {
                        if (sb.isNotEmpty() && (sb.last() == 'ा' || sb.last() == 'ो')) sb.append("ई")
                        else sb.append("ी")
                        i += 2; continue
                    }
                }
            }

            val c = text[i]
            when (c) {
                '\u0622' -> sb.append('आ')
                '\u0627' -> {
                    val isStart = (i == 0 || text[i - 1].isWhitespace())
                    if (isStart) {
                        sb.append('आ')
                    } else {
                        if (sb.isNotEmpty() && (sb.last() == 'ी' || sb.last() == 'ि')) sb.append("या")
                        else if (sb.isNotEmpty() && sb.last() == 'े') sb.append("या")
                        else if (sb.isNotEmpty() && sb.last() in listOf('ा', 'ो', 'ू')) sb.append("आ")
                        else sb.append('ा')
                    }
                }
                '\u0628' -> sb.append('ब')
                '\u067E' -> sb.append('प')
                '\u062A', '\u0637' -> sb.append('त')
                '\u0679' -> sb.append('ट')
                '\u062B', '\u0633', '\u0635' -> sb.append('स')
                '\u062C' -> sb.append('ज')
                '\u0686' -> sb.append('च')
                '\u062D', '\u06C1', '\u0647', '\u06BE' -> sb.append('ह')
                '\u062E' -> sb.append("ख़")
                '\u062F' -> sb.append('द')
                '\u0688' -> sb.append('ड')
                '\u0630', '\u0632', '\u0698', '\u0636', '\u0638' -> sb.append("ज़")
                '\u0631' -> sb.append('र')
                '\u0691' -> sb.append("ड़")
                '\u0634' -> sb.append('श')
                '\u0639' -> sb.append('अ')
                '\u063A' -> sb.append("ग़")
                '\u0641' -> sb.append("फ़")
                '\u0642' -> sb.append("क़")
                '\u06A9', '\u0643' -> sb.append('क')
                '\u06AF' -> sb.append('ग')
                '\u0644' -> sb.append('ल')
                '\u0645' -> sb.append('म')
                '\u0646' -> sb.append('न')
                '\u06BA' -> sb.append('ं')
                '\u0648' -> {
                    val isStart = (i == 0 || text[i - 1].isWhitespace())
                    if (isStart) sb.append('व')
                    else {
                        if (sb.isNotEmpty() && sb.last() == 'ा') sb.append("ओ")
                        else sb.append('ो')
                    }
                }
                '\u06CC', '\u064A' -> {
                    val isStart = (i == 0 || text[i - 1].isWhitespace())
                    if (isStart) sb.append('य')
                    else {
                        if (sb.isNotEmpty() && sb.last() == 'ा') sb.append("ई")
                        else if (sb.isNotEmpty() && sb.last() in listOf('ो', 'ू')) sb.append("ई")
                        else sb.append('ी')
                    }
                }
                '\u06D2' -> {
                    if (sb.isNotEmpty() && sb.last() == 'ा') sb.append("ए")
                    else if (sb.isNotEmpty() && (sb.last() == 'ी' || sb.last() == 'ि')) sb.append("ए")
                    else if (sb.isNotEmpty() && sb.last() in listOf('ो', 'ू')) sb.append("ए")
                    else sb.append('े')
                }
                '\u0626' -> sb.append('इ')
                '\u06D4' -> sb.append('।')
                else -> sb.append(c)
            }
            i++
        }
        return sanitizeIndicScript(sb.toString())
    }

    /**
     * Converts Roman / English letters to phonetic Devanagari script.
     * Preserves timestamp tags [mm:ss.xx] and punctuation.
     */
    fun romanToDevanagari(text: String): String {
        val lrcRegex = Regex("^\\[\\d{2}:\\d{2}\\.\\d{2}\\]")
        return text.lines().joinToString("\n") { line ->
            val timestamp = lrcRegex.find(line)?.value ?: ""
            val content = if (timestamp.isNotEmpty()) line.removePrefix(timestamp) else line
            timestamp + transliterateRomanLine(content)
        }
    }

    private fun transliterateRomanLine(line: String): String {
        val sb = StringBuilder(line.length * 2)
        var i = 0
        val len = line.length

        while (i < len) {
            val c = line[i]
            if ((c in 'a'..'z') || (c in 'A'..'Z')) {
                val start = i
                while (i < len && ((line[i] in 'a'..'z') || (line[i] in 'A'..'Z') || line[i] == '\'')) {
                    i++
                }
                val word = line.substring(start, i)
                sb.append(transliterateRomanWord(word))
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun transliterateRomanWord(rawWord: String): String {
        val word = rawWord.lowercase().replace("'", "")
        if (word.isEmpty()) return rawWord

        // High frequency song word fast path for natural lyric spelling
        when (word) {
            "hai" -> return "है"
            "hain" -> return "हैं"
            "ho" -> return "हो"
            "hoon", "hun" -> return "हूं"
            "tha" -> return "था"
            "thi" -> return "थी"
            "the" -> return "थे"
            "ki" -> return "की"
            "ka" -> return "का"
            "ke" -> return "के"
            "ko" -> return "को"
            "se" -> return "से"
            "me", "mein" -> return "में"
            "main" -> return "मैं"
            "tu" -> return "तू"
            "tum" -> return "तुम"
            "aap" -> return "आप"
            "hum" -> return "हम"
            "yeh", "ye" -> return "ये"
            "woh", "wo" -> return "वो"
            "kya" -> return "क्या"
            "kyun" -> return "क्यों"
            "dil" -> return "दिल"
            "jaan" -> return "जान"
            "zindagi" -> return "ज़िंदगी"
            "ishq" -> return "इश्क़"
            "pyar" -> return "प्यार"
            "mohabbat" -> return "मोहब्बत"
            "tera" -> return "तेरा"
            "teri" -> return "तेरी"
            "tere" -> return "तेरे"
            "mera" -> return "मेरा"
            "meri" -> return "मेरी"
            "mere" -> return "मेरे"
        }

        val sb = StringBuilder(word.length * 2)
        var i = 0
        val len = word.length
        var prevWasConsonant = false

        while (i < len) {
            // Check 3-character sequences
            if (i + 2 < len) {
                val sub3 = word.substring(i, i + 3)
                when (sub3) {
                    "chh" -> {
                        if (prevWasConsonant) sb.append('\u094D')
                        sb.append('छ')
                        prevWasConsonant = true
                        i += 3
                        continue
                    }
                    "shh" -> {
                        if (prevWasConsonant) sb.append('\u094D')
                        sb.append('ष')
                        prevWasConsonant = true
                        i += 3
                        continue
                    }
                }
            }

            // Check 2-character sequences
            if (i + 1 < len) {
                val sub2 = word.substring(i, i + 2)
                val cons2: String? = when (sub2) {
                    "kh" -> "ख"
                    "gh" -> "घ"
                    "ch" -> "च"
                    "jh" -> "झ"
                    "th" -> "थ"
                    "dh" -> "ध"
                    "ph" -> "फ"
                    "bh" -> "भ"
                    "sh" -> "श"
                    "zh" -> "ज़"
                    "rh" -> "ढ़"
                    "ng" -> "ङ"
                    "ny" -> "ञ"
                    else -> null
                }
                if (cons2 != null) {
                    if (prevWasConsonant) sb.append('\u094D')
                    sb.append(cons2)
                    prevWasConsonant = true
                    i += 2
                    continue
                }

                // 2-character vowels
                val isVowel2 = sub2 in listOf("aa", "ai", "au", "ee", "oo")
                if (isVowel2) {
                    if (prevWasConsonant) {
                        when (sub2) {
                            "aa" -> sb.append('\u093E') // ा
                            "ai" -> sb.append('\u0948') // ै
                            "au" -> sb.append('\u094C') // ौ
                            "ee" -> sb.append('\u0940') // ी
                            "oo" -> sb.append('\u0942') // ू
                        }
                    } else {
                        when (sub2) {
                            "aa" -> sb.append('आ')
                            "ai" -> sb.append('ऐ')
                            "au" -> sb.append('औ')
                            "ee" -> sb.append('ई')
                            "oo" -> sb.append('ऊ')
                        }
                    }
                    prevWasConsonant = false
                    i += 2
                    continue
                }
            }

            // Single character processing
            val c = word[i]
            when (c) {
                'a' -> {
                    if (prevWasConsonant) {
                        if (i == len - 1) {
                            sb.append('\u093E') // ा at word end
                        }
                    } else {
                        sb.append('अ')
                    }
                    prevWasConsonant = false
                }
                'i' -> {
                    if (prevWasConsonant) sb.append('\u093F') // ि
                    else sb.append('इ')
                    prevWasConsonant = false
                }
                'u' -> {
                    if (prevWasConsonant) sb.append('\u0941') // ु
                    else sb.append('उ')
                    prevWasConsonant = false
                }
                'e' -> {
                    if (prevWasConsonant) sb.append('\u0947') // े
                    else sb.append('ए')
                    prevWasConsonant = false
                }
                'o' -> {
                    if (prevWasConsonant) sb.append('\u094B') // ो
                    else sb.append('ओ')
                    prevWasConsonant = false
                }
                'n' -> {
                    val isAnusvara = (i + 1 < len && word[i + 1] !in "aeiou") || (i == len - 1 && !prevWasConsonant && sb.isNotEmpty())
                    if (isAnusvara && prevWasConsonant) {
                        sb.append('\u0902') // Anusvara ं
                        prevWasConsonant = false
                    } else {
                        if (prevWasConsonant) sb.append('\u094D')
                        sb.append('न')
                        prevWasConsonant = true
                    }
                }
                else -> {
                    val cons1: String? = when (c) {
                        'k', 'c' -> "क"
                        'g' -> "ग"
                        'j' -> "ज"
                        't' -> "त"
                        'd' -> "द"
                        'p' -> "प"
                        'b' -> "ब"
                        'm' -> "म"
                        'y' -> "य"
                        'r' -> "र"
                        'l' -> "ल"
                        'v', 'w' -> "व"
                        's' -> "स"
                        'h' -> "ह"
                        'z' -> "ज़"
                        'f' -> "फ़"
                        'q' -> "क़"
                        'x' -> {
                            if (prevWasConsonant) sb.append('\u094D')
                            sb.append("क्स")
                            prevWasConsonant = true
                            i++
                            continue
                        }
                        else -> null
                    }
                    if (cons1 != null) {
                        if (prevWasConsonant) sb.append('\u094D')
                        sb.append(cons1)
                        prevWasConsonant = true
                    } else {
                        sb.append(c)
                        prevWasConsonant = false
                    }
                }
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Identifies the primary non-Latin or Latin script present in text.
     * Accurately detects Hindi (Devanagari), Bengali, Urdu (Arabic script),
     * Punjabi (Gurmukhi), Gujarati, Tamil, Telugu, and Roman (Latin English).
     */
    fun detectDominantScript(text: String): String {
        var devanagari = 0
        var bengali = 0
        var urdu = 0
        var gurmukhi = 0
        var gujarati = 0
        var tamil = 0
        var telugu = 0
        var latin = 0

        for (c in text) {
            when (c) {
                in '\u0900'..'\u097F' -> devanagari++
                in '\u0980'..'\u09FF' -> bengali++
                in '\u0600'..'\u06FF' -> urdu++
                in '\u0A00'..'\u0A7F' -> gurmukhi++
                in '\u0A80'..'\u0AFF' -> gujarati++
                in '\u0B80'..'\u0BFF' -> tamil++
                in '\u0C00'..'\u0C7F' -> telugu++
                in 'a'..'z', in 'A'..'Z' -> latin++
            }
        }

        val counts = listOf(
            "devanagari" to devanagari,
            "bengali" to bengali,
            "urdu" to urdu,
            "gurmukhi" to gurmukhi,
            "gujarati" to gujarati,
            "tamil" to tamil,
            "telugu" to telugu,
            "roman" to latin
        )
        val max = counts.maxByOrNull { it.second }
        return if (max != null && max.second > 0) max.first else "roman"
    }

    /**
     * Universal script conversion between any source script and any target script.
     * Converts letters/alphabets only while preserving phonetics, words, line breaks,
     * and LRC timestamps [mm:ss.xx]. Does NOT machine translate meanings.
     */
    fun convertScript(text: String, fromScript: String, toScript: String): String {
        if (text.isBlank()) return text

        val src = fromScript.lowercase().trim()
        val dest = toScript.lowercase().trim()

        val actualFrom = if (src == "auto") detectDominantScript(text) else when (src) {
            "hi", "hindi", "devanagari" -> "devanagari"
            "bn", "bengali", "bangla" -> "bengali"
            "ur", "urdu", "arabic" -> "urdu"
            "pa", "punjabi", "gurmukhi" -> "gurmukhi"
            "gu", "gujarati" -> "gujarati"
            "ta", "tamil" -> "tamil"
            "te", "telugu" -> "telugu"
            "en", "roman", "latin", "latn" -> "roman"
            else -> "roman"
        }

        val actualTo = when (dest) {
            "hi", "hindi", "devanagari" -> "devanagari"
            "bn", "bengali", "bangla" -> "bengali"
            "ur", "urdu", "arabic" -> "urdu"
            "pa", "punjabi", "gurmukhi" -> "gurmukhi"
            "gu", "gujarati" -> "gujarati"
            "ta", "tamil" -> "tamil"
            "te", "telugu" -> "telugu"
            "en", "roman", "latin", "latn" -> "roman"
            else -> "roman"
        }

        if (actualFrom == actualTo) return text

        // Step 1: Normalize source to canonical Devanagari pivot
        val devanagariPivot = sanitizeIndicScript(when (actualFrom) {
            "devanagari" -> text
            "bengali" -> bengaliToDevanagari(text)
            "urdu" -> urduToDevanagari(text)
            "gurmukhi" -> gurmukhiToDevanagari(text)
            "gujarati" -> gujaratiToDevanagari(text)
            "tamil" -> tamilToDevanagari(text)
            "telugu" -> teluguToDevanagari(text)
            "roman" -> romanToDevanagari(text)
            else -> text
        })

        // Step 2: Convert from canonical Devanagari pivot to target script
        val result = when (actualTo) {
            "devanagari" -> devanagariPivot
            "bengali" -> devanagariToBengali(devanagariPivot)
            "urdu" -> devanagariToUrdu(devanagariPivot)
            "gurmukhi" -> devanagariToGurmukhi(devanagariPivot)
            "gujarati" -> devanagariToGujarati(devanagariPivot)
            "tamil" -> devanagariToTamil(devanagariPivot)
            "telugu" -> devanagariToTelugu(devanagariPivot)
            "roman" -> {
                when (actualFrom) {
                    "bengali" -> transliterateBengali(text)
                    "gurmukhi" -> transliterateGurmukhi(text)
                    "tamil" -> transliterateTamil(text)
                    "urdu" -> transliterateDevanagari(devanagariPivot)
                    else -> transliterateDevanagari(devanagariPivot)
                }
            }
            else -> devanagariPivot
        }
        return if (actualTo == "roman") cleanRomanizedSpacing(result) else sanitizeIndicScript(result)
    }

    private fun cleanRomanizedSpacing(s: String): String {
        return s.lines().joinToString("\n") { line ->
            line.replace(Regex("[ \\t]+"), " ")
                .replace("aa a", "aa ")
                .replace("a a", "a ")
                .trim()
        }
    }
}
