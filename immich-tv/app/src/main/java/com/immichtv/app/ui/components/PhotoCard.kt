package com.immichtv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.immichtv.app.ui.theme.ImmichYellow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhotoCard(
    thumbnailUrl: String,
    apiKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    aspectRatio: Float = 1f
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) ImmichYellow else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .background(Color(0xFF1E1E1E))
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                // Ensure DPAD_CENTER / Enter triggers click on TV remote
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(thumbnailUrl)
                .addHeader("x-api-key", apiKey)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Focus highlight
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        // Video badge (bottom-left corner)
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "▶",
                    color = ImmichYellow,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    name: String,
    thumbnailUrl: String?,
    apiKey: String,
    assetCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(200.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.DirectionCenter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isFocused) 3.dp else 0.dp,
                    color = if (isFocused) ImmichYellow else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(Color(0xFF1E1E1E))
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(thumbnailUrl)
                        .addHeader("x-api-key", apiKey)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text("📁", style = androidx.compose.ui.text.TextStyle(fontSize = 40.sp))
                }
            }

            if (isFocused) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.08f)))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        androidx.tv.material3.Text(
            text = name,
            style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
            color = if (isFocused) ImmichYellow else Color(0xFFEAEAEA),
            maxLines = 1
        )
        androidx.tv.material3.Text(
            text = "$assetCount items",
            style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )
    }
}
