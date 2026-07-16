# Cloud Sync & Theming Improvements — Design Spec

**Date:** 2026-07-15
**Status:** Approved for planning
**Scope:** Melodix Android app (`melodixplayer`)

## Overview

Four related changes to the Melodix player, spanning theming consistency and the Google Drive audio sync subsystem:

1. Make every screen follow the active theme, and make the system status/navigation bar icons adaptive (readable on any theme).
2. Remove the now-redundant favorites/history/playlist sync UI from the Cloud Sync screen (that metadata already auto-syncs).
3. Add real song deduplication so the same audio file is never uploaded or cached twice, scaled to libraries of thousands of songs.
4. Replace the coarse inline progress with an app-wide transfer progress system: background work, a persistent snackbar, and a reopenable per-item dialog with retry.

### Goals

- No screen renders on the wrong (non-theme) background.
- Status/nav bar icons are always legible regardless of theme luminance.
- Cloud Sync screen shows only what's relevant now that metadata sync is automatic.
- Uploading local songs, then downloading them back, does **not** create duplicates.
- Users see individual per-item download/upload progress, can retry failures, and are not trapped by a modal.

### Non-goals

- Cleaning up duplicates that already exist (explicit user decision: **prevent future only**).
- Matching "same song" across different encodings/bitrates (content-hash equality only).
- Changing the Firestore metadata sync (favorites/history/playlists) behavior itself.
- Redesigning Now Playing / Detail / other screens' visuals.

---

## Item 1 — Theme consistency + adaptive system bars

**Problem.** `EqualizerScreen` and `CloudSyncScreen` are standalone nav destinations rendered as a bare `Column(Modifier.fillMaxSize().statusBarsPadding())` with **no `.background()`**, so they show the window's default (black) instead of `colorScheme.background`. `MainScreen` avoids this only because it uses `Scaffold(containerColor = background)`. Separately, `MainActivity` calls `enableEdgeToEdge()` with no control over system-bar icon appearance, so on a light theme the status-bar icons can be invisible.

**Design.**

- **Root themed background (single fix for all screens).** In `core/navigation/NavGraph.kt`, wrap the `NavHost` in a `Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)`. Every destination inherits the themed background; screens that paint their own background still override it. This fixes Equalizer, Cloud Sync, and any other bare destination at once.
- **Adaptive system bar icons.** In `MainActivity.kt`, inside `setContent` after the theme is resolved, compute `val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f` and apply it in a `SideEffect` (or `DisposableEffect` keyed on `lightBars`):
  ```
  val controller = WindowCompat.getInsetsController(window, window.decorView)
  controller.isAppearanceLightStatusBars = lightBars
  controller.isAppearanceLightNavigationBars = lightBars
  ```
  This yields dark icons on light themes and light icons on dark themes, adapting to CUSTOM themes too. Applied reactively so switching theme updates the bars immediately.

**Verification.** Switch to a light theme (e.g., SUNSHINE) and a dark theme (MONO); confirm status-bar icons stay visible and Equalizer/Cloud Sync backgrounds match the theme.

---

## Item 2 — Remove redundant sync UI

**Problem.** The Cloud Sync screen still shows a "Your data" section (Favorites/History/Playlists stat chips), a `SyncStatusRow`, and a "Sync now" button. Favorites/history/playlists already sync automatically via `SyncCoordinator` (full sync on sign-in + debounced pushes), so this UI is redundant.

**Design.**

- **`ui/drive/CloudSyncScreen.kt`:** remove the `SectionLabel("Your data")`, the `Row` of `StatChip`s, the `SyncStatusRow`, the "Sync now" `MelodixButton`, and the private `StatChip` and `SyncStatusRow`/`StatusText` composables if unused afterward. Keep the Account section and the Google Drive stat cards.
- **`viewmodel/CloudSyncViewModel.kt`:** remove `syncNow()`, the `SyncStatus` sealed interface usage, the `favoritesCount`/`historyCount`/`playlistCount`/`syncStatus` fields from `CloudSyncUiState`, and the `observe(...)` calls + constructor dependencies (`favoritesRepository`, `playHistoryRepository`, `playlistRepository`, `syncManager`) that exist only to feed those. Verify none are referenced elsewhere in the VM before removing; update the Koin module in `core/di/Modules.kt` accordingly.

