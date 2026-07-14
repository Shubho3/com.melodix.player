package com.melodix.player.core.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.melodix.player.R

/**
 * Album/track artwork with the app logo as the default when a track has no artwork or the artwork
 * fails to load. Track art URIs are always non-null (built from MediaStore), so a missing embedded
 * cover surfaces here as a load error — hence logo is wired to placeholder, error, and fallback.
 */
@Composable
fun AlbumArtImage(
    artUri: Uri?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val logo = painterResource(R.mipmap.ic_launcher_foreground)
    AsyncImage(
        model = artUri,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        placeholder = logo,
        error = logo,
        fallback = logo,
    )
}
