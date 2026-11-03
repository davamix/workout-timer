package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.exampleWorkout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The specification §4.4 example: the acceptance test for the timeline. */
class ExampleWorkoutTest {
    /** One row of the §4.4 table. `null` stands for "—". */
    private data class Row(
        val phase: String,
        val round: String?,
        val name: String?,
        val seconds: Int,
    )

    private fun Phase.toRow(): Row =
        when (this) {
            is Phase.GetReady -> Row("Get ready", null, null, durationSeconds)
            is Phase.Exercise -> Row("Exercise", exercise.roundName, exercise.name, durationSeconds)
            is Phase.Rest ->
                when (kind) {
                    RestKind.EXERCISE -> Row("Exercise rest", previous.roundName, null, durationSeconds)
                    RestKind.ROUND -> Row("Round rest", null, null, durationSeconds)
                }
        }

    private val timeline = Timeline.of(exampleWorkout)

    @Test
    fun `the example gives exactly the phases of the table`() {
        val expected =
            listOf(
                Row("Get ready", null, null, 3),
                Row("Exercise", "Warm-up", "Jumping jacks", 30),
                Row("Exercise rest", "Warm-up", null, 15),
                Row("Exercise", "Warm-up", "Arm circles", 10),
                Row("Exercise rest", "Warm-up", null, 5),
                Row("Exercise", "Warm-up", "High knees", 40),
                Row("Round rest", null, null, 30),
                Row("Exercise", "Core", "Plank", 60),
                Row("Exercise rest", "Core", null, 30),
                Row("Exercise", "Core", "Crunches", 40),
            )

        assertEquals(expected, timeline.phases.map { it.toRow() })
    }

    @Test
    fun `the example lasts 4 23`() {
        assertEquals(263, timeline.totalSeconds)
        assertEquals(3 + 180 + 50 + 30, timeline.totalSeconds)
    }
}
