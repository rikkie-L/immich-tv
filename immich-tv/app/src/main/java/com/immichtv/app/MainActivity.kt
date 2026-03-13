package com.immichtv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.immichtv.app.data.repository.PreferencesRepository
import com.immichtv.app.ui.screens.MainScreen
import com.immichtv.app.ui.screens.SetupScreen
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichTVTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesRepository(this)

        setContent {
            ImmichTVTheme {
                // null = still loading, true = configured, false = needs setup
                var configured by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    val (url, key) = combine(prefs.serverUrl, prefs.apiKey) { url, key ->
                        Pair(url, key)
                    }.first()
                    configured = url.isNotEmpty() && key.isNotEmpty()
                }

                when (configured) {
                    null -> {
                        // Loading — show blank dark screen briefly
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ImmichBackground)
                        )
                    }
                    false -> SetupScreen(onConnected = { configured = true })
                    true -> MainScreen()
                }
            }
        }
    }
}
