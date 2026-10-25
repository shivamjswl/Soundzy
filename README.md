# MuseDock

Developed by **Shivam Jaiswal**. Version **0.6.0**, for **Android 10 / API 29 and newer**.

[Download APK](https://raw.githubusercontent.com/shivamjswl/soundzy/downloads/MuseDock-0.6.0.apk) · [Complete corresponding source archive](https://raw.githubusercontent.com/shivamjswl/soundzy/downloads/MuseDock-0.6.0-source.zip)

MuseDock is the renamed Soundzy/Cadence app. The application ID (`app.cadence`), database, and signing key stay the same so the update can retain existing playlists and downloads. The project directory remains `/workspace/Cadence`.

## New in 0.6.0

- **MuseDock** branding replaces Soundzy. The package, account preference storage, database and signing key stay the same for updates; the GitHub repository remains `shivamjswl/Soundzy`.
- Search defaults to **YouTube Music → Songs**. Switch to **Albums** for album/single/EP results with artwork and artists, then open, play, page through, and save their ordered tracks. **YouTube videos** remains available as a separate filter. The Listen screen also offers **Browse albums**, and suggested playlists now use YouTube Music's playlist catalog.
- **Now playing → Equalizer**, or **Info → Equalizer**, opens five frequency controls and Flat, Bass boost, Vocal and Treble boost presets. Settings and the on/off toggle persist. The player owns the effect on its own audio session and reattaches it when the session changes; unsupported outputs show a status instead of breaking playback. The requested curve is mapped to available device bands and clamped to device gain limits.
- **Now playing → Lyrics** opens a large lyric view with active-line highlighting, automatic scrolling, free scrolling, follow-song control and tap-to-seek. Tracks without timestamps show clearly labeled plain lyrics. Missing and instrumental tracks have their own states, and provider errors offer Retry.
- Lyrics are matched by title, artist and duration through LRCLIB's exact metadata API; no arbitrary search result is substituted. Loaded lyrics are cached privately for offline viewing (100 entries / 20 MB maximum). Closing the view cancels its worker and timers; stale responses cannot replace the current track's lyrics.

Songs, albums and playlists use NewPipe's actual YouTube Music catalog filters. YouTube Music and YouTube share video IDs and media delivery, so playback still resolves YouTube media streams through NewPipe. This is an unofficial integration; it does not use a Premium account, guarantee original masters, lossless audio, or unrestricted catalog availability. Album search can also return singles and EPs. Spotify/Apple Music subscription playback would require their supported SDKs, account authorization and service terms; neither service is silently used here.

Android 10+ remains the minimum, with compile/target API 37. The previously reported Pixel Android 17 installation failure (CP3A.260905.009) still has no package-manager diagnostic; a successful framework check does not prove installation on that phone. See [installation diagnostics](INSTALLATION.md).

## Install the update

Download `MuseDock-0.6.0.apk` above and open it on the phone to update the existing Cadence/Soundzy/MuseDock installation. The application ID and signing certificate match previous supplied builds. Existing data is retained by a compatible Android update; uninstalling clears app-private data.

This is a non-debuggable APK signed with the existing development key, not a Play Store release. Actual installation on the affected Pixel, live YouTube behavior, and device codec behavior still need phone testing.

## Music and playlists

- Search YouTube songs/artists or paste public video and playlist links.
- Discover public playlists through For you, Hindi, Chill, and Workout filters.
- Play or shuffle loaded playlist songs, load more pages, and save loaded song links into a local playlist.
- Create, rename, delete, and reorder playlists; like songs and revisit recent listening.
- Background audio, notification controls, audio focus, and headphone-disconnection handling use AndroidX Media3.
- The new queue sheet includes artwork, a highlighted current track, Up next, earlier tracks, shuffle, clearing, and per-track reorder/remove menus. Queue edits use stable timeline identities. Manual reordering turns shuffle off so the chosen order is respected.

YouTube extraction uses NewPipe Extractor rather than an official audio-download API. Restricted/private videos, regional restrictions, bot challenges, or source changes can prevent playback or saving. There are no in-app advertisements, but no official YouTube ad-free service guarantee is made. The app has no YouTube account cookies or Premium integration.

## Offline saves and MP3

Open a song's menu and choose **Save offline**. Pick:

- **Original audio**: preserves the original compressed audio and saves faster.
- **MP3**: downloads the original first, then converts locally using Android's decoder and a Java LAME encoder. MP3 is a compatibility option; transcoding does not improve quality and takes extra CPU, battery, and storage.

The previous shared client imposed a 70-second total call timeout. Long downloads could fail even while bytes were arriving. MuseDock now uses a dedicated client with no fixed total download deadline, bounded read/connect timeouts, 2 MiB range requests, automatic retries, checked response offsets/lengths, and fresh-link recovery. Partial bytes and a stream-identity checkpoint survive a failed attempt and process restart; tap Retry to continue when the source identity matches. Incompatible prefixes are discarded rather than combined with different audio.

Only a complete transfer is published. The saved file must contain an Android-recognized audio track. MP3 conversion starts after the original audio is complete. If conversion fails, the original audio is kept when available. Conversion can be cancelled; retries and storage cleanup are available in Offline. Download/convert jobs run one at a time in a foreground service with progress and cancellation notifications.

In a saved track's menu, **Export offline audio** opens Android's file picker. **Convert offline copy to MP3** converts a saved original without downloading again. Exported file names/MIME types match the detected audio container. Files remain private to the app until you choose to export them. Only download audio you have permission to save.

## Google sign-in and YouTube history

**Google's official API does not provide YouTube watch history. Signing in cannot directly import it.**

The Google sign-in integration is implemented in an optional source set. It needs OAuth registrations in your Google Cloud project. No client ID or registration was supplied, so the distributed APK leaves sign-in unavailable and does not bundle the optional Google SDK libraries. See [Google sign-in setup](GOOGLE_SIGN_IN_SETUP.md) for the exact package, signing fingerprint, and build command. Google sign-in is profile-only; there is no backend account or cloud sync.

History import is available now without login:

1. Open **Library → Import YouTube history** or **Info → Account & history**.
2. Export YouTube watch history from Google Takeout as JSON or HTML and unzip it.
3. Choose the history file in MuseDock, select entries that are songs, and import them.

The file is processed locally, not uploaded. Watch history can include non-music videos, so selection is explicit. Valid video IDs are deduplicated; JSON timestamps retain listening order, while existing local metadata and newer play times are kept. Up to 5,000 distinct entries and 20 MB per file are supported. The import saves song links in a local playlist, not offline audio. HTML timestamps are not parsed.

## Build and test

Prerequisites: a full JDK 21, Android SDK Platform 37.0 / Build Tools 37.0.0 with accepted SDK licenses, Git, and network access to Google Maven, Maven Central, Gradle distributions, and GitHub.

```bash
./scripts/fetch-deps.sh
# Configure sdk.dir in an ignored local.properties or use Android Studio's SDK.
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease
```

Windows users can fetch dependencies in Git Bash and use `gradlew.bat`. Release APK output: `app/build/outputs/apk/release/app-release.apk`. The configured release build uses the development keystore; preserve that original keystore securely when updating existing installations. A fresh checkout generates a different development key unless the original is supplied locally. Cloud-specific paths are in [DEVELOPMENT.md](DEVELOPMENT.md).

NewPipe Extractor and nanojson are pinned source dependencies under `.deps/`. Their source is included in the complete-source archive. Jump3r and desugar_jdk_libs corresponding source archives is in `third_party/`. An ordinary checkout can reconstruct the extractor dependencies with the fetch script. The default build uses `app/src/noGoogle/java`; providing `-PgoogleWebClientId=...` selects `app/src/google/java` and its official Google dependencies.

Run real-device instrumentation tests on an Android 10+ device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

See [VALIDATION.md](VALIDATION.md) for executed checks, remaining phone checks, and limitations. Screenshots under `validation/*-framework.png` render actual app views through Robolectric with explicit test fixtures; they are not phone screenshots. The test tone/encoder MP3 fixture is original generated audio and is not bundled as a song in the application.

## Privacy and licenses

Music searches use YouTube Music or the selected YouTube video filter; playback and artwork requests go to YouTube/Google media and image hosts. Opening Lyrics sends song title, artist and duration to `lrclib.net` for matching. Returned lyrics are cached locally for offline use; no account, history export, audio file or Google profile is sent to LRCLIB. Lyrics coverage, matching and timing can vary. The lyrics view credits LRCLIB and links to its website. Selected history exports and audio conversion stay on the device. Exporting writes to the destination/provider you choose in Android's file picker. Optional Google sign-in uses Google's native identity flow and stores only display name/email locally; no ID token is persisted or uploaded. No analytics or developer-hosted server is used.

Application code: GPL-3.0-or-later. Third-party material retains its own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), `licenses/`, and included source headers. The optional Google-enabled build is subject to Google's SDK terms and must be reviewed for license compatibility before redistribution. MuseDock is not affiliated with YouTube, Spotify, or Google.
