package com.gbros.tabslite.utilities

enum class LearnInstrument(val displayName: String, val stringCount: Int, val stringNames: List<String>, val openMidiPitches: List<Int>) {
    GUITAR("Guitar", 6, listOf("E", "A", "D", "G", "B", "e"), listOf(40, 45, 50, 55, 59, 64)),
    UKULELE("Ukulele", 4, listOf("G", "C", "E", "A"), listOf(67, 60, 64, 69)),
    PIANO("Piano", 0, emptyList(), emptyList()),
    BASS("Bass", 4, listOf("E", "A", "D", "G"), listOf(28, 33, 38, 43))
}

enum class PianoTone(
    val id: String,
    val displayName: String,
    val shortName: String,
    val programNumber: Int,
    val description: String,
    val reverbLevel: Int = 65,
    val chorusLevel: Int = 0,
    val brightness: Int = 80,
    val resonance: Int = 64
) {
    CONCERT_GRAND(
        id = "concert_grand",
        displayName = "Concert Grand",
        shortName = "Concert",
        programNumber = 0,
        description = "Rich, resonant Steinway concert grand with deep acoustic bass and hall ambience",
        reverbLevel = 75,
        chorusLevel = 0,
        brightness = 82,
        resonance = 64
    ),
    BRIGHT_ACOUSTIC(
        id = "bright_acoustic",
        displayName = "Bright Acoustic",
        shortName = "Bright",
        programNumber = 1,
        description = "Punchy, crystalline Yamaha C7-style acoustic piano designed for modern pop & rock",
        reverbLevel = 55,
        chorusLevel = 8,
        brightness = 105,
        resonance = 68
    ),
    WARM_STUDIO(
        id = "warm_studio",
        displayName = "Warm Studio",
        shortName = "Warm",
        programNumber = 0,
        description = "Mellow, intimate acoustic studio grand with soft hammer response and warm decay",
        reverbLevel = 45,
        chorusLevel = 5,
        brightness = 54,
        resonance = 56
    ),
    VINTAGE_UPRIGHT(
        id = "vintage_upright",
        displayName = "Vintage Upright",
        shortName = "Upright",
        programNumber = 3,
        description = "Charming acoustic upright saloon piano with authentic dual-string detuning",
        reverbLevel = 50,
        chorusLevel = 25,
        brightness = 75,
        resonance = 64
    ),
    ELECTRIC_GRAND(
        id = "electric_grand",
        displayName = "Electric Grand",
        shortName = "CP-80",
        programNumber = 2,
        description = "Classic 80s amplified stage piano tone with singing crystalline sustain",
        reverbLevel = 60,
        chorusLevel = 35,
        brightness = 90,
        resonance = 64
    )
}

enum class StrumGuitarSound(
    val id: String,
    val displayName: String,
    val shortName: String,
    val programNumber: Int,
    val isElectric: Boolean,
    val reverbLevel: Int = 45,
    val chorusLevel: Int = 0,
    val brightness: Int = 75,
    val resonance: Int = 64,
    val description: String
) {
    ACOUSTIC_STEEL(
        id = "steel",
        displayName = "Acoustic Steel",
        shortName = "Steel",
        programNumber = 25, // Acoustic Guitar (steel)
        isElectric = false,
        reverbLevel = 50,
        chorusLevel = 0,
        brightness = 85,
        resonance = 64,
        description = "Crisp, resonant dreadnought acoustic steel strings with rich full-bodied projection"
    ),
    ACOUSTIC_NYLON(
        id = "nylon",
        displayName = "Acoustic Nylon",
        shortName = "Nylon",
        programNumber = 24, // Acoustic Guitar (nylon)
        isElectric = false,
        reverbLevel = 45,
        chorusLevel = 0,
        brightness = 60,
        resonance = 58,
        description = "Warm, mellow classical Spanish acoustic tone with soft touch and gentle fingerstyle decay"
    ),
    ELECTRIC_CLEAN(
        id = "electric_clean",
        displayName = "Clean Electric",
        shortName = "Clean",
        programNumber = 27, // Electric Guitar (clean)
        isElectric = true,
        reverbLevel = 60,
        chorusLevel = 25,
        brightness = 90,
        resonance = 64,
        description = "Crystal-clear Stratocaster-style clean electric guitar with bell-like chime and hall reverb"
    ),
    ELECTRIC_CRUNCH(
        id = "electric_crunch",
        displayName = "Crunch Electric",
        shortName = "Crunch",
        programNumber = 29, // Overdriven Guitar
        isElectric = true,
        reverbLevel = 40,
        chorusLevel = 0,
        brightness = 95,
        resonance = 72,
        description = "Punchy overdriven rock electric guitar with warm tube bite and aggressive chord crunch"
    ),
    ELECTRIC_JAZZ(
        id = "electric_jazz",
        displayName = "Warm Jazz Electric",
        shortName = "Jazz",
        programNumber = 26, // Electric Guitar (jazz)
        isElectric = true,
        reverbLevel = 45,
        chorusLevel = 0,
        brightness = 55,
        resonance = 55,
        description = "Deep, rounded hollow-body archtop electric guitar tailored for jazz and neo-soul progressions"
    ),
    ACOUSTIC_12_STRING(
        id = "12_string",
        displayName = "12-String Shimmer",
        shortName = "12-String",
        programNumber = 25, // Steel + Chorus shimmer
        isElectric = false,
        reverbLevel = 65,
        chorusLevel = 75,
        brightness = 95,
        resonance = 64,
        description = "Wide, shimmering 12-string chorus resonance recreating paired octave and unison string chorus"
    )
}

