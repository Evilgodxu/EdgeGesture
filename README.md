# EdgeGesture

<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="EdgeGesture" />

**An Android app built on the accessibility service — edge gestures, back double-tap, expand panel, launch blocking and more.**

**English** | [简体中文](README.zh-CN.md)

![Release](https://img.shields.io/github/v/release/Evilgodxu/EdgeGesture?style=flat-square&color=4f46e5)
![License](https://img.shields.io/badge/license-AGPL--3.0-blue)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-purple)
![AGP](https://img.shields.io/badge/AGP-9.5.1-blue)
![Gradle](https://img.shields.io/badge/Gradle-9.9.1-blue)
![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2026.09.00-blue)
![minSdk](https://img.shields.io/badge/minSdk-34-orange)
![targetSdk](https://img.shields.io/badge/targetSdk-37-orange)

</div>

**EdgeGesture (边缘手势)** lets you drive the phone with edge swipes and taps or double-taps on the back of the device, launch apps from an expand panel, and block unwanted app launches.

## Features

### Edge gestures

- **Three edges** — independent trigger areas for the left, right and bottom edges
- **Up to 3 segments per edge** — every segment carries its own action set
- **9 actions per segment** — swipe and long-press-swipe in three directions, plus tap / double tap / long press
- **Custom trigger area** — edge width, height / position percent and segment count, with a live preview
- **Double swipe** — in fullscreen or landscape mode two consecutive swipes are required to trigger
- **Vibration** feedback on trigger
- **Overlay controls** — hide the trigger area, hide the app from Recents, and disable the trigger area while the keyboard is open
- **Statistics** — gesture-trigger and launch-block counts over 1 / 7 / 30-day periods

### Back double tap

Detects double taps on the back of the device with the accelerometer, using a heuristic signal-processing algorithm.

- **Sensitivity** (1–10) and **detection range** (1–10) tuning
- **Working modes** — always active, screen off, screen on
- **Pause while charging** to avoid accidental triggers
- Binds any single action, drawn from the same action list as edge gestures

### Actions

| Category | Actions |
| --- | --- |
| System navigation | Back, Home, Recents, Previous app |
| Media | Previous track, Next track |
| System | Flashlight, Voice assistant, Power menu, Lock screen, Screenshot |
| Window | Freeform window |
| Panel | Expand panel |
| Shortcut | Alipay scan, WeChat scan |
| App | Launch app — bind any installed app as the target |
| Other | Delay reminders (custom minutes), None |

### Expand panel

- **System controls** — brightness (written through `WRITE_SETTINGS`, converted with the same HLG gamma curve as AOSP) and alarm / ring / media volume sliders
- **App shortcuts** — 8 slots, tap to launch instantly
- **Freeform launch** — each slot can be toggled to open in a freeform (mini) window
- **Icon cache** — app icons are cached while scanning the app list

### Launch blocker

Built on the accessibility service, optionally assisted by **Shizuku**.

- Global switch plus per-rule enable; each rule has an optional **launcher** and a required **target** app (both entered as a package name, matched by substring)
- Only active while the gesture service itself is enabled
- **Block timing** — immediate, slight delay (500 ms) or delayed (1000 ms)
- On blocking, the launcher app is brought back to the foreground
- **Kill the launcher** on every hit (fumble protection: at most 5 consecutive kills, then a 15 s cooldown)
- Optionally **kill the target** process, and control whether system apps may be terminated
- Process killing uses Shizuku, so it stays inert without it; the page shows whether Shizuku is missing, stopped or unauthorized

### App switch blacklist

- Filters the apps that should not show up when switching to the previous app
- Auto-initializes with system apps and the app itself on first launch; permission-aware, with a `PackageManager` + `<queries>` fallback when `QUERY_ALL_PACKAGES` is unavailable
- Listens for app install / uninstall and updates automatically

### Settings

- **Theme** — light, dark or follow system, switched with a circular-reveal transition
- **Language** — 简体中文, English or follow system; hot-switched in-app without recreating the activity, and applied at cold start through the persisted locale
- **Gesture config** — import / export a JSON file covering edge gestures, back double-tap, trigger-area options and launch-block rules
- **Donate**, **about** (author / version / project link) and a **`[log]`** entry that shares the current day's crash log

### Permissions

Permission cards live on the main page and show their grant state; the whole group collapses once everything is granted. Tapping a card opens the system page and starts a 500 ms poll that refreshes state and brings the app back to the foreground as soon as the permission is granted. Required by feature:

- **Accessibility service** — core system-level gesture handling
- **Display over other apps** — trigger area and expand panel
- **Notification** — keeps the gesture service running in the background
- **Battery optimization** — ignored so the service stays alive
- **Query installed apps** — full app list scanning
- **Write system settings** — brightness slider in the expand panel

### In-app updates

An update check runs when the app returns to the foreground (at most once per day); in the single-column layout the version number can also be tapped to check on demand. The GitHub Releases API is queried over OkHttp; the APK is fetched with `DownloadManager` into the app-private directory and installed through `FileProvider` — only after its SHA-256 matches the digest published by GitHub, otherwise the file is discarded.

### Crash logs

Uncaught exceptions and handled exceptions are written to daily log files under the app-specific external directory, keeping the most recent 3 days.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.4.21 |
| UI | Jetpack Compose (BOM 2026.09.00) + Material 3 |
| Adaptive layout | androidx.window 1.5.1 + material3-adaptive 1.3.0 |
| DI | Manual `AppContainer` with constructor injection |
| Navigation | Navigation 3 — `navigation3-runtime` / `navigation3-ui` 1.2.1 (type-safe `NavKey`) |
| State | DataStore Preferences 1.3.0 + StateFlow |
| Permissions | Shizuku 13.1.5 + custom `UserService` (AIDL) |
| Network | OkHttp 5.5.1 |
| Serialization | kotlinx.serialization 1.13.0 |
| Hidden API bypass | hidden-api-bypass 6.2 (freeform windowing mode) |
| Build | AGP 9.5.1, Gradle 9.9.1, refreshVersions, JDK 21 |

## Project Structure

```
.
├── app/src/
│   ├── main/
│   │   ├── aidl/com/edgegesture/evilgodxu/service/   # ICommandService (Shizuku UserService)
│   │   ├── kotlin/com/edgegesture/evilgodxu/
│   │   │   ├── App.kt / AppContainer.kt      # Application + manual DI container
│   │   │   ├── AppUiState.kt / MainViewModel.kt
│   │   │   ├── data/
│   │   │   │   ├── app/             # AppRepository (app list + icon cache)
│   │   │   │   ├── config/          # GestureConfigTransfer (import / export)
│   │   │   │   ├── gesture/         # Gesture / expand-panel settings, stats repository
│   │   │   │   ├── launchblock/     # Launch-block rules
│   │   │   │   ├── permission/      # PermissionMonitor
│   │   │   │   ├── repository/      # Settings / update-check repository + DataStore impls
│   │   │   │   ├── settings/        # App-level settings (theme, language)
│   │   │   │   └── shizuku/         # ShizukuManager
│   │   │   ├── log/                 # CrashLogManager
│   │   │   ├── navigation/          # Screen (NavKey routes) + NavGraph (Navigation 3)
│   │   │   ├── screens/
│   │   │   │   ├── gesture/         #   Home page + edge configs
│   │   │   │   │   ├── assembly/    #   Layout assemblers
│   │   │   │   │   ├── component/   #   Page components
│   │   │   │   │   └── service/     #   Accessibility service + overlays
│   │   │   │   │       └── expandpanel/
│   │   │   │   ├── backtap/ blacklist/ expandpanel/
│   │   │   │   ├── launchblock/
│   │   │   │   └── settings/        #   All with assembly/ + component/
│   │   │   ├── service/             # CommandUserService
│   │   │   ├── ui/
│   │   │   │   ├── adaptive/        # Window size class from the window container
│   │   │   │   ├── component/       # AppTopBar / SectionCard / SwitchRow / SliderTrack
│   │   │   │   └── theme/           # Color tokens + theme
│   │   │   ├── update/              # UpdateChecker / Downloader / ViewModel / Dialog
│   │   │   └── utils/localization/  # In-app locale manager
│   │   └── res/                     # Resources (values / values-en)
│   └── test/kotlin/                 # Unit tests (pure logic + state holders)
├── gradle/
│   ├── libs.versions.toml           # Version catalog
│   └── wrapper/
├── AGENTS.md
├── LICENSE
├── NOTICE
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Architecture

State flows down and events flow up: `DataStore ⇄ Repository ⇄ ViewModel ⇄ UiState ⇄ Composable`.

- **Manual DI** — `App` builds a single `AppContainer` in `onCreate` that lazily provides the settings, update-check, app and gesture-stats repositories, the config transfer, the localization manager and the app version. Activities receive them through `viewModelFactory` + `initializer`; screens and the accessibility service use the `Context.appContainer` accessor. No Hilt, Koin or Dagger.
- **App-level state** — theme, language and version are aggregated into `AppUiState` by the activity-scoped `MainViewModel` and handed down through `LocalMainViewModel`, so no screen touches the data source directly.
- **Screen state** — each `{Screen}ViewModel` owns a `MutableStateFlow<{Screen}UiState>` and exposes it as an immutable `StateFlow`, scoped to the Navigation 3 entry through `rememberViewModelStoreNavEntryDecorator()`.
- **Screens** are organised as a skeleton (`{Screen}.kt` — ViewModel wiring, size classes, effects) plus layout assemblers in `assembly/` and business-grouped components in `component/`. Shared pieces (`AppTopBar`, `SectionCard`, `SwitchRow`, `SliderTrack`) live in `ui/component/`.
- **Gesture runtime** sits in `screens/gesture/service/`: `EdgeGestureAccessibilityService` detects edge swipes, edge taps and background taps and dispatches actions through `AccessibilityActionExecutor`. Each overlay (edge trigger area, expand panel) is a system window hosted by its own manager, mostly as a `ComposeView`, coordinated from the accessibility service. `ImeTouchGate` disables the trigger area while the IME is visible.

Settings persist in DataStore (`settings`, `gesture_settings`, `launch_block`), with statistics stored as daily counters and derived into a `Flow`. Optional system-level operations — stopping an app process, freeform windowing — go through Shizuku's `CommandUserService` or the hidden-API bypass, and simply stay inactive when Shizuku is unavailable.

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

APKs are emitted as `EdgeGesture-<versionName>-arm64.apk` under `app/build/outputs/apk/`. Only the `arm64-v8a` ABI is built. The release build enables code optimization and optimized resource shrinking through AGP's `optimization` block; lint errors fail the build.

### Release Signing

The release build reads signing credentials from `local.properties` in the project root:

```properties
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

The keystore is expected at `jh.keystore` in the project root (adjust `storeFile` in `app/build.gradle.kts` if needed). Both files are git-ignored — never commit them.

## License

[AGPL-3.0](LICENSE) © 2026 Evilgodxu
