package io.github.davamix.workouttimer.ui.runner

import io.github.davamix.workouttimer.core.engine.SessionState
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.data.exampleWorkout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RunnerViewTest {
    private val timeline = Timeline.of(exampleWorkout)

    private val warmUp = RoundLabel(1, 2, "Warm-up")
    private val core = RoundLabel(2, 2, "Core")

    /** The view of the example's phase [index], running at its full duration unless told otherwise. */
    private fun viewOf(
        index: Int,
        status: SessionStatus = SessionStatus.RUNNING,
        remainingSeconds: Int = timeline.phases[index].durationSeconds,
        canGoPrevious: Boolean = false,
    ) = SessionState(
        status = status,
        phaseIndex = index,
        phase = timeline.phases[index],
        remainingSeconds = remainingSeconds,
        awaitingAnnouncement = false,
        canGoPrevious = canGoPrevious,
    ).toRunnerView()

    @Test
    fun `get ready shows the first round and the first exercise next`() {
        assertEquals(
            RunnerView(
                kind = PhaseKind.GET_READY,
                paused = false,
                remainingSeconds = 3,
                round = warmUp,
                exercise = null,
                next = NextExercise("Jumping jacks", 30),
                nextRound = null,
                canGoPrevious = false,
            ),
            viewOf(0),
        )
    }

    @Test
    fun `an exercise shows its name and its place in the round`() {
        assertEquals(
            RunnerView(
                kind = PhaseKind.EXERCISE,
                paused = false,
                remainingSeconds = 30,
                round = warmUp,
                exercise = ExerciseLabel("Jumping jacks", 1, 3),
                next = null,
                nextRound = null,
                canGoPrevious = false,
            ),
            viewOf(1),
        )
    }

    @Test
    fun `an exercise rest shows its round and the next exercise, but no next round`() {
        assertEquals(
            RunnerView(
                kind = PhaseKind.REST,
                paused = false,
                remainingSeconds = 15,
                round = warmUp,
                exercise = null,
                next = NextExercise("Arm circles", 10),
                nextRound = null,
                canGoPrevious = false,
            ),
            viewOf(2),
        )
    }

    @Test
    fun `a round rest shows the round that ended, the next round and its first exercise`() {
        assertEquals(
            RunnerView(
                kind = PhaseKind.REST,
                paused = false,
                remainingSeconds = 30,
                round = warmUp,
                exercise = null,
                next = NextExercise("Plank", 60),
                nextRound = core,
                canGoPrevious = false,
            ),
            viewOf(6),
        )
    }

    @Test
    fun `every phase of the example maps to its look, round, exercise and next exercise`() {
        val views = timeline.phases.indices.map { viewOf(it) }

        assertEquals(
            listOf(
                PhaseKind.GET_READY,
                PhaseKind.EXERCISE,
                PhaseKind.REST,
                PhaseKind.EXERCISE,
                PhaseKind.REST,
                PhaseKind.EXERCISE,
                PhaseKind.REST,
                PhaseKind.EXERCISE,
                PhaseKind.REST,
                PhaseKind.EXERCISE,
            ),
            views.map { it.kind },
        )
        assertEquals(
            listOf(warmUp, warmUp, warmUp, warmUp, warmUp, warmUp, warmUp, core, core, core),
            views.map { it.round },
        )
        assertEquals(
            listOf(
                null,
                ExerciseLabel("Jumping jacks", 1, 3),
                null,
                ExerciseLabel("Arm circles", 2, 3),
                null,
                ExerciseLabel("High knees", 3, 3),
                null,
                ExerciseLabel("Plank", 1, 2),
                null,
                ExerciseLabel("Crunches", 2, 2),
            ),
            views.map { it.exercise },
        )
        assertEquals(
            listOf(
                NextExercise("Jumping jacks", 30),
                null,
                NextExercise("Arm circles", 10),
                null,
                NextExercise("High knees", 40),
                null,
                NextExercise("Plank", 60),
                null,
                NextExercise("Crunches", 40),
                null,
            ),
            views.map { it.next },
        )
        assertEquals(listOf(6), views.indices.filter { views[it].nextRound != null })
    }

    @Test
    fun `the remaining time comes from the state`() {
        assertEquals(1, viewOf(1, remainingSeconds = 1).remainingSeconds)
    }

    @Test
    fun `paused comes from the status`() {
        assertTrue(viewOf(1, status = SessionStatus.PAUSED).paused)
        assertFalse(viewOf(1, status = SessionStatus.RUNNING).paused)
    }

    @Test
    fun `previous follows the state`() {
        assertTrue(viewOf(1, status = SessionStatus.PAUSED, canGoPrevious = true).canGoPrevious)
        assertFalse(viewOf(0, status = SessionStatus.PAUSED, canGoPrevious = false).canGoPrevious)
    }

    @ParameterizedTest(name = "{0} s gives {1}")
    @CsvSource(
        "40, 00:40",
        "1, 00:01",
        "0, 00:00",
        "9, 00:09",
        "59, 00:59",
        "60, 01:00",
        "90, 01:30",
        "600, 10:00",
        "5999, 99:59",
    )
    fun `formats the phase time as minutes and seconds, two digits each`(
        seconds: Int,
        expected: String,
    ) {
        assertEquals(expected, formatPhaseTime(seconds))
    }

    @Test
    fun `a negative time gives zero`() {
        assertEquals("00:00", formatPhaseTime(-1))
    }
}
