# EdgeGesture

<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="EdgeGesture" />

**An edge-gesture app for Android, built on the accessibility service — plus back-tap, a global music panel and more.**

**English** | [简体中文](README.zh-CN.md)

![Release](https://img.shields.io/github/v/release/Evilgodxu/EdgeGesture?style=flat-square&color=4f46e5)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-purple)
![AGP](https://img.shields.io/badge/AGP-9.3.1-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.7.0-blue)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.08.00-blue)
![minSdk](https://img.shields.io/badge/minSdk-34-orange)
![targetSdk](https://img.shields.io/badge/targetSdk-37-orange)

</div>

**EdgeGesture (边缘手势)** lets you control your phone in a more natural way. Trigger actions with edge swipes or double-taps on the back of the device, launch shortcuts from an expansion panel, and pop up a **floating music panel** or **task panel** on top of any app — so common features are always one gesture away.

## Features

### Edge gestures

- **Three sides** — separate trigger areas for the left, right and bottom edges
- **Multiple directions** — each side supports 6 swipe gestures (short / long-press × 3 directions)
- **Flexible zones** — up to 3 segments per side, each with its own action
- **Custom trigger area** — tune edge width, height / position percent and segment count with a live preview
- **Double swipe** — on full-screen or landscape screens, two consecutive swipes are required to trigger
- **Feedback** — haptic vibration on swipe
- **Statistics** — tracks gesture triggers and launch-block counts over 1 / 7 / 30-day periods

### Back double tap

Detects double taps on the back of the device via the accelerometer, using a heuristic signal-processing algorithm.

- **Sensitivity** (1–10) and **detection range** (1–10) tuning
- **Working modes** — always active, screen off, screen on
- **Pause while charging** to avoid accidental triggers
- Supports the same set of actions as edge gestures

### Actions

| Category | Actions |
| --- | --- |
| System navigation | Back, Home, Recents, Previous app |
| Media | Previous track, Next track |
| System | Flashlight, Voice assistant, Power menu, Lock screen, Screenshot |
| Windows | Freeform window |
| Panel | Expand panel, Music panel, Task panel, Compass clock |
| Translate | Screen translate |
| Shortcut | Alipay scan, WeChat scan |
| Other | Delay reminders (1 / 3 / 5 / 10 / 15 min), None |

### Expand panel

- **System controls** — brightness, alarm / ring / media volume sliders
- **App shortcuts** — up to 8 favorite apps, tap to launch instantly
- **Freeform launch** — each app can be configured to open in a freeform (mini) window
- **Icon caching** — app icons are cached during scanning for faster loading

### Task panel

- Shows recent apps in an overlay panel; **tap to open**, **double-tap for a freeform window**

### Screen translate

- Reads the on-screen text via the accessibility service and translates it in place
- Falls back across **Microsoft Edge**, **Google** and a **free model** endpoints when one is unreachable, and remembers the last working provider

### Music panel

- Full-featured playback panel as a floating overlay (**SYSTEM_ALERT_WINDOW**), usable above any app
- **Mini player** — a compact floating bar shown in the background during playback; tap it to expand back into the full panel
- **Local library** — scans device storage via MediaStore and imports audio through `VIEW` / `SEND` intents
- **Multi-platform online search** — aggregated search across Netease (网易云), QQ Music, Kugou (酷狗) and Jamendo, with search history and direct online playback
- **Synced lyrics** — scrolling lyrics with online match / refresh
- **Cover management** — embedded art, local candidates and online search; covers and metadata can be written back to the file (Jaudiotagger)
- **USB audio exclusive** — automatic USB DAC / sound card detection with exclusive-mode routing, plus a real-time audio signal path view (format, source / output sample rates, bit depth, channels, DSD mode, route, output strategy & device)
- **Bluetooth headset** — connection detection with per-session volume initialization
- **Sleep timer**, play modes and favorites

### Launch blocker

Based on the accessibility service, optionally enhanced by **Shizuku** to intercept the launch of selected apps.

- Configurable **launcher** (optional) and **target** app
- **Block timing** — immediate, slight delay or delayed
- **Kill launcher** when triggered (with fumble protection under Shizuku: max 5 consecutive kills, then a 15 s cooldown)
- Optionally **kill the target** process and control whether system apps may be terminated (both need Shizuku)

### App switch blacklist

- Filters apps that should not appear when switching to the previous app
- Auto-initializes with system apps on first launch; permission-aware with a `PackageManager` + `<queries>` fallback
- Listens for app install / uninstall and updates automatically

### Settings

- **Theme** — Light, Dark or Follow System
- **Language** — 简体中文, English or Follow System; hot-switched in-app via the `LocaleManager` API without recreating the activity
- **Adaptive layout** — switches to a two-column layout on wide screens
- **Hide overlay**, **hide from recents**, **avoid keyboard overlap** during gesture triggering
- **Vibration** on swipe
- **Screen translation** target language / provider handling
- **Gesture config** — import / export the full gesture and launch-block configuration
- **Music cache** — manage cover and lyrics cache

### Permissions

Permission cards guide the grant flow; a granted card hides itself and permission state is re-checked automatically when the app resumes.

- **Accessibility service** — core system-level gesture handling
- **Display over other apps** — trigger area, expand panel and floating panels
- **Notification** — keeps the gesture service running in the background
- **Battery optimization** — ignores battery optimizations to stay alive
- **Query installed apps** — full app list scanning
- **Media / images** — local music library and artwork
- **All files access** (optional) — write back audio metadata

### In-app updates

A WorkManager job checks GitHub Releases periodically (24 h internal cooldown) and offers an in-app download dialog with a changelog.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.10 |
| UI | Jetpack Compose (BOM 2026.08.00) + Material 3 |
| Adaptive layout | Material3 Adaptive 1.3.0 |
| DI | Koin 4.2.2 |
| Navigation | Navigation Compose 2.9.8 (type-safe routes) |
| State | DataStore + StateFlow + MutableStateFlow |
| Background | WorkManager 2.11.2 |
| Permissions | Shizuku 13.1.5 + custom UserService |
| Audio | Media3 ExoPlayer 1.11.0 + MediaSessionService |
| Image loading | Coil 3.5.0 |
| Network | OkHttp 5.4.0 |
| Serialization | kotlinx.serialization 1.11.0 |
| Hidden API bypass | hidden-api-bypass 6.1 |
| Build | AGP 9.3.1, Gradle 9.7.0, refreshVersions, Jaudiotagger |

## Project Structure

```
.
├── app/
│   └── src/main/
│       ├── kotlin/com/edgegesture/evilgodxu/
│       │   ├── data/                    # Data layer (settings, app repo, Shizuku, translate)
│       │   ├── di/                      # Koin modules
│       │   ├── log/                     # CrashLogManager
│       │   ├── navigation/              # Navigation Compose type-safe routes
│       │   ├── screens/                 # Screens
│       │   │   ├── gesture/             #   Gesture settings + edge config
│       │   │   │   └── service/         #   Accessibility service + overlay UIs (music / expand / task / translate / compass clock)
│       │   │   ├── backtap/             #   Back double tap
│       │   │   ├── expandpanel/         #   Expand panel settings
│       │   │   ├── blacklist/           #   App switch blacklist
│       │   │   ├── launchblock/         #   Launch blocker
│       │   │   └── settings/            #   App settings + data config
│       │   ├── service/                 # Shizuku CommandUserService
│       │   ├── ui/                      # Material 3 theme + adaptive window size
│       │   ├── update/                  # In-app update
│       │   ├── utils/localization/      # In-app localization manager
│       │   ├── MainActivity.kt
│       │   └── MyApplication.kt
│       └── res/                         # Resources (values / values-en)
├── gradle/
│   ├── libs.versions.toml               # Version catalog (dependencies)
│   └── wrapper/
├── LICENSE
├── NOTICE
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Architecture

The app follows **MVVM with unidirectional data flow**: state flows down from `ViewModel` → `UiState` → UI, while events flow up from the UI to the `ViewModel`. Shared data logic lives in the `data/` layer behind repositories (settings, app list, launch-block rules, translation), and everything is wired together by Koin.

All gesture and overlay capabilities sit in `screens/gesture/service/`. The `EdgeGestureAccessibilityService` detects swipes and background taps and dispatches actions through an `AccessibilityActionExecutor`. Overlay UIs (edge trigger view, expand panel, music panel, task panel, translation, compass clock) are rendered as system windows through their own window managers, coordinated from the accessibility service. Settings persist via DataStore, and optional root-level operations (process kill, freeform windowing) go through Shizuku's `CommandUserService`.

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

APKs are emitted as `EdgeGesture-<versionName>-arm64.apk` under `app/build/outputs/apk/`. Only the `arm64-v8a` ABI is built.

### Release Signing

The release build reads signing credentials from `local.properties` in the project root:

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

The keystore file is expected at `jh.keystore` in the project root (adjust `storeFile` in `app/build.gradle.kts` if needed). Both files are git-ignored — never commit them.

## Disclaimer

The online music search and screen translation rely on third-party public web endpoints (Netease / QQ Music / Kugou / Jamendo / translation services), whose availability and policy may vary by region and content. The app and document are for learning and communication only — please support the copyright holders.

## License

[AGPL-3.0](LICENSE) © 2026 Evilgodxu