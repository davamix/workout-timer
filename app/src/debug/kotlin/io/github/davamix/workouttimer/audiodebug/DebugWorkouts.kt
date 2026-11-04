package io.github.davamix.workouttimer.audiodebug

import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout

// The workouts the audio debug screen runs (P5 SPEC §4.3.7). Built in code, so they don't depend on saved workouts.

/**
 * The specification §4.4 example, 4:23. Defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.
 * Round 1 "Warm-up", own round rest 30 s: Jumping jacks 30 s; Arm circles 10 s, own rest 5 s; High knees. Round 2
 * "Core": Plank 60 s, own rest 30 s; Crunches.
 */
private val example =
    Workout(
        id = "debug-example",
        name = "Example",
        defaultExerciseDurationSeconds = 40,
        defaultExerciseRestSeconds = 15,
        defaultRoundRestSeconds = 60,
        rounds =
            listOf(
                Round(
                    id = "debug-warm-up",
                    name = "Warm-up",
                    restSeconds = 30,
                    exercises =
                        listOf(
                            Exercise(id = "debug-jumping-jacks", name = "Jumping jacks", durationSeconds = 30),
                            Exercise(id = "debug-arm-circles", name = "Arm circles", durationSeconds = 10, restSeconds = 5),
                            Exercise(id = "debug-high-knees", name = "High knees"),
                        ),
                ),
                Round(
                    id = "debug-core",
                    name = "Core",
                    exercises =
                        listOf(
                            Exercise(id = "debug-plank", name = "Plank", durationSeconds = 60, restSeconds = 30),
                            Exercise(id = "debug-crunches", name = "Crunches"),
                        ),
                ),
            ),
    )

/**
 * The edge cases, 0:30. Every rest is 0 s. Round "A": "Ten" 10 s (no name: not longer than 10 s), then "Eleven"
 * 11 s (named). Round "B" follows at once, after the round-end sound: "Three" 3 s, "Two" 2 s and "One" 1 s, which
 * count down from their own duration.
 */
private val edgeCases =
    Workout(
        id = "debug-edge-cases",
        name = "Edge cases",
        defaultExerciseDurationSeconds = 40,
        defaultExerciseRestSeconds = 0,
        defaultRoundRestSeconds = 0,
        rounds =
            listOf(
                Round(
                    id = "debug-a",
                    name = "A",
                    exercises =
                        listOf(
                            Exercise(id = "debug-ten", name = "Ten", durationSeconds = 10),
                            Exercise(id = "debug-eleven", name = "Eleven", durationSeconds = 11),
                        ),
                ),
                Round(
                    id = "debug-b",
                    name = "B",
                    exercises =
                        listOf(
                            Exercise(id = "debug-three", name = "Three", durationSeconds = 3),
                            Exercise(id = "debug-two", name = "Two", durationSeconds = 2),
                            Exercise(id = "debug-one", name = "One", durationSeconds = 1),
                        ),
                ),
            ),
    )

/** The workouts to choose from, in the order shown. */
internal val debugWorkouts: List<Workout> = listOf(example, edgeCases)
