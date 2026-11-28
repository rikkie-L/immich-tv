package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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
import com.immichtv.app.ui.theme.ImmichSurface
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.viewmodel.HomeViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhotosScreen(
    onPhotoClick: (assetId: String, assets: List<Asset>, timeBucket: String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState(),
    focusMonth: String? = null,
    focusAssetId: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val monthFocus = remember { FocusRequester() }

    // Coming back from the viewer: put focus on the first photo of the day being viewed.
    LaunchedEffect(focusMonth, state.months.size) {
        val target = focusMonth ?: return@LaunchedEffect
        val index = state.months.indexOfFirst { it.timeBucket == target }
        if (index < 0) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.scrollToItem(index)
        }
        delay(100) // let the row compose
        try { monthFocus.requestFocus() } catch (_: IllegalStateException) { }
        onFocusRestored()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmichSurface)
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
                    state = listState,
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
                                // One row per day, newest day first, e.g. "Friday 27 November".
                                val days = remember(month.assets) {
                                    month.assets.groupBy { dayKey(it) }.toList()
                                }
                                // Day of the photo that was on screen when Back was pressed.
                                val returnDay = if (month.timeBucket == focusMonth) {
                                    month.assets.firstOrNull { it.id == focusAssetId }?.let { dayKey(it) }
                                        ?: days.firstOrNull()?.first
                                } else null
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    days.forEach { (day, dayAssets) ->
                                        Column {
                                            Text(
                                                text = dayLabel(day, dayAssets.size),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color(0xFFB0B0B0),
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                contentPadding = PaddingValues(end = 8.dp)
                                            ) {
                                                itemsIndexed(dayAssets) { i, asset ->
                                                    val isReturnTarget = i == 0 && day == returnDay
                                                    PhotoCard(
                                                        thumbnailUrl = viewModel.getThumbnailUrl(asset.id),
                                                        apiKey = state.apiKey,
                                                        isVideo = asset.type.uppercase() == "VIDEO",
                                                        onClick = { onPhotoClick(asset.id, month.assets, month.timeBucket) },
                                                        modifier = Modifier
                                                            .size(160.dp)
                                                            .then(if (isReturnTarget) Modifier.focusRequester(monthFocus) else Modifier)
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
        }
    }
}

/** The day a photo was taken (yyyy-MM-dd), in the photographer's local time. */
private fun dayKey(asset: Asset): String =
    asset.localDateTime.ifEmpty { asset.fileCreatedAt }.take(10)

/** "Friday 27 November · 12" in the TV's language, e.g. "Vrijdag 27 november · 12". */
private fun dayLabel(day: String, count: Int): String {
    val text = try {
        LocalDate.parse(day)
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    } catch (_: Exception) {
        day.ifEmpty { "Unknown date" }
    }
    return "$text \u00B7 $count"
}
