# MuseDock 0.6.0 validation

Checked on 25 October 2026. Source repository remains `shivamjswl/Soundzy`; the app and downloadable artifacts are renamed MuseDock.

| Check | Result |
| --- | --- |
| Release APK and instrumentation APK assembly | Passed |
| APK identity | `app.cadence`, versionCode 6, versionName 0.6.0, label MuseDock |
| Android SDK declarations | Minimum API 29 (Android 10), compile/target API 37 (Android 17); no maximum SDK |
| Release packaging | Debugging disabled; no testOnly flag; standalone APK |
| APK signature / upgrade continuity | Verified for API 29–37; v3 signature; certificate SHA-256 matches 0.5.0 |
| APK alignment / ABI | zipalign passed with 16 KiB checks; no native .so libraries or ABI splits |
| Java API backports | Core library desugaring present in APK DEX |
| JVM/framework suite | 127 passed; 0 failed; 0 skipped |
| Existing playlist SDK matrix | 45 checks across API 29/30/31/32/33/34/35/36/37, all passed |
| MuseDock feature UI/routing checks | 15 checks across API 29 and 37, including one native-render case on API 37 |
| Debug / release lint | Zero errors; advisory warnings retained in validation/ |
| Live YouTube Music / LRCLIB lookup | HTTP 403 from this cloud; not verified live |
| Physical device installation / audible equalizer | Not executed here |
| Previously affected Pixel CP3A.260905.009 installation | Still unverified; no package-manager diagnostic provided |

## What the tests establish

The new UI tests use the actual activity, SQLite database, native Android widgets, a real ExoPlayer timeline and generated silence when a prepared seekable timeline is needed. They check MuseDock branding, Songs and YouTube videos mode selection, album paging/deduplication/open/save, equalizer discovery and persistent preset/toggle controls, synced lyric rendering and tap-to-seek, a nonseekable-player guard, track changes, stale-response rejection, provider error/retry and labeled plain lyrics. The catalog routing check constructs NewPipe's actual search handlers and verifies music modes select its YouTube Music extractor. Album data in UI tests is explicitly injected fixture data, not a live catalog result.

AudioEqualizerTest checks persisted settings, interpolation, device-gain clamping, bypass, unsupported-effect handling, session changes and release using an injected effect adapter. This does not measure audible equalization or verify a phone audio driver. The production adapter uses Android Equalizer on the ExoPlayer session, never global session 0, and the manifest declares MODIFY_AUDIO_SETTINGS.

LyricsRepositoryTest uses real local HTTP requests through MockWebServer and original test lyric text. It verifies normalized exact-metadata queries, request attribution, matching/duration rejection, response-size bounds, missing/provider-error states, corrupt-cache recovery, refresh and cached retrieval without another HTTP request. LyricsTest covers repeated timestamps, decimal precision, positive/negative offsets, active-line boundaries, plain/instrumental/empty content and a 5,000-line bound. These tests do not establish real LRCLIB song coverage or matching accuracy for every upload.

The existing suite also covers SQLite playlist persistence, history import, stable queue edits, resumable downloads and actual Java MP3 encoding. Native framework renders in validation/ use original test fixtures; `musedock-lyrics-framework.png` and `musedock-equalizer-framework.png` show the new surfaces. They are not screenshots from a physical phone. No third-party song audio or lyric corpus is bundled.

## Provider and device limits

Songs, Albums and playlist discovery use NewPipe's music_songs, music_albums and music_playlists filters. The optional YouTube videos filter remains separate. Album track lists and playback resolve through YouTube, which shares media IDs/delivery with YouTube Music. This is an unofficial catalog integration, not an official Premium/Spotify/Apple Music SDK or a guarantee of original masters, lossless audio or source availability. Live requests to music.youtube.com and lrclib.net returned HTTP 403 in the current restricted cloud; tests do not bypass that restriction.

The lyric surface sends title, artist and duration to LRCLIB only when opened. Returned lyrics are cached privately with a 100-entry/20-MB cap, credited in the view and may be missing, mistimed or unmatched. Local files, history exports and account details are not sent to the lyric provider. Closing the view interrupts pending work and removes its timers; stale results are ignored. Auto-scrolling pauses for a manual gesture and resumes through its follow control or a line seek.

The application ID, original signing key, database schema and legacy account preference key are preserved. The user previously reported a Pixel Android 17 build CP3A.260905.009 install rejection. This cloud cannot confirm an installation on that phone; the exact package-manager error is still required if the new APK is rejected. No data reset or signing-key replacement is introduced.

Build toolchain: JDK 21, AGP 9.3.3, checksum-pinned Gradle 9.5.0, Android SDK 37.0 / Build Tools 37.0.0, Robolectric 4.17. The complete source archive contains pinned extractor/nanojson and corresponding jump3r/desugar sources and notices. Google login still needs OAuth configuration and cannot supply YouTube watch history.

APK SHA-256: `17d875f0bb749681a72073ef04c8658b6ae17d956f92e8d57997d0a233f4ce39`

## Phone acceptance still required

1. Install MuseDock-0.6.0.apk over Soundzy/Cadence and confirm retained playlists, downloads and account preferences. Android 10–17 are allowed by the SDK declarations; physical-device acceptance is still needed.
2. Search a known artist using Songs and Albums; verify album artwork and track order, paging, playback and saving. Check the optional YouTube videos filter.
3. Play online and offline songs; turn Equalizer on/off and compare presets and sliders with speakers, wired/Bluetooth headphones and output changes. Confirm persistence after restarting playback.
4. Open Lyrics for known supported recordings, including cached lyrics in airplane mode. Verify highlighting, pause/resume, seek, manual scrolling/follow, track changes and missing/instrumental/plain states.
5. Verify background playback, downloads, MP3 conversion, keyboard/navigation insets and export on Android 10+ devices. These have not been exercised on physical hardware here.
6. If Pixel Android 17 installation still fails, run `adb install -r MuseDock-0.6.0.apk` and retain the full error; see INSTALLATION.md.
