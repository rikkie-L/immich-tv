package com.immichtv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.immichtv.app.data.model.Asset
import com.immichtv.app.data.model.TimelineBucket
import com.immichtv.app.data.repository.ImmichRepository
import com.immichtv.app.data.repository.PreferencesRepository
import com.immichtv.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimelineMonth(
    val label: String,
    val timeBucket: String,
    val assets: List<Asset> = emptyList(),
    val isLoading: Boolean = false
)

data class PhotosUiState(
    val months: List<TimelineMonth> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String = "",
    val apiKey: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val _state = MutableStateFlow(PhotosUiState())
    val state: StateFlow<PhotosUiState> = _state.asStateFlow()

    private var repository: ImmichRepository? = null

    init {
        viewModelScope.launch {
            combine(prefs.serverUrl, prefs.apiKey, prefs.authMode) { url, key, mode ->
                Triple(url, key, mode)
            }.collect { (url, key, mode) ->
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    repository = ImmichRepository(url, key, mode)
                    _state.update { it.copy(serverUrl = url, apiKey = key) }
                    loadTimeline()
                }
            }
        }
    }

    fun loadTimeline() {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repo.getTimeBuckets()) {
                is Result.Success -> {
                    val months = result.data.map { bucket ->
                        TimelineMonth(
                            label = formatBucketLabel(bucket.timeBucket),
                            timeBucket = bucket.timeBucket
                        )
                    }
                    _state.update { it.copy(months = months, isLoading = false) }
                    // Load first few months eagerly
                    months.take(3).forEach { loadMonthAssets(it.timeBucket) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun loadMonthAssets(timeBucket: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.update { state ->
                state.copy(months = state.months.map {
                    if (it.timeBucket == timeBucket) it.copy(isLoading = true) else it
                })
            }
            when (val result = repo.getTimeBucket(timeBucket)) {
                is Result.Success -> {
                    _state.update { state ->
                        state.copy(months = state.months.map {
                            if (it.timeBucket == timeBucket)
                                it.copy(assets = result.data, isLoading = false)
                            else it
                        })
                    }
                }
                is Result.Error -> {
                    _state.update { state ->
                        state.copy(months = state.months.map {
                            if (it.timeBucket == timeBucket) it.copy(isLoading = false) else it
                        })
                    }
                }
                else -> {}
            }
        }
    }

    fun getThumbnailUrl(assetId: String): String =
        repository?.getThumbnailUrl(assetId) ?: ""

    fun getOriginalUrl(assetId: String): String =
        repository?.getOriginalUrl(assetId) ?: ""

    private fun formatBucketLabel(timeBucket: String): String {
        return try {
            val parts = timeBucket.split("T")[0].split("-")
            val year = parts[0]
            val month = when (parts[1].toInt()) {
                1 -> "January"; 2 -> "February"; 3 -> "March"
                4 -> "April"; 5 -> "May"; 6 -> "June"
                7 -> "July"; 8 -> "August"; 9 -> "September"
                10 -> "October"; 11 -> "November"; 12 -> "December"
                else -> parts[1]
            }
            "$month $year"
        } catch (e: Exception) {
            timeBucket
        }
    }
}
