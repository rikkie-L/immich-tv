package com.immichtv.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.immichtv.app.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.tv.material3.*
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichYellow
import com.immichtv.app.util.SetupWebServer
import com.immichtv.app.util.qrCodeBitmap
import com.immichtv.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupScreen(
    onConnected: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmichBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Logo / title area
            Image(
                painter = painterResource(R.drawable.immich_logo),
                contentDescription = "Immich",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(80.dp)
            )
            Text(
                text = "Connect to your Immich server",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Auth mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.setAuthMode("api_key") },
                    modifier = Modifier.weight(1f),
                    colors = if (state.authMode == "api_key") {
                        ButtonDefaults.colors()
                    } else {
                        ButtonDefaults.colors(
                            containerColor = Color(0xFF2A2A2A),
                            contentColor = Color(0xFF888888)
                        )
                    }
                ) {
                    Text("API Key")
                }
                Button(
                    onClick = { viewModel.setAuthMode("bearer") },
                    modifier = Modifier.weight(1f),
                    colors = if (state.authMode == "bearer") {
                        ButtonDefaults.colors()
                    } else {
                        ButtonDefaults.colors(
                            containerColor = Color(0xFF2A2A2A),
                            contentColor = Color(0xFF888888)
                        )
                    }
                ) {
                    Text("Email & Password")
                }
            }

            // Server URL
            LabeledField(
                label = "Server URL",
                hint = "Address of your Immich server, starting with https://",
                value = state.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                placeholder = "https://immich.familieleijen.nl",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            if (state.authMode == "api_key") {
                LabeledField(
                    label = "API Key",
                    hint = "Create one in Immich: Account Settings \u2192 API Keys \u2192 New API Key",
                    value = state.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    placeholder = "Paste your Immich API key",
                    isSecret = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            } else {
                LabeledField(
                    label = "Email",
                    hint = "The email address you use to log in to Immich",
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                LabeledField(
                    label = "Password",
                    hint = "Your Immich password",
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    placeholder = "Password",
                    isSecret = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }

            // Status messages
            state.error?.let {
                Text(text = it, color = Color(0xFFCF6679), style = MaterialTheme.typography.bodyMedium)
            }
            state.connectionStatus?.let {
                Text(text = it, color = ImmichYellow, style = MaterialTheme.typography.bodyMedium)
            }

            // Connect button
            Button(
                onClick = { viewModel.testAndSave() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.isLoading) {
                    Text(if (state.authMode == "bearer") "Signing in..." else "Connecting...")
                } else {
                    Text(if (state.authMode == "bearer") "Sign In" else "Connect")
                }
            }

        }

        // Phone setup: scan, paste on the phone, send to the TV
        QrSetupPanel(
            currentServerUrl = { state.serverUrl },
            onReceived = { url, key ->
                viewModel.setAuthMode("api_key")
                viewModel.updateServerUrl(url)
                viewModel.updateApiKey(key)
                viewModel.testAndSave()
            }
        )
        }
    }
}

/**
 * Shows a QR code that opens a small form on the phone. While this panel is
 * visible the TV runs [SetupWebServer]; it stops as soon as the panel leaves
 * the screen.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun QrSetupPanel(
    currentServerUrl: () -> String,
    onReceived: (serverUrl: String, apiKey: String) -> Unit
) {
    val token = remember { SetupWebServer.newToken() }
    val ip = remember { SetupWebServer.localIpv4() }
    val latestServerUrl by rememberUpdatedState(currentServerUrl)
    val latestOnReceived by rememberUpdatedState(onReceived)

    DisposableEffect(token) {
        val server = SetupWebServer(
            port = SetupWebServer.PORT,
            token = token,
            prefillUrl = { latestServerUrl() },
            onSubmit = { url, key -> latestOnReceived(url, key) }
        )
        server.start()
        onDispose { server.stop() }
    }

    Column(
        modifier = Modifier.width(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Or use your phone",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        if (ip == null) {
            Text(
                text = "No network connection found on this TV.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center
            )
        } else {
            val link = "http://$ip:${SetupWebServer.PORT}/?t=$token"
            val qr = remember(link) { qrCodeBitmap(link) }
            Image(
                bitmap = qr,
                contentDescription = "QR code to set up this TV from your phone",
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(8.dp)
            )
            Text(
                text = "Scan with your phone camera, paste the server URL and API key there, and tap Send to TV.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center
            )
            Text(
                text = "$ip:${SetupWebServer.PORT}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

/** Input field with an always-visible label above it and a short explanation below it. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LabeledField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    isSecret: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9E9E9E)) },
            visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ImmichYellow,
                unfocusedBorderColor = Color(0xFFBBBBBB),
                focusedPlaceholderColor = Color(0xFF9E9E9E),
                unfocusedPlaceholderColor = Color(0xFF9E9E9E),
                cursorColor = ImmichYellow
            ),
            singleLine = true
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCCCCCC)
        )
    }
}