**Verification.** Cloud Sync screen builds and shows Account + Drive sections only; automatic metadata sync still works (sign in, favorite a song, confirm it reaches Firestore).

---

## Item 3 — Song deduplication (content hash, scaled for thousands)

**Problem.** There is no content comparison anywhere. Upload always *creates* a new Drive file named `"${title}.mp3"` (no existence check; Drive allows duplicate names). Download only skips a Drive file whose Drive-id is already cached — it never checks whether the user already has that song locally. The only "same song" heuristic is `localTrack.title.lowercase() == driveFilename-without-ext.lowercase()`. Consequence: upload 15 local songs, then download them back → 15 duplicate cached copies of songs already on the device.

**Chosen method:** exact **content hash (MD5)** with a **size pre-filter** and a **persistent local-hash cache**. Rationale: Google Drive already exposes a native `md5Checksum` per file (zero client cost), and the user's case (upload exact bytes, then re-download the same bytes) is a guaranteed hash match. Size pre-filtering plus caching keeps it cheap at thousands of songs.

**Design.**

- **Drive side.**
  - `repo/drive/DriveApi.kt`: add `md5Checksum` to the requested `fields` (currently `id,name,mimeType,size,modifiedTime`).
  - `model/DriveFile.kt`: add `val md5Checksum: String?`.
- **Local side — cheap fingerprint first.**
  - `model/Track.kt`: add `val sizeBytes: Long` (from MediaStore `SIZE`; already available at query time — no file read). Populate it in `repo/local/MusicRepositoryImpl.kt`.
  - New Room table `local_file_hashes` (in the existing `AppDatabase`): columns `mediaStoreId: Long` (PK), `sizeBytes: Long`, `dateModified: Long`, `md5: String`. A new DAO with `get(mediaStoreId)` and `upsert(...)`.
  - New component `repo/local/hash/LocalHashRepository` (interface + impl): `md5For(track): String` that returns the cached hash when `(sizeBytes, dateModified)` are unchanged, otherwise streams the file through `MessageDigest("MD5")`, upserts, and returns it. Streamed (buffered) read — never load whole files into memory.
