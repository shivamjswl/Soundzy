# Soundzy 0.4.0 validation

Checked on 24 October 2026. The user clarified that the playlist issue was the lack of an obvious creation action from downloaded songs, rather than a confirmed Save-dialog failure. The prior 0.3.0 Library creation flow passed a baseline UI test before changes.

| Check | Result |
| --- | --- |
| Debug APK and instrumentation APK assembly | Passed |
| APK identity | `app.cadence`, versionCode 4, versionName 0.4.0, label Soundzy |
| Android SDK range | Minimum API 34 (Android 14), target/compile API 37 (Android 17), no maximum SDK |
| APK crypto verification | Passed for API 34–37, v3 signature |
| Upgrade signing key | Exact SHA-256 certificate digest matches 0.3.0 |
| APK alignment | zipalign passed, including 16 KiB alignment validation |
| ABI constraints | No native `.so` libraries or ABI-specific APK splits |
| JVM/framework tests | 72 passed; 0 failed; 0 skipped |
| Custom-playlist framework matrix | 5 tests on each of API 34/35/36/37: all 20 passed |
| Android lint | Passed with zero errors; advisory warnings retained in validation/lint-results.txt |
| Physical installation / live YouTube on Android 14–17 | Not executed in this cloud |

The matrix tests interact with the actual activity's widgets and SQLite database. They verify immediate creation/persistence of a personal playlist, creating a playlist from selected downloaded songs without search, adding downloaded songs to an existing empty playlist, blank-name validation and keyboard Done, and the visible per-song add-to-playlist action. Existing history, download-range recovery, MP3 encoder, queue, and playlist-discovery tests also passed.

The minimum SDK change removes the earlier hard incompatibility with Android 14 and 15. A minimum SDK of 36 never prohibited Android 17 by itself. The reported Android 17 error, “Something went wrong. App not installed,” is generic. It has not been reproduced on that phone and its precise cause remains unknown. A successful framework run and cryptographic/package verification are not a device-install result. The new package is prepared for Android 14–17 and keeps the old signing key, but phone installation still needs confirmation; INSTALLATION.md gives a non-destructive diagnostic command.

Build toolchain: JDK 21, AGP 9.3.3, Gradle 9.5.0 with pinned distribution checksum, Android Platform 37.0, Build Tools 37.0.0, protobuf plugin 0.10.0, Robolectric 4.17. Test JVM export of `java.base/jdk.internal.access` is required by Robolectric's Android shared-memory bridge; this is not an app permission or a change to device security.

The deprecated onBackPressed override was removed. All supported versions use the existing platform OnBackInvokedDispatcher callback. No SQLite schema change or data reset is introduced. Existing playlists, downloads, and local history are retained by a same-key update.

Google sign-in still needs OAuth setup and cannot expose YouTube watch history. Takeout import remains local. The prior real-PCM MP3 fixture is independently decodable; Android MediaCodec conversion and live YouTube behavior still require phone checks. Framework screenshots contain explicit test fixtures, not real users' music.

APK SHA-256: `d08cc3b7c4caf73ff81f30c6279c7c956cebbed3948fd18dcc027508e469bd4f`

## Phone acceptance

1. Download the complete new 0.4.0 APK and install it as an update over 0.3.0. Confirm retained data on Android 14/15/16/17 as applicable.
2. Open Offline → Create playlist from downloads, name it, select songs, and save. Confirm the new playlist opens in Library and plays downloaded songs in airplane mode.
3. Create an empty playlist in Library; use Add downloaded songs. Verify the **+** next to any song also opens playlist selection.
4. Exercise Back navigation, search, streaming, and offline saves on each supported device.
5. If installation on the affected Android 17 phone still fails, obtain the exact `adb install -r Soundzy-0.4.0.apk` package-manager error as described in INSTALLATION.md. No device policy or signing-conflict bypass is claimed.
