# SpotZones

**A location-aware Spotify companion that automatically changes your music based on where you are and what you're doing.**

SpotZones does **not** replace Spotify. You keep using Spotify exactly as you do today — your playlists, Spotify Connect, premium audio, and playback controls are all untouched. SpotZones sits alongside it and intelligently tells Spotify *what* to play *when*, based on geofenced **Zones** and contextual triggers.

> Enter the gym after 5 PM with your AirPods connected → Workout playlist fades in.
> Drive above 50 mph → Road Trip playlist.
> Home at night → Sleep playlist.

---

## Table of contents

- [Highlights](#highlights)
- [Screens](#screens)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Build & run](#build--run)
- [Spotify developer setup](#spotify-developer-setup)
- [Google Maps setup](#google-maps-setup)
- [How automation works](#how-automation-works)
- [Swapping to the App Remote SDK](#swapping-to-the-app-remote-sdk)
- [Testing](#testing)
- [Battery & privacy](#battery--privacy)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Highlights

- **Zones** with GPS center, radius, playlist, shuffle/repeat, crossfade, volume, fade-in, schedule (days + time window), priority (1–100) and advanced triggers.
- **Priority resolution** — when zones overlap, higher priority wins; ties break toward the *smaller* (more specific) geofence, so a tight "Bedroom" overrides a large enclosing "Home".
- **Advanced triggers** with AND/OR/NOT logic over location, time, day, Bluetooth device, charging, battery level, Wi-Fi, movement/speed and headphones.
- **Rule engine** that is pure and exhaustively unit-tested.
- **Smart transitions** — immediate / finish-current-song / crossfade, with per-playlist resume ("continue where you left off").
- **Now Playing** screen kept bidirectionally in sync with Spotify.
- **Interactive map** — long-press to create a zone, radius circles, dark mode.
- **Manual override** — pause automation for 15 min / 30 min / 1 h / until you leave the zone / until tomorrow / indefinitely.
- **History**, **search**, **JSON + AES-256 encrypted backups**, and **import/export**.
- **Battery-first** design built on the OS Geofencing API and WorkManager — no continuous GPS polling.
- **Material 3**, AMOLED dark theme, dynamic color, accent themes, edge-to-edge, accessible.
- **Future-AI abstractions** (playlist prediction, habit learning, schedule learning) wired as interfaces with deterministic no-op defaults.

## Screens

| Dashboard | Map | Now Playing | Zone editor | History | Settings | Search |
|-----------|-----|-------------|-------------|---------|----------|--------|
| Now playing, current zone, automation toggle, recent transitions, zone list | Zone markers + radius circles, long-press to create | Album art, scrubber, transport, shuffle/repeat, zone context | Every zone property incl. schedule & triggers, duplicate/export/delete | Searchable transition timeline | Theme, automation, backups, Spotify account, privacy | Zones, playlists, artists, songs |

---

## Architecture

SpotZones follows **Clean Architecture + MVVM** with a strict dependency rule: `presentation → domain ← data`. The **domain** layer is pure Kotlin (no Android types) and depends on nothing; everything else depends on it via interfaces (ports), and concrete implementations (adapters) live in `data`/`location`/`automation` and are bound with Hilt.

```
┌──────────────────────────────────────────────────────────────┐
│ presentation (Compose, ViewModels, navigation)                │
│   observes StateFlows, sends intents                          │
└───────────────┬──────────────────────────────────────────────┘
                │ depends on
┌───────────────▼──────────────────────────────────────────────┐
│ domain (pure Kotlin)                                          │
│   models · RuleEngine · ConditionEvaluator                   │
│   ports: ZoneRepository, SpotifyController, GeofenceManager,  │
│          ContextProvider, BackupManager, SpotifyCatalog, AI   │
└───────────────▲──────────────────────────────────────────────┘
                │ implemented by
┌───────────────┴──────────────────────────────────────────────┐
│ data / location / automation (Android)                        │
│   Room · DataStore · Retrofit(Web API) · Fused/Geofencing     │
│   AutomationCoordinator · AutomationService · Workers         │
└──────────────────────────────────────────────────────────────┘
```

**Why these choices**

| Decision | Reason |
|----------|--------|
| Pure `RuleEngine` + `ConditionEvaluator` | Deterministic, side-effect-free, 100% JVM-unit-testable; sensors/IO stay outside. |
| Ports & adapters around Spotify/location | Lets us ship a Web-API playback controller now and swap in the App Remote SDK later with one DI line change. |
| OS Geofencing API (not GPS polling) | The OS wakes us only on enter/exit transitions → excellent battery. |
| `Outcome<T>` instead of exceptions across boundaries | Forces graceful failure handling — the app degrades, never crashes. |
| Conditions/Actions as sealed + JSON columns | Deep, evolving trees persist & export with one serializer; schema stays stable as the model grows. |
| Idempotent `AutomationCoordinator` | Geofence jitter and periodic re-checks are cheap no-ops unless the winning zone actually changes. |

See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for the full deep-dive and dependency graph.

---

## Project structure

```
app/src/main/java/com/spotzones/
├── SpotZonesApp.kt                Application (Hilt, WorkManager, Timber)
├── core/                          cross-cutting: serialization, notifications,
│                                  permissions, crash abstraction, dispatchers
├── domain/                        ❶ pure business layer
│   ├── model/                     Zone, Condition, Action, Rule, Schedule, …
│   ├── engine/                    RuleEngine, ConditionEvaluator
│   ├── repository/  spotify/  location/  backup/  ai/   (ports)
│   └── util/Outcome.kt            Outcome + DomainError
├── data/                          ❷ persistence + network adapters
│   ├── local/                     Room (entities, DAOs, converters, db)
│   ├── remote/                    Web API (Retrofit), auth (PKCE), controller
│   ├── settings/  security/  backup/  repository/
├── location/                      Fused location, geofencing, context, movement
├── automation/                    Coordinator, foreground service, workers, boot
├── di/                            Hilt modules
├── presentation/                  ❸ Compose UI
│   ├── navigation/                routes, NavHost, bottom bar
│   ├── components/  util/
│   └── screens/                   dashboard, map, nowplaying, zoneeditor,
│                                  history, settings, search, onboarding
└── ui/theme/                      Material 3 theme, color, type, shape
```

---

## Build & run

### Requirements
- **Android Studio** Koala (or newer) / AGP 8.5
- **JDK 17**
- **Android SDK 34**, min SDK 26
- A device or emulator with the **Spotify app installed and signed into a Premium account** (Premium is required by Spotify for remote playback control)

### Steps
1. Clone the repo and open it in Android Studio (it will generate the Gradle wrapper if missing — or run `gradle wrapper`).
2. Copy `secrets.properties.example` → `secrets.properties` and fill in your keys (see below).
3. Add the SHA-1 of your debug keystore and the redirect URI to the Spotify dashboard (see below).
4. Run the `app` configuration.

> Secrets can also be supplied via environment variables or `~/.gradle/gradle.properties` for CI — `app/build.gradle.kts` reads `secrets.properties` first, then project/env.

---

## Spotify developer setup

SpotZones uses the **Authorization Code flow with PKCE** (a public client — **no client secret is ever shipped**).

1. Go to the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) → **Create app**.
2. Note the **Client ID** → put it in `secrets.properties` as `SPOTIFY_CLIENT_ID`.
3. Under **Edit settings → Redirect URIs**, add **exactly**: `spotzones://auth` (matches `SPOTIFY_REDIRECT_URI`).
4. Under **Edit settings → Android packages**, add package `com.spotzones` (and `com.spotzones.debug` for debug builds) with your keystore **SHA-1 fingerprint**:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Make sure your Spotify account is added under **Users and Access** while the app is in development mode.

Scopes requested: `user-read-private`, `user-read-playback-state`, `user-modify-playback-state`, `playlist-read-private`, `playlist-read-collaborative`, `app-remote-control`, `streaming`.

Full step-by-step with screenshots: **[docs/SPOTIFY_SETUP.md](docs/SPOTIFY_SETUP.md)**.

## Google Maps setup

1. In Google Cloud Console, enable **Maps SDK for Android** and create an API key.
2. Restrict it to your app's package + SHA-1.
3. Put it in `secrets.properties` as `MAPS_API_KEY` (injected into the manifest `meta-data`).

---

## How automation works

```
OS Geofence transition ─┐
Periodic safety tick ────┼──► AutomationWorker / AutomationService
Manual resume ───────────┘                  │
                                            ▼
                              AutomationCoordinator.evaluate()
                                            │
   1. respect global toggle + manual override (short-circuit)
   2. build EvaluationContext (location, time, sensors, system state)
   3. RuleEngine.evaluate(zones, rules, context) → winner by priority
   4. if winner changed → SpotifyController.apply(playback) + record history
   5. persist active candidate (idempotent next time)
```

- **Geofences** are registered with the OS (`GeofenceManager`). On reboot/app-update they're re-armed by `BootCompletedReceiver` → `GeofenceSyncWorker`.
- The **foreground service** runs only while you want continuous monitoring; it feeds the movement tracker and runs a sparse safety re-evaluation for non-geofence conditions (time, battery, Bluetooth).
- **Per-playlist positions** are saved so returning to a zone resumes where you left off.

---

## Swapping to the App Remote SDK

By default the active `SpotifyController` is `WebApiSpotifyController`, which controls playback through Spotify's **Web API "Connect" endpoints** — fully open-source dependencies, no proprietary `.aar`, works across any Connect device.

To use the richer **App Remote SDK** instead (lower latency, works without an "active device", local control):

1. Download the **Spotify App Remote** `.aar` from the [Spotify Android SDK](https://developer.spotify.com/documentation/android) and drop it into `app/libs/`.
2. Uncomment the `implementation(name = "spotify-app-remote-release-…", ext = "aar")` line in `app/build.gradle.kts`.
3. Implement `SpotifyController` against `com.spotify.android.appremote.api.SpotifyAppRemote` (a `AppRemoteSpotifyController`).
4. Change **one line** in `di/RepositoryModule.kt`:
   ```kotlin
   @Binds @Singleton abstract fun spotifyController(impl: AppRemoteSpotifyController): SpotifyController
   ```

Nothing else in the app changes — that is the whole point of the port/adapter boundary.

---

## Testing

Pure-JVM unit tests cover the heart of the app:

```bash
./gradlew testDebugUnitTest
```

| Test | What it guards |
|------|----------------|
| `RuleEngineTest` | priority resolution, radius tie-break, schedule gating, rule-vs-zone |
| `ConditionEvaluatorTest` | every condition type, AND/OR/NOT, missing-signal safety |
| `ScheduleTest` | time windows incl. crossing midnight |
| `GeoCoordinateTest` | haversine distance, validation |
| `SerializationRoundTripTest` | Condition/Action/Zone JSON ↔ entity fidelity |
| `AutomationCoordinatorTest` | idempotency, override suppression, enter/leave transitions |
| `PassphraseCryptoTest` (Robolectric) | AES-256-GCM backup encryption correctness |
| `SettingsViewModelTest` | ViewModel → repository wiring |

Instrumented tests use a Hilt test runner (`com.spotzones.HiltTestRunner`).

---

## Battery & privacy

- **No continuous GPS.** Detection is delegated to the OS Geofencing API; the fused location stream runs at balanced-power only while the foreground service is active.
- **Geofence cap aware.** When you have >95 zones, the nearest are registered and re-synced as you move.
- **Tokens are encrypted** at rest (`EncryptedSharedPreferences`, AES-256, Keystore) and excluded from cloud backup.
- **No secrets in source.** PKCE means no client secret; keys live in git-ignored `secrets.properties`.
- **Analytics off by default.** Crash reporting is an abstraction (`CrashReporter`) with a no-op default; nothing leaves the device unless you wire and enable a backend.

---

## Troubleshooting

Common issues (Spotify won't connect, no active device, geofences not firing, background location) are covered in **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)**.

## Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)** for coding standards, branch/commit conventions and the PR checklist.

---

*SpotZones is an independent project and is not affiliated with or endorsed by Spotify AB.*
