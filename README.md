# Soundzy downloads

Developed by Shivam Jaiswal. Version **0.5.0**, minimum **Android 10 (API 29)**, target Android 17 (API 37).

**[Download Soundzy 0.5.0 APK](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.5.0.apk)**

[Complete corresponding source](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.5.0-source.zip) · [APK SHA-256](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.5.0.apk.sha256)

This is a standalone, non-debuggable release-mode APK signed with the existing development key. Keep the previous Cadence/Soundzy installation and install as an update to preserve data. It is not a Play Store release. Checksum files accompany the APK and source archive.

Version 0.5.0 adds compatible Back navigation, screen/keyboard insets, receiver registration, notification handling, audio export and Java backports for Android 10+. Create your playlists through **Offline → Create playlist from downloads** or **Library → Create playlist**. Existing playlists offer **Add downloaded songs**, and songs have a visible + action.

All 97 JVM/framework tests passed, including 45 custom-playlist checks across API 29–37. Debug and release lint passed with zero errors. APK signing and alignment checks passed, and the certificate matches 0.4.0. These are framework/package checks, not physical-device installation results.

The cause of the reported failure on Pixel Android 17 build **CP3A.260905.009** is still unknown. This version changes packaging but has not been installed on that phone here. If it still fails, retain the exact result of `adb install -r Soundzy-0.5.0.apk`; see [installation diagnostics](https://github.com/shivamjswl/Soundzy/blob/main/INSTALLATION.md). Lowering minimum SDK alone does not diagnose an Android 17 rejection.

The app retains Soundzy branding, a live artwork queue, resumable offline saves, local MP3 conversion/export, YouTube playlist discovery and local Google Takeout history import. Optional Google sign-in requires OAuth setup in the source documentation and cannot expose YouTube watch history. No Google OAuth client was supplied, so sign-in remains unconfigured in this APK. Live YouTube availability and device audio conversion still require phone checks.

Application source is GPL-3.0-or-later. Third-party licenses and corresponding source are included in the source archive. The main branch contains the application source; this downloads branch distributes the APK and complete source archive. Earlier downloads remain available by filename.
