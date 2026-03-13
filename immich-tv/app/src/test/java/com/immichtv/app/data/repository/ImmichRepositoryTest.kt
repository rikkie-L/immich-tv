package com.immichtv.app.data.repository

import com.immichtv.app.data.api.ApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImmichRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ImmichRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString()
        repository = ImmichRepository(baseUrl, "test-api-key", "api_key")
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    // --- getAssets ---

    @Test
    fun `getAssets returns Success with asset list on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"abc123","type":"IMAGE","fileCreatedAt":"2024-01-01T00:00:00Z","fileModifiedAt":"2024-01-01T00:00:00Z","localDateTime":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z","originalPath":"/photos/img.jpg","originalFileName":"img.jpg","checksum":"abc","isFavorite":false,"isArchived":false,"isOffline":false}]""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getAssets()

        assertTrue(result is Result.Success)
        val assets = (result as Result.Success).data
        assertEquals(1, assets.size)
        assertEquals("abc123", assets[0].id)
    }

    @Test
    fun `getAssets returns Error on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = repository.getAssets()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("401"))
    }

    @Test
    fun `getAssets returns empty list when body is null`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getAssets()

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.size)
    }

    @Test
    fun `getAssets returns Error on network failure`() = runTest {
        server.shutdown()

        val result = repository.getAssets()

        assertTrue(result is Result.Error)
    }

    // --- getTimeBuckets ---

    @Test
    fun `getTimeBuckets returns Success with bucket list on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"count":15,"timeBucket":"2024-01-01T00:00:00.000Z"},{"count":8,"timeBucket":"2023-12-01T00:00:00.000Z"}]""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getTimeBuckets()

        assertTrue(result is Result.Success)
        val buckets = (result as Result.Success).data
        assertEquals(2, buckets.size)
        assertEquals(15, buckets[0].count)
        assertEquals("2024-01-01T00:00:00.000Z", buckets[0].timeBucket)
    }

    @Test
    fun `getTimeBuckets returns Error on 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.getTimeBuckets()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("500"))
    }

    // --- getTimeBucket ---

    @Test
    fun `getTimeBucket returns assets for given bucket`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"photo1","type":"IMAGE","fileCreatedAt":"2024-01-15T10:00:00Z","fileModifiedAt":"2024-01-15T10:00:00Z","localDateTime":"2024-01-15T10:00:00Z","updatedAt":"2024-01-15T10:00:00Z","originalPath":"/photos/photo1.jpg","originalFileName":"photo1.jpg","checksum":"xyz","isFavorite":false,"isArchived":false,"isOffline":false}]""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getTimeBucket("2024-01-01T00:00:00.000Z")

        assertTrue(result is Result.Success)
        assertEquals("photo1", (result as Result.Success).data[0].id)
    }

    // --- getAlbums ---

    @Test
    fun `getAlbums returns Success with album list on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":"album1","albumName":"Vacation","assetCount":42,"ownerId":"user1","shared":false}]""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getAlbums()

        assertTrue(result is Result.Success)
        val albums = (result as Result.Success).data
        assertEquals(1, albums.size)
        assertEquals("album1", albums[0].id)
        assertEquals("Vacation", albums[0].albumName)
        assertEquals(42, albums[0].assetCount)
    }

    @Test
    fun `getAlbums returns Error on 403`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val result = repository.getAlbums()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("403"))
    }

    // --- getAlbum ---

    @Test
    fun `getAlbum returns Success with album detail on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":"album1","albumName":"Vacation","assetCount":2,"ownerId":"user1","shared":false,"assets":[{"id":"a1","type":"IMAGE","fileCreatedAt":"2024-01-01T00:00:00Z","fileModifiedAt":"2024-01-01T00:00:00Z","localDateTime":"2024-01-01T00:00:00Z","updatedAt":"2024-01-01T00:00:00Z","originalPath":"/photos/a1.jpg","originalFileName":"a1.jpg","checksum":"c1","isFavorite":false,"isArchived":false,"isOffline":false}]}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getAlbum("album1")

        assertTrue(result is Result.Success)
        val album = (result as Result.Success).data
        assertEquals("album1", album.id)
        assertEquals(1, album.assets.size)
    }

    @Test
    fun `getAlbum returns Error on 404`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = repository.getAlbum("nonexistent")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("404"))
    }

    // --- login ---

    @Test
    fun `login returns Success with token on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"accessToken":"token123","userId":"user1","userEmail":"user@example.com","name":"Test User","profileImagePath":"","isAdmin":false}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.login("user@example.com", "password123")

        assertTrue(result is Result.Success)
        assertEquals("token123", (result as Result.Success).data.accessToken)
    }

    @Test
    fun `login returns Error with helpful message on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = repository.login("wrong@example.com", "badpassword")

        assertTrue(result is Result.Error)
        assertEquals("Invalid email or password", (result as Result.Error).message)
    }

    @Test
    fun `login returns Error on other failure codes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.login("user@example.com", "password123")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("500"))
    }

    // --- checkConnection ---

    @Test
    fun `checkConnection returns Success true on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"version":"1.100.0","versionUrl":"https://github.com/immich-app/immich"}""")
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.checkConnection()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }

    @Test
    fun `checkConnection returns Error on non-200`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = repository.checkConnection()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("503"))
    }

    // --- URL builders ---

    @Test
    fun `getThumbnailUrl builds correct URL with trailing slash`() {
        val repo = ImmichRepository("http://example.com/", "key", "api_key")
        val url = repo.getThumbnailUrl("asset123")
        assertEquals("http://example.com/api/assets/asset123/thumbnail?size=thumbnail", url)
    }

    @Test
    fun `getThumbnailUrl builds correct URL without trailing slash`() {
        val repo = ImmichRepository("http://example.com", "key", "api_key")
        val url = repo.getThumbnailUrl("asset123")
        assertEquals("http://example.com/api/assets/asset123/thumbnail?size=thumbnail", url)
    }

    @Test
    fun `getOriginalUrl builds correct URL`() {
        val repo = ImmichRepository("http://example.com", "key", "api_key")
        val url = repo.getOriginalUrl("asset456")
        assertEquals("http://example.com/api/assets/asset456/original", url)
    }

    @Test
    fun `getThumbnailUrl accepts custom size parameter`() {
        val repo = ImmichRepository("http://example.com", "key", "api_key")
        val url = repo.getThumbnailUrl("asset123", "preview")
        assertEquals("http://example.com/api/assets/asset123/thumbnail?size=preview", url)
    }

    // --- Request verification ---

    @Test
    fun `getAssets sends x-api-key header in api_key mode`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json")
        )

        repository.getAssets()

        val request = server.takeRequest()
        assertEquals("test-api-key", request.getHeader("x-api-key"))
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `getAssets sends Bearer token in bearer mode`() = runTest {
        val baseUrl = server.url("/").toString()
        val bearerRepo = ImmichRepository(baseUrl, "bearer-token-xyz", "bearer")

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
                .addHeader("Content-Type", "application/json")
        )

        bearerRepo.getAssets()

        val request = server.takeRequest()
        assertEquals("Bearer bearer-token-xyz", request.getHeader("Authorization"))
        assertNull(request.getHeader("x-api-key"))
    }
}
