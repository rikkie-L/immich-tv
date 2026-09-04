package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.immichtv.app.data.model.Asset
import com.immichtv.app.ui.components.PhotoCard
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.HomeViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhotosScreen(
    onPhotoClick: (assetId: String, assets: List<Asset>) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmichBackground)
    ) {
        when {
            state.isLoading && state.months.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ImmichYellow
                )
            }
            state.error != null && state.months.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        color = Color(0xFFCF6679),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadTimeline() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(state.months) { month ->
                        LaunchedEffect(month.timeBucket) {
                            if (month.assets.isEmpty() && !month.isLoading) {
                                viewModel.loadMonthAssets(month.timeBucket)
                            }
                        }

                        Column {
                            Text(
                                text = month.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFEAEAEA),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (month.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = ImmichYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else if (month.assets.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(end = 8.dp)
                                ) {
                                    items(month.assets) { asset ->
                                        PhotoCard(
                                            thumbnailUrl = viewModel.getThumbnailUrl(asset.id),
                                            apiKey = state.apiKey,
                                            isVideo = asset.type.uppercase() == "VIDEO",
                                            onClick = { onPhotoClick(asset.id, month.assets) },
                                            modifier = Modifier.size(160.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
