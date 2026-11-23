package com.immichtv.app.data.api

import com.google.gson.JsonElement
import com.immichtv.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ImmichApi {

    @GET("api/assets")
    suspend fun getAssets(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("order") order: String = "desc",
        @Query("withExif") withExif: Boolean = false
    ): Response<List<Asset>>

    @GET("api/assets/{id}")
    suspend fun getAsset(@Path("id") id: String): Response<Asset>

    @GET("api/albums")
    suspend fun getAlbums(
        @Query("shared") shared: Boolean? = null
    ): Response<List<Album>>

    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): Response<AlbumInfo>

    @GET("api/timeline/buckets")
    suspend fun getTimeBuckets(
        @Query("size") size: String = "MONTH",
        @Query("withPartners") withPartners: Boolean = true,
        @Query("isArchived") isArchived: Boolean = false
    ): Response<List<TimelineBucket>>

    @GET("api/timeline/bucket")
    suspend fun getTimeBucket(
        @Query("size") size: String = "MONTH",
        @Query("timeBucket") timeBucket: String,
        @Query("withPartners") withPartners: Boolean = true,
        @Query("isArchived") isArchived: Boolean = false
    ): Response<JsonElement>  // old Immich: array of assets; new Immich: columnar object

    /** Memories for one day (yyyy-MM-dd). Needs the memory.read permission. */
    @GET("api/memories")
    suspend fun getMemories(
        @Query("for") forDate: String? = null
    ): Response<List<Memory>>

    @GET("api/server/about")
    suspend fun getServerInfo(): Response<ServerInfo>
}