data class ChordQuality(
    val id: String,
    val suffix: String,
    val name: String,
    val formula: String,
    val intervals: List<Int>, // semitones from root
    val category: String,
    val description: String
)

data class ScaleDefinition(
    val id: String,
    val name: String,
    val category: String,
    val formulaText: String,
    val degrees: List<String>,
    val intervals: List<Int>, // semitones from root
    val mood: String,
    val description: String,
    val famousSongs: List<String>,
    val harmonizedTriads: List<String>
)

enum class StrumStrokeType(val symbol: String, val label: String, val shortLabel: String) {
    DOWN("↓", "Downstroke", "Down"),
    UP("↑", "Upstroke", "Up"),
    ACCENT_DOWN("▼", "Accent Down", "Accent"),
    ACCENT_UP("▲", "Accent Up", "Accent"),
    MUTE("✕", "Chop / Mute", "Mute"),
    BASS("B", "Bass Pick", "Bass"),
    REST("•", "Rest / Ghost", "Rest")
}

data class StrumBeat(
    val countLabel: String,
    val stroke: StrumStrokeType,
    val isPrimaryBeat: Boolean = false
)

data class StrumPattern(
    val id: String,
    val name: String,
    val timeSignature: String,
    val genre: String,
    val difficulty: String,
    val defaultBpm: Int,
    val countText: String,
    val description: String,
    val exampleSongs: List<String>,
    val beats: List<StrumBeat>
)

object MusicTheoryData {
    val NOTES_SHARP = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val NOTES_FLAT  = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    // All major Chord Qualities
    val CHORD_QUALITIES = listOf(
        ChordQuality("maj", "", "Major", "1 - 3 - 5", listOf(0, 4, 7), "Core Triads", "Bright, stable, foundational harmony."),
        ChordQuality("min", "m", "Minor", "1 - ♭3 - 5", listOf(0, 3, 7), "Core Triads", "Emotional, introspective, melancholic."),
        ChordQuality("dom7", "7", "Dominant 7th", "1 - 3 - 5 - ♭7", listOf(0, 4, 7, 10), "Seventh Chords", "Bluesy, restless tension wanting resolution to the I chord."),
        ChordQuality("maj7", "maj7", "Major 7th", "1 - 3 - 5 - 7", listOf(0, 4, 7, 11), "Seventh Chords", "Lush, jazzy, dreamy, sophisticated."),
        ChordQuality("min7", "m7", "Minor 7th", "1 - ♭3 - 5 - ♭7", listOf(0, 3, 7, 10), "Seventh Chords", "Smooth, warm, mellow jazz and R&B staple."),
        ChordQuality("sus4", "sus4", "Suspended 4th", "1 - 4 - 5", listOf(0, 5, 7), "Suspended & Added", "Anticipatory, open, classic folk-rock resolution chord."),
        ChordQuality("sus2", "sus2", "Suspended 2nd", "1 - 2 - 5", listOf(0, 2, 7), "Suspended & Added", "Airy, ethereal, modern acoustic favorite."),
        ChordQuality("7sus4", "7sus4", "7th Suspended 4th", "1 - 4 - 5 - ♭7", listOf(0, 5, 7, 10), "Suspended & Added", "Spacious modal tension, funk and gospel signature."),
        ChordQuality("add9", "add9", "Add 9", "1 - 3 - 5 - 9", listOf(0, 4, 7, 14 % 12), "Suspended & Added", "Bright sparkle added to a clean major chord."),
        ChordQuality("madd9", "madd9", "Minor Add 9", "1 - ♭3 - 5 - 9", listOf(0, 3, 7, 14 % 12), "Suspended & Added", "Poignant, cinematic, melancholic sparkle."),
        ChordQuality("dim", "dim", "Diminished", "1 - ♭3 - ♭5", listOf(0, 3, 6), "Diminished & Augmented", "Tense, unstable leading chord."),
        ChordQuality("dim7", "dim7", "Diminished 7th", "1 - ♭3 - ♭5 - ♭♭7", listOf(0, 3, 6, 9), "Diminished & Augmented", "Highly symmetrical chromatic tension, horror and dramatic drama."),
        ChordQuality("m7b5", "m7b5", "Half-Diminished (ø)", "1 - ♭3 - ♭5 - ♭7", listOf(0, 3, 6, 10), "Diminished & Augmented", "Crucial jazz minor ii-V-I progression chord."),
        ChordQuality("aug", "aug", "Augmented (+)", "1 - 3 - ♯5", listOf(0, 4, 8), "Diminished & Augmented", "Dreamy, mysterious, floating tension."),
        ChordQuality("6", "6", "Major 6th", "1 - 3 - 5 - 6", listOf(0, 4, 7, 9), "Extended & Color", "Vintage swing, warm jazz and bossa nova color."),
        ChordQuality("m6", "m6", "Minor 6th", "1 - ♭3 - 5 - 6", listOf(0, 3, 7, 9), "Extended & Color", "Noir detective mood, haunting spy harmony."),
        ChordQuality("9", "9", "Dominant 9th", "1 - 3 - 5 - ♭7 - 9", listOf(0, 4, 7, 10, 2), "Extended & Color", "Classic James Brown funk and Chicago blues chord."),
        ChordQuality("maj9", "maj9", "Major 9th", "1 - 3 - 5 - 7 - 9", listOf(0, 4, 7, 11, 2), "Extended & Color", "Silky modern neo-soul and smooth jazz."),
        ChordQuality("min9", "m9", "Minor 9th", "1 - ♭3 - 5 - ♭7 - 9", listOf(0, 3, 7, 10, 2), "Extended & Color", "Deep, atmospheric R&B and contemporary soul."),
        ChordQuality("5", "5", "Power Chord", "1 - 5", listOf(0, 7), "Rock & Metal", "Driving rock and metal foundation with high overdrive clarity.")
    )

