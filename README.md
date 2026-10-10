<div align="center">

# Chord Studio

**Modern, Hardware-Accelerated Guitar Tablature, Chords & AI Audio Studio**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.3-4285F4.svg?style=flat-square&logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux%20%7C%20Android-blue.svg?style=flat-square)](#)
[![License](https://img.shields.io/badge/License-GPLv3-green.svg?style=flat-square)](LICENSE)

*Chord Studio is a high-performance cross-platform music tablature viewer, chord progression analyzer, chromatic instrument tuner, and on-device AI transcription suite.*

[**📥 Get it on Microsoft Store**](https://apps.microsoft.com/detail/9P71JGK9JB2Q)

</div>

---

---

## Key Features

### 🎸 Precision Tablature & Interactive Chords
- **Monospace Tab Viewer**: High-legibility formatting with syntax-colored chord anchors and section markers (`[Verse]`, `[Chorus]`, `[Solo]`).
- **Interactive Chord Inspector**: Tap any chord to view both **Guitar Fretboard** and **61-Key Piano** fingerings across variations (Major, Minor, 7th, Maj7, Min7, Dim, Aug, Sus4).
- **Transpose Engine**: Instant real-time transposition (±12 semitones) with automatic enharmonic pitch corrections.
- **Smart Auto-Scroll**: Smooth variable-speed scrolling (0.5x – 3.0x) with floating pause-on-tap controls.
- **Capo Calculator**: Automatic adjustment for capo placement and transposed fingerings.

### 🎙️ AI Audio & Lyrics Studio
- **Neural Chord Recognition**: On-device AI chord classification powered by ONNX Runtime (`btc.onnx` & `chordnet.onnx`).
- **Real-Time CQT DSP Engine**: Subharmonic Constant-Q Transform audio processing for accurate frequency-to-chroma mapping.
- **AI Lyrics Transcription**: Local timestamp-synchronized lyrics extraction using Whisper AI.
- **Synchronized Waveform Studio**: Multi-track visual waveform player with real-time audio playback and chord timeline sync.

### 🎵 Precision Chromatic Tuner
- **Real-Time Pitch Detection**: High-accuracy YIN and autocorrelation algorithms for sub-cent pitch tracking.
- **Instrument Presets**: Support for Standard Guitar, Drop D, DADGAD, Open G, Bass, Ukulele, and Cello.
- **Visual Needle Display**: Live frequency meter with target frequency lock and cents offset indicator.

### 📄 Studio PDF Exporter
- **Clean A4 Tab Sheets**: Generate clean, printable PDF chord sheets with embedded Chord Studio branding.
- **Metadata & Page Numbering**: Formatted with song title, artist, capo, tuning, and automatic page break preservation.

### 💾 Library & Offline Storage
- **Playlists & Collections**: Organize songs into custom playlists with reordering support.
- **Viewing History**: Track recently played songs with fast timestamp navigation.
- **Offline Mode**: Favorites and playlists are saved locally via SQLite for instant zero-latency access without internet.
- **Library Backup**: Export and import your entire library to portable `.json` files.

---

## Desktop Quick Start & Build Options (Windows)

### Option 1: Microsoft Store Install
Install the certified, sandboxed MSIX package directly from the Microsoft Store:
- **Web Link**: [apps.microsoft.com/detail/9P71JGK9JB2Q](https://apps.microsoft.com/detail/9P71JGK9JB2Q)

### Option 2: Run Launcher
Double-click `run_desktop.bat` or execute from terminal:

```cmd
run_desktop.bat
```

**Launcher Options:**
- `run_desktop.bat` — Launch the existing binary
- `run_desktop.bat --recompile` — Recompile Kotlin sources and launch
- `run_desktop.bat --build` — Rebuild standalone executable only
- `run_desktop.bat --dev` — Run in Gradle development mode with live logs

### Option 3: Build Standalone Executable (.exe)
To generate the standalone `ChordStudio.exe` package:

```bash
./gradlew :desktopApp:createDistributable
```
The resulting executable will be created at `desktopApp/build/compose/binaries/main/app/ChordStudio/ChordStudio.exe`.

### Option 4: Build MSIX Package
To package the app as a Windows Store MSIX bundle:

```cmd
package_msix.bat
```

---

## Keyboard Shortcuts (Desktop)

| Shortcut | Action |
| :--- | :--- |
| `Ctrl + 1` | Search & Explore |
| `Ctrl + 2` | Favorites Library |
| `Ctrl + 3` | Playlists |
| `Ctrl + 4` | History |
| `Ctrl + 5` | Chromatic Tuner |
| `Ctrl + N` | Create Custom Tab |
| `Esc` | Navigate Back / Dismiss Modal |

---

## Architecture & Tech Stack

```
Chord Studio
├── desktopApp/         # Desktop application (Windows, macOS, Linux)
│   └── ui/             # JetBrains Compose Multiplatform UI
│   └── utilities/      # DSP Audio Tuner, ONNX Neural Engine, PDF Exporter
│   └── data/           # SQLite Database & repository cache
├── app/                # Native Android application
└── tools/              # Whisper AI offline transcription binaries
```

- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Skiko Skia-backed hardware accelerated renderer)
- **Neural Inference**: [ONNX Runtime](https://onnxruntime.ai/) (BTC & ChordNet Models)
- **Audio DSP**: 16 kHz resampler, Subharmonic CQT extractor, YIN Pitch Detection
- **PDF Generation**: [Apache PDFBox](https://pdfbox.apache.org/)
- **Local Database**: SQLite with JDBC driver

---

## Changelog Summary

For detailed version-by-version release notes from **v0.2.0** to **v0.8.0**, please refer to [CHANGELOG.md](CHANGELOG.md).

- **v0.8.0** — High-DPI icon mipmaps, branded studio PDF exporter, pixel-perfect alignment.
- **v0.7.0** — Windows 11 frameless title bar, non-overlapping scrollbar gutters, pinned chords toolbar.
- **v0.6.0** — On-device AI audio studio, ONNX chord recognition, Whisper lyrics extraction.
- **v0.5.0** — Compose Multiplatform desktop client, high-precision chromatic tuner.
- **v0.4.0** — Custom playlists, viewing history, JSON backup & migration.
- **v0.3.0** — Interactive guitar/piano chord inspector, auto-scroll engine, offline favorites.
- **v0.2.0** — Core tablature viewer, transpose engine, 1M+ catalog search.

---

## Author & Connect

**Chord Studio** is developed by **Sayan Dey**.

- **LinkedIn**: [linkedin.com/in/sayan-dey021](https://www.linkedin.com/in/sayan-dey021)
- **GitHub**: [github.com/sayandey021](https://github.com/sayandey021)
- **Project Documentation**: [Docs & Privacy Notice](docs/Privacy.md)
- **Release History**: [Full Changelog](CHANGELOG.md)

---

## License

This project is open source and available under the [GNU General Public License v3.0](LICENSE).
