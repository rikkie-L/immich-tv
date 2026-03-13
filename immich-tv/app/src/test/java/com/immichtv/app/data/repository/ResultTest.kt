package com.immichtv.app.data.repository

import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun `Success holds correct data`() {
        val result = Result.Success(listOf("a", "b"))
        assertEquals(listOf("a", "b"), result.data)
    }

    @Test
    fun `Error holds correct message`() {
        val result = Result.Error("Something went wrong")
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `Loading is a singleton object`() {
        assertSame(Result.Loading, Result.Loading)
    }

    @Test
    fun `Success is distinguishable from Error`() {
        val success: Result<Int> = Result.Success(42)
        val error: Result<Int> = Result.Error("oops")

        assertTrue(success is Result.Success)
        assertFalse(success is Result.Error)
        assertTrue(error is Result.Error)
        assertFalse(error is Result.Success)
    }

    @Test
    fun `Success with null-safe data`() {
        val result: Result<List<String>> = Result.Success(emptyList())
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `can pattern match Result types`() {
        val results: List<Result<String>> = listOf(
            Result.Success("ok"),
            Result.Error("fail"),
            Result.Loading
        )

        val messages = results.map { result ->
            when (result) {
                is Result.Success -> "success:${result.data}"
                is Result.Error -> "error:${result.message}"
                is Result.Loading -> "loading"
            }
        }

        assertEquals(listOf("success:ok", "error:fail", "loading"), messages)
    }
}
