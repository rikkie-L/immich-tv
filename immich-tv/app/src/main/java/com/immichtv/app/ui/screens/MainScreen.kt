package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

enum class NavDestination(val label: String, val icon: String) {
    Photos("Photos", "🖼"),
    Albums("Albums", "📁"),
    Settings("Settings", "⚙")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel = viewModel(),
    albumsViewModel: AlbumsViewModel = viewModel()
) {
    var currentDest by remember { mutableStateOf(NavDestination.Photos) }
    var viewerState by remember { mutableStateOf<Pair<String, List<Asset>>?>(null) }
    var albumDetailId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val photosState by homeViewModel.state.collectAsState()

    // Full-screen photo viewer
    viewerState?.let { (assetId, assets) ->
        PhotoViewerScreen(
            assetId = assetId,
            assets = assets,
            getOriginalUrl = { homeViewModel.getOriginalUrl(it) },
            apiKey = photosState.apiKey,
            onBack = { viewerState = null }
        )
        return
    }

    // Settings overlay
    if (showSettings) {
        SetupScreen(onConnected = { showSettings = false })
        return
    }

    NavigationDrawer(
        modifier = Modifier.fillMaxSize().background(ImmichBackground),
        drawerContent = {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(ImmichSurface)
                    .padding(vertical = 24.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "immich",
                    style = MaterialTheme.typography.titleLarge,
                    color = ImmichYellow,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
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
                            }
                        },
                        leadingContent = {
                            Text(dest.icon, style = MaterialTheme.typography.titleMedium)
                        }
                    ) {
                        Text(
                            text = dest.label,
                            color = if (isSelected) ImmichYellow else Color(0xFFEAEAEA)
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                currentDest == NavDestination.Photos -> {
                    PhotosScreen(
                        onPhotoClick = { assetId, assets -> viewerState = Pair(assetId, assets) },
                        viewModel = homeViewModel
                    )
                }
                currentDest == NavDestination.Albums && albumDetailId != null -> {
                    val albumId = albumDetailId ?: return@NavigationDrawer
                    AlbumDetailScreen(
                        albumId = albumId,
                        onPhotoClick = { assetId, assets -> viewerState = Pair(assetId, assets) },
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
