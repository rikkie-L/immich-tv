package com.immichtv.app.data.repository

import com.immichtv.app.data.api.ApiClient
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
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
                Result.Success(parseBucket(response.body()))
            } else {
                Result.Error("Failed to load bucket: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Immich changed the /timeline/bucket response:
     *  - older servers return a JSON array of asset objects;
     *  - newer servers (v1.133+) return one object with parallel arrays
     *    ("columnar"): { "id": [...], "isImage": [...], "duration": [...], ... }.
     * Both are converted to a plain list of [Asset].
     */
    private fun parseBucket(body: JsonElement?): List<Asset> {
        if (body == null || body.isJsonNull) return emptyList()
        if (body.isJsonArray) {
            val type = object : TypeToken<List<Asset>>() {}.type
            return Gson().fromJson(body, type)
        }
        if (!body.isJsonObject) return emptyList()
        val obj: JsonObject = body.asJsonObject
        val ids = obj.arr("id") ?: return emptyList()
        val isImage = obj.arr("isImage")
        val isFavorite = obj.arr("isFavorite")
        val duration = obj.arr("duration")
        val createdAt = obj.arr("fileCreatedAt")
        val thumbhash = obj.arr("thumbhash")
        val ownerId = obj.arr("ownerId")
        return (0 until ids.size()).map { i ->
            Asset(
                id = ids[i].asString,
                type = if (isImage.bool(i) == false) "VIDEO" else "IMAGE",
                isFavorite = isFavorite.bool(i) ?: false,
                duration = duration.str(i),
                fileCreatedAt = createdAt.str(i) ?: "",
                localDateTime = createdAt.str(i) ?: "",
                thumbhash = thumbhash.str(i),
                ownerId = ownerId.str(i) ?: ""
            )
        }
    }

    private fun JsonObject.arr(name: String): JsonArray? =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonArray?.str(i: Int): String? =
        this?.takeIf { i < it.size() }?.get(i)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonArray?.bool(i: Int): Boolean? =
        this?.takeIf { i < it.size() }?.get(i)?.takeIf { !it.isJsonNull }?.asBoolean

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
