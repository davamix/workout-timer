package io.github.davamix.workouttimer.ui.common

/**
 * A workout's total duration (WM-6) as text: `m:ss` under an hour, `h:mm:ss` from an hour. 263 gives "4:23" and 3663
 * gives "1:01:03". A value below 0 gives "0:00".
 *
 * The list and the editor show it (P6), and so may the completed screen (P7). It isn't the runner's `mm:ss` (UI-3).
 */
fun formatTotalDuration(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    val hours = total / SECONDS_PER_HOUR
    val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val secs = (total % SECONDS_PER_MINUTE).twoDigits()
    return if (hours > 0) "$hours:${minutes.twoDigits()}:$secs" else "$minutes:$secs"
}

private fun Int.twoDigits() = toString().padStart(2, '0')

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
