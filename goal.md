# ROLE

You are an elite senior Android architect, UI/UX designer, Kotlin engineer, Spotify API expert, and QA engineer. Your task is to design and implement a production-quality Android application that feels like a premium commercial product rather than a hobby project. Prioritize reliability, maintainability, smooth UX, low battery usage, and excellent code quality.

Do not create prototypes, placeholders, TODOs, mock implementations, or unfinished features unless absolutely unavoidable. If a decision must be made, choose the approach that produces the highest-quality user experience and long-term maintainability.

---

# PRODUCT

Create a premium Android APK called:

# SpotZones

A location-aware Spotify companion that automatically changes music based on context.

The app should NOT attempt to replace Spotify.

Instead, it should integrate seamlessly with the official Spotify app using the Spotify App Remote SDK and Spotify Web API.

The goal is that users continue using Spotify normally while SpotZones intelligently controls playback.

This approach is preferred because:

- Users keep all Spotify features.
- Existing playlists remain untouched.
- Spotify Connect still works.
- Premium audio quality remains.
- Song navigation and playback controls work exactly as users expect.
- No need to reinvent a music player.

The app should feel like a natural extension of Spotify.

---

# DESIGN PHILOSOPHY

Compete with premium apps.

Aim for:

- Google Material 3
- Fluid animations
- Zero clutter
- Beautiful dark mode
- AMOLED-friendly colors
- Accessibility support
- Responsive layouts
- Smooth transitions
- Fast startup
- Minimal battery usage

UI quality target:

9.8/10

Code quality target:

10/10

---

# TECH STACK

Language:

Kotlin

UI:

Jetpack Compose

Architecture:

MVVM + Clean Architecture

Dependency Injection:

Hilt

Navigation:

Compose Navigation

Database:

Room

Background tasks:

WorkManager

Geofencing:

Android Geofencing API

Spotify integration:

Spotify App Remote SDK
Spotify Web API

Settings:

DataStore

Serialization:

Kotlinx Serialization

Image loading:

Coil

Logging:

Timber

Crash reporting:

Prepare abstraction layer for Firebase Crashlytics

Testing:

JUnit
MockK

---

# CORE FEATURE

Users create "Zones".

Example:

Home
Gym
Work
Road Trip
Airport
Storm Chase Area

Each zone contains:

- Name
- Radius
- GPS center
- Playlist
- Shuffle mode
- Repeat mode
- Crossfade duration
- Volume level
- Fade in/out options
- Enable/disable toggle
- Priority level
- Active hours
- Days of week

When entering a zone:

SpotZones should automatically instruct Spotify to:

- Start playback
- Resume previous playlist position
- Shuffle
- Set volume
- Enable repeat
- Crossfade smoothly

Transitions should feel elegant.

Never abruptly interrupt songs unless configured.

---

# PRIORITY SYSTEM

If multiple zones overlap:

Higher priority wins.

Example:

Home radius = 500m
Bedroom radius = 20m

Bedroom playlist overrides Home playlist.

Priority values:

1-100

---

# SMART TRANSITIONS

Support:

Crossfade between playlists

Options:

- Immediate
- Finish current song
- Crossfade over N seconds

Remember playback position for each playlist.

Returning to a zone resumes where the user previously left off.

---

# ADVANCED TRIGGERS

Support combinations of:

Location

Time of day

Day of week

Bluetooth device

Charging state

Battery level

Wi-Fi network

Movement speed

Walking

Driving

Stationary

Headphones connected

Car stereo connected

Weather (architecture prepared)

Calendar events (architecture prepared)

Triggers should use AND/OR logic.

Example:

IF

Location = Gym

AND

Time = 5PM-8PM

AND

Bluetooth = AirPods

THEN

Play Workout Playlist.

---

# BATTERY OPTIMIZATION

Avoid polling GPS continuously.

Use:

Geofencing API

FusedLocationProvider

Adaptive refresh rates

Background restrictions awareness

Foreground service only when necessary

Target battery usage:

Excellent.

---

# PLAYBACK CONTROL SCREEN

Provide a Now Playing screen.

It should display:

Album art

Song title

Artist

Progress bar

Play/pause

Previous

Next

Shuffle

