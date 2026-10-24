# Soundzy 0.3.0 validation

Checked on 24 October 2026. Original Cadence playback was reported working by the user on an Android phone; that is user-provided evidence, not a cloud reproduction of this update.

| Check | Result |
| --- | --- |
| Default debug APK build | Passed |
| APK identity | `app.cadence`, versionCode 3, versionName 0.3.0, visible label Soundzy |
| Android requirements | min SDK 36, target SDK 36 |
| APK signature | Verified; same certificate as the supplied Cadence 0.2.0 APK |
| JVM / Android 16 framework tests | 51 passed; 0 failed; 0 skipped |
| Android lint | 0 errors; 4 advisory warnings (storage allocation and localization) |
| Optional Google integration | Compiled successfully with `-PgoogleSignIn=true` without inventing a client ID |
| Default APK optional Google libraries | Not bundled; checked dex contents |
| Google account picker / live authentication | Not executed: no OAuth client registrations supplied; setup guide included |
| Takeout parser / import | Tested locally with JSON, HTML, BOM, duplicates, invalid links, limits, and preservation of existing metadata/timestamps |
| Queue | Real ExoPlayer timeline tests for current/upcoming/earlier sections, shuffle coverage, stable identities after removal/reorder, empty state, and native-graphics rendering |
| Download recovery | Real HTTP fixture tests for multiple ranges, mid-body disconnection, persisted partial-file resume, servers ignoring Range, expired-link 403 recovery, cancellation, and changed source identity |
| MP3 encoding | Real PCM encoded to MP3; independently inspected and fully decoded by ffprobe/ffmpeg |
| Android MediaCodec decoding / full on-device conversion | Not executed on a physical phone |
| Live YouTube playback / downloads | Not verified in this cloud; proxy previously denied YouTube requests |
| Instrumentation APK | Compiled; not executed (hardware acceleration unavailable; prior software emulator could not complete boot) |

The old download client had a fixed 70-second total call timeout, which is a concrete cause that could explain long transfers failing halfway. The user's exact failing network transaction was not reproduced. The new downloader removes that limit, enforces bounded read/connect timeouts, retries bounded ranges, checks byte positions/lengths, and resumes only a matching stream prefix. A phone check remains necessary to establish that this fixes the reported YouTube failure.

The two-second generated stereo PCM fixture produced an MP3 with a decoded stream at 44100 Hz and 2 channels, approximately 2.037542 seconds including normal encoder padding. This validates the MP3 encoder, not Android's MediaCodec decoder on every supported phone.

Google sign-in cannot import YouTube watch history through the official API. The default APK deliberately leaves sign-in disabled until OAuth setup is completed. It includes a functional local Takeout import flow. See GOOGLE_SIGN_IN_SETUP.md. No claim of direct account-history synchronization is made.

All `*-framework.png` images are actual app views rendered through Robolectric native graphics with clearly labeled test data; they are not screenshots of a physical phone. Generated encoder/test audio is not a bundled music catalog.

APK SHA-256: `34fcf06381bce07398ead0538cafcd298af529adb1ab14485273d9078d9ca8dc`

## Phone acceptance

1. Install as an update over Cadence; confirm existing playlists and offline files remain and launcher/info say Soundzy.
2. Open the queue from the header/player; inspect artwork, current/upcoming sections, reorder/remove, shuffle, and clearing.
3. Save a permitted song as Original audio, including a transfer longer than 70 seconds. Interrupt the connection and retry; confirm the prefix resumes when compatible.
4. Save another song as MP3, or convert an offline copy from its menu. Verify progress, cancellation, and playback in airplane mode.
5. Export an offline file through Android's picker and play it in another audio player.
6. Import a small real Google Takeout JSON/HTML file, select songs, and verify the resulting playlist and recent order.
7. Configure OAuth using the guide, rebuild/sign with the same key, and test the native Google account picker and sign-out separately.
