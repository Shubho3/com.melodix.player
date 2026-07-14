package com.melodix.player.core.di

import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.PlayHistoryRepository
import com.melodix.player.repo.PlaylistRepository
import com.melodix.player.repo.SettingsRepository
import com.melodix.player.repo.local.FavoritesRepositoryImpl
import com.melodix.player.repo.local.MusicRepositoryImpl
import com.melodix.player.repo.local.PlayHistoryRepositoryImpl
import com.melodix.player.repo.local.PlaylistRepositoryImpl
import com.melodix.player.repo.local.SettingsRepositoryImpl
import com.melodix.player.service.PlaybackController
import com.melodix.player.viewmodel.AlbumDetailViewModel
import com.melodix.player.viewmodel.ArtistDetailViewModel
import com.melodix.player.viewmodel.HomeViewModel
import com.melodix.player.viewmodel.LibraryViewModel
import com.melodix.player.viewmodel.LikedSongsViewModel
import com.melodix.player.viewmodel.NowPlayingViewModel
import com.melodix.player.viewmodel.OnboardingViewModel
import com.melodix.player.viewmodel.PlaylistDetailViewModel
import com.melodix.player.viewmodel.PlaylistViewModel
import com.melodix.player.viewmodel.QueueViewModel
import com.melodix.player.viewmodel.RecentlyPlayedViewModel
import com.melodix.player.viewmodel.SearchViewModel
import com.melodix.player.viewmodel.SettingsViewModel
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.melodix.player.core.audio.AudioEffects
import com.melodix.player.viewmodel.EqualizerViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.melodix.player.repo.sync.SyncCoordinator
import com.melodix.player.repo.sync.SyncManager
import com.melodix.player.R
import com.melodix.player.repo.local.cache.AppDatabase
import com.melodix.player.core.auth.DriveAuthManager
import com.melodix.player.core.auth.GoogleAuthClient
import com.melodix.player.repo.AuthRepository
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.DriveRepository
import com.melodix.player.repo.auth.AuthRepositoryImpl
import com.melodix.player.repo.drive.DriveApi
import com.melodix.player.repo.drive.DriveRepositoryImpl
import com.melodix.player.repo.local.cache.CacheRepositoryImpl
import com.melodix.player.viewmodel.AuthViewModel
import com.melodix.player.viewmodel.CloudSyncViewModel
import com.melodix.player.viewmodel.DriveFolderPickerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { PlaybackController(get()) }
    single { AudioEffects(get()) }
}

val repoModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<MusicRepository> { MusicRepositoryImpl(get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<PlayHistoryRepository> { PlayHistoryRepositoryImpl(get()) }
}

val authModule = module {
    single { FirebaseAuth.getInstance() }
    single { GoogleAuthClient(androidContext().getString(R.string.default_web_client_id)) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
}

val driveModule = module {
    single { DriveApi() }
    single { DriveAuthManager(androidContext()) }
    single<DriveRepository> { DriveRepositoryImpl(get(), get()) }
}

val syncModule = module {
    single { FirebaseFirestore.getInstance() }
    single { SyncManager(get(), get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get(), get()) }
}

val cacheModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "melodix.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().cachedTrackDao() }
    single<CacheRepository> { CacheRepositoryImpl(androidContext(), get()) }
}

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::DriveFolderPickerViewModel)
    viewModelOf(::CloudSyncViewModel)
    viewModelOf(::EqualizerViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::NowPlayingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::QueueViewModel)
    viewModelOf(::PlaylistViewModel)
    viewModelOf(::AlbumDetailViewModel)
    viewModelOf(::ArtistDetailViewModel)
    viewModelOf(::PlaylistDetailViewModel)
    viewModelOf(::LikedSongsViewModel)
    viewModelOf(::RecentlyPlayedViewModel)
}
