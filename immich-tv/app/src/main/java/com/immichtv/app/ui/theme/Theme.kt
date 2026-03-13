package com.immichtv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val ImmichYellow = Color(0xFFE8B84B)
val ImmichBackground = Color(0xFF0D0D0D)
val ImmichSurface = Color(0xFF1A1A1A)
val ImmichSurfaceVariant = Color(0xFF242424)
val ImmichOnSurface = Color(0xFFEAEAEA)
val ImmichOnBackground = Color(0xFFEAEAEA)
val ImmichSecondary = Color(0xFF888888)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImmichTVTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = ImmichYellow,
        onPrimary = Color.Black,
        secondary = ImmichSecondary,
        background = ImmichBackground,
        surface = ImmichSurface,
        surfaceVariant = ImmichSurfaceVariant,
        onBackground = ImmichOnBackground,
        onSurface = ImmichOnSurface,
        onSurfaceVariant = Color(0xFFAAAAAA)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
