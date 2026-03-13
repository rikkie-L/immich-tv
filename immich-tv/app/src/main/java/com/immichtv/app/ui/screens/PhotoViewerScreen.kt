package com.immichtv.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.immichtv.app.ui.theme.ImmichYellow
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    assetId: String,
    assetIds: List<String>,
    getOriginalUrl: (String) -> String,
    apiKey: String,
    onBack: () -> Unit,
    slideshowMode: Boolean = false
) {
    var currentIndex by remember(assetId, assetIds) {
        mutableIntStateOf(assetIds.indexOf(assetId).coerceAtLeast(0))
    }
    var showControls by remember { mutableStateOf(true) }
    var isSlideshow by remember { mutableStateOf(slideshowMode) }
    val focusRequester = remember { FocusRequester() }

    // Hide controls after a few seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    // Slideshow auto-advance
    LaunchedEffect(isSlideshow, currentIndex) {
        if (isSlideshow) {
            delay(5000)
            if (currentIndex < assetIds.size - 1) {
                currentIndex++
            } else {
                currentIndex = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    showControls = true
                    when (event.key) {
                        Key.DirectionLeft, Key.NavigatePrevious -> {
                            if (currentIndex > 0) currentIndex--
                            true
                        }
                        Key.DirectionRight, Key.NavigateNext -> {
                            if (currentIndex < assetIds.size - 1) currentIndex++
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onBack()
                            true
                        }
                        Key.MediaPlayPause, Key.Enter -> {
                            isSlideshow = !isSlideshow
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        val currentAssetId = assetIds.getOrNull(currentIndex) ?: assetId

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(getOriginalUrl(currentAssetId))
                .addHeader("x-api-key", apiKey)
                .crossfade(300)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBack) { Text("← Back") }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${currentIndex + 1} / ${assetIds.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSlideshow) {
                        Text("⏵ Slideshow", color = ImmichYellow, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Button(onClick = { isSlideshow = !isSlideshow }) {
                    Text(if (isSlideshow) "Stop Slideshow" else "Slideshow")
                }
            }
        }

        // Navigation hints
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = "← → Navigate  •  OK / Play = Slideshow  •  Back = Exit",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