Repeat

Current zone

Upcoming zone

Queue preview

Playback device

Spotify connection status

All controls should interact with Spotify and feel instantaneous.

Users should never need to leave SpotZones.

However, everything must remain synchronized with Spotify itself.

Changing songs in Spotify should instantly update SpotZones.

Changing songs in SpotZones should instantly update Spotify.

---

# MAP SCREEN

Interactive Google Maps view.

Features:

Current location

Zone markers

Radius circles

Edit radius visually

Drag markers

Create zone by long press

Satellite mode

Dark mode support

Smooth animations

---

# ZONE EDITOR

Beautiful editor with:

Playlist picker

Radius slider

Priority slider

Color selection

Icon selection

Schedule configuration

Transition settings

Advanced triggers

Preview mode

Duplicate zone

Export zone

Import zone

---

# DASHBOARD

Display:

Current song

Current zone

Next likely zone

Playback status

Spotify status

Quick controls

Recent transitions

Upcoming schedule

---

# HISTORY

Maintain transition history.

Show:

Time entered

Time exited

Playlist used

Duration

Skipped songs

Manual overrides

---

# SEARCH

Search:

Zones

Playlists

Artists

Songs

History

---

# IMPORT / EXPORT

Support:

JSON backup

Encrypted backups

Share files

Restore files

Cloud sync architecture abstraction

---

# MANUAL OVERRIDE

Allow:

Pause automation for:

15 min

30 min

1 hour

Until leaving current zone

Until tomorrow

Permanent disable

---

# RULE ENGINE

Create a robust rules engine.

Examples:

IF speed > 50 mph
THEN Road Trip Playlist

IF Home AND Night
THEN Sleep Playlist

IF Work AND Monday-Friday
THEN Focus Playlist

IF Storm Chase Area
THEN Chase Playlist

Rules should be highly extensible.

---

# AI ARCHITECTURE

Prepare interfaces for future AI features:

Playlist prediction

Habit learning

Favorite songs by location

Automatic schedule learning

Context recommendations

Do not implement AI yet.

Create interfaces and abstractions.

---

# DATA MODEL

Zone
Trigger
PlaylistAssignment
PlaybackState
TransitionHistory
Settings
Rule
Condition
Action

All models should support future expansion.

---

# SECURITY

Never store Spotify credentials insecurely.

Use encrypted storage.

Proper token refresh.

Handle offline mode gracefully.

Protect against API failures.

Recover automatically.

---

# ERROR HANDLING

Handle:

Spotify app closed

No internet

Lost GPS

Revoked permissions

Battery restrictions

API failures

Authentication expiration

Background limitations

Every failure should degrade gracefully.

No crashes.

---

# SETTINGS

Theme

Accent colors

Battery optimization settings

Transition preferences

Notification controls

Geofence sensitivity

Backup settings

Privacy settings

Debug settings

---

# NOTIFICATIONS

Persistent playback controls.

Current zone indicator.

Upcoming zone changes.

Quick pause automation button.

Material 3 notification style.

---

# PERMISSIONS FLOW

Permissions should be requested progressively.

Explain clearly why each permission is needed.

Never overwhelm the user.

---

# QUALITY REQUIREMENTS

No placeholder UI.

No fake implementations.

No duplicated code.

No god classes.

No memory leaks.

No unnecessary recomposition.

Follow SOLID principles.

Follow Clean Architecture.

Optimize for maintainability.

Extensive comments only where useful.

Prefer self-documenting code.

---

# TESTING

Create:

Unit tests

Repository tests

Rule engine tests

Spotify integration tests

ViewModel tests

Background task tests

Geofence tests

Navigation tests

Edge case tests

---

# DOCUMENTATION

Generate:

README

Architecture diagrams

Folder structure explanation

Dependency graph

Setup guide

Spotify developer setup instructions

Build instructions

Troubleshooting guide

Contribution guide

---

# FINAL REQUIREMENT

Before writing code, thoroughly design the architecture and explain why each decision was made.

Then implement the application incrementally with production-quality code.

Continuously critique your own implementation and improve it whenever a better design exists.

The final result should feel like a polished premium Android application that could realistically be published on Google Play and compete with top-tier commercial apps.