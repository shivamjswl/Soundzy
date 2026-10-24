# Soundzy 0.5.0 validation

Checked on 24 October 2026.

| Check | Result |
| --- | --- |
| Release APK and debug instrumentation APK assembly | Passed |
| APK identity | `app.cadence`, versionCode 5, versionName 0.5.0, label Soundzy |
| Android SDK declarations | Minimum API 29 (Android 10), compile/target API 37 (Android 17); no maximum SDK |
| Release packaging | Debugging disabled; no testOnly flag; standalone APK |
| APK crypto verification | Passed for API 29–37; v3 signature |
| Upgrade signing key | Certificate SHA-256 digest matches the previously distributed 0.4.0 APK |
| APK alignment | zipalign passed, including 16 KiB alignment validation |
| ABI constraints | No native `.so` libraries or ABI-specific APK splits |
| Java API backports | Core library desugaring enabled; `j$/util/` classes present in APK DEX |
| JVM/framework tests | 97 passed; 0 failed; 0 skipped |
| Custom-playlist framework matrix | 5 workflows × API 29/30/31/32/33/34/35/36/37 = 45 cases, all passed |
| Debug and release Android lint | Zero errors; advisory warnings recorded under validation/ |
| Physical installation on Android 10–17 | Not executed in this cloud |
| Installation on Pixel build CP3A.260905.009 | Still unresolved/unverified; no package-manager output supplied |

The framework matrix exercises actual activity widgets and SQLite persistence: personal playlist creation, creating a playlist from selected downloaded songs, adding downloaded songs to an existing playlist, blank-name validation and keyboard Done, and the visible per-song add-to-playlist action. Existing history, HTTP download recovery, real-PCM MP3 encoder, queue timeline, and discovery tests also pass. Framework results do not prove live YouTube playback, Android device codec behavior, or phone installation.

Android 10 support required AndroidX Back navigation, compatible window insets and receiver registration, a notification-permission API guard, a compatible stream-copy loop, and Java library desugaring. On API 29–32 the framework test harness explicitly grants the app's own declared signature permission, matching installation state on Android; production receiver protection remains enabled. Test JVM exports for Robolectric shared memory do not change device permissions. The device test fixture also uses a compatible copy loop.

The supplied Settings screenshot confirms Android 17 and build CP3A.260905.009. It does not reveal why the package installer rejects Soundzy. The previous generic message was “Something went wrong. App not installed.” The original APK already targeted API 37, and lowering minimum SDK alone cannot resolve that failure. This APK uses a non-debuggable release build with the existing development signing key; this is a packaging improvement, not a confirmed diagnosis or guarantee of installation on that Pixel. No package name, database schema, or signing-key replacement is introduced.

Build toolchain: JDK 21, Android Gradle Plugin 9.3.3, pinned Gradle 9.5.0, Android SDK Platform 37.0, Build Tools 37.0.0, Robolectric 4.17. Source dependencies remain pinned; the complete source archive includes extractor, nanojson, jump3r, and desugar_jdk_libs source. Google sign-in remains optional and requires OAuth setup; it does not grant YouTube history access. Takeout import stays local.

APK SHA-256: `475378168e6ac3c2c5784945bf3aca78e788ad79c58c1187489210632a82460b`

## Phone acceptance still required

1. Download the complete 0.5.0 APK and install it as an update while keeping the existing app and data.
2. Confirm installation/launch on Android 10, 11, 12, 12L, 13, 14, 15, 16, and the affected Pixel Android 17 build where devices are available.
3. Exercise Back navigation, gesture/three-button insets, keyboard sizing, notification permission prompts on API 33+, and audio export.
4. Create a playlist from Offline, select songs, and play the saved playlist in airplane mode. Test adding more downloads and the song-row + action.
5. Check search, streaming, resumed downloads, and device MP3 conversion.
6. If the affected Pixel still rejects the APK, use `adb install -r Soundzy-0.5.0.apk` and retain the complete package-manager error; see INSTALLATION.md. No destructive uninstall or policy bypass is needed for diagnosis.
