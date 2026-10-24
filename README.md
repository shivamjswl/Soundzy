# Soundzy downloads

Developed by Shivam Jaiswal. Version 0.4.0, Android 14+.

**[Download Soundzy APK](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.4.0.apk)**

[Complete corresponding source](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.4.0-source.zip)

Install as an update over the previous Cadence/Soundzy app to retain local data. This is a development-signed test build. SHA-256 checksum files are included alongside the downloads.

This release adds Soundzy branding, an artwork queue, resumable downloads, optional on-device MP3 conversion/export, and local Google Takeout history import. Google sign-in requires the OAuth setup described in the source archive; the downloadable build has no client configured. Google sign-in does not expose YouTube watch history. Live YouTube downloads and Android device codec conversion need real-phone validation.

Application source is GPL-3.0-or-later. Third-party components retain their licenses and notices, included in the source archive. The main branch contains the Soundzy Android application source; this branch distributes its APK and complete corresponding source archive.


## 0.4.0 update

Create a personal playlist directly from Offline, select downloaded songs, and find it in Library. Existing playlists expose Add downloaded songs, and song rows have a visible + action.

The APK now declares minimum API 34 and target API 37, and uses the same certificate as earlier Soundzy downloads. All 72 JVM/framework tests passed, including five playlist workflows on each of Android 14, 15, 16, and 17. APK signature verification passed across API 34–37. Actual installation on the phone reporting the generic Android 17 error still requires confirmation; see INSTALLATION.md on main for diagnostics.
