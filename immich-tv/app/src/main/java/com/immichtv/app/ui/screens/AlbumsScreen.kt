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
import com.immichtv.app.ui.components.AlbumCard
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.AlbumsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (albumId: String) -> Unit,
    viewModel: AlbumsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            state.error != null -> {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = Color(0xFFCF6679))
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAlbums() }) { Text("Retry") }
                }
            }
            state.albums.isEmpty() -> {
                Text(
                    "No albums found",
                    color = Color(0xFF888888),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFEAEAEA),
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 24.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.albums) { album ->
                            val thumbUrl = album.albumThumbnailAssetId?.let {
                                viewModel.getThumbnailUrl(it)
                            }
                            AlbumCard(
                                name = album.albumName,
                                thumbnailUrl = thumbUrl,
                                apiKey = state.apiKey,
                                assetCount = album.assetCount,
                                onClick = { onAlbumClick(album.id) },
                                modifier = Modifier.focusable()
                            )
                        }
                    }
                }
            }
        }
    }
}
