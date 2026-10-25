# Google sign-in setup for MuseDock

This is an external prerequisite, not a key bundled with the test app. The supplied MuseDock APK has no OAuth client configured, so its Google sign-in button is disabled. Music playback, offline saves, playlists, and local Takeout history import remain usable without login.

**Google sign-in cannot import YouTube watch history.** Google's official YouTube API does not expose it. The implemented sign-in obtains a Google profile only. There is no YouTube history scope, cookie scraping, password capture, or embedded Google login web page. Use a user-selected Google Takeout JSON/HTML file to import history.

## Create the registrations

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create or select your own project.
2. In **Google Auth Platform**, configure Branding, Audience, and the OAuth consent screen. Use MuseDock as the application name and your own support email. If the project is in Testing, add the Google accounts that will test it to the test-user list.
3. Create an OAuth client with application type **Android**. Use package name **`app.cadence`**. This technical package ID is retained so compatible MuseDock updates preserve existing data.
4. Register the signing certificate's SHA-1 fingerprint. For the development APK supplied in this chat it is:

   ```text
   8D:19:87:CE:FD:44:12:95:4D:CC:05:6A:6E:FF:8B:58:87:BE:3E:2E
   ```

   For a build signed with a different key, register that key's fingerprint instead. A Play-distributed build uses the Play App Signing certificate, which may differ from a local upload/development key.
5. In the same project create a second OAuth client with application type **Web application**. Its **client ID** is used as the server client ID by Google's Android identity library. No redirect URI or backend is used by this profile-only Credential Manager flow.
6. Copy the public Web client ID, which ends in `.apps.googleusercontent.com`. Do not put a client secret, service-account credential, or private signing key in the app. A Web client ID is public configuration, not a secret.

## Build with Google identity enabled

From the project directory, with the standard Android prerequisites configured:

```bash
./gradlew -PgoogleWebClientId=YOUR_PUBLIC_WEB_CLIENT_ID :app:assembleDebug
```

The build automatically selects `app/src/google/java` and the official Android Credential Manager / Google ID dependencies. The ordinary build with no client ID selects `app/src/noGoogle/java` and does not bundle the optional Google SDK libraries. The optional SDK build is subject to Google's SDK license terms and must be reviewed for compatibility with the distribution's other licenses before redistribution; the supplied default APK contains no Google SDK integration binaries.

For compile-only verification of the Google integration without inventing a client ID:

```bash
./gradlew -PgoogleSignIn=true :app:compileDebugJavaWithJavac
```

That command tests compilation only; it does not enable login or substitute for real Google project registration.

Install using the same signing key as the working installation. In MuseDock, open **Info → Account & history → Sign in with Google**. Google Play services and an account/provider capable of handling Google credentials must be available on the phone. Verify the account picker, successful profile display, cancellation, and sign-out on a real registered device.

Only the returned display name and email are saved in the app's private preferences. The ID token is not stored or uploaded. The app has no backend account, cloud sync, or remote authorization based on that token. If a backend is added later, verify ID-token signatures, issuer, audience, expiry, and nonce on that server before trusting it for any privileged action.

## Import listening history now, without OAuth

1. Open **Library → Import YouTube history**, or **Info → Account & history**.
2. Open [Google Takeout](https://takeout.google.com/). Select **YouTube and YouTube Music**, include **history**, and choose a JSON or HTML watch-history export.
3. Download and unzip the export on the phone.
4. Choose `watch-history.json` or `watch-history.html` in MuseDock.
5. Select entries that are songs, then tap **Import selected**. Watch history may contain ordinary videos; MuseDock does not claim to classify every entry as music.

The selected file is processed locally. JSON timestamps preserve listening order and duplicate videos are merged. HTML imports preserve available titles and channel names, but localized HTML timestamps are not interpreted. Imports are limited to 20 MB and the first 5,000 distinct valid video IDs; the selection screen reports the entry limit. Existing local track metadata and more recent local listening timestamps are retained. The import creates a local playlist of links and does not download audio automatically.

Reference: [Android Credential Manager with Google ID](https://developer.android.com/identity/sign-in/credential-manager-siwg), [YouTube playlist API](https://developers.google.com/youtube/v3/docs/playlistItems/list), and [Google Takeout](https://takeout.google.com/).
