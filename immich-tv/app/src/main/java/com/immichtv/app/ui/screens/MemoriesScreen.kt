package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import com.immichtv.app.data.model.Asset
import com.immichtv.app.ui.components.AlbumCard
import com.immichtv.app.ui.components.PhotoCard
import com.immichtv.app.ui.theme.ImmichSurface
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.MemoriesViewModel
import com.immichtv.app.viewmodel.day
import com.immichtv.app.viewmodel.title
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All memories Immich has made, grouped per day, newest first. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MemoriesScreen(
    onMemoryClick: (memoryId: String) -> Unit,
    viewModel: MemoriesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(ImmichSurface)) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ImmichYellow
            )
            state.error != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error.orEmpty(), color = Color(0xFFCF6679))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadMemories() }) { Text("Retry") }
            }
            state.memories.isEmpty() -> Text(
                "No memories yet",
                color = Color(0xFF888888),
                modifier = Modifier.align(Alignment.Center)
            )
            else -> {
                // One row per day the memories were made for, newest first.
                val days = remember(state.memories) { state.memories.groupBy { it.day() }.toList() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 40.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    item {
                        Text(
                            text = "Memories",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFEAEAEA)
                        )
                    }
                    items(days, key = { it.first }) { (day, memories) ->
                        Column {
                            Text(
                                text = memoryDayLabel(day),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFB0B0B0),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(end = 8.dp)
                            ) {
                                items(memories, key = { it.id }) { memory ->
                                    AlbumCard(
                                        name = memory.title(),
                                        thumbnailUrl = memory.assets.firstOrNull()?.let { viewModel.getThumbnailUrl(it.id) },
                                        apiKey = state.apiKey,
                                        assetCount = memory.assets.size,
                                        onClick = { onMemoryClick(memory.id) }
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

/** "Today", "Yesterday", or e.g. "Vrijdag 27 november 2026" in the TV's language. */
private fun memoryDayLabel(day: String): String {
    val date = try { LocalDate.parse(day) } catch (_: Exception) { return day }
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}

/** One memory: a Slideshow button and all its photos. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memoryId: String,
    onPhotoClick: (assetId: String, assets: List<Asset>) -> Unit,
    onSlideshow: (assets: List<Asset>) -> Unit,
    onBack: () -> Unit,
    viewModel: MemoriesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val memory = state.memories.firstOrNull { it.id == memoryId }
    val slideshowFocus = remember { FocusRequester() }

    if (memory == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(memoryId) {
        try { slideshowFocus.requestFocus() } catch (_: IllegalStateException) { }
    }

    Column(modifier = Modifier.fillMaxSize().background(ImmichSurface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onBack) { Text("\u2190 Back") }
            Button(
                onClick = { onSlideshow(memory.assets) },
                modifier = Modifier.focusRequester(slideshowFocus)
            ) { Text("\u25B6 Slideshow") }
            Column {
                Text(
                    text = memory.title(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFEAEAEA)
                )
                Text(
                    text = "${memory.assets.size} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888)
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(memory.assets) { asset ->
                PhotoCard(
                    thumbnailUrl = viewModel.getThumbnailUrl(asset.id),
                    apiKey = state.apiKey,
                    isVideo = asset.type.uppercase() == "VIDEO",
                    onClick = { onPhotoClick(asset.id, memory.assets) }
                )
            }
        }
    }
}
