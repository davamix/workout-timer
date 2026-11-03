package io.github.davamix.workouttimer.di

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.davamix.workouttimer.WorkoutTimerApp
import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.WorkoutSession
import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import io.github.davamix.workouttimer.core.model.newId
import io.github.davamix.workouttimer.core.timeline.Timeline
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** The engine on the tablet: `AppContainer.clock` is `elapsedRealtime`, and a session keeps real time on it (D-6, NF-1). */
@RunWith(AndroidJUnit4::class)
class EngineClockDeviceTest {
    private val container =
        (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as WorkoutTimerApp).container

    @Test
    fun clockFollowsElapsedRealtime() {
        val before = SystemClock.elapsedRealtime()
        val reading = container.clock.nowMillis()
        val after = SystemClock.elapsedRealtime()

        assertTrue("$reading should be between $before and $after", reading in before..after)
    }

    @Test
    fun shortSessionRunsInRealTime() =
        runBlocking {
            // Get ready 3 s, exercise A 1 s, a 1 s rest, exercise B 1 s: 6 s in all.
            val workout =
                Workout(
                    id = newId(),
                    name = "Clock check",
                    defaultExerciseDurationSeconds = 1,
                    defaultExerciseRestSeconds = 1,
                    defaultRoundRestSeconds = 0,
                    rounds =
                        listOf(
                            Round(
                                id = newId(),
                                name = "Round",
                                exercises = listOf(Exercise(id = newId(), name = "A"), Exercise(id = newId(), name = "B")),
                            ),
                        ),
                )
            val timeline = Timeline.of(workout)
            assertEquals(6, timeline.totalSeconds)
            val session = WorkoutSession(timeline, container.clock, this)

            val startedAt = SystemClock.elapsedRealtime()
            session.start()
            val events = withTimeout(15_000) { session.events.toList() }
            val elapsed = SystemClock.elapsedRealtime() - startedAt

            assertEquals(SessionEvent.Completed, events.last())
            assertEquals(4, events.count { it is SessionEvent.PhaseStarted })
            assertTrue("The session took $elapsed ms instead of 6000 ms", abs(elapsed - 6_000) <= 200)
        }
}