- **Comparison logic (in `CloudSyncViewModel`, replacing the title/name match at `refresh()`):**
  1. Build `driveBySize: Map<Long, List<DriveFile>>` and `driveMd5s: Set<String>` from the Drive listing.
  2. For each local track, if its `sizeBytes` matches a Drive file's size (candidate), compute/lookup its MD5 via `LocalHashRepository`; otherwise treat as local-only without hashing.
  3. `localOnly` = local tracks whose MD5 ∉ `driveMd5s` (candidates) plus all non-candidates (no size match ⇒ not on Drive).
  4. `downloadAll()`: `toGet = driveFiles.filter { it.md5Checksum !in localMd5s && it.id !in cached }` — skip Drive files you already have locally *or* already cached. `localMd5s` = the set of MD5s computed for local tracks (candidates only; non-candidates can't match a Drive file by definition).
  5. `backupLocalOnly()`: unchanged trigger, but now fed the md5-accurate `localOnly` set.
- **Scope:** prevent future duplicates only. No migration/cleanup of existing duplicate cached rows.

**Performance note.** Only local files whose exact byte-size collides with a Drive file are hashed, and each file is hashed at most once (cached by id+size+mtime). For a 1000-song library with few real overlaps, this is a handful of hashes per sync at most. Log (via `log`-equivalent, i.e. Android `Log`) how many files were hashed vs skipped, so we never silently do O(n) full-library hashing.

**Verification.** With a Drive folder containing songs identical to local files: `localOnly` count excludes them, "Download from Drive" queues 0 for already-present songs, and re-running backup queues 0.

---

## Item 4 — Transfer progress system (background + snackbar + reopenable dialog)

**Problem.** Workers call no `setProgress`; the UI only shows aggregate `done/total` counts inline on the Cloud Sync screen, which disappears when you navigate away. There is no per-item progress, no per-item retry, and no persistence across navigation.

**Design.**

- **Per-item worker progress.**
  - `worker/DownloadWorker.kt` and `worker/UploadWorker.kt`: call `setProgress(workDataOf(KEY_PROGRESS to percent, KEY_DISPLAY_NAME to name, KEY_DIRECTION to "download"|"upload"))` as bytes transfer. Download already streams to a file — add a counting loop reporting percent from `size`. Upload currently `readBytes()` then posts; wrap the request body in a counting `RequestBody` (or a counting source) so upload reports real percent instead of loading fully into memory (also a memory win for large files).
  - Keep existing retry policy (`Result.retry()` up to 3 attempts, then `Result.failure()`).
- **App-scoped transfer observer.** New Koin **singleton** `repo/sync/TransferCenter` holding a `StateFlow<TransferState>`:
  - `TransferItem { uniqueName: String, displayName: String, direction: Direction, state: Queued|Running|Done|Failed, progress: Int, input: Data }`.
  - `TransferState { items: List<TransferItem>, activeCount, doneCount, failedCount, isActive }`.
  - It observes `WorkManager.getWorkInfosByTagFlow(TAG_DOWNLOAD)` and `TAG_UPLOAD`, joining each `WorkInfo` (state + `progress` data) with the enqueued item list it records at enqueue time (so queued items show names and failed items retain their input `Data` for retry). Matching is by unique work name.
  - `enqueueDownloads(files)` / `enqueueUploads(tracks)`: moved here from the ViewModel (or the VM delegates to it), so enqueue + tracking share one source of truth. `retry(uniqueName)` re-enqueues that unique work with `ExistingWorkPolicy.REPLACE` and the stored input.
- **UI hosting at app root.** In `NavGraph.kt` (or a thin wrapper composable around it), place a `SnackbarHost` + the dialog, driven by `TransferCenter`:
  - **Persistent snackbar** while `isActive`: e.g. `"Downloading 4/15 · 27%"` with `SnackbarDuration.Indefinite`; on completion `"Downloaded 15 · 2 failed"`. Tapping it (snackbar action / clickable) opens the dialog. A local `showDialog` state controls visibility.
  - **`TransferProgressDialog`** (new composable, dismissable): a `LazyColumn` of rows — `displayName` + `LinearProgressIndicator`; trailing shows a spinner/percent when Running, a **check** icon when Done, and an **error** icon + **Retry** button when Failed. A "Close" button (and scrim/back) dismisses back to the snackbar; work keeps running.
- **Triggering.** The Cloud Sync buttons call `TransferCenter.enqueueDownloads(...)` / `enqueueUploads(...)` and set `showDialog = true`. Because hosting is app-root, navigating away leaves the snackbar visible and the dialog reopenable from anywhere.
- **Cleanup.** Remove the old inline `QueueRow` aggregate UI and the `WorkQueue`/`toQueue()` count logic from `CloudSyncViewModel` (superseded by `TransferCenter`). The Drive stat cards remain.

**Verification.** Trigger a multi-song download; confirm: dialog shows each item with moving progress; dismissing the dialog leaves a persistent snackbar; navigating to other screens keeps the snackbar; tapping it reopens the dialog; a forced failure shows an error row with a working Retry; when all finish the snackbar shows the completion summary and can be dismissed.

---

## Cross-cutting: data model & DI summary

- `model/DriveFile.kt`: `+ md5Checksum: String?`
- `model/Track.kt`: `+ sizeBytes: Long`
- `AppDatabase`: `+ local_file_hashes` table + DAO (schema version bump / destructive-migration per existing app convention — confirm during planning).
- New: `LocalHashRepository` (+impl), `TransferCenter` (singleton). Register both in `core/di/Modules.kt`.
- Removed from `CloudSyncViewModel`: metadata-sync state/deps, `WorkQueue`/`toQueue`.

## Testing strategy

- Unit-test the dedup comparison (given local tracks with sizes/hashes and a Drive listing with md5s, assert `localOnly` and `toGet`).
- Unit-test `LocalHashRepository` cache-hit logic (unchanged size+mtime returns cached hash without re-reading).
- Unit-test `TransferCenter`'s WorkInfo→TransferState reduction (queued/running/done/failed, progress, retry re-enqueue).
- On-device: theme switch (light/dark) for bars + backgrounds; end-to-end download/upload with progress, dismiss/reopen, retry.

## Risks / open questions

- **Room migration:** adding `local_file_hashes` needs a DB version bump; confirm whether the app uses destructive migrations (check `AppDatabase`) during planning.
- **Upload progress body:** wrapping the OkHttp multipart body in a counting body must preserve the existing multipart format Drive expects; verify the request still succeeds.
- **Snackbar host placement:** must sit above nav content but not interfere with the existing `MiniPlayer`; confirm z-order and bottom insets during implementation.
