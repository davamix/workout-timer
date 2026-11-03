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