    // All Major Scales and Modes
    val SCALES = listOf(
        ScaleDefinition(
            id = "major",
            name = "Major (Ionian)",
            category = "Diatonic Core",
            formulaText = "W - W - H - W - W - W - H",
            degrees = listOf("1", "2", "3", "4", "5", "6", "7"),
            intervals = listOf(0, 2, 4, 5, 7, 9, 11),
            mood = "Bright, uplifting, triumphant, resolved",
            description = "The foundation of Western music harmony. Every major key and chord system derives from this pattern.",
            famousSongs = listOf("Let It Be", "Brown Eyed Girl", "Don't Stop Believin'"),
            harmonizedTriads = listOf("I (Maj)", "ii (min)", "iii (min)", "IV (Maj)", "V (Maj)", "vi (min)", "vii° (dim)")
        ),
        ScaleDefinition(
            id = "minor",
            name = "Natural Minor (Aeolian)",
            category = "Diatonic Core",
            formulaText = "W - H - W - W - H - W - W",
            degrees = listOf("1", "2", "♭3", "4", "5", "♭6", "♭7"),
            intervals = listOf(0, 2, 3, 5, 7, 8, 10),
            mood = "Melancholic, somber, epic, contemplative",
            description = "The relative minor of the major scale. Universally used in rock, metal, ballads, and film scores.",
            famousSongs = listOf("Stairway to Heaven", "Nothing Else Matters", "Hotel California"),
            harmonizedTriads = listOf("i (min)", "ii° (dim)", "III (Maj)", "iv (min)", "v (min)", "VI (Maj)", "VII (Maj)")
        ),
        ScaleDefinition(
            id = "pentatonic_minor",
            name = "Minor Pentatonic",
            category = "Pentatonic & Blues",
            formulaText = "1½ - W - W - 1½ - W",
            degrees = listOf("1", "♭3", "4", "5", "♭7"),
            intervals = listOf(0, 3, 5, 7, 10),
            mood = "Gritty, powerful, soulful, quintessential rock",
            description = "The most widely played soloing scale on guitar. Strips away the half steps so every note sounds killer.",
            famousSongs = listOf("Back in Black", "Smoke on the Water", "Comfortably Numb"),
            harmonizedTriads = listOf("i", "♭III", "iv", "v", "♭VII")
        ),
        ScaleDefinition(
            id = "pentatonic_major",
            name = "Major Pentatonic",
            category = "Pentatonic & Blues",
            formulaText = "W - W - 1½ - W - 1½",
            degrees = listOf("1", "2", "3", "5", "6"),
            intervals = listOf(0, 2, 4, 7, 9),
            mood = "Sweet, warm, country, optimistic",
            description = "The sound of southern rock, country, gospel, and the sweeter side of blues (B.B. King, Allman Brothers).",
            famousSongs = listOf("My Girl", "Wish You Were Here (Intro)", "Jessica"),
            harmonizedTriads = listOf("I", "ii", "iii", "V", "vi")
        ),
        ScaleDefinition(
            id = "blues",
            name = "Blues Scale",
            category = "Pentatonic & Blues",
            formulaText = "1½ - W - H - H - 1½ - W",
            degrees = listOf("1", "♭3", "4", "♭5", "5", "♭7"),
            intervals = listOf(0, 3, 5, 6, 7, 10),
            mood = "Dirty, emotive, stinging, expressive",
            description = "Adds the chromatic 'blue note' (♭5 tritone) to the minor pentatonic for trademark blues tension.",
            famousSongs = listOf("The Thrill Is Gone", "Crossroads", "Pride and Joy"),
            harmonizedTriads = listOf("i7", "IV7", "V7")
        ),
        ScaleDefinition(
            id = "dorian",
            name = "Dorian Mode",
            category = "Modes",
            formulaText = "W - H - W - W - W - H - W",
            degrees = listOf("1", "2", "♭3", "4", "5", "6", "♭7"),
            intervals = listOf(0, 2, 3, 5, 7, 9, 10),
            mood = "Sophisticated minor, groovy, Carlos Santana feel",
            description = "A minor scale with a raised natural 6th degree, preventing the darkness of natural minor and adding groove.",
            famousSongs = listOf("Oye Como Va", "So What (Miles Davis)", "Evil Ways"),
            harmonizedTriads = listOf("i (min)", "ii (min)", "♭III (Maj)", "IV (Maj)", "v (min)", "vi° (dim)", "♭VII (Maj)")
        ),
        ScaleDefinition(
            id = "mixolydian",
            name = "Mixolydian Mode",
            category = "Modes",
            formulaText = "W - W - H - W - W - H - W",
            degrees = listOf("1", "2", "3", "4", "5", "6", "♭7"),
            intervals = listOf(0, 2, 4, 5, 7, 9, 10),
            mood = "Southern rock, relaxed dominant, bluesy major",
            description = "A major scale with a flat 7th. Perfect for jamming over dominant 7th chords without the harsh leading tone.",
            famousSongs = listOf("Sweet Child O' Mine", "Sweet Home Alabama", "Norwegian Wood"),
            harmonizedTriads = listOf("I (Maj)", "ii (min)", "iii° (dim)", "IV (Maj)", "v (min)", "vi (min)", "♭VII (Maj)")
        ),
        ScaleDefinition(
            id = "harmonic_minor",
            name = "Harmonic Minor",
            category = "Exotic & Classical",
            formulaText = "W - H - W - W - H - 1½ - H",
            degrees = listOf("1", "2", "♭3", "4", "5", "♭6", "7"),
            intervals = listOf(0, 2, 3, 5, 7, 8, 11),
            mood = "Neoclassical, Middle Eastern, exotic, dramatic",
            description = "Features a natural 7th leading tone against a flat 6th, producing a distinct 1.5-step augmented jump.",
            famousSongs = listOf("Yngwie Malmsteen solos", "Sultans of Swing (Outro)", "Spanish Romance"),
            harmonizedTriads = listOf("i (min)", "ii° (dim)", "♭III+ (aug)", "iv (min)", "V (Maj)", "♭VI (Maj)", "vii° (dim)")
        ),
        ScaleDefinition(
            id = "melodic_minor",
            name = "Melodic Minor (Jazz Minor)",
            category = "Exotic & Classical",
            formulaText = "W - H - W - W - W - W - H",
            degrees = listOf("1", "2", "♭3", "4", "5", "6", "7"),
            intervals = listOf(0, 2, 3, 5, 7, 9, 11),
            mood = "Sleek, modern jazz, dark yet resolved",
            description = "Combines a minor 3rd with natural 6th and 7th degrees. The foundation of modern jazz improvisation.",
            famousSongs = listOf("Autumn Leaves (Modern)", "Jazz Standards", "Fusion Leads"),
            harmonizedTriads = listOf("i (min)", "ii (min)", "♭III+ (aug)", "IV (Maj)", "V (Maj)", "vi° (dim)", "vii° (dim)")
        ),
        ScaleDefinition(
            id = "phrygian",
            name = "Phrygian Mode",
            category = "Modes",
            formulaText = "H - W - W - W - H - W - W",
            degrees = listOf("1", "♭2", "♭3", "4", "5", "♭6", "♭7"),
            intervals = listOf(0, 1, 3, 5, 7, 8, 10),
            mood = "Flamenco, dark metal, tense, mysterious",
            description = "Defined by its minor 2nd (half-step above the root). Iconic in thrash metal riffs and Spanish guitar.",
            famousSongs = listOf("Wherever I May Roam", "White Rabbit", "Flamenco Rumbas"),
            harmonizedTriads = listOf("i (min)", "♭II (Maj)", "♭III (Maj)", "iv (min)", "v° (dim)", "♭VI (Maj)", "♭vii (min)")
        ),
        ScaleDefinition(
            id = "lydian",
            name = "Lydian Mode",
            category = "Modes",
            formulaText = "W - W - W - H - W - W - H",
            degrees = listOf("1", "2", "3", "♯4", "5", "6", "7"),
            intervals = listOf(0, 2, 4, 6, 7, 9, 11),
            mood = "Floating, celestial, filmic, dreamy",
            description = "Features a sharp 4th above a major scale, removing any 'grounded' gravitational pull.",
            famousSongs = listOf("The Simpsons Theme", "E.T. Flying Theme", "Flying in a Blue Dream"),
            harmonizedTriads = listOf("I (Maj)", "II (Maj)", "iii (min)", "♯iv° (dim)", "V (Maj)", "vi (min)", "vii (min)")
        ),
        ScaleDefinition(
            id = "locrian",
            name = "Locrian Mode",
            category = "Modes",
            formulaText = "H - W - W - H - W - W - W",
            degrees = listOf("1", "♭2", "♭3", "4", "♭5", "♭6", "♭7"),
            intervals = listOf(0, 1, 3, 5, 6, 8, 10),
            mood = "Chaotic, sinister, unresolved",
            description = "The 7th mode of the major scale, built on a diminished triad with a flat 5th. Used in heavy metal breakdowns.",
            famousSongs = listOf("Sad But True (riffs)", "Rush - YYZ intro", "Metal dissonances"),
            harmonizedTriads = listOf("i° (dim)", "♭II (Maj)", "♭iii (min)", "iv (min)", "♭V (Maj)", "♭VI (Maj)", "♭vii (min)")
        )
    )

