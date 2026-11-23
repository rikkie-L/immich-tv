package com.immichtv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.immichtv.app.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.immichtv.app.data.model.Asset
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichSurface
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.AlbumsViewModel
import com.immichtv.app.viewmodel.HomeViewModel
import com.immichtv.app.viewmodel.MemoriesViewModel

enum class NavDestination(val label: String, val icon: String) {
    Photos("Photos", "🖼"),
    Memories("Memories", "✨"),
    Albums("Albums", "📁"),
    Settings("Settings", "⚙")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel = viewModel(),
    albumsViewModel: AlbumsViewModel = viewModel(),
    memoriesViewModel: MemoriesViewModel = viewModel()
) {
    var currentDest by remember { mutableStateOf(NavDestination.Photos) }
    var viewerState by remember { mutableStateOf<ViewerState?>(null) }
    var albumDetailId by remember { mutableStateOf<String?>(null) }
    var memoryDetailId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val photosState by homeViewModel.state.collectAsState()

    // Full-screen photo viewer
    viewerState?.let { viewer ->
        PhotoViewerScreen(
            assetId = viewer.assetId,
            assets = viewer.assets,
            getOriginalUrl = { homeViewModel.getOriginalUrl(it) },
            apiKey = photosState.apiKey,
            onBack = { viewerState = null },
            startSlideshow = viewer.slideshow
        )
        return
    }

    // Settings overlay
    if (showSettings) {
        SetupScreen(onConnected = { showSettings = false })
        return
    }

    NavigationDrawer(
        modifier = Modifier.fillMaxSize().background(ImmichSurface),
        drawerContent = {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(ImmichBackground)
                    .padding(vertical = 24.dp, horizontal = 12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.immich_logo),
                    contentDescription = "Immich",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavDestination.entries.forEach { dest ->
                    val isSelected = currentDest == dest
                    NavigationDrawerItem(
                        selected = isSelected,
                        onClick = {
                            if (dest == NavDestination.Settings) {
                                showSettings = true
                            } else {
                                currentDest = dest
                                albumDetailId = null
                                memoryDetailId = null
                            }
                        },
                        leadingContent = {
                            Text(dest.icon, style = MaterialTheme.typography.titleMedium)
                        }
                    ) {
                        // No fixed colour: NavigationDrawerItem switches to dark text
                        // on its light focus highlight (a fixed light colour made the
                        // focused item unreadable).
                        Text(text = dest.label)
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                currentDest == NavDestination.Photos -> {
                    PhotosScreen(
                        onPhotoClick = { assetId, assets -> viewerState = ViewerState(assetId, assets) },
                        viewModel = homeViewModel
                    )
                }
                currentDest == NavDestination.Memories && memoryDetailId != null -> {
                    val memoryId = memoryDetailId ?: return@NavigationDrawer
                    MemoryDetailScreen(
                        memoryId = memoryId,
                        onPhotoClick = { assetId, assets -> viewerState = ViewerState(assetId, assets) },
                        onSlideshow = { assets ->
                            assets.firstOrNull()?.let {
                                viewerState = ViewerState(it.id, assets, slideshow = true)
                            }
                        },
                        onBack = { memoryDetailId = null },
                        viewModel = memoriesViewModel
                    )
                }
                currentDest == NavDestination.Memories -> {
                    MemoriesScreen(
                        onMemoryClick = { memoryDetailId = it },
                        viewModel = memoriesViewModel
                    )
                }
                currentDest == NavDestination.Albums && albumDetailId != null -> {
                    val albumId = albumDetailId ?: return@NavigationDrawer
                    AlbumDetailScreen(
                        albumId = albumId,
                        onPhotoClick = { assetId, assets -> viewerState = ViewerState(assetId, assets) },
                        onBack = { albumDetailId = null },
                        viewModel = albumsViewModel
                    )
                }
                currentDest == NavDestination.Albums -> {
                    AlbumsScreen(
                        onAlbumClick = { albumDetailId = it },
                        viewModel = albumsViewModel
                    )
                }
                else -> {}
            }
        }
    }
}

/** What the full-screen viewer shows, and whether it starts as a slideshow. */
private data class ViewerState(
    val assetId: String,
    val assets: List<Asset>,
    val slideshow: Boolean = false
)
