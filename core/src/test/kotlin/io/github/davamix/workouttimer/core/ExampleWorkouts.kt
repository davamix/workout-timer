package io.github.davamix.workouttimer.core

import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout

// Test fixtures for :core: small builders with fixed IDs. Shared by this phase's tests and later :core phases.

/** An exercise whose ID defaults to `e-<name>`. */
fun exercise(
    name: String,
    duration: Int? = null,
    rest: Int? = null,
    id: String = "e-$name",
) = Exercise(id = id, name = name, durationSeconds = duration, restSeconds = rest)

/** A round whose ID defaults to `r-<name>`. */
fun round(
    name: String,
    vararg exercises: Exercise,
    rest: Int? = null,
    id: String = "r-$name",
) = Round(id = id, name = name, restSeconds = rest, exercises = exercises.toList())

/**
 * The specification §4.4 example. Its timeline has 10 phases and lasts 263 s (4:23).
 *
 * Defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.
 * - Round 1 "Warm-up", own round rest 30 s: Jumping jacks 30 s; Arm circles 10 s, own rest 5 s; High knees.
 * - Round 2 "Core": Plank 60 s, own rest 30 s; Crunches.
 */
val exampleWorkout =
    workout(
        round(
            "Warm-up",
            exercise("Jumping jacks", duration = 30),
            exercise("Arm circles", duration = 10, rest = 5),
            exercise("High knees"),
            rest = 30,
        ),
        round(
            "Core",
            exercise("Plank", duration = 60, rest = 30),
            exercise("Crunches"),
        ),
        name = "Example",
        defaultDuration = 40,
        defaultExerciseRest = 15,
        defaultRoundRest = 60,
    )

/** A workout with the §4.4 example's defaults unless others are given: 40 s duration, 15 s rest, 60 s round rest. */
fun workout(
    vararg rounds: Round,
    name: String = "Test",
    defaultDuration: Int = 40,
    defaultExerciseRest: Int = 15,
    defaultRoundRest: Int = 60,
    id: String = "w-$name",
) = Workout(
    id = id,
    name = name,
    defaultExerciseDurationSeconds = defaultDuration,
    defaultExerciseRestSeconds = defaultExerciseRest,
    defaultRoundRestSeconds = defaultRoundRest,
    rounds = rounds.toList(),
)
