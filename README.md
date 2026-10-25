# MuseDock downloads

Developed by Shivam Jaiswal. Version **0.6.0**, for **Android 10 (API 29) and newer**, targeting Android 17 (API 37).

**[Download MuseDock 0.6.0 APK](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.6.0.apk)**

[Complete corresponding source](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.6.0-source.zip) · [APK SHA-256](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/MuseDock-0.6.0.apk.sha256)

MuseDock replaces the Soundzy name in the app. This repository remains named Soundzy. The standalone, non-debuggable APK retains `app.cadence`, the signing key, database and account preference storage, so it can update compatible existing Cadence/Soundzy installations and preserve local data. It uses the existing development signing key and is not a Play Store release.

## New in 0.6.0

- **YouTube Music catalog:** Search defaults to Songs. Choose Albums to browse album/single/EP results, open ordered track lists, play or save them. Listen also offers Browse albums and Music playlist suggestions. The separate YouTube videos filter remains available.
- **Equalizer:** Open Now playing → Equalizer or Info → Equalizer. Use five frequency bands or Flat, Bass boost, Vocal and Treble boost presets. Settings persist and apply to this player's streaming/offline session when the device supports audio effects.
- **Lyrics:** Open Now playing → Lyrics for large text, timed highlighting, automatic scrolling, manual scrolling/follow control and tap-to-seek. Plain, missing, instrumental and error/retry states are supported. LRCLIB matching uses song title, artist and duration; successful lookups are cached for offline viewing.

All **127** automated tests passed, including the existing API 29–37 playlist matrix and 15 MuseDock UI/catalog cases across API 29 and 37. Debug/release lint passed with zero errors. APK signature and alignment checks passed; the certificate matches Soundzy 0.5.0. Framework screenshots use original test fixtures. These checks do not confirm physical-device installation or audible equalization.

YouTube Music shares media IDs/delivery with YouTube, so catalog results still play through NewPipe-resolved YouTube audio streams. This is an unofficial integration without Premium, Spotify or Apple Music SDK access; original masters and source availability are not guaranteed. Live YouTube Music and LRCLIB requests returned HTTP 403 from this restricted cloud, so provider availability, lyric coverage and audible device effects still require phone testing. Opening Lyrics sends only track metadata to LRCLIB, not account profiles, audio or history exports.

The previously reported Pixel Android 17 build CP3A.260905.009 installation rejection remains undiagnosed. If this APK is also rejected, preserve the exact `adb install -r MuseDock-0.6.0.apk` result; see [installation diagnostics](https://github.com/shivamjswl/Soundzy/blob/main/INSTALLATION.md).

Google sign-in remains unconfigured until OAuth registrations are supplied and cannot expose YouTube history. Playlists, resumable offline saves, MP3 conversion/export, local Takeout history import and the artwork queue remain available.

Application code is GPL-3.0-or-later. Third-party notices and corresponding source are included in the source download; lyrics retain their respective authors' rights and no lyric corpus is bundled. Earlier Soundzy downloads remain available by filename.
