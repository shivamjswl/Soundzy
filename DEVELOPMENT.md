# Soundzy development in this cloud workspace

Cadence lives at `/workspace/Cadence`. REA remains at `/workspace/ReverseEngineerAnything` and is not a dependency of this app. Use the existing directories; each cloud task is already isolated, so no extra Git worktree is needed.

The Android tools installed for this build live under `/workspace/toolchains`. Run:

```bash
cd /workspace/Cadence
export JAVA_HOME=/workspace/toolchains/jdk/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
./scripts/build.sh
```

`local.properties` points to the installed SDK and is intentionally excluded from source control and source archives. A normal Android Studio installation should use its own SDK location. The Gradle wrapper and its distribution checksum are included.

This machine has a read-only system home directory. Robolectric's test home and SDK cache are therefore placed under `app/build/test-home` by the test configuration. Network proxy properties are inherited from the current Gradle process only when present; no credential values are stored in the project.

## Testing layers

- JVM tests exercise input validation and atomic download publication.
- Robolectric tests execute Android framework code for real SQLite persistence and screen navigation. The UI test deliberately treats the playback service as unavailable because Robolectric does not host its MediaSession Binder. This does not verify audio playback.
- Instrumentation tests in `app/src/androidTest` exercise a real Android device, including Media3 playback of a generated local test tone. Run them with `./gradlew :app:connectedDebugAndroidTest` on a usable Android 10+ device.
- Live YouTube checks require real network access to YouTube and its returned media/image hosts. This cloud's proxy denied the YouTube requests. No live extraction or streaming success is claimed.

The AOSP Android 16 emulator could not finish booting within the test window on this machine without hardware acceleration. Instrumentation tests were compiled but were not executed. Framework renders in `validation/*-framework.png` are actual app views drawn through Robolectric's native graphics mode, not screenshots from a phone.

For 0.2.0, `PlaylistSource` is the contract for public playlist discovery and paging. Production uses NewPipe through `YouTubeRepository`; framework tests inject deterministic playlist responses. These tests cover discovery/navigation/import but do not establish live YouTube compatibility.

For Soundzy 0.3.0, the default APK uses the no-Google source set. The optional Google integration was compiled separately with `-PgoogleSignIn=true`; this does not authenticate a user. To enable real login, use your public Web OAuth client ID as documented in GOOGLE_SIGN_IN_SETUP.md and register the Android package/signing certificate in the same project.

ResumableDownload is tested against a local MockWebServer with real HTTP bodies, range headers, connection loss, and 403 recovery. PcmMp3Encoder is tested with real PCM; its generated MP3 is independently inspected/decoded with ffprobe/ffmpeg. Android's MediaCodec-to-PCM path and Google account picker still require physical-device acceptance. The queue tests use a real unprepared ExoPlayer timeline, not a playback-service mock, to test stable queue identity and rendering without requiring hardware audio.

## Android 14–17 compatibility build (0.4.0)

The app's minimum SDK is 34; compile and target SDK are 37 (Android 17). Use Android SDK Platform 37.0, Build Tools 37.0.0, JDK 21, Android Gradle Plugin 9.3.3, and the checksum-pinned Gradle 9.5.0 wrapper. Shared prerequisites remain under /workspace/toolchains. The official current Android CLI can install SDK packages with `android --no-metrics --sdk=/absolute/sdk/path sdk install 'platforms;android-37.0' 'build-tools;37.0.0'`; the older `sdkmanager --licenses` flow is no longer used by the current CLI.

Robolectric 4.17 supplies API 34, 35, 36, and 37. AndroidCompatibilityTest executes the actual naming/selection widgets and SQLite persistence on each framework. The desktop test JVM exports only `java.base/jdk.internal.access` to enable Robolectric's Android 16/17 shared-memory file-descriptor bridge; this flag is not an Android app permission or a runtime security change.

A framework test is not a device install. The generic Android 17 install failure still needs package-manager output or a physical-device retry to diagnose. APK signatures and declared SDK range are verified separately. Keep the existing signing key; changing it breaks in-place updates and is unrelated to the visible app rename.

## Android 10–17 compatibility build (0.5.0)

Minimum SDK is now 29; compile/target remain 37. Build the delivered APK with `./gradlew :app:assembleRelease`. Release builds disable debugging and use the original locally retained development signing key for update continuity. Never commit the keystore; building with a newly generated key will not update an installation signed with the original.

MainActivity uses AndroidX ComponentActivity with OnBackPressedDispatcher, ViewCompat/WindowInsetsCompat, and ContextCompat's non-exported receiver registration. Notifications are requested only on API 33+. Audio export and the device test fixture copy streams without InputStream.transferTo. YouTube link parsing uses the older URLDecoder charset-name overload. Core library desugaring (`desugar_jdk_libs` 2.1.5) supplies Java library APIs on older Android runtimes. This goes beyond lowering the manifest minimum.

AndroidCompatibilityTest now covers API 29/30/31/32/33/34/35/36/37. On API 29–32 Robolectric must explicitly grant the app's own declared signature permission `app.cadence.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which Android normally grants at installation. This simulates installation state; the production receiver remains protected against external broadcasts. Test the default build with `./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest`.

The user's Android 17 build is CP3A.260905.009. No direct access to that Pixel is available here, and its install failure remains unresolved until package-manager output or a successful phone retry is supplied. See INSTALLATION.md.

## MuseDock 0.6.0

The checkout and application ID remain `/workspace/Cadence` and `app.cadence`; the remote remains `shivamjswl/Soundzy`. Preserve the `soundzy.account` preference key for Google profile continuity. Release output is still `app/build/outputs/apk/release/app-release.apk`; distribute it as MuseDock-0.6.0.apk.

MusicCatalog exposes explicit `music_songs`, `music_albums` and `videos` modes. YouTubeRepository routes the music modes to NewPipe's YouTube Music extractor; PlaylistSource supplies public album/playlist track lists. Album search pages are deduplicated by URL while track order is retained when saving. No new account credentials are needed.

AudioEqualizer owns an android.media.audiofx.Equalizer on the ExoPlayer audio session, attaches on session-ID changes, and releases with the service. It never uses session 0/global output effects. Its factory seam tests curve interpolation, gain clamping, unsupported devices and release/persistence behavior without claiming audible output in a cloud test. MODIFY_AUDIO_SETTINGS is a normal manifest permission. Device hearing checks are still required for speaker, wired/Bluetooth headphones and output changes.

LyricsSource is injected for UI tests. LyricsRepository uses LRCLIB's exact-match API with bounded timeouts/response sizes, conservative metadata checks, local cache and provider attribution. Lyrics parses repeated timestamps, signed offsets and decimal fractions; active-line lookup is binary search. LyricsPanel follows the current media item, ignores stale requests, labels untimed text and permits seeking only when the player supports it. Local MockWebServer tests verify the HTTP lookup and cache behavior using original test text. Live LRCLIB and YouTube Music requests returned HTTP 403 in this restricted cloud, so actual provider availability must be checked from a phone or an environment with those hosts permitted. Do not treat fixture tests as live service verification.
