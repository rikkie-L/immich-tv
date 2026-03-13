package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.immichtv.app.ui.components.PhotoCard
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.AlbumsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onPhotoClick: (assetId: String, assetIds: List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmichBackground)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ImmichYellow
                )
            }
            else -> {
                val album = state.selectedAlbum
                if (album != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onBack,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text("← Back")
                            }
                            Column {
                                Text(
                                    text = album.albumName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFFEAEAEA)
                                )
                                Text(
                                    text = "${album.assetCount} photos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        val allIds = album.assets.map { it.id }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(album.assets) { asset ->
                                PhotoCard(
                                    thumbnailUrl = viewModel.getThumbnailUrl(asset.id),
                                    apiKey = state.apiKey,
                                    onClick = { onPhotoClick(asset.id, allIds) },
                                    modifier = Modifier.focusable()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
