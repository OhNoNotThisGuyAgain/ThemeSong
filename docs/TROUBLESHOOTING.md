# Troubleshooting

## Sign-in / authorization

**"Connect" does nothing or no browser opens**
- `SPOTIFY_CLIENT_ID` is empty. Set it in `secrets.properties` and rebuild. The onboarding step shows a hint when the client id is missing.

**Browser opens but redirect fails / "INVALID_CLIENT" / "redirect uri mismatch"**
- The redirect URI on the dashboard must be **exactly** `spotzones://auth` and match `SPOTIFY_REDIRECT_URI`.
- The package + SHA-1 must be registered. Debug builds use `com.spotzones.debug`.

**"Sign-in could not be verified"**
- The OAuth `state` didn't match (possible if you started two sign-ins). Try again from a single tap.

**Signed in, but playback commands return 403**
- Spotify **Premium** is required for remote control.

**Playback commands return 404 / "No active device"**
- Open the Spotify app and play something once to make a device active, then retry. The error message in-app says the same.

## Geofencing / zones not triggering

**Music never changes automatically**
1. Grant **location**, and for background switching choose **Allow all the time** (Android 10+ background location).
2. Ensure **Automation enabled** is on (Settings) and there's no active **manual override** (Dashboard shows "Automation paused").
3. Geofences need a moment and some movement to settle; the dwell delay debounces zone edges (~30 s).
4. Battery optimizers (Samsung, Xiaomi, etc.) can kill background work — exclude PlayZones from aggressive battery management.

**Zones stop working after a reboot**
- They should re-arm automatically (`BootCompletedReceiver`). If a vendor blocks boot receivers, open the app once to re-sync.

**More than ~95 zones**
- The OS caps geofences at 100/app. PlayZones registers the nearest ~95 and re-syncs as you move; very large zone counts may have a short delay when entering a far-away cluster.

## Map

**Map is blank / grey**
- The device needs internet access to download OpenStreetMap tiles. Check Wi‑Fi or mobile data.
- Some networks block tile servers; try another connection.

## Build

**`secrets.properties` not found**
- It's optional; the build falls back to empty values (Spotify sign-in disabled). Copy from `secrets.properties.example` to enable them.

**Gradle wrapper missing**
- Run `gradle wrapper` once (or let Android Studio generate it).

**App Remote `.aar` errors**
- Only relevant if you opted into the App Remote SDK. Ensure the `.aar` is in `app/libs/` and the `implementation(name = …, ext = "aar")` line is uncommented.

## Notifications

**No persistent control notification (Android 13+)**
- Grant the **notifications** permission (onboarding or system settings).
