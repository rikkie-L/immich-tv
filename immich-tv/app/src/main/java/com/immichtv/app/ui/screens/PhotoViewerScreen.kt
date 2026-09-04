package com.immichtv.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.immichtv.app.data.model.Asset
import com.immichtv.app.ui.components.VideoPlayer
import com.immichtv.app.ui.theme.ImmichSurface
import com.immichtv.app.ui.theme.ImmichYellow
import kotlinx.coroutines.delay

private val SLIDESHOW_INTERVALS = listOf(3, 5, 10) // seconds

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    assetId: String,
    assets: List<Asset>,
    getOriginalUrl: (String) -> String,
    apiKey: String,
    onBack: () -> Unit
) {
    var currentIndex by remember(assetId, assets) {
        mutableIntStateOf(assets.indexOfFirst { it.id == assetId }.coerceAtLeast(0))
    }
    var showControls by remember { mutableStateOf(true) }
    var isSlideshow by remember { mutableStateOf(false) }
    var intervalIndex by remember { mutableIntStateOf(1) } // default 5s
    var videoPlaying by remember { mutableStateOf(true) }
    // Progress 0f–1f for image slideshow countdown
    var slideshowProgress by remember { mutableFloatStateOf(0f) }

    val currentAsset = assets.getOrNull(currentIndex) ?: return
    val isVideo = currentAsset.type.uppercase() == "VIDEO"
    val slideshowInterval = SLIDESHOW_INTERVALS[intervalIndex]
    val focusRequester = remember { FocusRequester() }

    fun advance() {
        currentIndex = if (currentIndex < assets.size - 1) currentIndex + 1 else 0
        slideshowProgress = 0f
        videoPlaying = true
    }

    fun retreat() {
        currentIndex = if (currentIndex > 0) currentIndex - 1 else assets.size - 1
        slideshowProgress = 0f
        videoPlaying = true
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    // Slideshow timer for images (videos use onEnded callback)
    LaunchedEffect(isSlideshow, currentIndex, isVideo) {
        if (isSlideshow && !isVideo) {
            val steps = 60
            val stepMs = (slideshowInterval * 1000L) / steps
            repeat(steps) { i ->
                slideshowProgress = i.toFloat() / steps
                delay(stepMs)
            }
            slideshowProgress = 1f
            advance()
        } else {
            slideshowProgress = 0f
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                        Key.DirectionLeft, Key.NavigatePrevious -> { retreat(); true }
                        Key.DirectionRight, Key.NavigateNext -> { advance(); true }
                        Key.Back, Key.Escape -> { onBack(); true }
                        Key.MediaPlayPause -> {
                            if (isVideo) videoPlaying = !videoPlaying
                            else isSlideshow = !isSlideshow
                            true
                        }
                        Key.Enter, Key.DirectionCenter -> {
                            isSlideshow = !isSlideshow
                            if (isVideo) videoPlaying = isSlideshow
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // ── Content ──────────────────────────────────────────────────────
        if (isVideo) {
            VideoPlayer(
                videoUrl = getOriginalUrl(currentAsset.id),
                apiKey = apiKey,
                isPlaying = videoPlaying,
                onEnded = { if (isSlideshow) advance() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(getOriginalUrl(currentAsset.id))
                    .addHeader("x-api-key", apiKey)
                    .crossfade(400)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Top controls bar ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back
                Button(onClick = onBack) { Text("← Back") }

                // Counter + type badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isVideo) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            colors = SurfaceDefaults.colors(containerColor = ImmichSurface)
                        ) {
                            Text(
                                "▶ VIDEO",
                                color = ImmichYellow,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "${currentIndex + 1} / ${assets.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSlideshow) {
                        Text(
                            if (isVideo) "⏵ Slideshow" else "⏵ ${slideshowInterval}s",
                            color = ImmichYellow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Right-side buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isVideo) {
                        // Interval cycle button (only relevant for photo slideshow)
                        Button(
                            onClick = {
                                intervalIndex = (intervalIndex + 1) % SLIDESHOW_INTERVALS.size
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF2A2A2A),
                                contentColor = Color(0xFFAAAAAA)
                            )
                        ) {
                            Text("${slideshowInterval}s")
                        }
                    }
                    if (isVideo) {
                        Button(onClick = { videoPlaying = !videoPlaying }) {
                            Text(if (videoPlaying) "⏸ Pause" else "▶ Play")
                        }
                    }
                    Button(onClick = {
                        isSlideshow = !isSlideshow
                        if (isVideo) videoPlaying = isSlideshow
                    }) {
                        Text(if (isSlideshow) "Stop" else "Slideshow")
                    }
                }
            }
        }

        // ── Slideshow progress bar (images only) ─────────────────────────
        if (isSlideshow && !isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(slideshowProgress)
                        .background(ImmichYellow)
                )
            }
        }

        // ── Bottom hint bar ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val hints = buildString {
                append("← → Navigate")
                if (isVideo) append("  •  OK/Play = Pause/Resume")
                append("  •  OK = Slideshow")
                append("  •  Back = Exit")
            }
            Text(
                text = hints,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}
