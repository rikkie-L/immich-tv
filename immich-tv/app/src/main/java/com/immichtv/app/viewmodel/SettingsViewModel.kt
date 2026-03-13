package com.immichtv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.app.data.repository.ImmichRepository
import com.immichtv.app.data.repository.PreferencesRepository
import com.immichtv.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val email: String = "",
    val password: String = "",
    val authMode: String = "api_key", // "api_key" or "bearer"
    val isLoading: Boolean = false,
    val connectionStatus: String? = null,
    val isConnected: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.serverUrl, prefs.apiKey, prefs.authMode) { url, key, mode ->
                Triple(url, key, mode)
            }.collect { (url, key, mode) ->
                _state.update { it.copy(serverUrl = url, apiKey = key, authMode = mode) }
            }
        }
    }

    fun updateServerUrl(url: String) = _state.update { it.copy(serverUrl = url, error = null) }
    fun updateApiKey(key: String) = _state.update { it.copy(apiKey = key, error = null) }
    fun updateEmail(email: String) = _state.update { it.copy(email = email, error = null) }
    fun updatePassword(password: String) = _state.update { it.copy(password = password, error = null) }
    fun setAuthMode(mode: String) = _state.update { it.copy(authMode = mode, error = null) }

    fun testAndSave() {
        if (_state.value.authMode == "bearer") {
            loginAndSave()
        } else {
            connectWithApiKey()
        }
    }

    private fun connectWithApiKey() {
        viewModelScope.launch {
            val url = _state.value.serverUrl.trim()
            val key = _state.value.apiKey.trim()
            if (url.isEmpty() || key.isEmpty()) {
                _state.update { it.copy(error = "Server URL and API key are required") }
                return@launch
            }
            _state.update { it.copy(isLoading = true, connectionStatus = "Connecting...", error = null) }
            val repo = ImmichRepository(url, key, "api_key")
            when (val result = repo.checkConnection()) {
                is Result.Success -> {
                    prefs.saveSettings(url, key, "api_key")
                    _state.update { it.copy(isLoading = false, isConnected = true, connectionStatus = "Connected!") }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, connectionStatus = null, error = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun loginAndSave() {
        viewModelScope.launch {
            val url = _state.value.serverUrl.trim()
            val email = _state.value.email.trim()
            val password = _state.value.password
            if (url.isEmpty() || email.isEmpty() || password.isEmpty()) {
                _state.update { it.copy(error = "Server URL, email, and password are required") }
                return@launch
            }
            _state.update { it.copy(isLoading = true, connectionStatus = "Signing in...", error = null) }
            val repo = ImmichRepository(url, "", "bearer")
            when (val result = repo.login(email, password)) {
                is Result.Success -> {
                    val token = result.data.accessToken
                    prefs.saveSettings(url, token, "bearer")
                    _state.update { it.copy(isLoading = false, isConnected = true, connectionStatus = "Signed in!") }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, connectionStatus = null, error = result.message) }
                }
                else -> {}
            }
        }
    }
}
