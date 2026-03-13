package com.immichtv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.tv.material3.*
import com.immichtv.app.ui.theme.ImmichBackground
import com.immichtv.app.ui.theme.ImmichYellow
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
        Column(
            modifier = Modifier.width(500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo / title area
            Text(
                text = "immich",
                style = MaterialTheme.typography.displayMedium,
                color = ImmichYellow
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
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.49:2283") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ImmichYellow,
                    focusedLabelColor = ImmichYellow
                ),
                singleLine = true
            )

            if (state.authMode == "api_key") {
                // API Key field
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("Your Immich API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmichYellow,
                        focusedLabelColor = ImmichYellow
                    ),
                    singleLine = true
                )
            } else {
                // Email field
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text("Email") },
                    placeholder = { Text("you@example.com") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmichYellow,
                        focusedLabelColor = ImmichYellow
                    ),
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmichYellow,
                        focusedLabelColor = ImmichYellow
                    ),
                    singleLine = true
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

            Spacer(modifier = Modifier.height(8.dp))
            if (state.authMode == "api_key") {
                Text(
                    text = "Get your API key from: Immich → Account Settings → API Keys",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF555555),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
