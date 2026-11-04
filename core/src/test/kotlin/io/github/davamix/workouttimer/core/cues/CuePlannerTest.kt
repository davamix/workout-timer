package io.github.davamix.workouttimer.core.cues

import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Resumed
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionEvent.SecondChanged
import io.github.davamix.workouttimer.core.engine.SessionEvent.Stopped
import io.github.davamix.workouttimer.core.engine.exampleTimeline
import io.github.davamix.workouttimer.core.timeline.Phase
import io.github.davamix.workouttimer.core.timeline.RestKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** The planner's rules, one event at a time (SPEC §4.3.1). */
class CuePlannerTest {
    private val voiceOn = CuePlanner(voiceCuesEnabled = true)
    private val voiceOff = CuePlanner(voiceCuesEnabled = false)

    private val getReady = exampleTimeline.phases[0]
    private val exerciseRest = exampleTimeline.phases[2]
    private val roundRest = exampleTimeline.phases[6]

    @Test
    fun `the name threshold is 10 s and the countdown speaks the last 3 s`() {
        assertEquals(10, ANNOUNCE_NAME_ABOVE_SECONDS)
        assertEquals(3, COUNTDOWN_SECONDS)
    }

    // Get ready (TTS-10)

    @Test
    fun `get ready says Get ready in when it starts, then three when its countdown starts`() {
        assertEquals(listOf("Get ready in"), voiceOn.codesFor(PhaseStarted(0, getReady)))
        assertEquals(listOf("three"), voiceOn.codesFor(CountdownStarted))
    }

    @Test
    fun `get ready says two and one as its display changes`() {
        assertEquals(listOf("two"), voiceOn.codesFor(SecondChanged(0, getReady, 2)))
        assertEquals(listOf("one"), voiceOn.codesFor(SecondChanged(0, getReady, 1)))
    }

    // Exercise names (TTS-1)

    @Test
    fun `an exercise of 11 s has its name spoken when it starts`() {
        assertEquals(listOf("Lunges"), voiceOn.codesFor(PhaseStarted(1, exercisePhase("Lunges", 11))))
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 5, 9, 10])
    fun `an exercise of 4 to 10 s gets nothing when it starts`(duration: Int) {
        assertEquals(emptyList<String>(), voiceOn.codesFor(PhaseStarted(1, exercisePhase("Lunges", duration))))
    }

    @Test
    fun `the name is spoken as written in the workout`() {
        val phase = exercisePhase("Push-ups, wide grip", 45)

        assertEquals(listOf(Cue.Speak(Utterance.ExerciseName("Push-ups, wide grip"))), voiceOn.cuesFor(PhaseStarted(1, phase)))
    }

    // Countdown (TTS-2)

    @Test
    fun `an exercise of 3 s or less starts its countdown at its own duration`() {
        assertEquals(listOf("three"), voiceOn.codesFor(PhaseStarted(1, exercisePhase("A", 3))))
        assertEquals(listOf("two"), voiceOn.codesFor(PhaseStarted(1, exercisePhase("A", 2))))
        assertEquals(listOf("one"), voiceOn.codesFor(PhaseStarted(1, exercisePhase("A", 1))))
    }

    @Test
    fun `an exercise says three, two and one as its display changes to 3, 2 and 1`() {
        val phase = exercisePhase("A", 40)

        assertEquals(listOf("three"), voiceOn.codesFor(SecondChanged(1, phase, 3)))
        assertEquals(listOf("two"), voiceOn.codesFor(SecondChanged(1, phase, 2)))
        assertEquals(listOf("one"), voiceOn.codesFor(SecondChanged(1, phase, 1)))
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 5, 10, 11, 39])
    fun `an exercise's display changing to more than 3 gets nothing`(seconds: Int) {
        assertEquals(emptyList<String>(), voiceOn.codesFor(SecondChanged(1, exercisePhase("A", 40), seconds)))
    }

    @Test
    fun `an exercise of 4 s says nothing at its start and three at 3`() {
        val phase = exercisePhase("A", 4)

        assertEquals(emptyList<String>(), voiceOn.codesFor(PhaseStarted(1, phase)))
        assertEquals(listOf("three"), voiceOn.codesFor(SecondChanged(1, phase, 3)))
    }

    // Rests (TTS-12)

    @Test
    fun `rests are silent, whatever their kind or length`() {
        val shortRest = Phase.Rest(RestKind.ROUND, 3, exerciseInfo("A", 20), exerciseInfo("B", 20))
        val restEvents =
            listOf(exerciseRest, roundRest, shortRest).flatMap { rest ->
                listOf(PhaseStarted(2, rest)) + (1..rest.durationSeconds).map { SecondChanged(2, rest, it) }
            }

        restEvents.forEach { assertEquals(emptyList<Cue>(), voiceOn.cuesFor(it), "$it") }
    }

    // Round end, pause, resume, completion and stop (SND-1, SND-2, TTS-3, TTS-7)

    @Test
    fun `a round's end plays the round-end sound`() {
        assertEquals(listOf(Cue.PlayRoundEndSound), voiceOn.cuesFor(RoundEnded(1)))
    }

    @Test
    fun `pausing and stopping stop speech, and resuming gives nothing`() {
        assertEquals(listOf(Cue.StopSpeech), voiceOn.cuesFor(Paused))
        assertEquals(listOf(Cue.StopSpeech), voiceOn.cuesFor(Stopped))
        assertEquals(emptyList<Cue>(), voiceOn.cuesFor(Resumed))
    }

    @Test
    fun `completion says Workout complete`() {
        assertEquals(listOf(Cue.Speak(Utterance.WorkoutComplete)), voiceOn.cuesFor(Completed))
    }

    // Voice cues off (TTS-9)

    @Test
    fun `with voice cues off, no event gives speech, and the sound and stopping speech stay`() {
        val exercise = exercisePhase("A", 20)
        val events: List<SessionEvent> =
            listOf(
                PhaseStarted(0, getReady),
                CountdownStarted,
                SecondChanged(0, getReady, 2),
                SecondChanged(0, getReady, 1),
                PhaseStarted(1, exercise),
                PhaseStarted(1, exercisePhase("B", 3)),
                SecondChanged(1, exercise, 3),
                RoundEnded(1),
                Paused,
                Resumed,
                Completed,
                Stopped,
            )

        val cues = events.flatMap { voiceOff.cuesFor(it) }

        assertEquals(listOf(Cue.PlayRoundEndSound, Cue.StopSpeech, Cue.StopSpeech), cues)
        assertEquals(false, voiceOff.voiceCuesEnabled)
    }

    // Count

    @Test
    fun `a countdown number is 1 to 3`() {
        assertEquals(listOf(1, 2, 3), (1..3).map { Utterance.Count(it).number })
        assertThrows<IllegalArgumentException> { Utterance.Count(0) }
        assertThrows<IllegalArgumentException> { Utterance.Count(4) }
    }
}
