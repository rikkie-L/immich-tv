package com.immichtv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.app.data.model.Memory
import com.immichtv.app.data.repository.ImmichRepository
import com.immichtv.app.data.repository.PreferencesRepository
import com.immichtv.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MemoriesUiState(
    val memories: List<Memory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKey: String = ""
)

class MemoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val _state = MutableStateFlow(MemoriesUiState())
    val state: StateFlow<MemoriesUiState> = _state.asStateFlow()
    private var repository: ImmichRepository? = null

    init {
        viewModelScope.launch {
            combine(prefs.serverUrl, prefs.apiKey, prefs.authMode) { url, key, mode ->
                Triple(url, key, mode)
            }.collect { (url, key, mode) ->
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    repository = ImmichRepository(url, key, mode)
                    _state.update { it.copy(apiKey = key) }
                    loadMemories()
                }
            }
        }
    }

    /**
     * Loads all memories Immich has made, newest first. Memories that are
     * planned for a later day (Immich creates them ahead) are left out.
     */
    fun loadMemories() {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getMemories()) {
                is Result.Success -> {
                    val today = LocalDate.now().toString()
                    val memories = result.data
                        .filter { it.assets.isNotEmpty() && it.day() <= today }
                        .sortedByDescending { it.memoryAt }
                    _state.update { it.copy(memories = memories, isLoading = false) }
                }
                is Result.Error -> _state.update { it.copy(error = result.message, isLoading = false) }
                else -> {}
            }
        }
    }

    fun getThumbnailUrl(assetId: String): String =
        repository?.getThumbnailUrl(assetId) ?: ""
}

/** The day this memory is for (yyyy-MM-dd). */
fun Memory.day(): String = memoryAt.take(10)

/** Title as Immich shows it: "3 years ago" (counted from the memory's own day), or "Anna's birthday". */
fun Memory.title(): String = when (type) {
    "birthday" -> data?.personName?.let { "$it's birthday" } ?: "Birthday"
    else -> data?.year?.let { year ->
        val memoryYear = day().take(4).toIntOrNull() ?: LocalDate.now().year
        val n = memoryYear - year
        if (n == 1) "1 year ago" else "$n years ago"
    } ?: "On this day"
}
