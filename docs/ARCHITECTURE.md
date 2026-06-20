# Architecture

This document explains the design of PlayZones and the reasoning behind each major decision.

## Goals that shaped the design

1. **Never crash; degrade gracefully.** Every external dependency (Spotify, GPS, network, permissions) can fail at any time.
2. **Excellent battery.** Monitoring must be event-driven, not poll-driven.
3. **Testable core.** The decision logic that decides *what plays where* must be verifiable without a device.
4. **Replaceable Spotify backend.** We must be able to switch between the Web API and the App Remote SDK without rippling changes.
5. **Long-term maintainability.** Clean layering, SOLID, no god classes.

## Layering (Clean Architecture)

```
presentation ─┐
              ├─► domain ◄─ data / location / automation
   (Android)  ┘   (pure)        (Android)
```

The **dependency rule**: source code dependencies only point inward, toward `domain`. The domain layer:

- contains **no Android imports** (it uses `java.time`, plain Kotlin, kotlinx-serialization),
- defines **ports** (interfaces) such as `ZoneRepository`, `SpotifyController`, `GeofenceManager`, `ContextProvider`, `BackupManager`, `SpotifyCatalog`, and the AI interfaces,
- owns the **models** and the **RuleEngine**.

Outer layers provide **adapters** that implement those ports and are wired by Hilt. This is what makes the Spotify backend swap a one-line change.

## The decision pipeline

The product reduces to one question asked repeatedly: *given everything true right now, which zone/rule should control playback?*

### `EvaluationContext`
An immutable snapshot assembled by `ContextProvider` from location, time, day, speed, movement, battery, charging, Bluetooth, Wi-Fi and audio route. **Every signal is nullable/neutral when unavailable**, so a denied permission disables the dependent trigger instead of throwing.

### `Condition` (sealed) + `ConditionEvaluator`
Conditions form a serializable tree (`All`/`Any`/`Not` + leaves). `ConditionEvaluator` is a pure function `(Condition, EvaluationContext, zoneLookup) → Boolean`. Missing signals evaluate to `false`. This is the most heavily unit-tested unit in the codebase.

### `RuleEngine`
Converts each enabled `Zone` into an `EvaluationCandidate` (its activation condition = inside-zone ∧ schedule ∧ advanced triggers; action = play its config) and merges standalone `Rule`s. It evaluates all candidates and ranks the matches:

1. **priority** (1–100, higher wins),
2. **radius** (smaller = more specific wins) — this is how *Bedroom* beats *Home*,
3. **updatedAt** (deterministic final tie-break).

Output: an `AutomationDecision(winner, matches)`. Pure, no IO.

### `AutomationCoordinator`
The only place that orchestrates IO. It:

- short-circuits on the global automation toggle and any active **manual override**,
- builds the context, loads zones/rules, runs the engine,
- **diffs** the winner against the persisted active candidate — if unchanged, it does nothing (idempotent),
- otherwise applies the action through `SpotifyController`, writes `TransitionHistory`, and persists the new active candidate.

A mutex serializes passes so a geofence event and a periodic tick can't race.

## Triggers without polling

```
OS Geofencing API ──(enter/exit)──► GeofenceBroadcastReceiver ──► AutomationWorker ──► Coordinator
Boot / app update ─────────────────► BootCompletedReceiver ─────► GeofenceSyncWorker (re-arm)
AutomationService (only when active) ── periodic safety tick ──► Coordinator
```

- **Geofences** are owned by the OS → the app sleeps until a transition. The 100-geofence platform cap is handled by registering the nearest ~95 and re-syncing on movement.
- **Non-geofence conditions** (time windows, battery, Bluetooth) are caught by a sparse periodic re-evaluation inside the foreground service, which only runs while continuous monitoring is desired.
- **WorkManager** runs the actual suspend work off the broadcast thread with retry/backoff.

## Persistence

- **Room** stores `Zone`, `Rule`, `TransitionHistory`, `PlaylistPosition`. Spatial/priority fields are real columns (cheap queries); `PlaybackConfig`, `Schedule`, `Condition`, `Action` are **JSON columns** via a single `kotlinx.serialization` `Json` instance — the same one used by import/export, guaranteeing round-trip fidelity (covered by `SerializationRoundTripTest`).
- **DataStore (Preferences)** stores `AppSettings` and the current `ManualOverride`.
- **EncryptedSharedPreferences** stores Spotify tokens (AES-256, Keystore), excluded from backup.

## Spotify integration

- **Auth:** Authorization Code + PKCE via a Chrome Custom Tab (`SpotifyAuthCoordinator`), tokens refreshed transparently behind the `SpotifyAuth` port, attached to API calls by an `AuthInterceptor`. No client secret.
- **Catalog:** `SpotifyCatalogImpl` (Retrofit) for playlists/search.
- **Playback:** `WebApiSpotifyController` implements the `SpotifyController` port using the Web API player endpoints; it exposes a hot `StateFlow<PlaybackState>` kept in sync by polling + post-command refresh (bidirectional sync). Swappable for an App Remote adapter.

## Error model

`Outcome<T>` (`Success`/`Failure(DomainError)`) crosses every boundary instead of thrown exceptions. `DomainError` is a closed set (`SpotifyNotAuthorized`, `NoNetwork`, `LocationUnavailable`, `PermissionDenied`, …) the UI maps to honest, actionable messages.

## Dependency graph (high level)

```
MainActivity ──► AppViewModel ──► SettingsRepository, SpotifyAuth, SpotifyAuthCoordinator
screens/* ─────► *ViewModel ────► repositories + SpotifyController + AutomationManager
AutomationService/Worker ──────► AutomationCoordinator ──► RuleEngine, repositories,
                                                            ContextProvider, SpotifyController
ZoneRepositoryImpl ────────────► ZoneDao, GeofenceManager
WebApiSpotifyController ────────► SpotifyApiService, SpotifyAuth, PlaybackPositionRepository
```

## SOLID notes

- **S** — `RuleEngine` decides, `AutomationCoordinator` orchestrates, `SpotifyController` plays, `GeofenceManager` registers. Each has one reason to change.
- **O/L** — new triggers = new `Condition` subtypes + evaluator branches; new actions = new `Action` subtypes. Existing code is untouched.
- **I** — narrow ports (`LocationProvider`, `ContextProvider`, `GeofenceManager` are separate) so callers depend only on what they use.
- **D** — everything depends on domain interfaces; Hilt injects implementations.
