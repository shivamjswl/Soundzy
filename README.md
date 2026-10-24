# Soundzy

Developed by **Shivam Jaiswal**. Version **0.4.0**, for **Android 14 / API 34 and newer**.

[Download APK](https://raw.githubusercontent.com/shivamjswl/soundzy/downloads/Soundzy-0.4.0.apk) · [Complete corresponding source archive](https://raw.githubusercontent.com/shivamjswl/soundzy/downloads/Soundzy-0.4.0-source.zip)

Soundzy is the renamed Cadence app. The application ID (`app.cadence`), database, and signing key stay the same so the update can retain existing playlists and downloads. The project directory remains `/workspace/Cadence`.

## New in 0.4.0

- Android 14 and 15 are now allowed by the APK's minimum SDK (34); the app is compiled/targeted against Android 17 (API 37). Android 14, 15, 16, and 17 receive dedicated framework tests for the custom-playlist flows.
- In **Offline**, use **Create playlist from downloads**, enter a name, select songs, and save. The playlist opens immediately in Library.
- In an existing playlist, use **Add downloaded songs** to fill it without searching online.
- Every song row exposes a visible **+** action to add that song to a playlist, including creating a new one.
- Playlist naming supports the keyboard Done action, keeps invalid blank names open for correction, and opens newly created playlists immediately.
- The update keeps the same application ID, signing certificate, and SQLite schema. The APK carries a modern v3 signature, verified across API 34–37.

The previous playlist creation dialog passed a basic UI test; this change addresses the reported missing/discoverable action from downloaded songs. The generic Android 17 "Something went wrong. App not installed" message has not been reproduced on a physical phone. See [installation troubleshooting](INSTALLATION.md) if it persists.

## Install the update

Open `Soundzy-0.4.0-debug.apk` on your phone and install it over the existing Cadence installation. Existing data is retained by an Android update with the same application ID/signing key. Uninstalling clears app-private data.

This is a development-signed test build, not a Play Store release. Live YouTube compatibility and device codec behavior still need phone testing.

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

The previous shared client imposed a 70-second total call timeout. Long downloads could fail even while bytes were arriving. Soundzy now uses a dedicated client with no fixed total download deadline, bounded read/connect timeouts, 2 MiB range requests, automatic retries, checked response offsets/lengths, and fresh-link recovery. Partial bytes and a stream-identity checkpoint survive a failed attempt and process restart; tap Retry to continue when the source identity matches. Incompatible prefixes are discarded rather than combined with different audio.

Only a complete transfer is published. The saved file must contain an Android-recognized audio track. MP3 conversion starts after the original audio is complete. If conversion fails, the original audio is kept when available. Conversion can be cancelled; retries and storage cleanup are available in Offline. Download/convert jobs run one at a time in a foreground service with progress and cancellation notifications.

In a saved track's menu, **Export offline audio** opens Android's file picker. **Convert offline copy to MP3** converts a saved original without downloading again. Exported file names/MIME types match the detected audio container. Files remain private to the app until you choose to export them. Only download audio you have permission to save.

## Google sign-in and YouTube history

**Google's official API does not provide YouTube watch history. Signing in cannot directly import it.**

The Google sign-in integration is implemented in an optional source set. It needs OAuth registrations in your Google Cloud project. No client ID or registration was supplied, so the distributed APK leaves sign-in unavailable and does not bundle the optional Google SDK libraries. See [Google sign-in setup](GOOGLE_SIGN_IN_SETUP.md) for the exact package, signing fingerprint, and build command. Google sign-in is profile-only; there is no backend account or cloud sync.

History import is available now without login:

1. Open **Library → Import YouTube history** or **Info → Account & history**.
2. Export YouTube watch history from Google Takeout as JSON or HTML and unzip it.
3. Choose the history file in Soundzy, select entries that are songs, and import them.

The file is processed locally, not uploaded. Watch history can include non-music videos, so selection is explicit. Valid video IDs are deduplicated; JSON timestamps retain listening order, while existing local metadata and newer play times are kept. Up to 5,000 distinct entries and 20 MB per file are supported. The import saves song links in a local playlist, not offline audio. HTML timestamps are not parsed.

## Build and test

Prerequisites: a full JDK 21, Android SDK Platform 37.0 / Build Tools 37.0.0 with accepted SDK licenses, Git, and network access to Google Maven, Maven Central, Gradle distributions, and GitHub.

```bash
./scripts/fetch-deps.sh
# Configure sdk.dir in an ignored local.properties or use Android Studio's SDK.
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Windows users can fetch dependencies in Git Bash and use `gradlew.bat`. APK output: `app/build/outputs/apk/debug/app-debug.apk`. Cloud-specific paths are in [DEVELOPMENT.md](DEVELOPMENT.md).

NewPipe Extractor and nanojson are pinned source dependencies under `.deps/`. Their source is included in the complete-source archive. Jump3r's corresponding source archive is in `third_party/`. An ordinary checkout can reconstruct the extractor dependencies with the fetch script. The default build uses `app/src/noGoogle/java`; providing `-PgoogleWebClientId=...` selects `app/src/google/java` and its official Google dependencies.

Run real-device instrumentation tests on an Android 14+ device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

See [VALIDATION.md](VALIDATION.md) for executed checks, remaining phone checks, and limitations. Screenshots under `validation/*-framework.png` render actual app views through Robolectric with explicit test fixtures; they are not phone screenshots. The test tone/encoder MP3 fixture is original generated audio and is not bundled as a song in the application.

## Privacy and licenses

YouTube searches, playback, and artwork requests go directly to YouTube/Google image/media hosts. Selected history exports and audio conversion stay on the device. Exporting writes to the destination/provider you choose in Android's file picker. Optional Google sign-in uses Google's native identity flow and stores only display name/email locally; no ID token is persisted or uploaded. No analytics or developer-hosted server is used.

Application code: GPL-3.0-or-later. Third-party material retains its own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), `licenses/`, and included source headers. The optional Google-enabled build is subject to Google's SDK terms and must be reviewed for license compatibility before redistribution. Soundzy is not affiliated with YouTube, Spotify, or Google.
