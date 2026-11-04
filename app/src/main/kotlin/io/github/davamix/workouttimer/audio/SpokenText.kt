package io.github.davamix.workouttimer.audio

import android.content.res.Resources
import io.github.davamix.workouttimer.R
import io.github.davamix.workouttimer.core.cues.Utterance

/**
 * The English text of each [Utterance], from `strings_audio.xml` (TTS-11). The phrases there are never translated, so
 * they are English whatever the device language. Exercise names are spoken as the workout has them.
 */
class SpokenText(
    private val resources: Resources,
) {
    fun of(utterance: Utterance): String =
        when (utterance) {
            Utterance.GetReadyIn -> resources.getString(R.string.cue_get_ready_in)
            is Utterance.Count ->
                resources.getString(
                    when (utterance.number) {
                        1 -> R.string.cue_one
                        2 -> R.string.cue_two
                        3 -> R.string.cue_three
                        else -> error("No phrase for the countdown number ${utterance.number}")
                    },
                )
            is Utterance.ExerciseName -> utterance.name
            Utterance.WorkoutComplete -> resources.getString(R.string.cue_workout_complete)
        }
}
