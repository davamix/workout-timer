package io.github.davamix.workouttimer.core.model

import io.github.davamix.workouttimer.core.exercise
import io.github.davamix.workouttimer.core.round
import io.github.davamix.workouttimer.core.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DuplicateTest {
    private val original =
        workout(
            round("Warm-up", exercise("Jumping jacks", duration = 30), exercise("Arm circles", rest = 5), rest = 30),
            round("Core", exercise("Plank", duration = 60, rest = 30), exercise("Crunches")),
            name = "Example",
            defaultDuration = 40,
            defaultExerciseRest = 15,
            defaultRoundRest = 60,
        )

    /** Gives `new-1`, `new-2`, … so the test can tell which IDs were replaced. */
    private fun sequentialIds(): () -> String {
        var next = 0
        return { "new-${++next}" }
    }

    private fun Workout.allIds(): List<String> = listOf(id) + rounds.flatMap { round -> listOf(round.id) + round.exercises.map { it.id } }

    @Test
    fun `a duplicated workout has a new ID at every level`() {
        val copy = original.duplicate("Copy", sequentialIds())

        val newIds = copy.allIds()
        assertEquals(newIds.size, newIds.toSet().size, "IDs must be unique: $newIds")
        assertEquals(emptySet<String>(), newIds.toSet() intersect original.allIds().toSet())
        assertEquals((1..newIds.size).map { "new-$it" }.toSet(), newIds.toSet())
    }

    @Test
    fun `a duplicated workout keeps every other value and takes the given name`() {
        val copy = original.duplicate("Copy", sequentialIds())

        assertEquals("Copy", copy.name)
        assertEquals(original.withoutIds().copy(name = "Copy"), copy.withoutIds())
    }

    @Test
    fun `a duplicated workout gets random IDs by default`() {
        val first = original.duplicate("Copy")
        val second = original.duplicate("Copy")

        val all = first.allIds() + second.allIds() + original.allIds()
        assertEquals(all.size, all.toSet().size, "IDs must be unique: $all")
    }

    @Test
    fun `a duplicated round has new IDs for itself and its exercises and keeps the rest`() {
        val source = original.rounds[0]
        val copy = source.duplicate(sequentialIds())

        assertEquals("new-1", copy.id)
        assertEquals(listOf("new-2", "new-3"), copy.exercises.map { it.id })
        assertEquals(source.withoutIds(), copy.withoutIds())
    }

    @Test
    fun `a duplicated exercise has a new ID and keeps the rest`() {
        val source = original.rounds[1].exercises[0]
        val copy = source.duplicate { "new" }

        assertEquals(source.copy(id = "new"), copy)
    }

    @Test
    fun `newId gives a different ID each time`() {
        val ids = List(100) { newId() }

        assertEquals(100, ids.toSet().size)
    }

    private fun Workout.withoutIds() = copy(id = "", rounds = rounds.map { it.withoutIds() })

    private fun Round.withoutIds() = copy(id = "", exercises = exercises.map { it.copy(id = "") })
}
