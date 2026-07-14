# Melodix Player — Project Context

> Context file for AI code editors. Describes what this project is, how it's
> structured, and the conventions to follow when generating or modifying code.

## Working Rules (for AI editors)

- **Never run git.** Do NOT stage, commit, branch, push, or run any `git`
  command. The user manages all version control themselves. Make file changes
  only and leave the entire git workflow to the user.

## Overview

**Melodix** is a native Android music player built entirely with Jetpack Compose
and Material 3. It plays the user's on-device music library, backed by Media3
ExoPlayer running in a foreground `MediaSessionService`. The app covers
onboarding, a home feed, a multi-tab library, search, now-playing, a play queue,
and settings.

- **Package / applicationId:** `com.melodix.player`
- **App name:** Melodix
- **Version:** 1.0 (versionCode 1)

## Tech Stack

| Area              | Choice                                             |
| ----------------- | -------------------------------------------------- |
| Language          | Kotlin 2.2.10                                       |
| UI                | Jetpack Compose, Material 3                         |
| Build             | Gradle (Kotlin DSL), AGP 9.2.1                      |
| Compose BOM       | 2026.02.01                                          |
| Dependency Inj.   | Koin 4.0 (BOM)                                      |
| Playback          | Media3 ExoPlayer + Media3 Session 1.5.1            |
| Navigation        | Navigation-Compose 2.8.5                            |
| Persistence       | DataStore Preferences 1.1.1                        |
| Images            | Coil 3.1.0                                          |
| Immutable state   | kotlinx.collections.immutable 0.3.8                |

Version catalog lives in `gradle/libs.versions.toml`; always add/upgrade
dependencies there and reference them via `libs.*` aliases in
`app/build.gradle.kts`.

## SDK / Build Configuration

- **minSdk:** 30
- **targetSdk / compileSdk:** 37
- **Java compatibility:** 11 (source & target)
- **Compose:** enabled via `buildFeatures { compose = true }`
- **Release build:** code optimization currently disabled
  (`optimization { enable = false }`) — no minify/shrink.
- Toolchain versions are intentionally forward-dated (SDK 37, AGP 9.2,
  Compose BOM 2026.02); a build requires the matching preview tooling.

## Architecture

MVVM with a clean repository layer and Koin for wiring. Package root:
`com.melodix.player`.

```
com.melodix.player
├── MainActivity.kt            # Single activity, Compose host
├── MelodixApp.kt              # Application class (Koin startup)
├── model/                     # Domain models
│   ├── Track, Album, Artist, Playlist
├── repo/                      # Repository interfaces
│   ├── MusicRepository, FavoritesRepository, PlaylistRepository,
│   │   PlayHistoryRepository, SettingsRepository
│   └── local/                 # Implementations (MediaStore + DataStore)
├── service/
│   ├── PlaybackService.kt     # Media3 MediaSessionService (foreground)
│   └── PlaybackController.kt  # Bridge between UI/VMs and the session
├── viewmodel/                 # One VM per screen/feature (state holders)
├── ui/                        # Compose screens
│   ├── onboarding/ (+ components/)
│   ├── home/  library/  search/  nowplaying/  queue/  settings/
│   ├── detail/  main/
│   └── theme/                 # (screen-level theme entry)
└── core/
    ├── components/            # Reusable UI (MiniPlayer, TrackListItem,
    │                          #   AlbumCard, TrackOptionsSheet, ...)
    ├── navigation/            # NavGraph.kt, Routes.kt
    ├── di/Modules.kt          # Koin module definitions
    └── theme/                 # Color, Shape, Theme, Type
```

### Layering rules

- **UI (Compose)** observes **ViewModels**; it never touches repositories or the
  player directly.
- **ViewModels** depend on **repository interfaces** (from `repo/`) and on
  `PlaybackController`; they expose immutable UI state.
- **Repositories** (`repo/local/`) own data access — MediaStore for the music
  library, DataStore for favorites/history/playlists/settings.
- **Playback** goes through `PlaybackController` → `PlaybackService`
  (Media3 session). Do not instantiate ExoPlayer inside screens or VMs.
- **DI:** register new repositories/VMs/controllers in
  `core/di/Modules.kt`. Prefer constructor injection.

## Feature Surface

- **Onboarding** — permission request, theme picker, feature intro
  (gestures, smart playlists). Strings prefixed `onboarding_`, `permission_`,
  `theme_`, `feature_`, `gestures_`, `playlists_`.
- **Home** — recently played, quick picks.
- **Library** — tabs: Songs, Albums, Artists, Playlists (with create-playlist).
- **Search** — artists / songs / albums.
- **Now Playing** — like, queue, lyrics, EQ entry points.
- **Queue** — view/clear the play queue.
- **Settings** — playback (EQ, sleep timer, lossless), appearance
  (dark mode/theme), notifications, storage/cache, about.

## Conventions

- **UI text:** all user-facing strings go in `app/src/main/res/values/strings.xml`
  (grouped by screen with comment headers). No hardcoded strings in Composables.
- **Theming:** use the `core/theme/` tokens (`Color`, `Shape`, `Type`,
  `Theme`) — Material 3. Avoid raw hex/dp scattered in screens.
- **State:** prefer immutable collections (`kotlinx.collections.immutable`) in
  UI state classes to keep Compose recomposition stable.
- **Naming:** Composable screens end in `Screen`; per-screen state holders end in
  `ViewModel`; detail screens share `DetailUiState`.
- **Navigation:** add destinations in `core/navigation/Routes.kt` and wire them
  in `NavGraph.kt`.

## Permissions (AndroidManifest)

- `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` (33+),
  `READ_EXTERNAL_STORAGE` (maxSdk 32)
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`, `WAKE_LOCK`
- `PlaybackService` is declared with `foregroundServiceType="mediaPlayback"`
  and the `MediaSessionService` intent filter.

## Testing

- Only default scaffolding exists so far: `ExampleUnitTest` (JVM) and
  `ExampleInstrumentedTest` (androidTest). **No real coverage yet** — new
  business logic in repos/VMs should come with unit tests.

## Build & Run

```bash
./gradlew assembleDebug        # build debug APK
./gradlew test                 # JVM unit tests
./gradlew connectedAndroidTest # instrumented tests (device/emulator)
```

## Known Gaps / TODO

- No README / user docs.
- Release build shrinking/optimization is disabled.
- Test coverage is effectively zero beyond generated samples.
