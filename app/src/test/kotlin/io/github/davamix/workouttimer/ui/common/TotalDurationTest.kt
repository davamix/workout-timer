package io.github.davamix.workouttimer.ui.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TotalDurationTest {
    @ParameterizedTest(name = "{0} s gives {1}")
    @CsvSource(
        "0, 0:00",
        "5, 0:05",
        "59, 0:59",
        "60, 1:00",
        "263, 4:23",
        "600, 10:00",
        "3599, 59:59",
        "3600, 1:00:00",
        "3663, 1:01:03",
        "36000, 10:00:00",
    )
    fun `formats minutes and seconds, and hours from an hour`(
        seconds: Int,
        expected: String,
    ) {
        assertEquals(expected, formatTotalDuration(seconds))
    }

    @ParameterizedTest(name = "{0} s gives 0:00")
    @CsvSource("-1", "-3600")
    fun `a negative total gives zero`(seconds: Int) {
        assertEquals("0:00", formatTotalDuration(seconds))
    }
}
