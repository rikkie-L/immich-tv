package com.immichtv.app.data.repository

import com.immichtv.app.data.api.ApiClient
import com.immichtv.app.data.model.*

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class ImmichRepository(
    private val serverUrl: String,
    private val apiKey: String,
    private val authMode: String = "api_key"
) {

    private val api get() = ApiClient.getApi(serverUrl, apiKey, authMode)

    suspend fun getAssets(page: Int = 1, pageSize: Int = 100): Result<List<Asset>> {
        return try {
            val response = api.getAssets(page = page, pageSize = pageSize)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load photos: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getTimeBuckets(): Result<List<TimelineBucket>> {
        return try {
            val response = api.getTimeBuckets()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load timeline: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getTimeBucket(timeBucket: String): Result<List<Asset>> {
        return try {
            val response = api.getTimeBucket(timeBucket = timeBucket)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load bucket: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getAlbums(): Result<List<Album>> {
        return try {
            val response = api.getAlbums()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Failed to load albums: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getAlbum(id: String): Result<AlbumInfo> {
        return try {
            val response = api.getAlbum(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body)
            } else {
                Result.Error("Failed to load album: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val authApi = ApiClient.getAuthApi(serverUrl)
            val response = authApi.login(LoginRequest(email, password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body)
            } else {
                val code = response.code()
                Result.Error(if (code == 401) "Invalid email or password" else "Login failed: $code")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Cannot connect to server")
        }
    }

    suspend fun checkConnection(): Result<Boolean> {
        return try {
            val response = api.getServerInfo()
            if (response.isSuccessful) Result.Success(true)
            else Result.Error("Connection failed: ${response.code()}")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Cannot connect to server")
        }
    }

    fun getThumbnailUrl(assetId: String, size: String = "thumbnail"): String {
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${base}api/assets/$assetId/thumbnail?size=$size"
    }

    fun getOriginalUrl(assetId: String): String {
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${base}api/assets/$assetId/original"
    }
}
