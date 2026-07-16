package com.melodix.player

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import okio.Path.Companion.toPath
import com.melodix.player.core.di.appModule
import com.melodix.player.core.di.authModule
import com.melodix.player.core.di.cacheModule
import com.melodix.player.core.di.driveModule
import com.melodix.player.core.di.repoModule
import com.melodix.player.core.di.syncModule
import com.melodix.player.core.di.viewModelModule
import com.melodix.player.repo.sync.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MelodixApp : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncCoordinator: SyncCoordinator by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MelodixApp)
            modules(appModule, repoModule, viewModelModule, authModule, driveModule, cacheModule, syncModule)
        }
        // Auto-sync favorites/history/playlists to Firestore whenever they change (while signed in).
        syncCoordinator.start(appScope)
    }

    // Cap Coil's album-art memory cache (default is 25% of app RAM) and crossfade for smoothness.
    // A bounded disk cache under cacheDir/image_cache persists cover art across launches and is what
    // the Storage screen's "Album art" category measures and clears (see StorageRepositoryImpl).
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_DIR).path.toPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()

    companion object {
        /** Coil disk-cache subdirectory of [android.content.Context.getCacheDir]. */
        const val IMAGE_CACHE_DIR = "image_cache"
    }
}
