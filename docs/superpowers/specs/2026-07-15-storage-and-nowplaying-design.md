# Storage management + Now Playing player features — Design

Date: 2026-07-15

Three related improvements to Melodix:

1. **Storage/cache management** — replace the fake, hardcoded storage row with a real, dedicated screen that lists every cache the app holds and lets the user delete any one, or all.
2. **Now Playing top menu** — the overflow menu currently holds only the sleep timer. Replace it with proper player actions, and fix the dead Equalizer button.
3. **Sleep timer** — move it to a dedicated moon icon that opens a dialog with a live countdown and a custom digital-clock stepper (±1 min), Start when idle, Stop when running.

---

## Feature 1 — Storage / cache management

### Problem
`SettingsScreen.SettingsStorageItem` shows a hardcoded `1.2 GB / 4 GB` at 30% and does nothing.

### Cache categories (real, in the app's own storage)
| Category  | Location                         | Clear action |
|-----------|----------------------------------|--------------|
| Downloads | `filesDir/drive_cache`           | `CacheRepository.clear()` (deletes files + Room rows) |
| Images    | `cacheDir/image_cache` (Coil)    | `imageLoader.diskCache?.clear()` + delete dir |
| Other     | rest of `cacheDir`               | delete `cacheDir` children except `image_cache` |

Coil currently configures only a memory cache. We add a Coil `DiskCache` at `cacheDir/image_cache` in `MelodixApp` so the Images category is real and clearable.

### Components / contracts
```kotlin
// repo/StorageRepository.kt
enum class CacheType { DOWNLOADS, IMAGES, OTHER }
data class CacheCategory(val type: CacheType, val sizeBytes: Long)
interface StorageRepository {
    suspend fun categories(): List<CacheCategory>   // off-main-thread
    suspend fun totalBytes(): Long
    suspend fun clear(type: CacheType)
    suspend fun clearAll()
}
```
- `StorageRepositoryImpl(context, cacheRepository)` — Dispatchers.IO; recursive dir-size walk; image-cache dir name constant shared with `MelodixApp` (`"image_cache"`).
- `CacheRepository.clear()` (new) → `dao.deleteAll()` (new DAO query) + wipe `drive_cache` dir.
- `StorageViewModel(storageRepository)` → `StorageUiState(categories, totalBytes, isLoading)`; `refresh()`, `clear(type)`, `clearAll()` (each re-refreshes).
- `StorageScreen(onBack, viewModel)` — one row per category (human label + formatted size + trash w/ confirm dialog), a **Clear all cache** button, empty/zero states. Screen chrome mirrors `EqualizerScreen`.

### Wiring
- `Routes.Storage = "storage"`; `NavGraph` composable → `StorageScreen`.
- `MainScreen` + `SettingsScreen` gain `onOpenStorage`; the storage row becomes a normal `SettingsItem` (chevron) whose subtitle is the **real** total cache size from `SettingsViewModel.totalCacheBytes` (adds `StorageRepository` dependency).
- DI: `single<StorageRepository>`, `viewModelOf(::StorageViewModel)`, extra arg on `SettingsViewModel`.

---

## Feature 2 — Now Playing: EQ + top menu

- **EQ fix:** `NowPlayingScreen` gains `onOpenEqualizer`; the `GraphicEq` action button calls it; `NavGraph` navigates to the existing `Routes.Equalizer`.
- **Top overflow menu** (replaces sleep-timer dropdown) — `DropdownMenu` with:
  - **Add to playlist** → `AddToPlaylistSheet(track)` (uses `PlaylistViewModel`).
  - **Go to album** → `onOpenAlbum(track.albumId)` → `AlbumDetail`.
  - **Song details** → `SongDetailsDialog(track)` (title/artist/album/duration/format from uri).
  - **Share song** → `ACTION_SEND`, `type=audio/*`, `EXTRA_STREAM=track.uri`, grant read.

---

## Feature 3 — Sleep timer

- **Moon icon** in the top bar (left of overflow). Tints to accent + filled when a timer is active. Tapping opens `SleepTimerDialog`.
- **`SleepTimerDialog(remainingMs, onStart, onStop, onDismiss)`**:
  - `remainingMs <= 0` (idle): custom digital-clock stepper — large monospace minutes readout, round **−**/**+** buttons stepping 1 min, range 1..120, default 15 → **Start**.
  - `remainingMs > 0` (running): large **mm:ss** countdown → **Stop** (Start hidden).
- **`PlaybackController`**: add `sleepTimerRemainingMs` to `PlaybackState`; `setSleepTimer(minutes)` runs a 1-second countdown loop (updates remaining, pauses + resets at 0); add `cancelSleepTimer()`.
- **`NowPlayingViewModel`/`NowPlayingUiState`**: expose `sleepTimerRemainingMs`; add `cancelSleepTimer()`.

---

## Testing
- Unit: `StorageRepositoryImpl` (per-category + total sizes, clear semantics on temp dirs); `PlaybackController` sleep-timer countdown (ticks down, pauses at zero, cancel resets).
- Device: EQ opens; each menu action works; timer counts down + stops; storage screen lists sizes and deletes per-item and all.

## YAGNI
No timer presets, no ±long-press accel, no set-as-ringtone, no artist nav (Track has no artistId).

## Parallelization
- Agent A (new files): `StorageRepository`, `StorageRepositoryImpl`, `StorageViewModel`, `StorageScreen`.
- Agent B (new files): `SleepTimerDialog`, `SongDetailsDialog`, `AddToPlaylistSheet`.
- Main thread owns all shared/glue edits (PlaybackController, NowPlayingScreen/VM, NavGraph, Routes, MainScreen, SettingsScreen/VM, Modules, MelodixApp, CacheRepository/DAO, strings).
