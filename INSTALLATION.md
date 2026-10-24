# Install Soundzy 0.4.0

Use the complete, standalone APK from the **downloads** branch:

[Download Soundzy 0.4.0](https://raw.githubusercontent.com/shivamjswl/Soundzy/downloads/Soundzy-0.4.0.apk)

The APK declares minimum SDK 34 (Android 14) and target SDK 37 (Android 17), with no maximum SDK and no device-ABI restriction. It has no packaged native `.so` libraries. Its signing certificate matches the previously supplied Cadence/Soundzy APK, and its versionCode is 4, so Android can update those versions while preserving data. This is a development-signed build.

## Create a playlist from downloaded songs

1. Open **Offline**.
2. Tap **Create playlist from downloads**.
3. Name it and tap **Save** or the keyboard **Done** action.
4. Choose songs (or **Select all**) and tap **Create playlist**.
5. The saved playlist opens in **Library** and can play its downloaded songs without a connection.

To create an empty playlist, use **Library → Create playlist**. In a playlist, use **Add downloaded songs**, or use the visible **+** beside a song to add it to a playlist.

## If Android says “Something went wrong. App not installed”

That generic message does not identify the package-manager failure. A minimum SDK of 36 blocked Android 14 and 15 in the earlier APK; it did not, by itself, block Android 17. Version 0.4.0 lowers the minimum and updates the build/signature, but a successful installation on the affected Android 17 phone is still unverified.

Download the new APK completely from the direct link, then open it in Files and permit installation from that specific file manager if Android requests it. Keep the original app installed for an in-place update. Confirm adequate free storage. The expected APK size and SHA-256 checksum are published alongside the download.

If it still fails and you have Android Platform Tools on a computer, connect the phone with USB debugging authorized and run:

```bash
adb install -r Soundzy-0.4.0.apk
```

This keeps app data on a compatible update and reports a concrete reason. Useful examples:

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: an installed copy is signed with a different key. That must be resolved without assuming the data can be discarded.
- `INSTALL_FAILED_VERSION_DOWNGRADE`: the installed version has a higher versionCode than this APK.
- `INSTALL_FAILED_INSUFFICIENT_STORAGE`: free device storage is too low.
- `INSTALL_FAILED_OLDER_SDK`: the device is below the APK's declared minimum (Android 14).
- An invalid/corrupt APK error: compare the downloaded file's size/checksum to the published values.
- A device-policy, developer-verification, or installer block: follow the phone's own diagnostic guidance; rebuilding the playlist UI does not establish that such a block has been resolved.

Report the exact `adb install` result if available. No account password or token is needed. Soundzy cannot bypass device-management or system install policy.
