package com.immichtv.app.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlbumsViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        server.shutdown()
    }

    @Test
    fun `initial state has empty albums list`() = runTest {
        val vm = AlbumsViewModel(app)
        val state = vm.state.value
        assertTrue(state.albums.isEmpty())
        assertNull(state.selectedAlbum)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `clearSelectedAlbum sets selectedAlbum to null`() = runTest {
        val vm = AlbumsViewModel(app)
        // selectedAlbum starts null; clearing it again is safe
        vm.clearSelectedAlbum()
        assertNull(vm.state.value.selectedAlbum)
    }

    @Test
    fun `getThumbnailUrl returns empty string before repository is set`() = runTest {
        val vm = AlbumsViewModel(app)
        assertEquals("", vm.getThumbnailUrl("asset-id"))
    }

    @Test
    fun `getOriginalUrl returns empty string before repository is set`() = runTest {
        val vm = AlbumsViewModel(app)
        assertEquals("", vm.getOriginalUrl("asset-id"))
    }

    @Test
    fun `AlbumsUiState default values are correct`() {
        val state = AlbumsUiState()
        assertTrue(state.albums.isEmpty())
        assertNull(state.selectedAlbum)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("", state.apiKey)
    }

    @Test
    fun `state flow always has a value`() = runTest {
        val vm = AlbumsViewModel(app)
        assertNotNull(vm.state.value)
    }

    @Test
    fun `multiple clearSelectedAlbum calls are idempotent`() = runTest {
        val vm = AlbumsViewModel(app)
        vm.clearSelectedAlbum()
        vm.clearSelectedAlbum()
        assertNull(vm.state.value.selectedAlbum)
    }
}
