package io.github.davamix.workouttimer.core.timeline

import io.github.davamix.workouttimer.core.model.Workout

/**
 * The workout's total duration in seconds, from Start to completion (WM-6), for the editor. Unlike
 * [Timeline.of], it works on workouts that aren't valid yet, so the total can update while editing:
 *
 * - rounds without exercises are ignored, as if they weren't there;
 * - negative durations and rests count as 0;
 * - a workout with no exercises gives 0;
 * - otherwise the §4.3 rules apply, as in the timeline, including the get-ready countdown.
 *
 * For any valid workout it equals `Timeline.of(workout).totalSeconds`. Names and upper limits aren't checked; a
 * sum too large for an [Int] gives [Int.MAX_VALUE].
 */
fun Workout.estimatedTotalSeconds(): Int {
    val steps = exerciseSteps()
    if (steps.isEmpty()) return 0
    var total = GET_READY_SECONDS.toLong()
    for (step in steps) {
        total += step.exercise.durationSeconds.coerceAtLeast(0)
        total += step.restSeconds.coerceAtLeast(0)
    }
    return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