    // Strumming Patterns for Different Time Signatures
    val STRUMMING_PATTERNS = listOf(
        // === 4/4 TIME SIGNATURE ===
        StrumPattern(
            id = "4_4_simple_four_beats",
            name = "Foundational 4-Beat Downstrums (D D D D)",
            timeSignature = "4/4",
            genre = "Acoustic / Beginner Universal",
            difficulty = "Beginner",
            defaultBpm = 80,
            countText = "1   &   2   &   3   &   4   &",
            description = "The first and most universal strumming rhythm in music. Solid downstrums on every quarter-note beat with relaxed wrist motion.",
            exampleSongs = listOf("Knockin' on Heaven's Door - Bob Dylan", "A Horse with No Name - America", "Boulevard of Broken Dreams (intro)"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("3", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST)
            )
        ),
        StrumPattern(
            id = "4_4_down_down_up_down_up",
            name = "5-Stroke Pop Groove (D - D U - D U)",
            timeSignature = "4/4",
            genre = "Pop / Folk / Singalong",
            difficulty = "Beginner",
            defaultBpm = 88,
            countText = "1   &   2   &   3   &   4   &",
            description = "Down - Down Up - Down Up. The iconic 5-stroke acoustic pattern without the beat 3 gap. Clean, upbeat, and natural to sing over.",
            exampleSongs = listOf("Stand By Me - Ben E. King", "Leaving On A Jet Plane - John Denver", "Have You Ever Seen The Rain - CCR", "Let It Be"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_down_down_up_down_up_down",
            name = "6-Stroke Folk Flow (D - D U D U D -)",
            timeSignature = "4/4",
            genre = "Folk Rock / Acoustic Pop",
            difficulty = "Beginner",
            defaultBpm = 94,
            countText = "1   &   2   &   3   &   4   &",
            description = "Down - Down Up - Down Up - Down. Legendary 6-stroke flow with continuous forward motion and sweet acoustic ring.",
            exampleSongs = listOf("Good Riddance (Time of Your Life) - Green Day", "Sweet Home Alabama - Lynyrd Skynyrd", "Heart of Gold - Neil Young", "Country Roads"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST)
            )
        ),
        StrumPattern(
            id = "4_4_campfire_essential",
            name = "The Campfire Essential (D - D U - D - D U)",
            timeSignature = "4/4",
            genre = "Folk / Country / Acoustic",
            difficulty = "Beginner",
            defaultBpm = 90,
            countText = "1   &   2   &   3   &   4   &",
            description = "Down on 1, Down-Up on 2, Down on 3, Down-Up on 4. The foundational rhythm powering campfire favorites and acoustic classics.",
            exampleSongs = listOf("Blowin' In The Wind - Bob Dylan", "Peaceful Easy Feeling - Eagles", "Brown Eyed Girl", "Teach Your Children"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_half_measure_verse",
            name = "Classic Verse Pickup (D - D - D - D U)",
            timeSignature = "4/4",
            genre = "Classic Rock / Pop / Country",
            difficulty = "Beginner",
            defaultBpm = 100,
            countText = "1   &   2   &   3   &   4   &",
            description = "Three solid downstrums on beats 1, 2, and 3, ending with a snappy down-up pickup on beat 4 driving into the next bar.",
            exampleSongs = listOf("Take It Easy - Eagles", "Proud Mary - CCR", "Bad Moon Rising", "Ring of Fire"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("3", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_continuous_flow_8ths",
            name = "Continuous Flow 8ths (D U D U D U D U)",
            timeSignature = "4/4",
            genre = "Upbeat Folk / Bluegrass / Pop",
            difficulty = "Beginner",
            defaultBpm = 105,
            countText = "1   &   2   &   3   &   4   &",
            description = "Continuous alternating down-up strokes like a steady pendulum. Excellent for building stamina, timing, and upbeat acoustic drive.",
            exampleSongs = listOf("Wagon Wheel - Darius Rucker", "Mrs. Robinson - Simon & Garfunkel", "I'm A Believer - The Monkees"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("2", StrumStrokeType.ACCENT_DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_syncopated_heartbeat",
            name = "Syncopated Acoustic Pop (D - - U - U D -)",
            timeSignature = "4/4",
            genre = "Modern Pop / Alt-Rock",
            difficulty = "Intermediate",
            defaultBpm = 82,
            countText = "1   &   2   &   3   &   4   &",
            description = "Down on 1, skip to the upbeat '&' of 2 and 3, ending on 4. Iconic modern pop heartbeat groove.",
            exampleSongs = listOf("Yellow - Coldplay", "Zombie - The Cranberries", "Save Tonight - Eagle-Eye Cherry", "Counting Stars"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST)
            )
        ),
        StrumPattern(
            id = "4_4_pop_punk_driving",
            name = "Pop Punk Energy Drive (D D D D D D D U)",
            timeSignature = "4/4",
            genre = "Pop Punk / Alternative Rock",
            difficulty = "Beginner",
            defaultBpm = 135,
            countText = "1   &   2   &   3   &   4   &",
            description = "Relentless aggressive downstrokes with accented snare hits on 2 and 4, snapping up on the 4-& to pivot into the next chord.",
            exampleSongs = listOf("All The Small Things - Blink-182", "When I Come Around - Green Day", "The Middle - Jimmy Eat World"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("3", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_universal_pop",
            name = "Universal Pop / Folk 8th Strum",
            timeSignature = "4/4",
            genre = "Pop / Rock / Folk",
            difficulty = "Beginner",
            defaultBpm = 86,
            countText = "1   &   2   &   3   &   4   &",
            description = "The single most famous rhythm on acoustic guitar. The missing downstroke on beat 3 gives it iconic momentum.",
            exampleSongs = listOf("Wonderwall - Oasis", "Riptide - Vance Joy", "Brown Eyed Girl - Van Morrison", "Let It Be - Beatles"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_island_strum",
            name = "The Island / Calypso Strum",
            timeSignature = "4/4",
            genre = "Acoustic / Ukulele",
            difficulty = "Beginner",
            defaultBpm = 95,
            countText = "1   &   2   &   3   &   4   &",
            description = "Beloved ukulele and beach campfire rhythm. Accented on beats 2 and 4 with an upbeat skip on the 3-&.",
            exampleSongs = listOf("Somewhere Over the Rainbow - Israel K", "I'm Yours - Jason Mraz", "Hey Soul Sister - Train"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST)
            )
        ),
        StrumPattern(
            id = "4_4_driving_rock_8ths",
            name = "Driving Rock Steady 8ths",
            timeSignature = "4/4",
            genre = "Classic Rock / Punk",
            difficulty = "Beginner",
            defaultBpm = 120,
            countText = "1   &   2   &   3   &   4   &",
            description = "All downstrokes driving the pulse with heavy accented snare beats on 2 and 4. Perfect for high energy rock.",
            exampleSongs = listOf("Blitzkrieg Bop - Ramones", "You Shook Me All Night Long - AC/DC", "Bad Moon Rising - CCR"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("2", StrumStrokeType.ACCENT_DOWN),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("3", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN),
                StrumBeat("&", StrumStrokeType.DOWN)
            )
        ),
        StrumPattern(
            id = "4_4_country_boom_chick",
            name = "Country Boom-Chicka (Alternating Bass)",
            timeSignature = "4/4",
            genre = "Country / Bluegrass",
            difficulty = "Intermediate",
            defaultBpm = 104,
            countText = "1   &   2   &   3   &   4   &",
            description = "Pick the lowest root bass string on beats 1 and 3, followed by a snappy strum on 2 and 4. The sound of Johnny Cash.",
            exampleSongs = listOf("Folsom Prison Blues - Johnny Cash", "Take Me Home, Country Roads", "Ring of Fire"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.BASS, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.BASS, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_ballad_16th",
            name = "Acoustic Ballad 16th Note Strum",
            timeSignature = "4/4",
            genre = "Pop Ballad / Modern Acoustic",
            difficulty = "Intermediate",
            defaultBpm = 72,
            countText = "1 e & a  2 e & a  3 e & a  4 e & a",
            description = "Sophisticated, fluid continuous arm motion with syncopated ghost strums. The modern Ed Sheeran & John Mayer acoustic sound.",
            exampleSongs = listOf("Perfect - Ed Sheeran", "Gravity - John Mayer", "Shallow - Lady Gaga"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("e", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("a", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("e", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("e", StrumStrokeType.UP),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.REST),
                StrumBeat("4", StrumStrokeType.DOWN),
                StrumBeat("e", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_funk_scratch",
            name = "Funk 16th Chop & Mute Groove",
            timeSignature = "4/4",
            genre = "Funk / R&B",
            difficulty = "Advanced",
            defaultBpm = 108,
            countText = "1 e & a  2 e & a  3 e & a  4 e & a",
            description = "Tight left-hand fretting mutes combined with explosive accents on the 'and' of beats. Nile Rodgers Chic rhythm.",
            exampleSongs = listOf("Get Lucky - Daft Punk", "Uptown Funk - Bruno Mars", "Le Freak - Chic"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("e", StrumStrokeType.MUTE),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.UP),
                StrumBeat("2", StrumStrokeType.MUTE),
                StrumBeat("e", StrumStrokeType.UP),
                StrumBeat("&", StrumStrokeType.ACCENT_DOWN),
                StrumBeat("a", StrumStrokeType.MUTE),
                StrumBeat("3", StrumStrokeType.DOWN, true),
                StrumBeat("e", StrumStrokeType.MUTE),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.MUTE),
                StrumBeat("e", StrumStrokeType.UP),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("a", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "4_4_reggae_skank",
            name = "Reggae / Ska Off-Beat Skank",
            timeSignature = "4/4",
            genre = "Reggae / Ska",
            difficulty = "Intermediate",
            defaultBpm = 80,
            countText = "1   &   2   &   3   &   4   &",
            description = "Dead silence on the downbeats with crisp, muted chops on the off-beat '&'. Bob Marley signature groove.",
            exampleSongs = listOf("Three Little Birds - Bob Marley", "One Love", "Santeria - Sublime"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.MUTE),
                StrumBeat("2", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.MUTE),
                StrumBeat("3", StrumStrokeType.REST, true),
                StrumBeat("&", StrumStrokeType.MUTE),
                StrumBeat("4", StrumStrokeType.REST),
                StrumBeat("&", StrumStrokeType.MUTE)
            )
        ),

        // === 3/4 WALTZ TIME SIGNATURE ===
        StrumPattern(
            id = "3_4_classic_waltz",
            name = "Classic 3/4 Acoustic Waltz",
            timeSignature = "3/4",
            genre = "Folk / Waltz",
            difficulty = "Beginner",
            defaultBpm = 110,
            countText = "1     2     3",
            description = "Heavy emphasized bass note on beat 1, followed by gentle, warm chord strums on beats 2 and 3. Boom-chick-chick.",
            exampleSongs = listOf("Piano Man - Billy Joel", "Que Sera Sera", "The Times They Are A-Changin' - Bob Dylan"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.BASS, true),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("3", StrumStrokeType.DOWN)
            )
        ),
        StrumPattern(
            id = "3_4_flowing_waltz",
            name = "Flowing Folk Waltz (8th Notes)",
            timeSignature = "3/4",
            genre = "Folk / Celtic",
            difficulty = "Intermediate",
            defaultBpm = 96,
            countText = "1   &   2   &   3   &",
            description = "Swinging continuous eighth notes in triple meter. Provides rolling, cinematic momentum to ballads.",
            exampleSongs = listOf("Iris - Goo Goo Dolls", "Rainbow Connection", "Breakaway - Kelly Clarkson"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "3_4_country_waltz",
            name = "Country 3/4 Ballad Strum",
            timeSignature = "3/4",
            genre = "Country / Bluegrass",
            difficulty = "Intermediate",
            defaultBpm = 100,
            countText = "1   &   2   &   3   &",
            description = "Bass string pick on 1, chord brush on 2, and double up/down swing brush on 3.",
            exampleSongs = listOf("Tennessee Waltz - Patti Page", "Sweet Baby James - James Taylor"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.BASS, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("3", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),

        // === 6/8 COMPOUND DUPLE TIME SIGNATURE ===
        StrumPattern(
            id = "6_8_slow_ballad",
            name = "Epic 6/8 Rock / Folk Ballad",
            timeSignature = "6/8",
            genre = "Rock / Ballad",
            difficulty = "Intermediate",
            defaultBpm = 58,
            countText = "1   2   3   4   5   6",
            description = "Two primary pulses (1 and 4) subdivided into triplets. Deep emotional sway powering legendary anthems.",
            exampleSongs = listOf("Hallelujah - Jeff Buckley", "Nothing Else Matters - Metallica", "House of the Rising Sun - Animals"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("2", StrumStrokeType.REST),
                StrumBeat("3", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("5", StrumStrokeType.DOWN),
                StrumBeat("6", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "6_8_slow_blues",
            name = "Slow Blues 6/8 Shuffle",
            timeSignature = "6/8",
            genre = "Blues / Soul",
            difficulty = "Intermediate",
            defaultBpm = 52,
            countText = "1   2   3   4   5   6",
            description = "Steady downstrokes on all 6 beats with deep expressive accents on 1 and 4. Pure B.B. King and Stevie Ray Vaughan.",
            exampleSongs = listOf("Texas Flood - Stevie Ray Vaughan", "Red House - Jimi Hendrix", "I'd Rather Go Blind - Etta James"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("3", StrumStrokeType.DOWN),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("5", StrumStrokeType.DOWN),
                StrumBeat("6", StrumStrokeType.DOWN)
            )
        ),
        StrumPattern(
            id = "6_8_celtic_jig",
            name = "Irish Celtic 6/8 Jig",
            timeSignature = "6/8",
            genre = "Celtic / Folk",
            difficulty = "Advanced",
            defaultBpm = 116,
            countText = "1   2   3   4   5   6",
            description = "Fast galloping pulse with D-U-D-D-U-D strokes powering lively traditional dances and acoustic celebrations.",
            exampleSongs = listOf("The Irish Rover - Dubliners", "Whiskey in the Jar (traditional)", "Toss the Feathers"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("2", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.DOWN),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("5", StrumStrokeType.UP),
                StrumBeat("6", StrumStrokeType.DOWN)
            )
        ),

        // === 2/4 MARCH & POLKA TIME SIGNATURE ===
        StrumPattern(
            id = "2_4_simple_folk_march",
            name = "Simple 2/4 Folk March (D - D U)",
            timeSignature = "2/4",
            genre = "Folk / Traditional",
            difficulty = "Beginner",
            defaultBpm = 96,
            countText = "1   &   2   &",
            description = "Solid downstroke on beat 1 followed by a lively down-up on beat 2. Crisp, rhythmic, and traditional.",
            exampleSongs = listOf("I Walk the Line - Johnny Cash", "You Are My Sunshine", "Hey Good Lookin' - Hank Williams"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "2_4_country_polka",
            name = "Two-Step Country March",
            timeSignature = "2/4",
            genre = "Country / Bluegrass",
            difficulty = "Beginner",
            defaultBpm = 108,
            countText = "1   &   2   &",
            description = "Compact 2-beat measure with alternating bass on the beats and crisp upbeat strums.",
            exampleSongs = listOf("I Walk the Line - Johnny Cash", "Wagon Wheel - Old Crow Medicine Show"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.BASS, true),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("2", StrumStrokeType.DOWN, true),
                StrumBeat("&", StrumStrokeType.UP)
            )
        ),
        StrumPattern(
            id = "2_4_punk_fast",
            name = "Fast 2/4 Punk Attack",
            timeSignature = "2/4",
            genre = "Punk / Alternative",
            difficulty = "Intermediate",
            defaultBpm = 160,
            countText = "1   &   2   &",
            description = "Blistering rapid-fire downstrokes driving fast 2-step tempos with maximum aggression.",
            exampleSongs = listOf("All the Small Things - Blink-182", "American Idiot - Green Day"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN),
                StrumBeat("2", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.DOWN)
            )
        ),

        // === 12/8 SLOW BLUES & DOO-WOP TIME SIGNATURE ===
        StrumPattern(
            id = "12_8_doo_wop",
            name = "50s Doo-Wop & Soul 12/8",
            timeSignature = "12/8",
            genre = "Soul / 50s Rock",
            difficulty = "Intermediate",
            defaultBpm = 64,
            countText = "1 2 3  2 2 3  3 2 3  4 2 3",
            description = "Four main beats with triplet subdivisions (12 total eighth notes per measure). The classic sound of 1950s love ballads.",
            exampleSongs = listOf("Earth Angel - Penguins", "Stand By Me (Ballad variation)", "Unchained Melody"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("2", StrumStrokeType.REST),
                StrumBeat("3", StrumStrokeType.UP),
                StrumBeat("4", StrumStrokeType.DOWN, true),
                StrumBeat("5", StrumStrokeType.REST),
                StrumBeat("6", StrumStrokeType.UP),
                StrumBeat("7", StrumStrokeType.DOWN, true),
                StrumBeat("8", StrumStrokeType.REST),
                StrumBeat("9", StrumStrokeType.UP),
                StrumBeat("10", StrumStrokeType.DOWN, true),
                StrumBeat("11", StrumStrokeType.REST),
                StrumBeat("12", StrumStrokeType.UP)
            )
        ),

        // === 5/4 ODD METER TIME SIGNATURE ===
        StrumPattern(
            id = "5_4_take_five",
            name = "Take Five 5/4 (3 + 2 Grouping)",
            timeSignature = "5/4",
            genre = "Jazz / Progressive Rock",
            difficulty = "Advanced",
            defaultBpm = 130,
            countText = "1   &   2   &   3   |   4   &   5   &",
            description = "Odd meter rhythm split into a triplet groove (1-2-3) followed by a duple groove (4-5). Dave Brubeck masterpiece.",
            exampleSongs = listOf("Take Five - Dave Brubeck", "Mission Impossible Theme", "River Man - Nick Drake"),
            beats = listOf(
                StrumBeat("1", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("2", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP),
                StrumBeat("3", StrumStrokeType.DOWN),
                StrumBeat("4", StrumStrokeType.ACCENT_DOWN, true),
                StrumBeat("&", StrumStrokeType.REST),
                StrumBeat("5", StrumStrokeType.DOWN),
                StrumBeat("&", StrumStrokeType.UP)
            )
        )
    )

    /**
     * Compute exact scale notes given root and scale definition
     */
    fun getScaleNotes(root: String, scale: ScaleDefinition, useFlats: Boolean = false): List<String> {
        val noteList = if (useFlats) NOTES_FLAT else NOTES_SHARP
        var rootIdx = NOTES_SHARP.indexOf(root)
        if (rootIdx == -1) rootIdx = NOTES_FLAT.indexOf(root)
        if (rootIdx == -1) rootIdx = 0

        return scale.intervals.map { interval ->
            val pitch = (rootIdx + interval) % 12
            noteList[pitch]
        }
    }

    /**
     * Computes the notes on each fret for guitar, bass, or ukulele
     * Returns a matrix of (stringIndex, fretIndex) -> ScaleNoteInfo?
     */
    data class FretNoteInfo(
        val noteName: String,
        val fret: Int,
        val stringIdx: Int,
        val isRoot: Boolean,
        val scaleDegree: String
    )

    fun getFretboardScaleNotes(
        root: String,
        scale: ScaleDefinition,
        instrument: LearnInstrument,
        maxFrets: Int = 15,
        useFlats: Boolean = false
    ): List<FretNoteInfo> {
        val scaleNotes = getScaleNotes(root, scale, useFlats)
        val noteList = if (useFlats) NOTES_FLAT else NOTES_SHARP

        var rootIdx = NOTES_SHARP.indexOf(root)
        if (rootIdx == -1) rootIdx = NOTES_FLAT.indexOf(root)
        if (rootIdx == -1) rootIdx = 0

        val results = mutableListOf<FretNoteInfo>()

        instrument.openMidiPitches.forEachIndexed { sIdx, openPitch ->
            for (fret in 0..maxFrets) {
                val currentPitch = (openPitch + fret) % 12
                val currentNote = noteList[currentPitch]

                val noteIndexInScale = scaleNotes.indexOf(currentNote)
                if (noteIndexInScale != -1) {
                    val degree = scale.degrees.getOrElse(noteIndexInScale) { (noteIndexInScale + 1).toString() }
                    val isRoot = (currentPitch == rootIdx)
                    results.add(FretNoteInfo(currentNote, fret, sIdx, isRoot, degree))
                }
            }
        }

        return results
    }

    /**
     * Pre-calculated comprehensive chord voicings across the neck for any given chord and instrument.
     */
    fun getAllChordVoicings(chordName: String, instrument: LearnInstrument): List<ChordShape> {
        return when (instrument) {
            LearnInstrument.GUITAR -> TabTransposer.getGuitarChordShapes(chordName)
            LearnInstrument.UKULELE -> TabTransposer.getUkuleleChordShapes(chordName)
            LearnInstrument.BASS -> getBassChordArpeggios(chordName)
            LearnInstrument.PIANO -> emptyList() // Rendered via PianoKeyboardVisualizer
        }
    }

    private fun getBassChordArpeggios(chord: String): List<ChordShape> {
        val root = TabTransposer.extractRootNote(chord)
        var rootIdx = NOTES_SHARP.indexOf(root)
        if (rootIdx == -1) rootIdx = NOTES_FLAT.indexOf(root)
        if (rootIdx == -1) rootIdx = 0

        val isMinor = chord.contains("m") && !chord.contains("maj")
        val thirdOffset = if (isMinor) 3 else 4

        // Bass 4 strings: E (fret rootIdx), A, D, G
        val eFret = (rootIdx - 4 + 12) % 12
        val baseFret = eFret.coerceAtLeast(1)

        val rootFret = if (eFret == 0) 0 else eFret
        val thirdFret = (rootFret + thirdOffset)
        val fifthFret = (rootFret + 7)

        return listOf(
            ChordShape(chord, "Bass", listOf(rootFret, (rootFret + 7) % 12, (rootFret + thirdOffset) % 12, -1), listOf(1, 4, 2, 0), baseFret = baseFret, voicingName = "Root-5th-3rd Shape"),
            ChordShape(chord, "Bass", listOf(rootFret, -1, -1, -1), listOf(1, 0, 0, 0), baseFret = baseFret, voicingName = "Root Foundation")
        )
    }
}
