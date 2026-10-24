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
- Robolectric tests execute Android 16 framework code for real SQLite persistence and screen navigation. The UI test deliberately treats the playback service as unavailable because Robolectric does not host its MediaSession Binder. This does not verify audio playback.
- Instrumentation tests in `app/src/androidTest` exercise a real Android device, including Media3 playback of a generated local test tone. Run them with `./gradlew :app:connectedDebugAndroidTest` on a usable Android 16+ device.
- Live YouTube checks require real network access to YouTube and its returned media/image hosts. This cloud's proxy denied the YouTube requests. No live extraction or streaming success is claimed.

The AOSP Android 16 emulator could not finish booting within the test window on this machine without hardware acceleration. Instrumentation tests were compiled but were not executed. Framework renders in `validation/*-framework.png` are actual app views drawn through Robolectric's native graphics mode, not screenshots from a phone.

For 0.2.0, `PlaylistSource` is the contract for public playlist discovery and paging. Production uses NewPipe through `YouTubeRepository`; framework tests inject deterministic playlist responses. These tests cover discovery/navigation/import but do not establish live YouTube compatibility.

For Soundzy 0.3.0, the default APK uses the no-Google source set. The optional Google integration was compiled separately with `-PgoogleSignIn=true`; this does not authenticate a user. To enable real login, use your public Web OAuth client ID as documented in GOOGLE_SIGN_IN_SETUP.md and register the Android package/signing certificate in the same project.

ResumableDownload is tested against a local MockWebServer with real HTTP bodies, range headers, connection loss, and 403 recovery. PcmMp3Encoder is tested with real PCM; its generated MP3 is independently inspected/decoded with ffprobe/ffmpeg. Android's MediaCodec-to-PCM path and Google account picker still require physical-device acceptance. The queue tests use a real unprepared ExoPlayer timeline, not a playback-service mock, to test stable queue identity and rendering without requiring hardware audio.
