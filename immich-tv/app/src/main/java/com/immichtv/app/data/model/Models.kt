package com.immichtv.app.data.model

import com.google.gson.annotations.SerializedName

data class Asset(
    val id: String,
    val deviceAssetId: String = "",
    val ownerId: String = "",
    val deviceId: String = "",
    val type: String = "IMAGE",
    val originalPath: String = "",
    val originalFileName: String = "",
    val thumbhash: String? = null,
    val fileCreatedAt: String = "",
    val fileModifiedAt: String = "",
    val localDateTime: String = "",
    val updatedAt: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isOffline: Boolean = false,
    val checksum: String = "",
    val exifInfo: ExifInfo? = null,
    val smartInfo: SmartInfo? = null,
    val duration: String? = null
)

data class ExifInfo(
    val make: String? = null,
    val model: String? = null,
    val exifImageWidth: Int? = null,
    val exifImageHeight: Int? = null,
    val dateTimeOriginal: String? = null,
    val description: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class SmartInfo(
    val tags: List<String>? = null,
    val objects: List<String>? = null
)

data class Album(
    val id: String,
    val albumName: String,
    val description: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val albumThumbnailAssetId: String? = null,
    val ownerId: String = "",
    val assetCount: Int = 0,
    val assets: List<Asset> = emptyList(),
    val shared: Boolean = false
)

data class AlbumInfo(
    val id: String,
    val albumName: String,
    val description: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val albumThumbnailAssetId: String? = null,
    val ownerId: String = "",
    val assetCount: Int = 0,
    val assets: List<Asset> = emptyList(),
    val shared: Boolean = false
)

data class TimelineBucket(
    val count: Int,
    val timeBucket: String
)

data class ServerInfo(
    val version: String = "",
    val versionUrl: String = ""
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val userEmail: String,
    val name: String,
    val profileImagePath: String,
    val isAdmin: Boolean
)
