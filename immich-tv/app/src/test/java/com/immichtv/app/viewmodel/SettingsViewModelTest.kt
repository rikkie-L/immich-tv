package com.immichtv.app.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsViewModelTest {

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

    // --- State mutation ---

    @Test
    fun `updateServerUrl updates state and clears error`() = runTest {
        val vm = SettingsViewModel(app)
        vm.updateServerUrl("http://192.168.1.1:2283")
        assertEquals("http://192.168.1.1:2283", vm.state.value.serverUrl)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `updateApiKey updates state and clears error`() = runTest {
        val vm = SettingsViewModel(app)
        vm.updateApiKey("my-secret-key")
        assertEquals("my-secret-key", vm.state.value.apiKey)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `updateEmail updates state and clears error`() = runTest {
        val vm = SettingsViewModel(app)
        vm.updateEmail("user@example.com")
        assertEquals("user@example.com", vm.state.value.email)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `updatePassword updates state and clears error`() = runTest {
        val vm = SettingsViewModel(app)
        vm.updatePassword("supersecret")
        assertEquals("supersecret", vm.state.value.password)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `setAuthMode switches to bearer`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        assertEquals("bearer", vm.state.value.authMode)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `setAuthMode switches back to api_key`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.setAuthMode("api_key")
        assertEquals("api_key", vm.state.value.authMode)
    }

    // --- Default state ---

    @Test
    fun `SettingsUiState default values are correct`() {
        val state = SettingsUiState()
        assertEquals("", state.serverUrl)
        assertEquals("", state.apiKey)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("api_key", state.authMode)
        assertFalse(state.isLoading)
        assertNull(state.connectionStatus)
        assertFalse(state.isConnected)
        assertNull(state.error)
    }

    // --- testAndSave with api_key mode ---

    @Test
    fun `testAndSave with api_key mode sets error when URL is empty`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("api_key")
        vm.updateServerUrl("")
        vm.updateApiKey("some-key")
        vm.testAndSave()
        assertEquals("Server URL and API key are required", vm.state.value.error)
        assertFalse(vm.state.value.isConnected)
    }

    @Test
    fun `testAndSave with api_key mode sets error when API key is empty`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("api_key")
        vm.updateServerUrl("http://192.168.1.1:2283")
        vm.updateApiKey("")
        vm.testAndSave()
        assertEquals("Server URL and API key are required", vm.state.value.error)
    }

    @Test
    fun `testAndSave with api_key mode succeeds on 200`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"version":"1.100.0","versionUrl":"https://github.com"}""")
                .addHeader("Content-Type", "application/json")
        )
        val vm = SettingsViewModel(app)
        vm.setAuthMode("api_key")
        vm.updateServerUrl(server.url("/").toString())
        vm.updateApiKey("valid-api-key")
        vm.testAndSave()
        assertTrue(vm.state.value.isConnected)
        assertEquals("Connected!", vm.state.value.connectionStatus)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `testAndSave with api_key mode sets error on connection failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val vm = SettingsViewModel(app)
        vm.setAuthMode("api_key")
        vm.updateServerUrl(server.url("/").toString())
        vm.updateApiKey("bad-key")
        vm.testAndSave()
        assertFalse(vm.state.value.isConnected)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.connectionStatus)
    }

    // --- testAndSave with bearer mode ---

    @Test
    fun `testAndSave with bearer mode sets error when email is empty`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.updateServerUrl("http://192.168.1.1:2283")
        vm.updateEmail("")
        vm.updatePassword("password")
        vm.testAndSave()
        assertEquals("Server URL, email, and password are required", vm.state.value.error)
    }

    @Test
    fun `testAndSave with bearer mode sets error when password is empty`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.updateServerUrl("http://192.168.1.1:2283")
        vm.updateEmail("user@example.com")
        vm.updatePassword("")
        vm.testAndSave()
        assertEquals("Server URL, email, and password are required", vm.state.value.error)
    }

    @Test
    fun `testAndSave with bearer mode sets error when server URL is empty`() = runTest {
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.updateServerUrl("")
        vm.updateEmail("user@example.com")
        vm.updatePassword("password")
        vm.testAndSave()
        assertEquals("Server URL, email, and password are required", vm.state.value.error)
    }

    @Test
    fun `testAndSave with bearer mode succeeds on valid login`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"accessToken":"tok123","userId":"u1","userEmail":"user@example.com","name":"User","profileImagePath":"","isAdmin":false}""")
                .addHeader("Content-Type", "application/json")
        )
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.updateServerUrl(server.url("/").toString())
        vm.updateEmail("user@example.com")
        vm.updatePassword("correctpassword")
        vm.testAndSave()
        assertTrue(vm.state.value.isConnected)
        assertEquals("Signed in!", vm.state.value.connectionStatus)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `testAndSave with bearer mode sets friendly error on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val vm = SettingsViewModel(app)
        vm.setAuthMode("bearer")
        vm.updateServerUrl(server.url("/").toString())
        vm.updateEmail("user@example.com")
        vm.updatePassword("wrongpassword")
        vm.testAndSave()
        assertFalse(vm.state.value.isConnected)
        assertEquals("Invalid email or password", vm.state.value.error)
    }

    @Test
    fun `updateServerUrl trims leading and trailing whitespace on connect`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"version":"1.0","versionUrl":""}""")
                .addHeader("Content-Type", "application/json")
        )
        val vm = SettingsViewModel(app)
        vm.setAuthMode("api_key")
        vm.updateServerUrl("  " + server.url("/").toString() + "  ")
        vm.updateApiKey("key")
        vm.testAndSave()
        // URL with whitespace is trimmed inside connectWithApiKey; connection should succeed
        assertTrue(vm.state.value.isConnected)
    }
}
