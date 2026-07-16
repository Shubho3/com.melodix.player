# Cloud Sync & Theming Improvements — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every screen theme-consistent with adaptive system bars, remove the redundant metadata-sync UI, add content-hash song deduplication, and add an app-wide download/upload progress system (background + snackbar + reopenable per-item dialog with retry).

**Architecture:** Themed background is applied once at the `NavHost` root; system bars adapt to theme luminance in `MainActivity`. Deduplication uses Google Drive's native `md5Checksum` plus a size-pre-filtered, cached local MD5, compared by a pure `SyncDiff` reducer. Transfer progress is owned by an app-scoped `TransferCenter` singleton observing WorkManager, rendered by a root-level snackbar + `TransferProgressDialog`; the two workers report per-item byte progress via `setProgress`.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Koin DI, Room, WorkManager, OkHttp, kotlinx.serialization, JUnit4 (unit tests in `app/src/test`).

## Global Constraints

- Package root: `com.melodix.player`. Build/install from `D:\Projects\Dump\melodix\melodixplayer`.
- Unit tests: plain JUnit4 under `app/src/test/java/...`; run with `.\gradlew.bat testDebugUnitTest`. **Unit-tested code MUST NOT import `android.*`** (no instrumentation available) — keep pure logic Android-free.
- Build: `.\gradlew.bat assembleDebug`. Install/run on device per `melodix-device-testing` memory (`adb install -r`, screenshots via Bash `adb exec-out screencap -p > file.png`, wake device if black).
- Room uses `fallbackToDestructiveMigration(dropAllTables = true)` — adding an entity only needs a `version` bump; **no manual Migration required** (the cache DB is disposable).
- Not currently a git repo. "Commit" steps below are recommended checkpoints; either run `git init` first or treat each commit step as "checkpoint reached." Do not block on git.
- Follow existing code style: 4-space indent, trailing commas, `MaterialTheme.colorScheme.*` for all colors, no hardcoded hex in UI.

---

## Phase 1 — Theming (Item 1)

### Task 1: Root themed background + adaptive system bars

**Files:**
- Modify: `app/src/main/java/com/melodix/player/core/navigation/NavGraph.kt` (wrap `NavHost`)
- Modify: `app/src/main/java/com/melodix/player/MainActivity.kt` (adaptive bars)

**Interfaces:**
- Produces: no new symbols; visual/behavioral change only.

- [ ] **Step 1: Wrap the NavHost in a themed Surface**

In `NavGraph.kt`, add imports:
```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
```
Wrap the existing `NavHost(...) { ... }` body of `NavGraph` so the whole `NavHost` is inside:
```kotlin
Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // ...existing transitions and composable{} entries unchanged...
    ) {
        // ...unchanged...
    }
}
```

- [ ] **Step 2: Make system bar icons adaptive in MainActivity**

In `MainActivity.kt`, add imports:
```kotlin
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
```
Inside `setContent { MelodixTheme(...) { ... } }`, immediately inside the `MelodixTheme` lambda (before the `if (theme == null ...)` block), add:
```kotlin
val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
val view = androidx.compose.ui.platform.LocalView.current
SideEffect {
    val controller = WindowCompat.getInsetsController(window, view)
    controller.isAppearanceLightStatusBars = lightBars
    controller.isAppearanceLightNavigationBars = lightBars
}
```

- [ ] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: On-device verification (light + dark theme)**

Install: `adb install -r app\build\outputs\apk\debug\app-debug.apk`; launch `adb shell am start -n com.melodix.player/.MainActivity`.
- Settings → Theme → **Sunshine** (light). Screenshot (Bash): `adb exec-out screencap -p > sun.png`. Confirm status-bar clock/icons are **dark** and readable; open Settings → Equalizer and → Backup & Download and confirm both backgrounds match the theme (not black).
- Settings → Theme → **Monochrome** (dark). Screenshot. Confirm status-bar icons are **light**; Equalizer/Cloud Sync backgrounds are the dark theme background.

- [ ] **Step 5: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/core/navigation/NavGraph.kt app/src/main/java/com/melodix/player/MainActivity.kt
git commit -m "feat(theme): root themed background + adaptive system bar icons"
```

---

## Phase 2 — Remove redundant sync UI (Item 2)

### Task 2: Strip metadata-sync UI and dead ViewModel state

**Files:**
- Modify: `app/src/main/java/com/melodix/player/ui/drive/CloudSyncScreen.kt`
- Modify: `app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt`

**Interfaces:**
- Produces: `CloudSyncUiState` loses `favoritesCount`, `historyCount`, `playlistCount`, `syncStatus`. `CloudSyncViewModel` constructor loses `favoritesRepository`, `playHistoryRepository`, `playlistRepository`, `syncManager`. Consumed by Task 9 (which further edits this VM) and Koin (Task 2 Step 3).

- [ ] **Step 1: Remove the "Your data" block from CloudSyncScreen**

In `CloudSyncScreen.kt`, delete the entire section between the Account block and the Google Drive block — remove:
```kotlin
Spacer(Modifier.height(24.dp))

// ── Firestore data (favorites / history / playlists) ──
SectionLabel("Your data")
Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    StatChip("Favorites", state.favoritesCount, Modifier.weight(1f))
    StatChip("History", state.historyCount, Modifier.weight(1f))
    StatChip("Playlists", state.playlistCount, Modifier.weight(1f))
}
Spacer(Modifier.height(12.dp))
SyncStatusRow(state.syncStatus)
Spacer(Modifier.height(12.dp))
MelodixButton(
    text = "Sync now",
    onClick = { viewModel.syncNow() },
)
```
Then delete the now-unused private composables `SyncStatusRow`, `StatusText`, and `StatChip`, and remove the unused imports for `CircularProgressIndicator`, `SyncStatus`, `SimpleDateFormat`, `Date`, `Locale`, and `Arrangement` **only if** no remaining code references them (keep `Arrangement` if `QueueRow`/`StatCard` still use it — they do, so keep it). Leave `MelodixButton` import (still used by the Drive buttons).

- [ ] **Step 2: Remove dead state and deps from CloudSyncViewModel**

In `CloudSyncViewModel.kt`:
- Delete the `SyncStatus` sealed interface (lines 33-38) and its import usage.
- From `CloudSyncUiState`, delete `favoritesCount`, `historyCount`, `playlistCount`, and `syncStatus`.
- From the constructor, delete parameters `favoritesRepository`, `playHistoryRepository`, `playlistRepository`, `syncManager`.
- In `init {}`, delete the three `observe(...)` lines for favorites/history/playlists counts.
- Delete the entire `syncNow()` function.
- Remove now-unused imports: `FavoritesRepository`, `PlayHistoryRepository`, `PlaylistRepository`, `SyncManager`.

- [ ] **Step 3: Update Koin registration**

`viewModelOf(::CloudSyncViewModel)` in `core/di/Modules.kt` resolves constructor args positionally via `get()`, so removing constructor params needs no change there. Verify by building. (No edit expected unless the build complains.)

- [ ] **Step 4: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` with no "unresolved reference" errors.

