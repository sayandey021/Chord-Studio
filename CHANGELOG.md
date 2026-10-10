# Changelog

All notable changes to **Chord Studio** are documented in this file.

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
