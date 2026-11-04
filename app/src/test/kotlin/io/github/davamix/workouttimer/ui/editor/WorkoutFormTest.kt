package io.github.davamix.workouttimer.ui.editor

import io.github.davamix.workouttimer.core.model.TimeField
import io.github.davamix.workouttimer.core.model.ValidationError
import io.github.davamix.workouttimer.core.model.validate
import io.github.davamix.workouttimer.core.timeline.estimatedTotalSeconds
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.data.exercise
import io.github.davamix.workouttimer.data.round
import io.github.davamix.workouttimer.data.workout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WorkoutFormTest {
    /** IDs n1, n2, … in the order they're asked for. */
    private fun ids(): () -> String {
        var next = 0
        return { "n${++next}" }
    }

    private val example = exampleWorkout.toForm()

    // The example's IDs (SampleWorkouts.kt).
    private val warmUp = "r-warm-up"
    private val core = "r-core"
    private val jumpingJacks = "e-jumping-jacks"
    private val armCircles = "e-arm-circles"
    private val highKnees = "e-high-knees"
    private val plank = "e-plank"
    private val crunches = "e-crunches"

    private fun WorkoutForm.roundIds() = rounds.map { it.id }

    private fun WorkoutForm.exerciseIds(roundId: String) = rounds.single { it.id == roundId }.exercises.map { it.id }

    private fun WorkoutForm.exercise(id: String) = rounds.flatMap { it.exercises }.single { it.id == id }

    private fun WorkoutForm.round(id: String) = rounds.single { it.id == id }

    // Mapping

    @Test
    fun `toForm then toWorkout gives back the example`() {
        assertEquals(exampleWorkout, example.toWorkout())
    }

    @Test
    fun `toForm writes numbers as digits and own values that are not set as empty text`() {
        assertEquals("40", example.defaultExerciseDuration)
        assertEquals("15", example.defaultExerciseRest)
        assertEquals("60", example.defaultRoundRest)
        assertEquals("30", example.round(warmUp).rest)
        assertEquals("", example.round(core).rest)
        assertEquals(ExerciseForm(jumpingJacks, "Jumping jacks", "30", ""), example.exercise(jumpingJacks))
        assertEquals(ExerciseForm(armCircles, "Arm circles", "10", "5"), example.exercise(armCircles))
        assertEquals(ExerciseForm(highKnees, "High knees", "", ""), example.exercise(highKnees))
    }

    @Test
    fun `toWorkout gives null for an empty own value, so it uses the default`() {
        val workout = example.withExerciseDuration(jumpingJacks, "").withRoundRest(warmUp, "").toWorkout()

        val exercise = workout.rounds[0].exercises[0]
        assertNull(exercise.durationSeconds)
        assertNull(workout.rounds[0].restSeconds)
    }

    @Test
    fun `an own value equal to the default stays an own value`() {
        val original = workout(round("R", exercise("Squats", duration = 40, rest = 15), rest = 60))

        val workout = original.toForm().toWorkout()

        assertEquals(40, workout.rounds[0].exercises[0].durationSeconds)
        assertEquals(15, workout.rounds[0].exercises[0].restSeconds)
        assertEquals(60, workout.rounds[0].restSeconds)
    }

    @Test
    fun `unused values are kept`() {
        val original =
            workout(
                round("A", exercise("Squats"), exercise("Lunges", rest = 20), rest = 45),
                round("B", exercise("Plank", rest = 25), rest = 90),
            )

        assertEquals(original, original.toForm().toWorkout())
    }

    @Test
    fun `toWorkout trims names`() {
        val form =
            example
                .withName("  Example  ")
                .withRoundName(warmUp, " Warm-up ")
                .withExerciseName(plank, "Plank\t")

        val workout = form.toWorkout()

        assertEquals(exampleWorkout, workout)
    }

    @Test
    fun `an empty default is reported as out of range and counts as zero in the total`() {
        val form = example.withDefault(DefaultField.EXERCISE_DURATION, "").withDefault(DefaultField.EXERCISE_REST, "")

        val workout = form.toWorkout()

        val errors = workout.validate()
        assertTrue(ValidationError.TimeOutOfRange(TimeField.DefaultExerciseDuration, 1, 5999) in errors)
        assertTrue(ValidationError.TimeOutOfRange(TimeField.DefaultExerciseRest, 0, 5999) in errors)
        // High knees and Crunches lose their 40 s; Jumping jacks loses its 15 s rest.
        assertEquals(263 - 40 - 40 - 15, workout.estimatedTotalSeconds())
    }

    // Time fields

    @ParameterizedTest(name = "\"{0}\" keeps \"{1}\"")
    @CsvSource(
        "'', ''",
        "45, 45",
        "0, 0",
        "045, 045",
        "1a2, 12",
        "-5, 5",
        "+7, 7",
        "' 3 0 ', 30",
        "1:30, 130",
        "'1,5', 15",
        "2.5, 25",
        "12345, 1234",
        "99999, 9999",
        "abc, ''",
        "٣٤, ''",
    )
    fun `timeText keeps only the digits 0 to 9, at most four`(
        input: String,
        expected: String,
    ) {
        assertEquals(expected, timeText(input))
    }

    @Test
    fun `every time field keeps only digits`() {
        val form =
            example
                .withDefault(DefaultField.EXERCISE_DURATION, "4x5")
                .withDefault(DefaultField.EXERCISE_REST, "-10")
                .withDefault(DefaultField.ROUND_REST, "1:00")
                .withRoundRest(warmUp, "3 0")
                .withExerciseDuration(plank, "60s")
                .withExerciseRest(plank, "123456")

        assertEquals("45", form.defaultExerciseDuration)
        assertEquals("10", form.defaultExerciseRest)
        assertEquals("100", form.defaultRoundRest)
        assertEquals("30", form.round(warmUp).rest)
        assertEquals("60", form.exercise(plank).duration)
        assertEquals("1234", form.exercise(plank).rest)
    }

    @Test
    fun `a leading zero gives the same number`() {
        assertEquals(
            40,
            example
                .withExerciseDuration(plank, "040")
                .toWorkout()
                .rounds[1]
                .exercises[0]
                .durationSeconds,
        )
    }

    // A new workout

    @Test
    fun `a new workout has no name, the example defaults, and one round with one exercise to name`() {
        val form = newWorkoutForm("Round 1", ids())

        assertEquals(
            WorkoutForm(
                id = "n1",
                name = "",
                defaultExerciseDuration = "40",
                defaultExerciseRest = "15",
                defaultRoundRest = "60",
                rounds = listOf(RoundForm("n2", "Round 1", "", listOf(ExerciseForm("n3", "", "", "")))),
            ),
            form,
        )
    }

    @Test
    fun `a new workout is invalid only because its name and its exercise name are missing`() {
        val errors = newWorkoutForm("Round 1", ids()).toWorkout().validate()

        assertEquals(listOf(ValidationError.WorkoutNameMissing, ValidationError.ExerciseNameMissing("n3")), errors)
    }

    // Names and defaults

    @Test
    fun `names are stored as typed`() {
        val form =
            example
                .withName(" New name ")
                .withRoundName(core, "Abs ")
                .withExerciseName(crunches, " Sit-ups")

        assertEquals(" New name ", form.name)
        assertEquals("Abs ", form.round(core).name)
        assertEquals(" Sit-ups", form.exercise(crunches).name)
    }

    @Test
    fun `each default is set in its own field`() {
        val form =
            example
                .withDefault(DefaultField.EXERCISE_DURATION, "45")
                .withDefault(DefaultField.EXERCISE_REST, "10")
                .withDefault(DefaultField.ROUND_REST, "90")

        assertEquals(listOf("45", "10", "90"), listOf(form.defaultExerciseDuration, form.defaultExerciseRest, form.defaultRoundRest))
        assertEquals(example.rounds, form.rounds)
    }

    @Test
    fun `changing a default changes the linked values only`() {
        val workout = example.withDefault(DefaultField.EXERCISE_DURATION, "45").toWorkout()

        // Jumping jacks keeps its own 30 s; High knees and Crunches follow the default.
        assertEquals(263 + 5 + 5, workout.estimatedTotalSeconds())
    }

    // Adding

    @Test
    fun `add round appends a round with one exercise to name`() {
        val form = example.addRound("Round 3", ids())

        assertEquals(listOf(warmUp, core, "n1"), form.roundIds())
        assertEquals(RoundForm("n1", "Round 3", "", listOf(ExerciseForm("n2", "", "", ""))), form.rounds.last())
    }

    @Test
    fun `add exercise appends an exercise to name to its round only`() {
        val form = example.addExercise(warmUp, ids())

        assertEquals(listOf(jumpingJacks, armCircles, highKnees, "n1"), form.exerciseIds(warmUp))
        assertEquals(ExerciseForm("n1", "", "", ""), form.exercise("n1"))
        assertEquals(example.round(core), form.round(core))
    }

    // Moving

    @Test
    fun `a round moves up and down one place`() {
        val withThird = example.addRound("Round 3", ids())

        assertEquals(listOf(core, warmUp, "n1"), withThird.moveRound(core, -1).roundIds())
        assertEquals(listOf(core, warmUp, "n1"), withThird.moveRound(warmUp, +1).roundIds())
        assertEquals(listOf(warmUp, "n1", core), withThird.moveRound(core, +1).roundIds())
    }

    @Test
    fun `a round does not move past either end`() {
        assertEquals(example, example.moveRound(warmUp, -1))
        assertEquals(example, example.moveRound(core, +1))
    }

    @Test
    fun `an exercise moves up and down within its round`() {
        assertEquals(listOf(armCircles, jumpingJacks, highKnees), example.moveExercise(armCircles, -1).exerciseIds(warmUp))
        assertEquals(listOf(jumpingJacks, highKnees, armCircles), example.moveExercise(armCircles, +1).exerciseIds(warmUp))
        assertEquals(listOf(crunches, plank), example.moveExercise(plank, +1).exerciseIds(core))
    }

    @Test
    fun `an exercise does not move past either end of its round, even into another round`() {
        assertEquals(example, example.moveExercise(jumpingJacks, -1))
        assertEquals(example, example.moveExercise(highKnees, +1))
        assertEquals(example, example.moveExercise(plank, -1))
        assertEquals(example, example.moveExercise(crunches, +1))
    }

    @Test
    fun `moving keeps every value, so unused values apply again`() {
        // High knees is last in Warm-up, so its rest is unused. Moved up, it has the default rest again.
        val moved = example.withExerciseRest(highKnees, "20").moveExercise(highKnees, -1)

        assertEquals(ExerciseForm(highKnees, "High knees", "", "20"), moved.exercise(highKnees))
        assertEquals(listOf(jumpingJacks, highKnees, armCircles), moved.exerciseIds(warmUp))
    }

    // Duplicating

    @Test
    fun `a duplicated round goes right after the original, with new IDs and the same name and values`() {
        val form = example.duplicateRound(warmUp, ids())

        assertEquals(listOf(warmUp, "n1", core), form.roundIds())
        val original = example.round(warmUp)
        val copy = form.round("n1")
        assertEquals(original.copy(id = "n1", exercises = original.exercises.mapIndexed { i, e -> e.copy(id = "n${i + 2}") }), copy)
        assertEquals(original, form.round(warmUp))
    }

    @Test
    fun `a duplicated exercise goes right after the original in its round, with a new ID and the same name and values`() {
        val form = example.duplicateExercise(armCircles, ids())

        assertEquals(listOf(jumpingJacks, armCircles, "n1", highKnees), form.exerciseIds(warmUp))
        assertEquals(example.exercise(armCircles).copy(id = "n1"), form.exercise("n1"))
        assertEquals(example.round(core), form.round(core))
    }

    @Test
    fun `the last round and the last exercise can be duplicated`() {
        assertEquals(listOf(warmUp, core, "n1"), example.duplicateRound(core, ids()).roundIds())
        assertEquals(listOf(plank, crunches, "n1"), example.duplicateExercise(crunches, ids()).exerciseIds(core))
    }

    // Removing

    @Test
    fun `a round or an exercise is removed`() {
        assertEquals(listOf(core), example.removeRound(warmUp).roundIds())
        assertEquals(listOf(jumpingJacks, highKnees), example.removeExercise(armCircles).exerciseIds(warmUp))
    }

    @Test
    fun `removing every round is allowed, and validation reports it`() {
        val form = example.removeRound(warmUp).removeRound(core)

        assertEquals(emptyList<RoundForm>(), form.rounds)
        assertEquals(listOf(ValidationError.NoRounds), form.toWorkout().validate())
    }

    @Test
    fun `removing every exercise of a round is allowed, and validation reports it`() {
        val form = example.removeExercise(plank).removeExercise(crunches)

        assertEquals(emptyList<ExerciseForm>(), form.round(core).exercises)
        assertEquals(listOf(ValidationError.RoundHasNoExercises(core)), form.toWorkout().validate())
    }

    // Unknown IDs

    @Test
    fun `an ID that is not in the form leaves it as it is`() {
        val unknown = "missing"
        val operations: List<(WorkoutForm) -> WorkoutForm> =
            listOf(
                { it.withRoundName(unknown, "X") },
                { it.withRoundRest(unknown, "5") },
                { it.withExerciseName(unknown, "X") },
                { it.withExerciseDuration(unknown, "5") },
                { it.withExerciseRest(unknown, "5") },
                { it.addExercise(unknown, ids()) },
                { it.moveRound(unknown, -1) },
                { it.moveExercise(unknown, +1) },
                { it.duplicateRound(unknown, ids()) },
                { it.duplicateExercise(unknown, ids()) },
                { it.removeRound(unknown) },
                { it.removeExercise(unknown) },
            )

        for (operation in operations) assertEquals(example, operation(example))
    }
}
