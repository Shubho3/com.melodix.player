package com.melodix.player

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.melodix.player.core.di.appModule
import com.melodix.player.core.di.repoModule
import com.melodix.player.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MelodixApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MelodixApp)
            modules(appModule, repoModule, viewModelModule)
        }
    }

    // Cap Coil's album-art memory cache (default is 25% of app RAM) and crossfade for smoothness.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .crossfade(true)
            .build()
}
