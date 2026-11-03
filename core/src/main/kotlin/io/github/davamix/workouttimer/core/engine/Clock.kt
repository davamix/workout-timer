package io.github.davamix.workouttimer.core.engine

/**
 * A monotonic clock, in milliseconds (plan D-6). Only differences between two readings matter: the origin is
 * arbitrary, and the clock never jumps when the device's date or time changes.
 *
 * On the tablet it is `SystemClock.elapsedRealtime()`, provided by `AppContainer.clock`. In tests,
 * `Clock { testScheduler.currentTime }` follows the coroutine test scheduler's virtual time, so `delay` and the clock
 * agree.
 */
fun interface Clock {
    /** The current time, in milliseconds. It never decreases. */
    fun nowMillis(): Long
}
