package com.melodix.player.core.navigation

sealed class Routes(val route: String) {
    data object Onboarding : Routes("onboarding")
    data object Main : Routes("main")
    data object NowPlaying : Routes("now_playing")
    data object Queue : Routes("queue")

    data object AlbumDetail : Routes("album_detail/{albumId}") {
        const val ARG = "albumId"
        fun create(albumId: Long) = "album_detail/$albumId"
    }

    data object ArtistDetail : Routes("artist_detail/{artistId}") {
        const val ARG = "artistId"
        fun create(artistId: Long) = "artist_detail/$artistId"
    }

    data object PlaylistDetail : Routes("playlist_detail/{playlistId}") {
        const val ARG = "playlistId"
        fun create(playlistId: String) = "playlist_detail/$playlistId"
    }

    data object LikedSongs : Routes("liked_songs")

    data object RecentlyPlayed : Routes("recently_played")

    data object DrivePicker : Routes("drive_picker")

    data object CloudSync : Routes("cloud_sync")

    data object Equalizer : Routes("equalizer")
}