- [ ] **Step 5: On-device verification**

Install + open Settings → Backup & Download. Confirm the "Your data" chips and "Sync now" button are **gone**; Account + Google Drive sections remain. Sign in, favorite a track, and confirm auto-sync still works (favorite persists after `adb shell pm clear` + re-sign-in, proving Firestore round-trip via `SyncCoordinator`).

- [ ] **Step 6: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/ui/drive/CloudSyncScreen.kt app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt
git commit -m "refactor(sync): remove redundant favorites/history/playlist sync UI"
```

---

## Phase 3 — Deduplication (Item 3)

### Task 3: Request Drive md5Checksum

**Files:**
- Modify: `app/src/main/java/com/melodix/player/repo/drive/DriveApi.kt:41`
- Modify: `app/src/main/java/com/melodix/player/model/DriveFile.kt`

**Interfaces:**
- Produces: `DriveFile.md5Checksum: String?`. Consumed by Task 6.

- [ ] **Step 1: Add the field to the model**

In `DriveFile.kt`, add after `modifiedTime`:
```kotlin
    val md5Checksum: String? = null,   // Drive-computed MD5 of the file bytes; null for folders
```

- [ ] **Step 2: Request it from the list API**

In `DriveApi.kt`, change the `fields` query param (line 41) to include `md5Checksum`:
```kotlin
.addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum)")
```

- [ ] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/repo/drive/DriveApi.kt app/src/main/java/com/melodix/player/model/DriveFile.kt
git commit -m "feat(drive): fetch native md5Checksum for files"
```

### Task 4: Add file size + modified time to Track

**Files:**
- Modify: `app/src/main/java/com/melodix/player/model/Track.kt`
- Modify: `app/src/main/java/com/melodix/player/repo/local/MusicRepositoryImpl.kt`

**Interfaces:**
- Produces: `Track.sizeBytes: Long`, `Track.dateModified: Long`. Consumed by Tasks 5, 6, 9.

- [ ] **Step 1: Add fields to Track**

In `Track.kt`, add after `folderName`:
```kotlin
    val sizeBytes: Long = 0L,      // MediaStore SIZE (bytes)
    val dateModified: Long = 0L,   // MediaStore DATE_MODIFIED (epoch seconds)
```

- [ ] **Step 2: Populate from MediaStore**

In `MusicRepositoryImpl.kt` `queryTracks()`:
Add to `projection`:
```kotlin
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
```
Add column indices after `bucketNameCol`:
```kotlin
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
```
Add to the `Track(...)` constructor call:
```kotlin
                        sizeBytes = cursor.getLong(sizeCol),
                        dateModified = cursor.getLong(dateModifiedCol),
```

- [ ] **Step 3: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/model/Track.kt app/src/main/java/com/melodix/player/repo/local/MusicRepositoryImpl.kt
git commit -m "feat(library): expose file size + modified time on Track"
```

### Task 5: Local MD5 hash cache (Room table + repository)

**Files:**
- Create: `app/src/main/java/com/melodix/player/repo/local/cache/LocalFileHashEntity.kt`
- Create: `app/src/main/java/com/melodix/player/repo/local/cache/LocalFileHashDao.kt`
- Modify: `app/src/main/java/com/melodix/player/repo/local/cache/AppDatabase.kt`
- Create: `app/src/main/java/com/melodix/player/repo/local/hash/LocalHashRepository.kt`
- Create: `app/src/main/java/com/melodix/player/repo/local/hash/LocalHashRepositoryImpl.kt`
- Create: `app/src/main/java/com/melodix/player/repo/local/hash/AudioHasher.kt`
- Modify: `app/src/main/java/com/melodix/player/core/di/Modules.kt`
- Test: `app/src/test/java/com/melodix/player/repo/local/hash/LocalHashRepositoryImplTest.kt`

**Interfaces:**
- Produces:
  - `interface LocalHashRepository { suspend fun md5For(mediaStoreId: Long, sizeBytes: Long, dateModified: Long, uri: Uri): String? }`
  - `interface AudioHasher { suspend fun md5(uri: Uri): String? }`
  - `LocalFileHashEntity(mediaStoreId: Long, sizeBytes: Long, dateModified: Long, md5: String)`
  - `LocalFileHashDao { suspend fun getById(id: Long): LocalFileHashEntity?; suspend fun upsert(e: LocalFileHashEntity) }`
- Consumed by Task 6.

- [ ] **Step 1: Write the failing test (cache-hit does not re-hash)**

Create `app/src/test/java/com/melodix/player/repo/local/hash/LocalHashRepositoryImplTest.kt`:
```kotlin
package com.melodix.player.repo.local.hash

