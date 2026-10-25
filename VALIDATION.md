# MuseDock 0.7.0 validation

Checked on 25 October 2026.

| Check | Result |
| --- | --- |
| Release APK / instrumentation APK assembly | Passed |
| Gradle project / Application class | MuseDock / MuseDockApp |
| APK identity | MuseDock, versionCode 7, versionName 0.7.0; retained installation identity |
| SDK declarations | Minimum API 29 (Android 10), compile/target 37 (Android 17), no maximum SDK |
| Release packaging | Debugging disabled; no testOnly flag; standalone APK |
| APK signature | Verified for API 29–37; v3 signature; certificate matches MuseDock 0.6.0 |
| Alignment / ABI | zipalign passed with 16 KiB checks; no native .so libraries or ABI splits |
| Java backports | Desugared Java library classes present in DEX |
| Lyrics removal | No lyric control in player/Library checks; provider source removed; app lyric DEX classes absent |
| JVM/framework suite | 123 passed; 0 failed; 0 skipped |
| Playlist SDK matrix | 45 checks across API 29/30/31/32/33/34/35/36/37 |
| Catalog feature checks | 8 checks across API 29 and 37 |
| Appearance / language UI | 11 checks across API 29 and 37, including a native-render case on API 37 |
| Theme renders | Library, player and equalizer captured for all four themes |
| Debug / release lint | Zero errors; advisory warnings recorded in validation/ |
| Physical device / audible equalizer | Not executed in this cloud |

## What changed and what the tests establish

Library headings are 26 sp and playlist names 15 sp, with smaller artwork and compact rows that can grow for larger system fonts. Lyrics and its provider classes are removed. Equalizer remains functional through the app/header and player overflow menus. Native UI tests open the actual player through its mini-player, verify missing large controls, and open the equalizer from its menu. Current app text and project headings use MuseDock. The original installation/storage identifiers and signing certificate are intentionally retained for data-compatible updates.

ThemePalette supplies colors, system-bar appearance and native window/dialog styles for Material, Light, Dark and Liquid. Material follows system night mode and uses dynamic accent resources on Android 12+, with fallback colors on older Android. Liquid is a gradient/glass-like visual treatment without a blur API requirement. Tests verify preference persistence, Material light/night selection, text contrast, smaller Library type, menu placement, and actual widget rendering. The native-render case captures Library/player/equalizer for every palette using generated original test metadata. Framework renders are not screenshots from a phone.

MusicLanguage offers All languages and 16 specific languages including English and Punjabi. It persists a discovery preference and enriches songs, albums and suggested-playlist queries. The UI test changes language and exercises search/page behavior without duplicating the language keyword or changing the user's displayed query. Unit tests preserve direct video and playlist links. This is query-guided discovery, not audio-language verification or a guarantee that every provider result matches the chosen language.

Existing catalog tests verify routing to NewPipe's YouTube Music extractor, album opening/paging/deduplication/save, and the YouTube videos alternative. Existing database, custom-playlist, queue, download recovery, history-import and Java MP3 encoder tests also pass. Equalizer effect-adapter tests check settings persistence, interpolation, hardware clamping, bypass, unsupported effects and session ownership; they do not establish audible output from a phone's audio driver.

The obsolete lyric tests were removed together with the lyric feature. No new lyric lookup is made and no lyric corpus is bundled. Other service limitations remain: catalog access is unofficial, source availability can change, and live YouTube/Music requests were previously denied by this restricted cloud. No new live-service, MediaCodec conversion or physical installation success is claimed.

The retained package ID, SQLite filename, account preferences and original signing key allow compatible updates to keep playlists/downloads. The Application class and Gradle project name now use MuseDock. No database schema change, reset or key replacement was introduced. The previously reported Pixel Android 17 build CP3A.260905.009 installation issue still needs device confirmation or an exact package-manager error; framework and APK checks do not diagnose that phone's installer.

Toolchain: JDK 21, AGP 9.3.3, checksum-pinned Gradle 9.5.0, SDK Platform 37.0 / Build Tools 37.0.0, Robolectric 4.17. The complete source download includes pinned extractor/nanojson and corresponding jump3r/desugar sources and notices. Google sign-in still needs OAuth setup and cannot expose YouTube watch history.

APK SHA-256: `2597ccaf04e1bd433e3da6cfd62dfe0fa414b4a5ef51ee9469c85581e7c63759`

## Phone acceptance still required

1. Install MuseDock-0.7.0.apk as an update, preserving playlists, offline files and compatible account settings.
2. Open Library and confirm the compact typography, navigation arrows and smaller rows on the actual phone/font scale.
3. Use ⋯ → Appearance for all four themes. Check status/navigation icons, dialogs, queue, player and equalizer, then restart to verify persistence. Change system night mode while using Material.
4. Select English or Punjabi through the language chip/menu, search songs/albums, page through results and open recommendations. Test direct links and All languages.
5. Open Now playing → ⋯ → Equalizer. Confirm the compact entry, absence of lyrics and large extra buttons, and audible effects with online/offline tracks and available audio outputs.
6. Check background playback, downloads, MP3 conversion and export on Android 10+ hardware. If the affected Pixel still rejects installation, retain the exact result of `adb install -r MuseDock-0.7.0.apk`; see INSTALLATION.md.
