package com.immichtv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.app.data.model.Album
import com.immichtv.app.data.model.AlbumInfo
import com.immichtv.app.data.repository.ImmichRepository
import com.immichtv.app.data.repository.PreferencesRepository
import com.immichtv.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbum: AlbumInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKey: String = ""
)

class AlbumsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val _state = MutableStateFlow(AlbumsUiState())
    val state: StateFlow<AlbumsUiState> = _state.asStateFlow()
    private var repository: ImmichRepository? = null

    init {
        viewModelScope.launch {
            combine(prefs.serverUrl, prefs.apiKey, prefs.authMode) { url, key, mode ->
                Triple(url, key, mode)
            }.collect { (url, key, mode) ->
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    repository = ImmichRepository(url, key, mode)
                    _state.update { it.copy(apiKey = key) }
                    loadAlbums()
                }
            }
        }
    }

    fun loadAlbums() {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getAlbums()) {
                is Result.Success -> _state.update { it.copy(albums = result.data, isLoading = false) }
                is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                else -> {}
            }
        }
    }

    fun loadAlbum(id: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repo.getAlbum(id)) {
                is Result.Success -> _state.update { it.copy(selectedAlbum = result.data, isLoading = false) }
                is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                else -> {}
            }
        }
    }

    fun clearSelectedAlbum() = _state.update { it.copy(selectedAlbum = null) }

    fun getThumbnailUrl(assetId: String): String =
        repository?.getThumbnailUrl(assetId) ?: ""

    fun getOriginalUrl(assetId: String): String =
        repository?.getOriginalUrl(assetId) ?: ""
}
