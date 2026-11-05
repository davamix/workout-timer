package io.github.davamix.workouttimer.ui

import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout

// Fixtures for the Compose UI tests. app/src/test's SampleWorkouts.kt isn't visible here, so this is a small copy.

/**
 * The specification §4.4 example, with fixed IDs. Total 4:23.
 *
 * Defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.
 * - Round 1 "Warm-up", own round rest 30 s: Jumping jacks 30 s; Arm circles 10 s, own rest 5 s; High knees.
 * - Round 2 "Core": Plank 60 s, own rest 30 s; Crunches.
 */
val uiExampleWorkout =
    Workout(
        id = "w-example",
        name = "Example",
        defaultExerciseDurationSeconds = 40,
        defaultExerciseRestSeconds = 15,
        defaultRoundRestSeconds = 60,
        rounds =
            listOf(
                Round(
                    id = "r-warm-up",
                    name = "Warm-up",
                    restSeconds = 30,
                    exercises =
                        listOf(
                            Exercise("e-jumping-jacks", "Jumping jacks", durationSeconds = 30),
                            Exercise("e-arm-circles", "Arm circles", durationSeconds = 10, restSeconds = 5),
                            Exercise("e-high-knees", "High knees"),
                        ),
                ),
                Round(
                    id = "r-core",
                    name = "Core",
                    exercises =
                        listOf(
                            Exercise("e-plank", "Plank", durationSeconds = 60, restSeconds = 30),
                            Exercise("e-crunches", "Crunches"),
                        ),
                ),
            ),
    )

/** A one-exercise workout called [name], with the example's defaults: total 0:43. IDs are built from [id]. */
fun simpleWorkout(
    name: String,
    id: String,
) = Workout(
    id = id,
    name = name,
    defaultExerciseDurationSeconds = 40,
    defaultExerciseRestSeconds = 15,
    defaultRoundRestSeconds = 60,
    rounds = listOf(Round(id = "r-$id", name = "Round 1", exercises = listOf(Exercise("e-$id", "Squats")))),
)

/** This workout with every ID blanked, to compare structures apart from IDs. */
fun Workout.withoutIds(): Workout =
    copy(
        id = "",
        rounds = rounds.map { round -> round.copy(id = "", exercises = round.exercises.map { it.copy(id = "") }) },
    )
