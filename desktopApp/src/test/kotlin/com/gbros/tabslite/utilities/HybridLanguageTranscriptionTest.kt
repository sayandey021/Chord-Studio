package com.gbros.tabslite.utilities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridLanguageTranscriptionTest {

    @Test
    fun testDevanagariHinglishTransliteration() {
        val devanagariText = "तुम ही हो"
        val transliterated = PhoneticTransliterator.transliterateDevanagari(devanagariText)
        println("Hindi Devanagari: '$devanagariText' -> Hinglish: '$transliterated'")
        assertTrue(transliterated.contains("tum", ignoreCase = true), "Transliteration should contain 'tum'")
        assertTrue(transliterated.contains("ho", ignoreCase = true), "Transliteration should contain 'ho'")

        val songLyrics = "दिल दे दिया है जां तुम्हें देंगे"
        val romanizedSong = PhoneticTransliterator.transliterateDevanagari(songLyrics)
        println("Song Devanagari: '$songLyrics' -> Hinglish: '$romanizedSong'")
        assertTrue(romanizedSong.contains("dil", ignoreCase = true), "Transliteration should contain 'dil'")
    }

    @Test
    fun testBengaliBenglishTransliteration() {
        val banglaText = "তুমি রবে নীরবে"
        val transliterated = PhoneticTransliterator.transliterateBengali(banglaText)
        println("Bangla: '$banglaText' -> Benglish: '$transliterated'")
        assertTrue(transliterated.contains("tumi", ignoreCase = true), "Transliteration should contain 'tumi'")

        val banglaSong = "আমি তোমায় ভালোবাসি"
        val romanizedBangla = PhoneticTransliterator.transliterateBengali(banglaSong)
        println("Song Bangla: '$banglaSong' -> Benglish: '$romanizedBangla'")
        assertTrue(romanizedBangla.contains("ami", ignoreCase = true), "Transliteration should contain 'ami'")
    }

    @Test
    fun testSupportedLanguagesCatalog() {
        val total = LyricsExtractor.SUPPORTED_LANGUAGES.size
        println("Total supported languages: $total")
        assertTrue(total >= 30, "Should support at least 30 languages")

        // Verify major regional languages
        val hi = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "hi" }
        assertTrue(hi != null, "Hindi must be present")

        val bn = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "bn" }
        assertTrue(bn != null, "Bengali must be present")

        val ur = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "ur" }
        assertTrue(ur != null, "Urdu must be present")

        val pa = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "pa" }
        assertTrue(pa != null, "Punjabi must be present")

        val ta = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "ta" }
        assertTrue(ta != null, "Tamil must be present")

        val te = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "te" }
        assertTrue(te != null, "Telugu must be present")

        val es = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "es" }
        assertTrue(es != null, "Spanish must be present")

        val ja = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "ja" }
        assertTrue(ja != null, "Japanese must be present")

        val ko = LyricsExtractor.SUPPORTED_LANGUAGES.firstOrNull { it.code == "ko" }
        assertTrue(ko != null, "Korean must be present")
    }

    @Test
    fun testHybridTextProcessor() {
        val mixedDevanagari = "Tum hi ho तुम ही हो ab tum hi ho"
        val processedHinglish = PhoneticTransliterator.processHybridText(mixedDevanagari, "hinglish")
        println("Mixed -> Hinglish: '$processedHinglish'")
        assertTrue(
            processedHinglish.none { it in '\u0900'..'\u097F' },
            "Devanagari characters should be converted to Latin in Hinglish mode"
        )

        val mixedBangla = "Ami tumake bhalobashi আমি তোমায় ভালোবাসি"
        val processedBenglish = PhoneticTransliterator.processHybridText(mixedBangla, "benglish")
        println("Mixed -> Benglish: '$processedBenglish'")
        assertTrue(
            processedBenglish.none { it in '\u0980'..'\u09FF' },
            "Bengali characters should be converted to Latin in Benglish mode"
        )

        val gurmukhiSong = "ਲਵਰ ੨੯੫ ਪਾਸੂਰੀ"
        val processedPinglish = PhoneticTransliterator.processHybridText(gurmukhiSong, "pinglish")
        println("Gurmukhi -> Pinglish: '$processedPinglish'")
        assertTrue(
            processedPinglish.none { it in '\u0A00'..'\u0A7F' },
            "Gurmukhi characters should be converted to Latin in Pinglish mode"
        )

        val tamilSong = "என் காதலே"
        val processedTanglish = PhoneticTransliterator.processHybridText(tamilSong, "tanglish")
        println("Tamil -> Tanglish: '$processedTanglish'")
        assertTrue(
            processedTanglish.none { it in '\u0B80'..'\u0BFF' },
            "Tamil characters should be converted to Latin in Tanglish mode"
        )
    }

    @Test
    fun testDominantScriptDetection() {
        val hindi = "तुम ही हो अब तुम ही हो मेरी आशिक़ी अब तुम ही हो"
        assertEquals("devanagari", PhoneticTransliterator.detectDominantScript(hindi))

        val bangla = "আমি তোমায় ভালোবাসি এই সুন্দর রাতে"
        assertEquals("bengali", PhoneticTransliterator.detectDominantScript(bangla))

        val urdu = "دل دل پاکستان جاں جاں پاکستان"
        assertEquals("urdu", PhoneticTransliterator.detectDominantScript(urdu))

        val english = "Hum tere bin ab reh nahi sakte tere bina kya wajood mera"
        assertEquals("roman", PhoneticTransliterator.detectDominantScript(english))
    }

    @Test
    fun testUniversalCrossScriptConversion() {
        // Roman -> Devanagari
        val romanInput = "tum hi ho tere bina"
        val convertedDevanagari = PhoneticTransliterator.convertScript(romanInput, "roman", "devanagari")
        println("Roman -> Devanagari: '$convertedDevanagari'")
        assertTrue(convertedDevanagari.contains("तुम") || convertedDevanagari.contains("हो"))

        // Hindi -> Bengali
        val hindiInput = "तुम ही हो"
        val convertedBangla = PhoneticTransliterator.convertScript(hindiInput, "devanagari", "bengali")
        println("Hindi -> Bangla: '$convertedBangla'")
        assertTrue(convertedBangla.any { it in '\u0980'..'\u09FF' })

        // Hindi -> Urdu
        val convertedUrdu = PhoneticTransliterator.convertScript(hindiInput, "devanagari", "urdu")
        println("Hindi -> Urdu: '$convertedUrdu'")
        assertTrue(convertedUrdu.any { it in '\u0600'..'\u06FF' })

        // Urdu -> Hindi
        val urduInput = "تم ہی ہو"
        val convertedBackHindi = PhoneticTransliterator.convertScript(urduInput, "urdu", "devanagari")
        println("Urdu -> Hindi: '$convertedBackHindi'")
        assertTrue(convertedBackHindi.any { it in '\u0900'..'\u097F' })
    }

    @Test
    fun testSanitizeIndicScriptDottedCircleRemovals() {
        // Broken combinations from user screenshot
        val brokenDevanagari = "ना रा\u093E\u0940स होन मेन बोद\u093C\u093E तेरे आ\u0940स दील को चोराने के ली\u0940\u093E हे जोही आ\u0947क धोन जोब भी बोरोसात आ\u093E"
        val healed = PhoneticTransliterator.sanitizeIndicScript(brokenDevanagari)
        println("Broken: $brokenDevanagari")
        println("Healed: $healed")

        // Must not contain consecutive matras or independent vowels with matras
        kotlin.test.assertFalse(healed.contains("\u0906\u0940"), "Should not contain आ + ी")
        kotlin.test.assertFalse(healed.contains("\u0906\u0947"), "Should not contain आ + े")
        kotlin.test.assertFalse(healed.contains("\u0940\u093E"), "Should not contain ी + ा")
        kotlin.test.assertFalse(healed.contains("\u093E\u0940"), "Should not contain ा + ी")
        kotlin.test.assertFalse(healed.contains("\u0926\u093C"), "Should not contain invalid nukta on da (द़)")

        // Bengali broken nukta and matra repair
        val brokenBengali = "না রীস ছোন মেন বোদ\u09BC\u09BE"
        val healedBengali = PhoneticTransliterator.sanitizeIndicScript(brokenBengali)
        println("Healed Bengali: $healedBengali")
        kotlin.test.assertFalse(healedBengali.contains("\u09A6\u09BC"), "Bengali da should not have broken nukta")

        // Gurmukhi broken nukta repair
        val brokenGurmukhi = "ਨਾ ਰੀਸ ਹੋਨ ਮੇਨ ਬੋਦ\u0A3C\u0A3E"
        val healedGurmukhi = PhoneticTransliterator.sanitizeIndicScript(brokenGurmukhi)
        println("Healed Gurmukhi: $healedGurmukhi")
        kotlin.test.assertFalse(healedGurmukhi.contains("\u0A26\u0A3C"), "Gurmukhi da should not have broken nukta")
    }

    @Test
    fun testIsBengaliInDevanagariAndAutoConversion() {
        val bengaliInDevanagariSong = "आमि तोमाय भालोबाशी मोनेर कोथा बोलबो थाकते दाओ"
        assertTrue(PhoneticTransliterator.isBengaliInDevanagari(bengaliInDevanagariSong), "Should detect Bengali song in Devanagari")

        val convertedBangla = PhoneticTransliterator.devanagariToBengali(bengaliInDevanagariSong)
        println("Auto-converted to Bangla: $convertedBangla")
        assertTrue(convertedBangla.contains("আমি"), "Should contain Bengali 'আমি'")
        assertTrue(convertedBangla.contains("তোমায়") || convertedBangla.contains("তোমায়"), "Should contain Bengali 'তোমায়'")
        assertTrue(convertedBangla.contains("ভালোবা"), "Should contain Bengali 'ভালোবা'")

        val modernBengaliInDevanagari = "एक्टा गान गाइबो तोमार जोन्नो मोन जे बोझेना"
        assertTrue(PhoneticTransliterator.isBengaliInDevanagari(modernBengaliInDevanagari), "Should detect modern Bengali lyrics in Devanagari")

        val pureHindi = "तुम ही हो अब तुम ही हो ज़िन्दगी अब तुम ही हो"
        kotlin.test.assertFalse(PhoneticTransliterator.isBengaliInDevanagari(pureHindi), "Pure Hindi song should not be detected as Bengali")
    }
}

