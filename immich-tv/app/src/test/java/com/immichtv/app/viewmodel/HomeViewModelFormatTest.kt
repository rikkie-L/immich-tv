package com.immichtv.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the bucket label formatting logic extracted from HomeViewModel.
 * Since HomeViewModel uses AndroidViewModel (requires Application context),
 * we test the pure formatting logic in isolation here.
 */
class HomeViewModelFormatTest {

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

    @Test
    fun `formats January correctly`() {
        assertEquals("January 2024", formatBucketLabel("2024-01-01T00:00:00.000Z"))
    }

    @Test
    fun `formats December correctly`() {
        assertEquals("December 2023", formatBucketLabel("2023-12-01T00:00:00.000Z"))
    }

    @Test
    fun `formats all months correctly`() {
        val expected = mapOf(
            "01" to "January",
            "02" to "February",
            "03" to "March",
            "04" to "April",
            "05" to "May",
            "06" to "June",
            "07" to "July",
            "08" to "August",
            "09" to "September",
            "10" to "October",
            "11" to "November",
            "12" to "December"
        )
        for ((num, name) in expected) {
            assertEquals("$name 2024", formatBucketLabel("2024-${num}-01T00:00:00.000Z"))
        }
    }

    @Test
    fun `returns original string on malformed input`() {
        val malformed = "not-a-date"
        assertEquals(malformed, formatBucketLabel(malformed))
    }

    @Test
    fun `handles bucket without time component`() {
        assertEquals("March 2022", formatBucketLabel("2022-03-01"))
    }
}
