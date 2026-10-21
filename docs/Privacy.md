# Privacy Policy — Chord Studio

**Last Updated: October 10, 2026**

This Privacy Policy describes how **Chord Studio** (developed by **SaayanSoft** / **Sayan Dey**, "we", "us", or "our") handles your information when you use the Chord Studio desktop application, MSIX package, and related services (collectively, the "Application").

By using Chord Studio, you agree to the terms described in this Privacy Policy.

---

## 1. Summary of Core Privacy Principles

* **100% Offline-First**: Your tabs, playlists, favorites, transpose preferences, custom chords, and viewing history are stored locally on your device in a private SQLite database.
* **On-Device AI Inference**: Audio-to-chord neural network inference (Microsoft ONNX Runtime) executes entirely on your local machine. Your imported songs and audio files are **never** uploaded to any external server or cloud provider.
* **Transient Microphone Usage**: Microphone access is used exclusively in real-time RAM for the Chromatic Instrument Tuner and optional local audio recording. Tuner audio frames are processed in volatile memory and discarded immediately.
* **Zero Telemetry & Zero Ads**: Chord Studio contains no third-party tracking SDKs, no behavioral analytics, no profiling, and no advertising networks.
* **No Account Required**: You can use all core features of Chord Studio without signing up or providing any personal information.

---

## 2. Information We Collect and How We Use It

### A. Local Data Storage (On-Device Only)
All user data created during normal use is saved exclusively to your local device storage:
* **Saved Tabs & Chords**: Tabs and chord sheets you save or create.
* **Playlists & Favorites**: Custom playlists, reordered songs, and starred favorites.
* **Playback History**: Timestamps and recently viewed tab entries.
* **App Preferences**: Theme choices, accent colors, default font sizes, autoscroll speeds, and audio device preferences.

This data never leaves your device unless you explicitly export your database using the built-in **Export Library (.json)** feature.

### B. Audio Input (Microphone)
Chord Studio requests permission to access your audio input hardware (microphone) solely for the following optional features:
1. **Chromatic Instrument Tuner**: Real-time microphone audio is captured in temporary volatile memory buffers to perform pitch detection (YIN and autocorrelation algorithms). Captured audio samples are processed in RAM to compute frequency in Hertz and are **never saved to disk or transmitted across any network**.
2. **Local Mic Recording (AI Studio)**: If you choose to record your singing or playing through the "Add Audio / Mic" feature, the recorded audio is stored strictly on your local PC in volatile memory or chosen directory for local chord analysis.

### C. On-Device AI Models & Audio Files
When you import audio files (MP3, WAV, FLAC, M4A, OGG) into the AI Audio Studio:
* The audio is decoded and processed on your local CPU/GPU using on-device models (`btc.onnx` and `chordnet.onnx`).
* Processing is completely self-contained within your local application sandbox.
* We do not receive, store, or inspect your audio files.

### D. Network Requests (Public APIs)
Chord Studio only makes outbound HTTPS requests to the internet when you interact with online catalog features:
* **Tab Search & Suggestions**: When you search for songs or artists, search queries are sent via secure HTTPS to fetch public tab and chord documents.
* **Synchronized Lyrics (LRCLIB)**: If enabled, Chord Studio queries the public LRCLIB API to download time-aligned lyrics matching the song title and artist name of your imported audio track.

These requests contain only the song metadata or search query needed to retrieve the requested musical content. No device serial numbers, advertising IDs, or personal identification data are transmitted.

---

## 3. Data Sharing and Third-Party Disclosures

We **do not sell, rent, trade, or share** your personal information or audio data with any third parties.

* **No Advertising Networks**: Chord Studio does not integrate with any ad networks (such as Google AdMob or Unity Ads).
* **No Analytics Providers**: Chord Studio does not use third-party analytics trackers (such as Google Analytics, Firebase, or Mixpanel).
* **No Cloud Backups without Consent**: Your data is yours. Backups are created only when you manually save a `.json` backup file to a location of your choice.

---

## 4. Data Security

Because Chord Studio operates on an offline-first architecture, your saved tabs, playlists, and settings remain isolated within your operating system's secure application container (`%LOCALAPPDATA%` on Windows, sandboxed per Windows MSIX / AppContainer standards).

---

## 5. Children's Privacy

Chord Studio does not knowingly collect, solicit, or store personal information from children under the age of 13 (or under 16 in the EEA). Because the Application does not collect personally identifiable information, it is safe for users of all ages.

---

## 6. Your Rights and Data Control

You retain full ownership and control over all data created within Chord Studio:
* **Access & Export**: You can export your complete database (tabs, favorites, playlists, history) at any time via **Settings $\rightarrow$ Data & Storage $\rightarrow$ Export Library (.json)**.
* **Deletion**: You can clear your search cache, remove your playback history, or delete specific tabs and playlists directly within the application settings at any time.
* **Complete Removal**: Uninstalling Chord Studio removes the application and its local data sandbox in accordance with Windows package management.

---

## 7. Changes to this Privacy Policy

We may update this Privacy Policy from time to time to reflect new features, improvements, or regulatory requirements. Any updates will be reflected with a revised "Last Updated" date at the top of this document and will be posted in our official repository:
[https://github.com/sayandey021/Chord-Studio](https://github.com/sayandey021/Chord-Studio)

---

## 8. Contact Information

If you have any questions, concerns, or feedback regarding this Privacy Policy or Chord Studio's privacy practices, please contact us:

* **Developer**: Sayan Dey (`SaayanSoft`)
* **Website**: [https://saayansoft.netlify.app/](https://saayansoft.netlify.app/)
* **Email**: [saayanstudiosoft@gmail.com]
* **GitHub Repository**: [https://github.com/sayandey021/Chord-Studio](https://github.com/sayandey021/Chord-Studio)
* **Issues & Support**: [https://github.com/sayandey021/Chord-Studio/issues](https://github.com/sayandey021/Chord-Studio/issues)
* **LinkedIn**: [https://www.linkedin.com/in/sayan-dey021](https://www.linkedin.com/in/sayan-dey021)
