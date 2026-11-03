package io.github.davamix.workouttimer.core.model

/**
 * A round-based workout (specification §4.2): the workout defaults and its ordered rounds.
 *
 * All durations and rests are whole seconds. Values aren't checked on creation: call [validate] before
 * building a timeline or saving. The effective value of an optional field comes from [durationOf] and
 * [restAfter].
 *
 * @property id Unique ID. A random UUID in the app (see [newId]); fixed values in tests.
 * @property name Name shown in the list and the editor. Required (WM-7).
 * @property defaultExerciseDurationSeconds Duration of every exercise that doesn't set its own (WM-1).
 * @property defaultExerciseRestSeconds Rest after every exercise that doesn't set its own (WM-2). 0 means no rest.
 * @property defaultRoundRestSeconds Rest after every round that doesn't set its own (WM-2). 0 means no rest.
 * @property rounds Rounds in the order they run. A valid workout has at least one.
 */
data class Workout(
    val id: String,
    val name: String,
    val defaultExerciseDurationSeconds: Int,
    val defaultExerciseRestSeconds: Int,
    val defaultRoundRestSeconds: Int,
    val rounds: List<Round>,
)

/**
 * One round of a [Workout]: its exercises, and the rest that runs after it.
 *
 * @property id Unique ID within the app. See [newId].
 * @property name Name shown while running and in the editor. Required (WM-7).
 * @property restSeconds Own round rest, or `null` to use the workout's default round rest. Unused on the last
 *   round, but kept (see [Workout.isRestUsed]).
 * @property exercises Exercises in the order they run. A valid round has at least one.
 */
data class Round(
    val id: String,
    val name: String,
    val restSeconds: Int? = null,
    val exercises: List<Exercise>,
)

/**
 * One exercise of a [Round].
 *
 * @property id Unique ID within the app. See [newId].
 * @property name Name shown and spoken while running. Required (WM-7).
 * @property durationSeconds Own duration, or `null` to use the workout's default exercise duration.
 * @property restSeconds Own rest after this exercise, or `null` to use the workout's default exercise rest. Unused
 *   on the last exercise of a round, but kept (see [Round.isRestUsed]).
 */
data class Exercise(
    val id: String,
    val name: String,
    val durationSeconds: Int? = null,
    val restSeconds: Int? = null,
)