import android.net.Uri
import com.melodix.player.repo.local.cache.LocalFileHashDao
import com.melodix.player.repo.local.cache.LocalFileHashEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalHashRepositoryImplTest {

    private class FakeDao : LocalFileHashDao {
        val rows = mutableMapOf<Long, LocalFileHashEntity>()
        override suspend fun getById(id: Long) = rows[id]
        override suspend fun upsert(e: LocalFileHashEntity) { rows[e.mediaStoreId] = e }
    }

    private class CountingHasher(private val value: String) : AudioHasher {
        var calls = 0
        override suspend fun md5(uri: Uri): String? { calls++; return value }
    }

    @Test
    fun returnsCachedHashWithoutRehashingWhenSizeAndDateUnchanged() = runBlocking {
        val dao = FakeDao()
        dao.rows[1L] = LocalFileHashEntity(1L, 100L, 55L, "cached-md5")
        val hasher = CountingHasher("fresh-md5")
        val repo = LocalHashRepositoryImpl(dao, hasher)

        val result = repo.md5For(1L, 100L, 55L, Uri.EMPTY)

        assertEquals("cached-md5", result)
        assertEquals(0, hasher.calls) // never recomputed
    }

    @Test
    fun rehashesWhenSizeChanged() = runBlocking {
        val dao = FakeDao()
        dao.rows[1L] = LocalFileHashEntity(1L, 100L, 55L, "cached-md5")
        val hasher = CountingHasher("fresh-md5")
        val repo = LocalHashRepositoryImpl(dao, hasher)

        val result = repo.md5For(1L, 200L, 55L, Uri.EMPTY) // size differs

        assertEquals("fresh-md5", result)
        assertEquals(1, hasher.calls)
        assertEquals("fresh-md5", dao.rows[1L]?.md5) // cache updated
    }
}
```
> Note: `Uri.EMPTY` is a static field reference; the fake hasher ignores it, so no Android runtime is needed.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.local.hash.LocalHashRepositoryImplTest"`
Expected: FAIL — unresolved references (`LocalHashRepositoryImpl`, `AudioHasher`, `LocalFileHashDao`, `LocalFileHashEntity` don't exist yet).

- [ ] **Step 3: Create the entity and DAO**

`LocalFileHashEntity.kt`:
```kotlin
package com.melodix.player.repo.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_file_hashes")
data class LocalFileHashEntity(
    @PrimaryKey val mediaStoreId: Long,
    val sizeBytes: Long,
    val dateModified: Long,
    val md5: String,
)
```
`LocalFileHashDao.kt`:
```kotlin
package com.melodix.player.repo.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalFileHashDao {
    @Query("SELECT * FROM local_file_hashes WHERE mediaStoreId = :id")
    suspend fun getById(id: Long): LocalFileHashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: LocalFileHashEntity)
}
```

- [ ] **Step 4: Register the entity in the database**

In `AppDatabase.kt`, bump version and add the entity + DAO accessor:
```kotlin
@Database(entities = [CachedTrackEntity::class, LocalFileHashEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedTrackDao(): CachedTrackDao
    abstract fun localFileHashDao(): LocalFileHashDao
}
```

- [ ] **Step 5: Create AudioHasher + LocalHashRepository**

`AudioHasher.kt`:
```kotlin
package com.melodix.player.repo.local.hash

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

interface AudioHasher {
    /** Streams the content at [uri] and returns its lowercase hex MD5, or null if unreadable. */
    suspend fun md5(uri: Uri): String?
}

class ContentResolverAudioHasher(private val context: Context) : AudioHasher {
    override suspend fun md5(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(1 shl 16)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            } ?: return@runCatching null
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}
```
`LocalHashRepository.kt`:
```kotlin
package com.melodix.player.repo.local.hash

import android.net.Uri

interface LocalHashRepository {
    /**
     * Returns the MD5 of the file identified by [mediaStoreId], using the cached value when
     * [sizeBytes] and [dateModified] are unchanged, otherwise streaming [uri] to recompute.
     * Returns null if the file cannot be read.
     */
    suspend fun md5For(mediaStoreId: Long, sizeBytes: Long, dateModified: Long, uri: Uri): String?
}
```
`LocalHashRepositoryImpl.kt`:
```kotlin
package com.melodix.player.repo.local.hash

import android.net.Uri
import com.melodix.player.repo.local.cache.LocalFileHashDao
import com.melodix.player.repo.local.cache.LocalFileHashEntity

class LocalHashRepositoryImpl(
    private val dao: LocalFileHashDao,
    private val hasher: AudioHasher,
) : LocalHashRepository {

    override suspend fun md5For(mediaStoreId: Long, sizeBytes: Long, dateModified: Long, uri: Uri): String? {
        val cached = dao.getById(mediaStoreId)
        if (cached != null && cached.sizeBytes == sizeBytes && cached.dateModified == dateModified) {
            return cached.md5
        }
        val fresh = hasher.md5(uri) ?: return null
        dao.upsert(LocalFileHashEntity(mediaStoreId, sizeBytes, dateModified, fresh))
        return fresh
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.local.hash.LocalHashRepositoryImplTest"`
Expected: PASS (2 tests).

- [ ] **Step 7: Register in Koin**

In `core/di/Modules.kt` `cacheModule`, after the `cachedTrackDao()` line add:
```kotlin
        single { get<AppDatabase>().localFileHashDao() }
        single<com.melodix.player.repo.local.hash.AudioHasher> { com.melodix.player.repo.local.hash.ContentResolverAudioHasher(androidContext()) }
        single<com.melodix.player.repo.local.hash.LocalHashRepository> { com.melodix.player.repo.local.hash.LocalHashRepositoryImpl(get(), get()) }
```

- [ ] **Step 8: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` (Room codegen picks up the new entity).

- [ ] **Step 9: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/repo/local/cache/LocalFileHashEntity.kt app/src/main/java/com/melodix/player/repo/local/cache/LocalFileHashDao.kt app/src/main/java/com/melodix/player/repo/local/cache/AppDatabase.kt app/src/main/java/com/melodix/player/repo/local/hash/ app/src/main/java/com/melodix/player/core/di/Modules.kt app/src/test/java/com/melodix/player/repo/local/hash/LocalHashRepositoryImplTest.kt
git commit -m "feat(dedup): cached local MD5 hashing (Room table + repository)"
```

### Task 6: SyncDiff reducer + wire into CloudSyncViewModel

**Files:**
- Create: `app/src/main/java/com/melodix/player/repo/sync/SyncDiff.kt`
- Test: `app/src/test/java/com/melodix/player/repo/sync/SyncDiffTest.kt`
- Modify: `app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt`

**Interfaces:**
- Consumes: `LocalHashRepository.md5For(...)` (Task 5), `DriveFile.md5Checksum` (Task 3), `Track.sizeBytes` (Task 4).
- Produces: `SyncDiff.compute(locals, remotes, cachedRemoteIds): SyncDiff.Result` with `Result(localOnlyIds: Set<Long>, downloadableIds: Set<String>)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/melodix/player/repo/sync/SyncDiffTest.kt`:
```kotlin
package com.melodix.player.repo.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDiffTest {

    @Test
    fun localWithMatchingHashIsNotLocalOnlyAndDriveFileIsNotDownloadable() {
        val locals = listOf(SyncDiff.Local(id = 1L, sizeBytes = 100L, md5 = "aaa"))
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(locals, remotes, cachedRemoteIds = emptySet())

        assertEquals(emptySet<Long>(), result.localOnlyIds)      // already on Drive
        assertEquals(emptySet<String>(), result.downloadableIds) // already have it locally
    }

    @Test
    fun unmatchedLocalIsLocalOnly() {
        val locals = listOf(SyncDiff.Local(id = 2L, sizeBytes = 50L, md5 = null)) // no size match => not hashed
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(locals, remotes, cachedRemoteIds = emptySet())

        assertEquals(setOf(2L), result.localOnlyIds)
        assertEquals(setOf("d1"), result.downloadableIds) // not present locally, not cached
    }

    @Test
    fun cachedDriveFileIsNotDownloadable() {
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(emptyList(), remotes, cachedRemoteIds = setOf("d1"))

        assertEquals(emptySet<String>(), result.downloadableIds)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.sync.SyncDiffTest"`
Expected: FAIL — `SyncDiff` unresolved.

- [ ] **Step 3: Implement SyncDiff**

`SyncDiff.kt`:
```kotlin
package com.melodix.player.repo.sync

/** Pure content-hash diff between the local library and a Drive folder listing. */
object SyncDiff {
    data class Local(val id: Long, val sizeBytes: Long, val md5: String?)
    data class Remote(val id: String, val sizeBytes: Long, val md5: String?)
    data class Result(val localOnlyIds: Set<Long>, val downloadableIds: Set<String>)

    fun compute(locals: List<Local>, remotes: List<Remote>, cachedRemoteIds: Set<String>): Result {
        val remoteMd5s = remotes.mapNotNull { it.md5 }.toSet()
        val localMd5s = locals.mapNotNull { it.md5 }.toSet()

        val localOnly = locals
            .filter { it.md5 == null || it.md5 !in remoteMd5s }
            .map { it.id }
            .toSet()

        val downloadable = remotes
            .filter { it.id !in cachedRemoteIds && (it.md5 == null || it.md5 !in localMd5s) }
            .map { it.id }
            .toSet()

        return Result(localOnly, downloadable)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.sync.SyncDiffTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Wire SyncDiff into CloudSyncViewModel.refresh()**

In `CloudSyncViewModel.kt`:
- Add constructor dependency `private val localHashRepository: com.melodix.player.repo.local.hash.LocalHashRepository,` (append to the parameter list).
- Add fields near `localOnly`: keep `private var localOnly: List<Track> = emptyList()`, and add `private var downloadableIds: Set<String> = emptySet()`.
- Replace the body of `refresh()`'s `.onSuccess { files -> ... }` with the hash-based diff:
```kotlin
                .onSuccess { files ->
                    driveFiles = files
                    val localTracks = runCatching { musicRepository.getTracks().first() }.getOrDefault(emptyList())
                    val remoteSizes = files.mapNotNull { it.md5Checksum?.let { _ -> it.sizeBytes } }.toSet()

                    // Size pre-filter: only hash local files whose exact byte-size collides with a Drive file.
                    var hashed = 0
                    val locals = localTracks.map { t ->
                        val md5 = if (t.sizeBytes in remoteSizes) {
                            hashed++
                            localHashRepository.md5For(t.id, t.sizeBytes, t.dateModified, t.uri)
                        } else null
                        SyncDiff.Local(id = t.id, sizeBytes = t.sizeBytes, md5 = md5)
                    }
                    android.util.Log.d("Melodix", "SyncDiff: hashed $hashed of ${localTracks.size} local files")

                    val remotes = files.map { SyncDiff.Remote(id = it.id, sizeBytes = it.sizeBytes, md5 = it.md5Checksum) }
                    val cached = cacheRepository.cachedIds().toSet()
                    val diff = SyncDiff.compute(locals, remotes, cached)

                    localOnly = localTracks.filter { it.id in diff.localOnlyIds }
                    downloadableIds = diff.downloadableIds
                    _uiState.update {
                        it.copy(isLoading = false, driveFileCount = files.size, localOnlyCount = localOnly.size)
                    }
                }
```
- In `downloadAll()`, replace the filter line
  `val toGet = driveFiles.filter { it.id !in cached }`
  with:
```kotlin
            val toGet = driveFiles.filter { it.id in downloadableIds }
```
  and delete the now-unused `val cached = cacheRepository.cachedIds()` line above it.
- Register the new dependency: no Koin edit needed (`viewModelOf` resolves positionally), but confirm `cacheRepository.cachedIds()` returns a `List<String>` — wrap with `.toSet()` as shown.

- [ ] **Step 6: Build + run unit tests**

Run: `.\gradlew.bat testDebugUnitTest` then `.\gradlew.bat assembleDebug`
Expected: all unit tests PASS; `BUILD SUCCESSFUL`.

- [ ] **Step 7: On-device verification (dedup)**

Precondition: a Drive folder already containing copies of local songs (from prior backups). Install + open Backup & Download.
- Confirm **"Local songs not on Drive"** excludes songs already on Drive (count reflects only genuinely-new local files).
- Tap **Download from Drive**: confirm it queues **0** items for songs you already have locally (check the message "Queued 0 download(s)." when all Drive songs match local files), and only downloads genuinely-absent ones.
- Check logcat: `adb logcat -d | grep "SyncDiff:"` shows a small "hashed N of M" where N ≪ M.

- [ ] **Step 8: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/repo/sync/SyncDiff.kt app/src/test/java/com/melodix/player/repo/sync/SyncDiffTest.kt app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt
git commit -m "feat(dedup): content-hash diff prevents duplicate uploads/downloads"
```

---

## Phase 4 — Transfer progress system (Item 4)

### Task 7: Per-item worker progress (streaming upload + download progress)

**Files:**
- Create: `app/src/main/java/com/melodix/player/repo/drive/ProgressRequestBody.kt`
- Modify: `app/src/main/java/com/melodix/player/repo/drive/DriveApi.kt` (uploadFile + downloadFile signatures)
- Modify: `app/src/main/java/com/melodix/player/worker/UploadWorker.kt`
- Modify: `app/src/main/java/com/melodix/player/worker/DownloadWorker.kt`

**Interfaces:**
- Produces: worker progress via `setProgress(workDataOf(KEY_PROGRESS to Int))`; `KEY_PROGRESS = "progress"` on both workers. `UploadWorker.KEY_SIZE = "size"`. `DriveApi.uploadFile(...)` gains `contentLength: Long, openStream: () -> InputStream, onProgress: (Long, Long) -> Unit`. `DriveApi.downloadFile(...)` gains `onProgress: (Long, Long) -> Unit`.
- Consumed by Task 8 (`TransferCenter` reads `WorkInfo.progress.getInt("progress", 0)`).

- [ ] **Step 1: Create ProgressRequestBody**

`ProgressRequestBody.kt`:
```kotlin
package com.melodix.player.repo.drive

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

/** Streams [openStream] into the request, reporting cumulative bytes via [onProgress]. */
class ProgressRequestBody(
    private val contentType: MediaType?,
    private val contentLength: Long,
    private val openStream: () -> InputStream,
    private val onProgress: (uploaded: Long, total: Long) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType
    override fun contentLength(): Long = contentLength
    override fun writeTo(sink: BufferedSink) {
        openStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            var uploaded = 0L
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, contentLength)
            }
        }
    }
}
```

- [ ] **Step 2: Stream the upload with progress**

In `DriveApi.kt`, replace `uploadFile` with a streaming version:
```kotlin
    suspend fun uploadFile(
        accessToken: String,
        folderId: String,
        name: String,
        mimeType: String,
        contentLength: Long,
        openStream: () -> java.io.InputStream,
        onProgress: (uploaded: Long, total: Long) -> Unit = { _, _ -> },
    ): DriveFile = withContext(Dispatchers.IO) {
        val metadata = json.encodeToString(FileMetadata(name = name, parents = listOf(folderId)))
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(ProgressRequestBody(mimeType.toMediaType(), contentLength, openStream, onProgress))
            .build()
        val url = UPLOAD_URL.toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id,name,mimeType,size")
            .build()
        val request = Request.Builder().url(url).bearer(accessToken).post(body).build()
        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw DriveHttpException(resp.code, respBody)
            json.decodeFromString<DriveFile>(respBody)
        }
    }
```

- [ ] **Step 3: Report download progress**

In `DriveApi.kt`, replace `downloadFile` with:
```kotlin
    suspend fun downloadFile(
        accessToken: String,
        fileId: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) = withContext(Dispatchers.IO) {
        val url = "$FILES_URL/$fileId".toHttpUrl().newBuilder()
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder().url(url).bearer(accessToken).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw DriveHttpException(resp.code, resp.message)
            val total = resp.body!!.contentLength()
            dest.outputStream().use { out ->
                resp.body!!.byteStream().use { input ->
                    val buffer = ByteArray(1 shl 16)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }
        Unit
    }
```

- [ ] **Step 4: Update UploadWorker to stream + report progress**

In `UploadWorker.kt` `doWork()`, replace the body-reading + `uploadFile` call:
```kotlin
        return try {
            val token = driveRepository.accessToken().getOrThrow()
            val uri = Uri.parse(uriStr)
            val size = inputData.getLong(KEY_SIZE, -1L).let { if (it > 0) it else resolveSize(uri) }
            driveApi.uploadFile(
                accessToken = token,
                folderId = folderId,
                name = name,
                mimeType = mimeType,
                contentLength = size,
                openStream = { applicationContext.contentResolver.openInputStream(uri)!! },
            ) { uploaded, total ->
                val pct = if (total > 0) (uploaded * 100 / total).toInt() else 0
                setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to pct))
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
```
Add a helper + companion keys:
```kotlin
    private fun resolveSize(uri: Uri): Long =
        runCatching {
            applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()?.takeIf { it > 0 } ?: -1L
```
In the companion object add:
```kotlin
        const val KEY_SIZE = "size"
        const val KEY_PROGRESS = "progress"
```

- [ ] **Step 5: Update DownloadWorker to report progress**

In `DownloadWorker.kt` `doWork()`, change the download call to report progress:
```kotlin
            driveApi.downloadFile(token, fileId, dest) { downloaded, total ->
                val pct = if (total > 0) (downloaded * 100 / total).toInt() else 0
                setProgressAsync(androidx.work.workDataOf(KEY_PROGRESS to pct))
            }
```
In the companion object add:
```kotlin
        const val KEY_PROGRESS = "progress"
```

- [ ] **Step 6: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/repo/drive/ProgressRequestBody.kt app/src/main/java/com/melodix/player/repo/drive/DriveApi.kt app/src/main/java/com/melodix/player/worker/UploadWorker.kt app/src/main/java/com/melodix/player/worker/DownloadWorker.kt
git commit -m "feat(transfer): per-item byte progress for upload/download workers"
```

### Task 8: TransferReducer + TransferCenter (app-scoped observer)

**Files:**
- Create: `app/src/main/java/com/melodix/player/repo/sync/TransferModels.kt`
- Create: `app/src/main/java/com/melodix/player/repo/sync/TransferReducer.kt`
- Test: `app/src/test/java/com/melodix/player/repo/sync/TransferReducerTest.kt`
- Create: `app/src/main/java/com/melodix/player/repo/sync/TransferCenter.kt`
- Modify: `app/src/main/java/com/melodix/player/core/di/Modules.kt`

**Interfaces:**
- Produces:
  - `enum class Direction { DOWNLOAD, UPLOAD }`
  - `enum class TransferItemState { QUEUED, RUNNING, DONE, FAILED }`
  - `data class TransferItem(val uniqueName: String, val displayName: String, val direction: Direction, val state: TransferItemState, val progress: Int)`
  - `data class TransferSnapshot(val items: List<TransferItem>)` with derived `activeCount`, `doneCount`, `failedCount`, `isActive`, `overallPercent`.
  - `TransferReducer.reduce(tracked: List<TransferReducer.Tracked>, statuses: Map<String, TransferReducer.WorkStatus>): List<TransferItem>`
  - `class TransferCenter` with `val snapshot: StateFlow<TransferSnapshot>`, `fun enqueueDownloads(files: List<DriveFile>)`, `fun enqueueUploads(tracks: List<Track>, folderId: String)`, `fun retry(uniqueName: String)`.
- Consumed by Task 9 (UI) and Task 9 rewires Cloud Sync buttons to call it.

- [ ] **Step 1: Create the shared models**

`TransferModels.kt`:
```kotlin
package com.melodix.player.repo.sync

enum class Direction { DOWNLOAD, UPLOAD }
enum class TransferItemState { QUEUED, RUNNING, DONE, FAILED }

data class TransferItem(
    val uniqueName: String,
    val displayName: String,
    val direction: Direction,
    val state: TransferItemState,
    val progress: Int,
)

data class TransferSnapshot(val items: List<TransferItem> = emptyList()) {
    val activeCount: Int get() = items.count { it.state == TransferItemState.QUEUED || it.state == TransferItemState.RUNNING }
    val doneCount: Int get() = items.count { it.state == TransferItemState.DONE }
    val failedCount: Int get() = items.count { it.state == TransferItemState.FAILED }
    val isActive: Boolean get() = activeCount > 0
    val overallPercent: Int get() = if (items.isEmpty()) 0 else items.sumOf { it.progress } / items.size
}
```

- [ ] **Step 2: Write the failing reducer test**

`TransferReducerTest.kt`:
```kotlin
package com.melodix.player.repo.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferReducerTest {

    @Test
    fun joinsTrackedItemsWithWorkStatusesPreservingOrder() {
        val tracked = listOf(
            TransferReducer.Tracked("download_a", "A.mp3", Direction.DOWNLOAD),
            TransferReducer.Tracked("download_b", "B.mp3", Direction.DOWNLOAD),
        )
        val statuses = mapOf(
            "download_a" to TransferReducer.WorkStatus(TransferItemState.RUNNING, 40),
            "download_b" to TransferReducer.WorkStatus(TransferItemState.DONE, 100),
        )

        val items = TransferReducer.reduce(tracked, statuses)

        assertEquals(2, items.size)
        assertEquals("A.mp3", items[0].displayName)
        assertEquals(TransferItemState.RUNNING, items[0].state)
        assertEquals(40, items[0].progress)
        assertEquals(TransferItemState.DONE, items[1].state)
        assertEquals(100, items[1].progress)
    }

    @Test
    fun missingStatusDefaultsToQueued() {
        val tracked = listOf(TransferReducer.Tracked("upload_x", "X.mp3", Direction.UPLOAD))
        val items = TransferReducer.reduce(tracked, emptyMap())
        assertEquals(TransferItemState.QUEUED, items[0].state)
        assertEquals(0, items[0].progress)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.sync.TransferReducerTest"`
Expected: FAIL — `TransferReducer` unresolved.

- [ ] **Step 4: Implement TransferReducer**

`TransferReducer.kt`:
```kotlin
package com.melodix.player.repo.sync

object TransferReducer {
    data class Tracked(val uniqueName: String, val displayName: String, val direction: Direction)
    data class WorkStatus(val state: TransferItemState, val progress: Int)

    fun reduce(tracked: List<Tracked>, statuses: Map<String, WorkStatus>): List<TransferItem> =
        tracked.map { t ->
            val s = statuses[t.uniqueName] ?: WorkStatus(TransferItemState.QUEUED, 0)
            TransferItem(
                uniqueName = t.uniqueName,
                displayName = t.displayName,
                direction = t.direction,
                state = s.state,
                progress = if (s.state == TransferItemState.DONE) 100 else s.progress,
            )
        }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.melodix.player.repo.sync.TransferReducerTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Implement TransferCenter**

`TransferCenter.kt`:
```kotlin
package com.melodix.player.repo.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.melodix.player.model.DriveFile
import com.melodix.player.model.Track
import com.melodix.player.worker.DownloadWorker
import com.melodix.player.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** App-scoped owner of the download/upload transfer queue and its live progress. */
class TransferCenter(context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Tracked items keyed by unique work name; retains display name + retry input.
    private data class Entry(val displayName: String, val direction: Direction, val input: Data)
    private val tracked = MutableStateFlow<Map<String, Entry>>(emptyMap())

    private val _snapshot = MutableStateFlow(TransferSnapshot())
    val snapshot: StateFlow<TransferSnapshot> = _snapshot.asStateFlow()

    init {
        val downloads = workManager.getWorkInfosByTagFlow(TAG_DOWNLOAD)
        val uploads = workManager.getWorkInfosByTagFlow(TAG_UPLOAD)
        scope.launch {
            combine(tracked, downloads, uploads) { entries, dl, ul ->
                val statuses = (dl + ul).associate { it.toUniqueName() to it.toStatus() }
                    .filterKeys { it != null }
                    .mapKeys { it.key!! }
                val trackedList = entries.map { (name, e) ->
                    TransferReducer.Tracked(name, e.displayName, e.direction)
                }
                TransferSnapshot(TransferReducer.reduce(trackedList, statuses))
            }.collect { snap -> _snapshot.update { snap } }
        }
    }

    fun enqueueDownloads(files: List<DriveFile>) {
        files.forEach { file ->
            val unique = "download_${file.id}"
            val input = workDataOf(
                DownloadWorker.KEY_FILE_ID to file.id,
                DownloadWorker.KEY_NAME to file.name,
                DownloadWorker.KEY_MIME to file.mimeType,
            )
            track(unique, Entry(file.name, Direction.DOWNLOAD, input))
            enqueue(unique, TAG_DOWNLOAD, OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(input))
        }
    }

    fun enqueueUploads(tracks: List<Track>, folderId: String) {
        tracks.forEach { t ->
            val unique = "upload_${t.id}"
            val name = "${t.title}.mp3"
            val input = workDataOf(
                UploadWorker.KEY_URI to t.uri.toString(),
                UploadWorker.KEY_NAME to name,
                UploadWorker.KEY_MIME to "audio/mpeg",
                UploadWorker.KEY_FOLDER_ID to folderId,
                UploadWorker.KEY_SIZE to t.sizeBytes,
            )
            track(unique, Entry(name, Direction.UPLOAD, input))
            enqueue(unique, TAG_UPLOAD, OneTimeWorkRequestBuilder<UploadWorker>().setInputData(input))
        }
    }

    fun retry(uniqueName: String) {
        val entry = tracked.value[uniqueName] ?: return
        val tag = if (entry.direction == Direction.DOWNLOAD) TAG_DOWNLOAD else TAG_UPLOAD
        val builder = if (entry.direction == Direction.DOWNLOAD) {
            OneTimeWorkRequestBuilder<DownloadWorker>()
        } else {
            OneTimeWorkRequestBuilder<UploadWorker>()
        }.setInputData(entry.input)
        val request = builder.addTag(tag).addTag(uniqueName)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
    }

    private fun track(unique: String, entry: Entry) {
        tracked.update { it + (unique to entry) }
    }

    private fun enqueue(unique: String, tag: String, builder: androidx.work.OneTimeWorkRequest.Builder) {
        val request = builder.addTag(tag).addTag(unique) // per-item tag so the snapshot can recover the unique name
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(unique, ExistingWorkPolicy.KEEP, request)
    }

    private fun WorkInfo.toUniqueName(): String? =
        tags.firstOrNull { it.startsWith("download_") || it.startsWith("upload_") }

    private fun WorkInfo.toStatus(): TransferReducer.WorkStatus = when (state) {
        WorkInfo.State.RUNNING -> TransferReducer.WorkStatus(TransferItemState.RUNNING, progress.getInt("progress", 0))
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> TransferReducer.WorkStatus(TransferItemState.QUEUED, 0)
        WorkInfo.State.SUCCEEDED -> TransferReducer.WorkStatus(TransferItemState.DONE, 100)
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> TransferReducer.WorkStatus(TransferItemState.FAILED, 0)
    }

    companion object {
        const val TAG_DOWNLOAD = "melodix_download"
        const val TAG_UPLOAD = "melodix_upload"
    }
}
```
> Note: `enqueue()` and `retry()` add the unique work name as a per-item tag (`.addTag(unique)`), which is how `toUniqueName()` recovers it from each `WorkInfo`.

- [ ] **Step 7: Register TransferCenter in Koin**

In `core/di/Modules.kt`, add to `syncModule`:
```kotlin
    single { com.melodix.player.repo.sync.TransferCenter(androidContext()) }
```

- [ ] **Step 8: Build + unit tests**

Run: `.\gradlew.bat testDebugUnitTest` then `.\gradlew.bat assembleDebug`
Expected: all tests PASS; `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/repo/sync/TransferModels.kt app/src/main/java/com/melodix/player/repo/sync/TransferReducer.kt app/src/main/java/com/melodix/player/repo/sync/TransferCenter.kt app/src/test/java/com/melodix/player/repo/sync/TransferReducerTest.kt app/src/main/java/com/melodix/player/core/di/Modules.kt
git commit -m "feat(transfer): app-scoped TransferCenter with live per-item snapshot"
```

### Task 9: Root-level progress UI (dialog + persistent snackbar) and rewire buttons

**Files:**
- Create: `app/src/main/java/com/melodix/player/ui/drive/TransferProgressDialog.kt`
- Create: `app/src/main/java/com/melodix/player/ui/drive/TransferProgressHost.kt`
- Modify: `app/src/main/java/com/melodix/player/core/navigation/NavGraph.kt` (host the snackbar/dialog)
- Modify: `app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt` (delegate enqueue to TransferCenter; drop WorkQueue)
- Modify: `app/src/main/java/com/melodix/player/ui/drive/CloudSyncScreen.kt` (remove old QueueRow)

**Interfaces:**
- Consumes: `TransferCenter.snapshot`, `enqueueDownloads`, `enqueueUploads`, `retry`, `clearFinished` (Task 8); `TransferItem`/`TransferItemState`/`Direction`/`TransferSnapshot` (Task 8).
- Produces: `TransferProgressHost(snapshot, onRetry, onDismissDialog, ...)` composable; `TransferProgressDialog(...)` composable.

- [ ] **Step 1: Create the dialog composable**

`TransferProgressDialog.kt`:
```kotlin
package com.melodix.player.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.melodix.player.repo.sync.TransferItem
import com.melodix.player.repo.sync.TransferItemState
import com.melodix.player.repo.sync.TransferSnapshot

@Composable
fun TransferProgressDialog(
    snapshot: TransferSnapshot,
    onRetry: (String) -> Unit,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Transfers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${snapshot.doneCount}/${snapshot.items.size} done" +
                        if (snapshot.failedCount > 0) " · ${snapshot.failedCount} failed" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(snapshot.items, key = { it.uniqueName }) { item ->
                        TransferRow(item, onRetry)
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) { Text(if (snapshot.isActive) "Hide" else "Close") }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(item: TransferItem, onRetry: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.size(4.dp))
            LinearProgressIndicator(
                progress = { item.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        Spacer(Modifier.size(12.dp))
        when (item.state) {
            TransferItemState.DONE -> Icon(
                Icons.Rounded.CheckCircle, contentDescription = "Done",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp),
            )
            TransferItemState.FAILED -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ErrorOutline, contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp),
                )
                TextButton(onClick = { onRetry(item.uniqueName) }) { Text("Retry") }
            }
            TransferItemState.RUNNING -> Text(
                "${item.progress}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            TransferItemState.QUEUED -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 2: Create the host (snackbar + dialog wiring)**

`TransferProgressHost.kt`:
```kotlin
package com.melodix.player.ui.drive

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.melodix.player.repo.sync.TransferSnapshot

/**
 * Renders a persistent snackbar while transfers are active and, on tap, the full dialog.
 * [openDialogSignal] is incremented by callers (e.g. Cloud Sync buttons) to force the dialog open.
 */
@Composable
fun TransferProgressHost(
    snapshot: TransferSnapshot,
    snackbarHostState: SnackbarHostState,
    openDialogSignal: Int,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(openDialogSignal) {
        if (openDialogSignal > 0) showDialog = true
    }

    // Drive a persistent snackbar off the snapshot; tapping it reopens the dialog.
    LaunchedEffect(snapshot.isActive, snapshot.doneCount, snapshot.failedCount, snapshot.items.size) {
        if (snapshot.items.isEmpty() || showDialog) return@LaunchedEffect
        val label = if (snapshot.isActive) {
            "Transferring ${snapshot.doneCount}/${snapshot.items.size} · ${snapshot.overallPercent}%"
        } else {
            "Done ${snapshot.doneCount}/${snapshot.items.size}" +
                if (snapshot.failedCount > 0) " · ${snapshot.failedCount} failed" else ""
        }
        val result = snackbarHostState.showSnackbar(
            message = label,
            actionLabel = "View",
            duration = if (snapshot.isActive) SnackbarDuration.Indefinite else SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) showDialog = true
    }

    if (showDialog) {
        TransferProgressDialog(
            snapshot = snapshot,
            onRetry = onRetry,
            onClose = { showDialog = false },
        )
    }

    SnackbarHost(hostState = snackbarHostState, modifier = modifier)
}
```

- [ ] **Step 3: Host it at the NavGraph root**

The `openSignal` is a plain counter hoisted in `NavGraph`: incrementing it opens the dialog. Because `CloudSyncScreen` is a **top-level `NavGraph` destination** (not nested in `MainScreen`), the `onOpenTransfers` callback is passed directly at its `composable(...)` entry — no threading through `MainScreen`.

In `NavGraph.kt`, add imports:
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.melodix.player.repo.sync.TransferCenter
import com.melodix.player.ui.drive.TransferProgressHost
import org.koin.compose.koinInject
```
Wrap the `NavHost` (already inside the Task 1 `Surface`) in a `Box` and add the host. `openSignal` and the `CloudSync` route wiring are declared here:
```kotlin
Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    val transferCenter: TransferCenter = koinInject()
    val transferSnapshot by transferCenter.snapshot.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var openSignal by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost( /* ...existing navController/startDestination/transitions... */ ) {
            // ...all existing composable(...) entries unchanged EXCEPT the CloudSync route below...
            composable(Routes.CloudSync.route) {
                CloudSyncScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTransfers = { openSignal++ },
                )
            }
        }

        TransferProgressHost(
            snapshot = transferSnapshot,
            snackbarHostState = snackbarHostState,
            openDialogSignal = openSignal,
            onRetry = { transferCenter.retry(it) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
```
> Replace only the existing `composable(Routes.CloudSync.route) { CloudSyncScreen(onBack = ...) }` block with the version above; leave every other route untouched.

- [ ] **Step 4: Delegate enqueue to TransferCenter in CloudSyncViewModel**

In `CloudSyncViewModel.kt`:
- Add constructor dependency `private val transferCenter: com.melodix.player.repo.sync.TransferCenter,`.
- Delete the `WorkQueue` data class, the `downloads`/`uploads` fields from `CloudSyncUiState`, the two `observe(workManager...)` lines, the `enqueue(...)` helper, the `toQueue()` extension, the `workManager` field, the `context: Context` constructor parameter (now unused after `workManager` is gone), and the `TAG_DOWNLOAD`/`TAG_UPLOAD` companion constants (delete the whole now-empty `companion object`).
- Replace `downloadAll()` body with:
```kotlin
    fun downloadAll() {
        val toGet = driveFiles.filter { it.id in downloadableIds }
        transferCenter.enqueueDownloads(toGet)
        _uiState.update { it.copy(message = "Queued ${toGet.size} download(s).") }
    }
```
- Replace `backupLocalOnly()` body with:
```kotlin
    fun backupLocalOnly() {
        val id = folderId ?: return
        transferCenter.enqueueUploads(localOnly, id)
        _uiState.update { it.copy(message = "Queued ${localOnly.size} upload(s).") }
    }
```
- Remove unused imports (`WorkManager`, `WorkInfo`, `OneTimeWorkRequest*`, `Constraints`, `ExistingWorkPolicy`, `NetworkType`, `workDataOf`, `DownloadWorker`, `UploadWorker`, `Context`) that are no longer referenced.

- [ ] **Step 5: Remove old QueueRow UI; trigger dialog from buttons**

In `CloudSyncScreen.kt`:
- Delete the `if (state.downloads.total > 0) { ... QueueRow("Downloads", state.downloads) }` and the equivalent uploads block.
- Delete the private `QueueRow` composable and the `WorkQueue` import.
- Change the composable signature to accept `onOpenTransfers: () -> Unit = {}` and call it in both button handlers:
```kotlin
MelodixButton(text = "Download from Drive", onClick = { viewModel.downloadAll(); onOpenTransfers() })
Spacer(Modifier.height(12.dp))
MelodixOutlinedButton(text = "Back up local songs to Drive", onClick = { viewModel.backupLocalOnly(); onOpenTransfers() })
```
- The `onOpenTransfers` wiring is already provided at the `composable(Routes.CloudSync.route)` entry in Step 3 (`onOpenTransfers = { openSignal++ }`) — no further threading needed.

- [ ] **Step 6: Build + unit tests**

Run: `.\gradlew.bat testDebugUnitTest` then `.\gradlew.bat assembleDebug`
Expected: all tests PASS; `BUILD SUCCESSFUL`.

- [ ] **Step 7: On-device end-to-end verification**

Install + open Backup & Download with a connected Drive folder that has some new files.
- Tap **Download from Drive**: the dialog opens showing each item with a moving progress bar and percent; a **✓** appears as each finishes.
- Tap **Hide**: the dialog closes and a **persistent snackbar** shows "Transferring N/M · X%". Navigate to Home/Library — the snackbar stays.
- Tap the snackbar's **View**: the dialog reopens.
- Force a failure (turn off Wi-Fi mid-batch so a worker fails after retries): the failed row shows an **error icon + Retry**; tap **Retry** (with Wi-Fi back) and confirm it re-runs to success.
- When all finish: snackbar shows "Done M/M" (and "· K failed" if any) and can be dismissed.
Capture screenshots via Bash `adb exec-out screencap -p > transfer_*.png` at each step.

- [ ] **Step 8: Commit (checkpoint)**

```bash
git add app/src/main/java/com/melodix/player/ui/drive/TransferProgressDialog.kt app/src/main/java/com/melodix/player/ui/drive/TransferProgressHost.kt app/src/main/java/com/melodix/player/core/navigation/NavGraph.kt app/src/main/java/com/melodix/player/viewmodel/CloudSyncViewModel.kt app/src/main/java/com/melodix/player/ui/drive/CloudSyncScreen.kt
git commit -m "feat(transfer): root-level progress dialog + persistent snackbar with retry"
```

---

## Final verification (all items)

- [ ] `.\gradlew.bat testDebugUnitTest` — all unit tests green (LocalHashRepositoryImplTest, SyncDiffTest, TransferReducerTest).
- [ ] `.\gradlew.bat assembleDebug` — `BUILD SUCCESSFUL`.
- [ ] On device: theme light/dark → bars + backgrounds correct (Item 1); Cloud Sync has no "Your data" section (Item 2); re-download of already-local songs queues 0 (Item 3); transfer dialog + persistent snackbar + retry work and survive navigation (Item 4).
- [ ] Update `melodix-project` / `melodix-release-signing` memory only if signing or project-status facts changed (they don't here).

## Notes / deferred

- Upload now streams from the content URI (no full in-memory read) — a memory win beyond progress reporting.
- Existing duplicate cached copies are **not** cleaned up (explicit scope decision); only future duplicates are prevented.
- If a future need arises to match the same song across different encodings, extend `SyncDiff` with a metadata fallback — the reducer is already isolated and unit-tested.
