# EdgeGesture

<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="EdgeGesture" />

**An Android app built on the accessibility service — edge gestures, back double-tap, expand / task panels, a floating music panel and more.**

**English** | [简体中文](README.zh-CN.md)

![Release](https://img.shields.io/github/v/release/Evilgodxu/EdgeGesture?style=flat-square&color=4f46e5)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple)
![AGP](https://img.shields.io/badge/AGP-9.4.0-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.1-blue)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.09.00-blue)
![minSdk](https://img.shields.io/badge/minSdk-34-orange)
![targetSdk](https://img.shields.io/badge/targetSdk-37-orange)

</div>

**EdgeGesture (边缘手势)** lets you drive the phone with edge swipes and taps or double-taps on the back of the device, launch apps from an expand panel, and pull a **floating music panel**, a **task panel** or a **compass clock** over any app.

## Features

### Edge gestures

- **Three edges** — independent trigger areas for the left, right and bottom edges
- **Up to 3 segments per edge** — every segment carries its own action set
- **9 actions per segment** — swipe and long-press-swipe in three directions, plus tap / double tap / long press
- **Custom trigger area** — edge width, height / position percent and segment count, with a live preview
- **Double swipe** — in fullscreen or landscape mode two consecutive swipes are required to trigger
- **Vibration** feedback on trigger
- **Statistics** — gesture-trigger and launch-block counts over 1 / 7 / 30-day periods

### Back double tap

Detects double taps on the back of the device with the accelerometer, using a heuristic signal-processing algorithm.

- **Sensitivity** (1–10) and **detection range** (1–10) tuning
- **Working modes** — always active, screen off, screen on
- **Pause while charging** to avoid accidental triggers
- Shares the same action set as edge gestures

### Actions

| Category | Actions |
| --- | --- |
| System navigation | Back, Home, Recents, Previous app |
| Media | Previous track, Next track |
| System | Flashlight, Voice assistant, Power menu, Lock screen, Screenshot |
| Window | Freeform window |
| Panel | Expand panel, Music panel, Task panel, Compass clock |
| Translate | Screen translate |
| Shortcut | Alipay scan, WeChat scan |
| App | Launch app — bind any installed app as the target |
| Other | Delay reminders (1 / 3 / 5 / 10 / 15 min), None |

### Expand panel

- **System controls** — brightness (written through `WRITE_SETTINGS`, converted with the same HLG gamma curve as AOSP) and alarm / ring / media volume sliders
- **App shortcuts** — 8 slots, tap to launch instantly
- **Freeform launch** — each slot can be toggled to open in a freeform (mini) window
- **Icon cache** — app icons are cached while scanning the app list

### Task panel

- Lists the recent-app history the accessibility service has collected (max 10 entries, filtered by the app-switch blacklist)
- **Tap to open**, **double-tap for a freeform window**, **swipe or clear to dismiss** — dismissal removes the task through Shizuku

### Screen translate

- Reads on-screen text and its coordinates from the accessibility tree and overlays the translation in place; the overlay is not touchable, so the app underneath keeps working
- Falls back across **Microsoft Edge**, **Google** and a **free model** endpoint, and remembers the last provider that succeeded
- Toggle-style: trigger again (or switch apps) to dismiss

### Music panel

- Full playback panel rendered as a floating overlay on top of any app
- **Mini player** — a compact bar pinned under the status bar while playing; tap to expand back into the full panel
- **Local library** — MediaStore scan plus audio import through `VIEW` / `SEND` intents; search is local and keeps a history
- **Lyrics** — embedded lyrics and local `.lrc` files, including enhanced LRC with per-word timestamps and `[tr]` translation blocks; word-by-word highlighting can be switched off
- **Covers** — system thumbnails / embedded art plus local image candidates (Coil)
- **Metadata write-back** — rename title / artist and write cover art into MP3 (ID3v2), MP4 / M4A, FLAC and Opus files with a built-in tag writer
- **Audio signal path** panel — format, source / output sample rate, bit depth, channels, output strategy, output device and route of the current track
- Play modes (repeat one / repeat all / shuffle), favorites, sleep timer, playlist with long-press removal, and playback settings

### Launch blocker

Built on the accessibility service, optionally assisted by **Shizuku**.

- Global switch plus per-rule enable; each rule has an optional **launcher** and a required **target** app (both entered as a package name, matched by substring)
- Only active while the gesture service itself is enabled
- **Block timing** — immediate, slight delay (500 ms) or delayed (1000 ms)
- On blocking, the launcher app is brought back to the foreground
- **Kill the launcher** on every hit (fumble protection: at most 5 consecutive kills, then a 15 s cooldown)
- Optionally **kill the target** process, and control whether system apps may be terminated
- Process killing uses Shizuku, so it stays inert without it

### App switch blacklist

- Filters the apps that should not show up when switching to the previous app
- Auto-initializes with system apps and the app itself on first launch; permission-aware, with a `PackageManager` + `<queries>` fallback when `QUERY_ALL_PACKAGES` is unavailable
- Listens for app install / uninstall and updates automatically

### Compass clock

A canvas overlay: the year sits in the center and seven concentric rings carry month / day / weekday / Chinese double-hour / hour / minute / second, with the current value at the 3 o'clock position. It plays an opening sequence, spins 720°, then ticks in real time; trigger it again to close.

### Settings

- **Theme** — light, dark or follow system, switched with a circular-reveal transition
- **Language** — 简体中文, English or follow system; hot-switched in-app without recreating the activity, and applied at cold start through the persisted locale
- **Gesture config** — import / export a JSON file covering edge gestures, back double-tap, trigger-area options and launch-block rules
- **Donate**, **about** (author / version / project link) and the Shizuku state

### Permissions

Permission cards live on the main page and show their grant state; the whole group collapses once everything is granted. Tapping a card opens the system page and starts a 500 ms poll that refreshes state and brings the app back to the foreground as soon as the permission is granted. Required by feature:

- **Accessibility service** — core system-level gesture handling
- **Display over other apps** — trigger area and all floating panels
- **Notification** — keeps the gesture service running in the background
- **Battery optimization** — ignored so the service stays alive
- **Query installed apps** — full app list scanning
- **Write system settings** — brightness slider in the expand panel
- **Media audio / images** — local music library and artwork
- **All files access** (optional) — write metadata back into audio files

### In-app updates

An update check runs when the app returns to the foreground (at most once per day); in the single-column layout the version number can also be tapped to check on demand. The GitHub Releases API is queried over OkHttp; the APK is fetched with `DownloadManager` into the app-private directory and installed through `FileProvider` — only after its SHA-256 matches the digest published by GitHub, otherwise the file is discarded.

### Crash logs

Uncaught exceptions and handled exceptions are written to daily log files under the app-specific external directory, keeping the most recent 3 days.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose (BOM 2026.09.00) + Material 3 |
| Adaptive layout | androidx.window 1.5.1 (`WindowSizeClass`) |
| DI | AndroidX ViewModel factories + app-level singletons |
| Navigation | Navigation 3 — `navigation3-runtime` / `navigation3-ui` 1.1.7 (type-safe `NavKey`) |
| State | DataStore Preferences 1.2.1 + StateFlow / MutableStateFlow |
| Permissions | Shizuku 13.1.5 + custom `UserService` (AIDL) |
| Audio | Media3 ExoPlayer 1.11.1 + MediaSessionService |
| Image loading | Coil 3.6.2 (local cover candidates) |
| Network | OkHttp 5.5.0 |
| Serialization | kotlinx.serialization 1.11.0 |
| Coroutines | kotlinx.coroutines 1.11.0 (incl. coroutines-guava for MediaController) |
| Hidden API bypass | hidden-api-bypass 6.1 (freeform windowing mode) |
| Build | AGP 9.4.0, Gradle 9.7.1, refreshVersions, JDK 21 |

## Project Structure

```
.
├── app/
│   └── src/main/
│       ├── aidl/com/edgegesture/evilgodxu/service/   # ICommandService (Shizuku UserService)
│       ├── kotlin/com/edgegesture/evilgodxu/
│       │   ├── data/
│       │   │   ├── app/             # AppRepository (app list + icon cache), DataConfigManager
│       │   │   ├── gesture/         # Gesture / expand-panel settings, GestureStatsManager
│       │   │   ├── launchblock/     # Launch-block rules
│       │   │   ├── permission/      # PermissionMonitor
│       │   │   ├── shizuku/         # ShizukuManager
│       │   │   └── translate/       # TranslationService
│       │   ├── log/                 # CrashLogManager
│       │   ├── navigation/          # NavGraph (Navigation 3 type-safe keys)
│       │   ├── screens/
│       │   │   ├── gesture/         #   Main page + edge config
│       │   │   │   ├── components/  #   Shared page components
│       │   │   │   └── service/     #   Accessibility service + overlays
│       │   │   │       ├── compassclock/
│       │   │   │       ├── expandpanel/
│       │   │   │       ├── musicpanel/
│       │   │   │       ├── taskpanel/
│       │   │   │       └── translate/
│       │   │   ├── backtap/         #   Back double tap
│       │   │   ├── blacklist/       #   App switch blacklist
│       │   │   ├── expandpanel/     #   Expand panel settings
│       │   │   ├── launchblock/     #   Launch blocker
│       │   │   └── settings/components/
│       │   ├── service/             # CommandUserService
│       │   ├── ui/                  # adaptive/ (window size class) + theme/
│       │   ├── update/              # UpdateManager / UpdateViewModel / UpdateDialog
│       │   ├── utils/localization/  # In-app locale manager
│       │   ├── MainActivity.kt
│       │   └── MyApplication.kt
│       └── res/                     # Resources (values / values-en)
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/
├── LICENSE
├── NOTICE
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Architecture

The app follows **MVVM with unidirectional data flow**: state flows down from `ViewModel` → `UiState` → UI while events flow up from the UI to the `ViewModel`. Shared data logic lives in `data/` behind repository-style objects (settings, app list, launch-block rules, translation, permissions, Shizuku), wired together by AndroidX ViewModel factories and app-level singletons.

Gesture and overlay capabilities sit in `screens/gesture/service/`:
`EdgeGestureAccessibilityService` detects edge swipes, edge taps and background taps and dispatches actions through `AccessibilityActionExecutor`. Each overlay (edge trigger area, expand panel, music panel, mini player, task panel, translation, compass clock) is a system window hosted by its own manager, mostly as a `ComposeView`, coordinated from the accessibility service.

Settings persist in DataStore (`gesture_settings`, `settings`, `launch_block`). Optional system-level operations — removing a task / stopping an app process, freeform windowing — go through Shizuku's `CommandUserService` or the hidden-API bypass, and simply stay inactive when Shizuku is unavailable.

## Getting Started

### Prerequisites

- JDK 21
- Android Studio (latest stable recommended)
- Android SDK with API 37 (`compileSdk`)

### Build

```bash
git clone https://github.com/Evilgodxu/EdgeGesture.git
cd EdgeGesture

# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config, see below)
./gradlew assembleRelease
```

APKs are emitted as `EdgeGesture-<versionName>-arm64.apk` under `app/build/outputs/apk/`. Only the `arm64-v8a` ABI is built. The release build enables minification and resource shrinking.

### Release Signing

The release build reads signing credentials from `local.properties` in the project root:

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

The keystore is expected at `jh.keystore` in the project root (adjust `storeFile` in `app/build.gradle.kts` if needed). Both files are git-ignored — never commit them.

## Disclaimer

Screen translation relies on third-party public endpoints (Microsoft Edge / Google / free model), whose availability and policy may vary by region and content. The app and document are for learning and communication only — please support the copyright holders.

## License

[AGPL-3.0](LICENSE) © 2026 Evilgodxu
