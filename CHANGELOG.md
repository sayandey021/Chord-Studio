# Changelog

All notable changes to **Chord Studio** are documented in this file.

---
## [0.9.2] - 2026-10-14

### Fixed & Enhanced
- **Modern Native Desktop Audio File Picker**:
  - Restored the modern Windows 11 / Windows 10 Common Item Dialog via FileKit (`FileKit.pickFile`) with dynamic active window binding (`FileKitPlatformSettings(parentWindow)`), ensuring the modern explorer UI opens seamlessly without modal window lockup.
  - Enhanced "Analyze & Open in Studio Workstation" button: if clicked before an audio file is selected, it automatically prompts the modern file picker and seamlessly begins analysis and loads the track into the studio once picked.
- **Universal Multi-Format Audio Decoding (MP3, WAV, FLAC, M4A, AAC, OGG, OPUS)**:
  - Upgraded [`AudioAnalysisEngine.loadAudioFile`](file:///c:/Users/sayan/Documents/GitHub/Chord%20Studio/desktopApp/src/main/kotlin/com/gbros/tabslite/utilities/AudioAnalysisEngine.kt) with an on-device FFmpeg pipe decoding engine, supporting FLAC, M4A, AAC, OGG, OPUS, WMA, and ALAC files directly into 16kHz float PCM samples.
  - Added automatic cross-fallback between standard Java audio, pure Java JLayer MP3 decoder, and FFmpeg for corrupted or high-bitrate files.

### Changed & Streamlined
- **Consolidated on Pure High-Performance Multi-Core CPU Engine**:
  - Completely removed the GPU acceleration subsystem (`HardwareAccelerationManager`, DirectML provider binding, and GPU driver query routines).
  - Benchmarks proved CPU execution with AVX2 vector SIMD instructions is significantly faster (~2.5x speedup) and lower latency than GPU shader dispatch for real-time audio and speech inference on desktop hardware.
  - Simplified `OnnxChordModel` to execute exclusively via optimized CPU execution provider (`OrtSession` with multi-core intra-op threads).
  - Simplified `LyricsExtractor` and Whisper speech-to-text to run with `-ng` (pure CPU mode) across all available cores (`4` to `12` threads), eliminating GPU shader/driver dispatch overhead.
  - Removed GPU hardware cards, device toggles, and DirectML settings from **Settings → AI Studio & Audio**.
- **Universal Inter-Script & Romanization Converter ("Letters Only")**:
  - Replaced the single-directional romanize menu with a comprehensive, multi-directional **Universal Script Converter** (`🔤 Script Converter`):
    - Transliterates characters/alphabets only while preserving phonetics, words, rhyme schemes, and LRC timestamp tags (`[mm:ss.xx]`) with zero semantic machine translation.
    - Added direct inter-script conversions between Indian languages:
      - Hindi (Devanagari) ↔ Bengali (বাংলা)
      - Hindi (Devanagari) ↔ Punjabi (ਗੁਰਮੁਖੀ)
      - Hindi (Devanagari) ↔ Gujarati (ગુજરાતી)
      - Bengali (বাংলা) ↔ Hindi (देवनागरी) / Punjabi (ਗੁਰਮੁਖੀ)
      - Urdu (اردو) ↔ Hindi (देवनागरी) / Bengali (বাংলা) / Roman English
      - Roman / English → Hindi (देवनागरी) / Bengali (বাংলা) / Urdu (اردو) / Punjabi (ਗੁਰਮੁਖੀ) / Gujarati (ગુજરાતી)
    - Added **Context-Aware Dynamic Conversion Menus**:
      - Auto-detects the dominant script of lyrics (Hindi, Bengali, Urdu, English/Roman, Punjabi, Gujarati, Tamil, Telugu).
      - Dynamically filters the quick conversion dropdown to show **only valid, supported target options for that detected script** instead of a crowded static list.
    - Added dedicated **Universal Script Converter Studio** modal dialog:
      - Interactive `From Script` and `To Script` pickers supporting Auto-Detect, Devanagari, Bengali, Urdu, Punjabi, Gujarati, Tamil, Telugu, and Roman English.
      - Quick preset chips and a instant script swap button (`⇄`).
      - Real-time side-by-side live preview comparing original lyric lines with converted letter outputs prior to applying.
    - Added **Universal Indic Script Sanitizer & Broken Text Healer (`sanitizeIndicScript`)**:
      - Automatically detects and heals broken font glyphs (the dotted circle `◌` issue) across Hindi (Devanagari), Bengali (Bangla), and Punjabi (Gurmukhi).
      - Resolves invalid independent vowels attached to dependent vowel signs (e.g. `आ◌ीस` → `आईस`, `आ◌ेक` → `एक`, `आ◌ा` → `आया`).
      - Resolves stacked or consecutive matras (e.g. `ली◌ा` → `लिया`, `रा◌ीस` → `राईस`).
      - Cleans invalid nuktas on unsupported base consonants (e.g. `द़ / দ় / ਦ਼` in `बोद्र◌ा`, `বোদ়◌া`, `ਬੋਦ਼◌ਾ` → `बोदा / বোদা / ਬੋਦਾ`).
      - Re-maps orphan matras at word boundaries to proper independent vowels.
    - Updated Indic & Perso-Arabic script detection banners in both `Live Synced` and `Edit Lyrics` tabs to offer instant access to script conversion.
      - `Pinglish (Punjabi / Gurmukhi → Roman)`: e.g. ਪੰਜਾਬੀ → Punjabi.
      - `Tanglish (Tamil → Roman)`: e.g. தமிழ் → Tamil.
    - Added an intelligent detection banner alerting when non-Latin scripts are present with a 1-click `Auto Romanize` shortcut.
    - Updated `PhoneticTransliterator` to process line-by-line, preserving song line breaks and `[mm:ss.xx]` timestamped LRC tags.
- **AI Transcribe UI Streamlining & Clean Status Presentation**:
  - Removed Indic script detection banner from the **Live Synced** lyrics tab, leaving the viewer sleek and distraction-free.
  - Horizontally and vertically centered the AI audio transcription progress spinner and live status text across the full container width.
  - Replaced all visible "Whisper" branding in the UI with "AI" (e.g. `AI processing audio...`, `AI Model`, `Extract Lyrics from Audio with AI`, and `AI Vocal Transcriber (BETA)`).
  - Consolidated duplicate success cards: completely eliminated redundant cards and moved all functionality into the dedicated completion section below the button.
- **Enhanced Language Auto-Detection & Bengali Script Accuracy**:
  - Upgraded [`LyricsExtractor.detectAudioLanguage`](file:///c:/Users/sayan/Documents/GitHub/Chord%20Studio/desktopApp/src/main/kotlin/com/gbros/tabslite/utilities/LyricsExtractor.kt) with multi-chunk acoustic sampling, energy-ranked offsets, and language probability voting across tracks.
  - Added user hint title and artist ingestion to prevent misclassifications before transcription begins.
  - Fixed Whisper's statistical training bias towards Hindi over Bengali by adding intelligent Bengali phoneme boosting and context-aware lexical checking.
  - Enhanced [`PhoneticTransliterator.isBengaliInDevanagari`](file:///c:/Users/sayan/Documents/GitHub/Chord%20Studio/desktopApp/src/main/kotlin/com/gbros/tabslite/utilities/PhoneticTransliterator.kt) with comprehensive Bengali morphological markers (verb inflections like `-bo`, `-te`, `-echhe`, `-echhilo`, and plural/genitive forms) to automatically detect when a Bengali song was transcribed with Devanagari/Hindi letters, automatically converting it into authentic Bengali script (`বাংলা`).
  - Corrected Devanagari `य` mapping in `devanagariToBengali` so intervocalic and post-vowel positions correctly convert to antastha `য়` (e.g. `তোমায়`, `আমায়`).

---

## [0.9.1] - 2026-10-12

### Added
- **Hardware Acceleration & Universal GPU Auto-Detection**:
  - Intelligent hardware detection engine (`HardwareAccelerationManager`) automatically discovering installed graphics cards across **NVIDIA**, **AMD**, and **Intel**.
  - Registry-based sub-30ms hardware interrogation reading display adapter descriptions, VRAM size, driver versions, and physical hardware characteristics while filtering out virtual/remote display adapters (Parsec, Spacedesk, Microsoft Basic Display Adapter).
  - Universal DirectML execution provider integration into `OnnxChordModel` powering deep learning BTC Transformer chord recognition with automatic fallback to multi-core CPU.
  - Multi-backend acceleration support in `LyricsExtractor` and Whisper speech-to-text:
    - Automatically enables GPU compute or applies CPU multi-threading.
    - Honors OpenVINO Intel GPU acceleration (`-oved GPU`) for Intel Arc and Iris Xe.
    - PyTorch device auto-selection in `transcribe_lyrics.py` across CUDA, DirectML, Intel XPU, and CPU.
  - Dedicated **Hardware Acceleration & GPU Auto-Detection** card in **Settings → AI Studio & Audio**:
    - Live hardware status pill (`⚡ GPU DIRECTML` vs `🖥 CPU VECTOR`).
    - Physical hardware cards showing GPU vendor badges (NVIDIA Lime, AMD Crimson, Intel Neon Blue), VRAM capacity, driver versions, and primary adapter tags.
    - CPU architecture breakdown (logical cores, architecture, JVM memory heap).
    - 3-Way Hardware Execution Mode switcher: `Auto Detect (Recommended)`, `DirectML GPU Acceleration`, and `CPU Multi-Core (Safe Mode)` with instant on-the-fly model re-binding.
    - Fixed GPU mode retention ensuring that when `Force GPU` or `Auto Detect` is selected with an AMD Radeon, NVIDIA GeForce, or Intel GPU present, the active engine state stays on the primary GPU (`⚡ Active Engine`) and routes all AI audio processing accordingly.
    - Pixel-perfect vertical centering and icon alignment across all badges and pill tags (`[⚡ GPU DIRECTML]`, `[🖥 CPU VECTOR]`, `[PRIMARY]`, `[Active Engine]`, `[AMD]`, `[CPU]`, and language pills).
- **Multilingual Whisper Lyrics Transcription & Hybrid Language Engine**:
  - Expanded Whisper speech-to-text language catalog in AI Lyrics Studio to 38+ supported languages spanning 6 categories: **Hybrids & Code-Mixed**, **Indian & South Asian**, **Popular Global**, **European**, **East Asian**, and **Middle Eastern**.
  - First-class support for Romanized **Hybrid** code-mixed languages: **Hinglish** (Hindi in Roman script for Bollywood/indie pop) and **Benglish** (Bangla in Roman script for Rabindra sangeet/Bangla rock), as well as **Tanglish** (Tamil in Roman script) and **Pinglish** (Punjabi in Roman script).
  - Dual-layer Romanization pipeline ensuring clean Latin sing-along guitar chord charts:
    1. Whisper model conditioning via English target language with phonetic Roman seed prompts.
    2. Pure Kotlin on-device transliteration engine ([PhoneticTransliterator.kt](file:///c:/Users/sayan/Documents/GitHub/Chord%20Studio/desktopApp/src/main/kotlin/com/gbros/tabslite/utilities/PhoneticTransliterator.kt)) mapping any residual Devanagari or Bengali script characters directly into natural Roman phonetics.
  - Searchable **Language Picker Modal** with live search query filtering, category chips, hybrid badges, and quick-pick pills (`Auto Detect`, `Hinglish ★`, `Benglish ★`, `English`, `Hindi`, `Bengali`, `Punjabi`, `+ More...`) directly in the Lyrics Studio modal.
- **Round Speaker Sound Preview Button**:
  - Added on-demand circular audio preview speaker button to Chord Inspector cards and Studio Chord cards, allowing users to listen to chords only when explicitly clicking the preview button.
- **Vocal Section Language Auto-Detection**:
  - Enhanced language detection sampling high-energy vocal segments past intro instrumentals to correctly resolve languages for singing tracks.
- **Native Acoustic Model Routing & Transliteration for Hybrid Languages**:
  - Fixed issue where selecting hybrid languages (**Hinglish ★**, **Benglish ★**, **Pinglish ★**, **Tanglish ★**) failed or returned garbled hallucinations.
  - Resolved root cause: Whisper was previously invoked with `-l en` and an initial prompt, causing the English decoder to fail on Hindi/Bengali vocal phonemes.
  - Now routes audio recognition through Whisper's native acoustic models (`hi` for Hinglish, `bn` for Benglish, `pa` for Pinglish, `ta` for Tanglish) for accurate speech recognition, and automatically pipes the resulting tokens through `PhoneticTransliterator` to produce Romanized sing-along text.
  - Added Gurmukhi (Punjabi -> Pinglish) and Tamil (Tamil -> Tanglish) transliterators with native numeral support (0-9).
  - Added 1-click **"★ Romanize (Hinglish/Benglish)"** action banner and button directly on the **Live Synced** and **Edit Lyrics** tabs whenever non-Latin Indic script is detected.

---

## [0.9.0] - 2026-10-11

### Added
- **Whisper AI Model Downloader & Manager (Hardware-Adaptive)**:
  - Added an in-app AI Model Manager in **Settings → Audio & AI** enabling users to download, inspect, select as default, or delete OpenAI Whisper speech-to-text models (`Base`, `Small`, `Medium`, `Turbo`, `Large v3`) based on their PC hardware specs and accuracy needs.
  - Complete hardware spec recommendations displayed directly on each model card (RAM required, recommended CPU/GPU tier, model size, and multilingual accuracy rating).
  - Background streaming downloader with automatic redirect handling (Hugging Face CDN CloudFront redirects), real-time progress bar, live download speed indicator (MB/s), downloaded/total MB counters, and instant cancellation.
  - One-click active/default model selection (`preferredWhisperModel`) saved to persistent settings.
  - Disk space reclamation with one-click model deletion.
  - Automatic dynamic model detection in the AI Lyrics Studio dialog, automatically listing all installed models and auto-selecting the user's active model.
- **Learn & Theory Studio**:
  - **Chord Encyclopedia**: Master chord dictionary across Guitar, Ukulele, Bass, and Piano with shape voicings and notes.
  - **Scales & Modes Explorer**: Fretboard patterns, scale degrees, and note overlays for 18 core scales and modes.
  - **Strumming Patterns & Rhythm Lab**: Synchronized metronome click engine with animated cursor, tempo slider, stroke types (down, up, accent, bass, mute), and technique tips.
    - Added essential universal acoustic patterns including Foundational 4-Beat Downstrums (`D D D D`), 5-Stroke Pop Groove (`D - D U - D U`), 6-Stroke Folk Flow (`D - D U D U D -`), The Campfire Essential (`D - D U - D - D U`), Classic Verse Pickup (`D - D - D - D U`), Continuous Flow 8ths (`D U D U D U D U`), Syncopated Acoustic Pop (`D - - U - U D -`), Pop Punk Energy Drive (`D D D D D D D U`), and Simple 2/4 Folk March (`D - D U`).
  - Global audio stop controls with real-time audio playback indicators.
- **Multi-Tone Acoustic & Electric Sound Engine**:
  - Real sampled audio tones for Guitar, Ukulele, Bass, Piano, Banjo, Violin, and Mandolin on fret and note clicks.
  - 5 authentic acoustic piano profiles (Concert Grand, Bright Acoustic, Warm Studio, Vintage Upright, Electric Grand).
  - 6 guitar sound options for rhythm strumming: Acoustic Steel, Acoustic Nylon, Clean Electric, Crunch Rock Electric, Warm Jazz Electric, and 12-String Shimmer.
  - Natural human strumming brush sweeps (2–3ms micro-stagger across strings) for authentic acoustic and electric plectrum attacks.
  - Live on-the-fly guitar tone switching without interrupting strumming loop playback.
- **Universal Desktop Horizontal Scrolling System**:
  - Mouse-wheel horizontal redirection modifier (`Modifier.horizontalMouseWheelScroll`) converting vertical wheel scrolls to smooth horizontal motion while consuming pointer events to protect parent vertical scroll containers.
  - `HorizontalScrollableFilterBar` with persistent left `<` and right `>` navigation chevron buttons, smooth `animateScrollBy` navigation, edge auto-dimming, and auto-scroll to selected items.
  - Applied across all tabs: Scale selector pills, Chord quality pills, Rhythm patterns, Time signature chips, Tuner category & preset chips, and Tab Viewer chords bar.
- **Chromatic Tuner Natural Audio & Mandolin Presets**:
  - Replaced synthetic sine-wave tone generator with natural sampled instrument tones.
  - Added dedicated Mandolin presets: Mandolin Standard (GDAE) and Mandolin Open G (GDGD).
- **Instant Hover Chord Diagram Previews in Tab Viewer**:
  - Hovering mouse cursor directly over any chord markup (`[ch]...[/ch]`) above lyrics instantly displays a floating high-contrast chord diagram card with fretboard grid, string notes, base fret, finger positions, and voicing details.
  - Sub-pixel bounding box hit-testing with anti-flicker hysteresis and 140ms debounce to prevent flashing/glitching when moving the cursor across chord characters.
  - Guaranteed air-gap vertical positioning (strictly above or below the chord line) preventing the card from overlapping the cursor and triggering pointer occlusion loops.
  - Kept the top song chords bar as clean click-to-inspect buttons without disruptive hover popups.
  - Clicking any chord or hover card opens the full interactive multi-voicing Chord Inspector dialog.
  - Precision string coordinate alignment locking top open/mute indicators, vertical string wires, finger dots, and bottom string tuning letters (`E A D G B e`) to the exact same vertical center line.
  - Base fret indicators (`Xfr`) positioned in the left margin without displacing the fretboard grid.
  - Eliminated vertical clipping on hover cards by removing rigid container bounds and allowing natural wrap content height so bottom tuning letters and voicing names render with full clarity.
  - Diagrams adapt dynamically to the user's preferred instrument setting (Guitar or Ukulele).

### Changed
- **Interactive Chord Shape Audio Previewing**:
  - Automatically plays authentic chord strum sounds when opening the Chord Inspector dialog or switching between voicings and shapes (`<` / `>` pagination and dot indicators) across Guitar, Ukulele, and Piano.
  - Added an audio preview speaker button next to the chord title and made the "Voicing X of Y" label clickable with a volume icon to replay the chord shape sound at any time.
  - Integrated shape audio previews into the Learn & Theory Studio Chord Encyclopedia so navigating through chord shapes immediately plays the voicing.
- **Studio Audio Pitch Shift Engine (Lag / Stutter Elimination)**:
  - Replaced legacy naive fixed-window granular looping in `StudioAudioPlayer` with a high-fidelity SOLA (Synchronized Overlap-Add) audio engine.
  - Implemented real-time AMDF (Average Magnitude Difference Function) waveform similarity cross-correlation to align grain phase peaks prior to overlap.
  - Added smooth cosine crossfade blending across overlapping audio grains, completely eliminating the repeating echo, comb-filter flutter, and lagging artifact when shifting pitch up or down.
  - Retained direct lossless pass-through when pitch shift is 0 and speed is 1.0x.
- **Persistent AI Studio Session Across Navigation Tabs**:
  - Encapsulated AI Audio to Tab workstation state (`AiAudioStudioState`) at the scaffold level, ensuring analyzed audio tracks, waveforms, extracted chords, lyrics, capo/transpose settings, and playback position remain fully preserved when switching to Search, Learn Hub, Tuner, or Library tabs and back.
  - Pauses audio playback cleanly on tab switch to avoid background sound bleed, and seamlessly retains playback position and audio buffer for instant resumption upon return.
  - Added a dedicated "New Song" action button in the workstation top bar to allow users to intentionally clear and start a fresh session at any time.
- **Immediate Note Silencing / Monophonic Note Switching**:
  - Pressing any new note on the fretboard, keyboard, scale badges, or tuner strings immediately silences and pauses previously playing notes to eliminate sound overlap and hanging drones.
- **Short Sound Durations for Violin & Mandolin**:
  - Reduced Violin tone duration from 2.8s to a crisp ~800ms bowed stroke.
  - Configured Mandolin with authentic ~650ms pizzicato string pluck.
- **UI & Typography Alignment**:
  - Precision vertical centering for navigation sidebar items (`Search & Explore`, `Learn Hub`, `Favorites`, `Playlists`, `Recently Played`, `AI Audio to Tab`, `Chromatic Tuner`, `Create Custom Tab`, `Settings`).
  - Precision vertical centering for Settings category tabs (`Appearance & Theme`, `Tab Viewer & Playing`, `AI Studio & Audio`, `Data & Storage`, `About & Info`) with optical icon-to-text midpoint locking.
  - Implemented `LineHeightStyle` vertical centering and trimmed font ascent/descent leading to lock label baselines and cap-heights to the exact optical midpoint of the leading icons.
  - Aligned notification badges to the vertical midpoint of each navigation item row.
  - Dedicated Non-Overlapping Scrollbar for Strumming Patterns: Embedded the Available Rhythms list and interactive workspace columns in horizontal row containers with dedicated air-gapped `VerticalScrollbar` tracks so scrollbars never overlap rhythm cards, badges, or text.
  - Removed redundant top Stop Audio button in Learn Hub header, consolidating controls into the active playback deck.
  - Strumming Diagram Beat Cards: Fixed stroke label wrapping and vertical clipping (concise single-line tags like 'Rest', 'Accent', 'Down', 'Up' with dedicated height containers) and aligned all stroke symbols and playheads on a uniform horizontal baseline.
  - Strumming Legend Indicator Alignment: Optically aligned the Rest circle dot with the vertical midpoint of the "Rest" text label (balanced size to 5dp and centered with font cap-height/baseline).
  - Fixed clipping on scale notes and chords badges.
  - Aligned all buttons, text labels, and category badges with pixel-perfect vertical centering.
- **Responsive Multi-Window Size Optimization**:
  - Dynamically adapts Learn & Theory Studio layouts across compact window sizes, split-screen snap (half-screen), laptop displays (720p/768p), and high-res monitors.
  - Added dedicated vertical scrolling to the right columns of Chord Encyclopedia and Strumming Patterns, ensuring fretboard diagrams, practice tips, and theory cards are fully visible without bottom-edge cutoff.
  - Fixed "Play Rhythm Loop" button collapsing into a vertical character bar by enforcing minimum widths, proper weight distribution, and single-line constraints.
  - Restructured overcrowded control ribbons into clean two-tier rows across Chords and Scales labs, preventing right-edge truncation of the "Play Scale" and "Arpeggiate" buttons.
  - Integrated horizontal scrolling for playback chord selection and guitar tone chips in the rhythm lab.

---

## [0.8.0] - 2026-10-10

### Added
- **Studio PDF Export Engine**:
  - Export transposed tabs and chords directly to printable A4 PDF documents.
  - Branded PDF header with vector Chord Studio logo, artist, song, version, capo, tuning, and difficulty.
  - Page-numbered footer with embedded Chord Studio icon and clean divider lines.
  - Automated page breaking with chord line formatting preservation.
- **High-DPI Multi-Tier Icon System**:
  - Crisp, anti-aliased icon rendering at all UI scales (`app_icon_48`, `app_icon_72`, `app_icon_128`).
  - Windows `.ico` container with full multi-resolution mipmaps (16px to 256px).

### Changed
- Refined all badge, pill, and rating alignments for pixel-perfect vertical centering.
- Optimized taskbar tooltip and Windows jump list naming to "Chord Studio".
- Updated build and packaging metadata with SaayanSoft publisher credentials.

---

## [0.7.0] - 2026-10-09

### Added
- **Windows 11 Native Aesthetic & Custom Title Bar**:
  - Frameless modern window title bar with double-click maximize/restore and smooth dragging.
  - Custom dark-themed window control buttons (Minimize, Maximize/Restore, Close) with hover feedback.
  - Windows 11 DWM dark mode integration via JNA.
- **Non-Overlapping Scrollbar Layout**:
  - Redesigned vertical scrollbars across Settings, Browse, Search, and Viewer screens to sit in dedicated gutters.
  - Scrollbars now begin below header sections and avoid overlapping content.
- **Pinned Chords Toolbar**:
  - Tab viewer pinned chords bar with one-click guitar fingering and piano chord shapes.

### Changed
- Upgraded UI typography and contrast with curated HSL dark palette.
- Enhanced sidebar collapse/expand animations with smooth width transitions.

---

## [0.6.0] - 2026-10-08

### Added
- **AI Audio & Lyrics Studio**:
  - On-device neural network chord recognition powered by ONNX Runtime (`btc.onnx` & `chordnet.onnx`).
  - Real-time subharmonic Constant-Q Transform (CQT) audio feature extractor.
  - Local AI lyrics transcription and timestamp synchronization with Whisper AI.
  - Interactive multi-track waveform player with synchronized audio playback and chord overlays.
  - Model selector supporting BTC, ChordNet, and Hybrid recognition architectures.
- **Audio Device Management**:
  - Input device selector for studio recording and live analysis.
  - Buffer size and sample rate configuration (16 kHz / 44.1 kHz / 48 kHz).

---

## [0.5.0] - 2026-10-06

### Added
- **Compose Multiplatform Desktop Support**:
  - Full desktop client for Windows, macOS, and Linux powered by JetBrains Compose Multiplatform & Skiko (60fps hardware accelerated).
  - Multi-window navigation system with keyboard shortcuts (`Ctrl+1` through `Ctrl+5`).
- **Precision Chromatic Guitar & Instrument Tuner**:
  - Real-time microphone audio input analysis with YIN algorithm & autocorrelation pitch detection.
  - Multiple instrument presets: Standard Guitar, Drop D, DADGAD, Open G, Bass, Ukulele, and Cello.
  - Visual needle meter with cents offset, target frequency, and in-tune lock indicator.
- **Desktop Database Layer**:
  - High-performance SQLite local database (`sqlite-jdbc`) for desktop offline caching.

---

## [0.4.0] - 2026-10-02

### Added
- **Playlist & Collection Manager**:
  - Create and manage custom song playlists.
  - Reorder songs within playlists with drag-and-drop support.
  - Quick-add tabs to playlists directly from the Tab Viewer.
- **Viewing History & Recently Played**:
  - Automatic tracking of recently viewed tabs with timestamp sorting.
  - One-click history clearing and privacy controls in Settings.
- **Database Backup & Migration**:
  - Export entire library (favorites, playlists, history) to JSON backup.
  - Native file picker import with schema validation and conflict resolution.

### Changed
- Refactored settings view into categorized tabs (Appearance, Tab Viewer, Audio & AI, Data & Storage, About).

---

## [0.3.0] - 2026-09-25

### Added
- **Interactive Chord Inspector**:
  - Tap any chord in song sheets to inspect fingering diagrams.
  - Dual visualization: Guitar fretboard diagrams and 61-key Piano chord keys.
  - Variations selector for major, minor, 7th, maj7, min7, dim, aug, and suspended chords.
- **Favorites System & Offline Caching**:
  - One-tap favorite toggling with instant local caching for offline viewing.
  - Sort favorites by Date Added, Song Title, Artist Name, or Rating.
- **Smart Auto-Scroll Engine**:
  - Smooth vertical auto-scrolling with adjustable speed slider (0.5x to 3.0x).
  - Floating playback controls with pause on tap.

---

## [0.2.0] - 2026-09-15

### Added
- **Core Tablature & Chords Viewer**:
  - Monospace tab sheet rendering with formatted chord anchors.
  - Full tonality transposition engine (+12 / -12 semitones) with intelligent enharmonic sharp/flat conversion.
  - Capo indicator with automatic transposed fingering calculation.
- **Search & Catalog Integration**:
  - Fast search across over 1,000,000 community guitar, ukulele, and bass tabs.
  - Filtering by tab type (Chords, Tab, Bass, Ukulele) and difficulty rating.
  - Popular and trending tab discovery curated by genre categories.
- **Cross-Platform Architecture**:
  - Kotlin Multiplatform foundation for shared data models, database layer, and network connection.
