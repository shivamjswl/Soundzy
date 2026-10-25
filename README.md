# MuseDock downloads

Developed by Shivam Jaiswal. Version **0.7.0**, for **Android 10 (API 29) and newer**, targeting Android 17 (API 37).

**[Download MuseDock 0.7.0 APK](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.7.0.apk)**

[Complete source archive](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.7.0-source.zip) · [APK SHA-256](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.7.0.apk.sha256)

## New in 0.7.0

- Compact Library typography, smaller artwork/rows, clean navigation arrows and a subdued history-import link.
- Lyrics and its provider integration removed. Equalizer is a small **⋯ → Equalizer** menu item in the app header and current player's options, with five bands and persistent presets.
- **Music language:** choose English, Punjabi, Hindi or another of 16 languages, or All languages. The setting guides song, album and playlist discovery while preserving direct links and the original search text. It is a discovery preference, not verified audio-language classification.
- **Appearance:** use **⋯ → Appearance** for Material, Light, Dark or Liquid. Material follows system night mode and uses dynamic colors on Android 12+. Light uses bright surfaces, Dark uses charcoal and green, and Liquid uses blue gradients and glass-like cards. Queue, player, equalizer and dialogs share the appearance.
- MuseDock branding throughout current app text and project headings. Installation/storage identifiers remain compatible so an update can retain existing playlists, downloads and account settings.

All **123 automated tests passed**, including the API29–37 playlist matrix, music-catalog checks, preference persistence, menu placement, language-aware search/paging and four native theme render sets. Debug/release lint passed with zero errors. The standalone, non-debuggable APK passes signature/alignment checks and matches the previous release's signing certificate. It is signed with the retained development key, not distributed through Play Store.

The app retains YouTube Music Songs/Albums discovery, public playlists, resumable offline audio saves, local MP3 conversion/export, the artwork queue and local Takeout history import. Music streaming uses NewPipe-resolved YouTube media streams shared with YouTube Music. Source availability can change. Audible equalizer output, MediaCodec conversion and installation on particular phones still need device checks; framework renders are not physical-phone screenshots. See [validation](https://github.com/shivamjswl/Soundzy/blob/main/VALIDATION.md) and [installation help](https://github.com/shivamjswl/Soundzy/blob/main/INSTALLATION.md).

Google sign-in remains optional and unconfigured until OAuth registrations are supplied; it cannot expose YouTube watch history. Appearance and language preferences stay on the device. No lyrics provider is contacted.

Application source is GPL-3.0-or-later. Third-party notices and corresponding source are included in the source archive. Earlier download files remain available by filename.
