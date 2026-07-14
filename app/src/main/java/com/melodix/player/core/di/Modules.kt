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
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { PlaybackController(get()) }
}

val repoModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<MusicRepository> { MusicRepositoryImpl(get(), get()) }
    single<PlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<PlayHistoryRepository> { PlayHistoryRepositoryImpl(get()) }
}

val viewModelModule = module {
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
