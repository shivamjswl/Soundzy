# MuseDock development

Use the existing checkout at `/workspace/Cadence`; the application is MuseDock. This cloud task is already isolated, so do not create an extra worktree unless requested. Preserve user changes. The separate REA checkout is not an app dependency.

## Build

```bash
cd /workspace/Cadence
export JAVA_HOME=/workspace/toolchains/jdk/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
./scripts/fetch-deps.sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest
```

The installed toolchain is JDK 21, AGP 9.3.3, checksum-pinned Gradle 9.5.0, SDK Platform 37.0 and Build Tools 37.0.0. Minimum SDK is 29 (Android 10), compile/target 37 (Android 17). Core library desugaring supplies Java library backports. The ignored local.properties SDK path and writable caches are retained under /workspace/toolchains. No app server or Docker container is needed. Release output is app/build/outputs/apk/release/app-release.apk; distribute it as MuseDock-0.7.0.apk.

Release builds are non-debuggable and signed with the original locally retained development key. Do not publish or replace that key. Technical compatibility identifiers `app.cadence`, `cadence.db`, and `soundzy.account` stay unchanged to preserve installation identity, playlists/downloads and account preferences. They are not visible app branding. The Application class is MuseDockApp. Current project and UI labels use MuseDock.

## Feature boundaries

ThemePalette supplies the shared palette and window style. Material follows system night mode and resolves dynamic Android 12+ accent resources, with fallback colors on older devices. Light and Dark use their own native window/dialog styles; Liquid uses gradient surfaces and outlined glass-like cards without requiring a blur API. Changing theme recreates the activity and rebinds its controller; the playback service and database remain intact. Test colors and text contrast on both light and dark backgrounds.

MusicLanguage persists a discovery preference. Language keywords enrich song/album/playlist queries; direct video and playlist links are exempt. Search pagination keeps the original user query and invalidates old responses when language changes. Do not describe this as verified audio-language classification. MusicCatalog still uses music_songs/music_albums/videos through NewPipe; public albums resolve to track playlists.

Equalizer is a small overflow-menu entry. AudioEqualizer attaches Android Equalizer to the ExoPlayer session, maps/clamps the saved curve to device bands and releases with the service. It never applies a global session-0 effect. Tests use an injected effect adapter; audible behavior still needs physical-device checks. The lyrics surface, source classes and provider calls are removed in 0.7.0; tests now verify that no lyrics control appears.

## Verification limits

Robolectric tests exercise SQLite, actual widgets, native framework rendering and ExoPlayer timelines. The API29–37 compatibility matrix verifies custom playlists. On API29–32 the test harness grants the app's own signature receiver permission, matching installation state. It deliberately treats the playback-service Binder as unavailable; tests can attach a real local ExoPlayer for player UI checks. JVM settings already export the file-descriptor bridge needed by Android 16/17 shadows, and test output uses musedock.validation.dir.

Run ./gradlew :app:connectedDebugAndroidTest with a usable Android10+ device for instrumentation. Instrumentation APKs compile here, but no physical-device installation, hearing test or MediaCodec acceptance is claimed. The unaccelerated emulator previously did not finish booting, so framework renders are not phone screenshots. Live YouTube/Music requests may be denied by the restricted cloud; retain proxy and TLS verification. Use native HTTPS Git authentication and never extract proxy credentials.

ResumableDownload uses real local HTTP tests for range recovery and expired links. PcmMp3Encoder uses real PCM, with the generated fixture independently decoded. MediaCodec conversion and live streaming still need device acceptance. Google sign-in remains optional, requires OAuth registration and cannot expose YouTube history; see GOOGLE_SIGN_IN_SETUP.md. The previously reported Pixel Android17 build CP3A.260905.009 installation issue still needs exact package-manager output if it persists; see INSTALLATION.md.
