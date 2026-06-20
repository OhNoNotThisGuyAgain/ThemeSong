# Spotify developer setup

SpotZones controls playback through your own Spotify Developer app using **Authorization Code + PKCE**. No client secret is ever embedded in the app.

## 1. Create a Spotify app
1. Sign in at <https://developer.spotify.com/dashboard>.
2. Click **Create app**.
3. Name it (e.g. "SpotZones – dev"), accept the terms, **Save**.

## 2. Get the Client ID
1. Open the app → **Settings**.
2. Copy **Client ID**.
3. In the repo, copy `secrets.properties.example` → `secrets.properties` and set:
   ```properties
   SPOTIFY_CLIENT_ID=paste_client_id_here
   SPOTIFY_REDIRECT_URI=spotzones://auth
   ```

## 3. Register the redirect URI
1. In **Settings → Redirect URIs**, add **exactly**:
   ```
   spotzones://auth
   ```
2. Click **Add**, then **Save**. (It must match `SPOTIFY_REDIRECT_URI` character-for-character.)

## 4. Register the Android package + SHA-1
Spotify ties Android apps to a package name and signing fingerprint.

1. Get your debug SHA-1:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore \
     -alias androiddebugkey -storepass android -keypass android
   ```
   For release builds, run the same against your release keystore.
2. In **Settings → Android Packages**, add:
   - Package: `com.spotzones.debug` (debug) and/or `com.spotzones` (release)
   - SHA-1: the fingerprint(s) from step 1
3. **Save**.

## 5. Add yourself as a user
While the app is in **Development mode**, only allow-listed users can authorize it.
1. Open **User Management** (or **Users and Access**).
2. Add the email of the Spotify account you'll test with.

## 6. Scopes
SpotZones requests these scopes during sign-in — no extra dashboard config needed:

```
user-read-private
user-read-playback-state
user-modify-playback-state
playlist-read-private
playlist-read-collaborative
app-remote-control
streaming
```

## 7. Premium requirement
The Web API player endpoints (play/pause/skip/volume) **require Spotify Premium** and an **active device**. Open Spotify and start playing once so a device is active; SpotZones can then take over.

## Verifying
1. Build & run SpotZones.
2. In onboarding, tap **Connect** → a browser opens the Spotify consent screen.
3. Approve → you're redirected back via `spotzones://auth` and the dashboard shows **Connected**.

If the redirect fails, double-check the redirect URI and that `com.spotzones.debug` + SHA-1 are registered. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
