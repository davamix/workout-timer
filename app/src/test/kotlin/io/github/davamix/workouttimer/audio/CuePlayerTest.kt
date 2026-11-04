package io.github.davamix.workouttimer.audio

import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.engine.SessionEvent
import io.github.davamix.workouttimer.core.engine.SessionEvent.Completed
import io.github.davamix.workouttimer.core.engine.SessionEvent.CountdownStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.Paused
import io.github.davamix.workouttimer.core.engine.SessionEvent.PhaseStarted
import io.github.davamix.workouttimer.core.engine.SessionEvent.RoundEnded
import io.github.davamix.workouttimer.core.engine.SessionEvent.SecondChanged
import io.github.davamix.workouttimer.core.engine.SessionStatus
import io.github.davamix.workouttimer.core.engine.WorkoutSession
import io.github.davamix.workouttimer.core.timeline.Timeline
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.data.exercise
import io.github.davamix.workouttimer.data.round
import io.github.davamix.workouttimer.data.workout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The cue player with a fake speaker and sound player, in virtual time (SPEC §4.3.2; TTS-3, TTS-9, TTS-10, SND-1). */
@OptIn(ExperimentalCoroutinesApi::class)
class CuePlayerTest {
    private val example = Timeline.of(exampleWorkout).phases
    private val getReady = example[0]
    private val jumpingJacks = example[1]
    private val plank = example[7]
    private val shortExercise = Timeline.of(workout(round("R", exercise("Short", duration = 3)))).phases[1]

    /** A cue player playing events that the test sends one by one, with fakes logging to [log]. */
    private class Harness(
        private val scope: TestScope,
        status: SpeakerStatus = SpeakerStatus.READY,
        voiceCuesEnabled: Boolean = true,
    ) {
        val log = AudioLog { scope.testScheduler.currentTime }
        val speaker = FakeSpeaker(log, status)
        val sound = FakeSoundPlayer(log)
        val player = CuePlayer(speaker, sound, ::testText)
        private val events = Channel<SessionEvent>(Channel.UNLIMITED)
        var announcementsFinished = 0
            private set

        val job: Job =
            scope.launch {
                player.play(events.receiveAsFlow(), voiceCuesEnabled, announcementFinished = { announcementsFinished++ })
            }

        /** Sends [sent] and lets the player handle them. */
        fun send(vararg sent: SessionEvent) {
            sent.forEach { events.trySend(it) }
            scope.runCurrent()
        }

        /** Ends the events and waits for the player to return. */
        suspend fun end() {
            events.close()
            job.join()
        }
    }

    @Test
    fun `speech goes to the speaker in cue order`() =
        runTest {
            val harness = Harness(this)

            harness.send(
                PhaseStarted(1, jumpingJacks),
                SecondChanged(1, jumpingJacks, 3),
                SecondChanged(1, jumpingJacks, 2),
                SecondChanged(1, jumpingJacks, 1),
                Completed,
            )
            harness.end()

            val expected =
                listOf("speak Jumping jacks", "speak three", "speak two", "speak one", "speak Workout complete. Well done!")
            assertEquals(expected, harness.log.actions)
        }

    // Get ready (TTS-10)

    @Test
    fun `Get ready in reports announcementFinished once it has been spoken`() =
        runTest {
            val harness = Harness(this)

            harness.send(PhaseStarted(0, getReady))
            assertEquals(listOf("speak Get ready in"), harness.log.actions)
            assertEquals(0, harness.announcementsFinished)

            harness.speaker.finish("Get ready in")
            assertEquals(1, harness.announcementsFinished)
            harness.end()
        }

    @Test
    fun `when the speaker isn't ready, Get ready in reports at once and nothing is spoken`() =
        runTest {
            for (status in listOf(SpeakerStatus.INITIALIZING, SpeakerStatus.UNAVAILABLE)) {
                val harness = Harness(this, status)

                harness.send(PhaseStarted(0, getReady))

                assertEquals(1, harness.announcementsFinished, "$status")
                assertEquals(emptyList<String>(), harness.log.actions, "$status")
                harness.end()
            }
        }

    @Test
    fun `when speaking fails, Get ready in reports at once`() =
        runTest {
            val harness = Harness(this)
            harness.speaker.failSpeaking = true

            harness.send(PhaseStarted(0, getReady))

            assertEquals(1, harness.announcementsFinished)
            assertEquals(listOf("unheard Get ready in"), harness.log.actions)
            harness.end()
        }

    // The round-end sound (SND-1)

    @Test
    fun `a name after the round-end sound waits for it to finish`() =
        runTest {
            val harness = Harness(this)

            harness.send(RoundEnded(1), PhaseStarted(7, plank))
            assertEquals(listOf("sound"), harness.log.actions)

            harness.sound.finish()
            runCurrent()
            assertEquals(listOf("sound", "speak Plank"), harness.log.actions)
            harness.end()
        }

    @Test
    fun `numbers after the round-end sound wait for it, then go in order`() =
        runTest {
            val harness = Harness(this)

            harness.send(RoundEnded(1), PhaseStarted(1, shortExercise), SecondChanged(1, shortExercise, 2))
            assertEquals(listOf("sound"), harness.log.actions)

            harness.sound.finish()
            runCurrent()
            assertEquals(listOf("sound", "speak three", "speak two"), harness.log.actions)
            harness.end()
        }

    @Test
    fun `if the sound never reports, waiting speech goes after the timeout`() =
        runTest {
            val harness = Harness(this)

            harness.send(RoundEnded(1), PhaseStarted(7, plank))
            advanceTimeBy(ROUND_END_SOUND_TIMEOUT_MILLIS - 1)
            runCurrent()
            assertEquals(listOf("0 sound"), harness.log.entries)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("0 sound", "2000 speak Plank"), harness.log.entries)

            // A late report changes nothing.
            harness.sound.finish()
            runCurrent()
            assertEquals(listOf("0 sound", "2000 speak Plank"), harness.log.entries)
            harness.end()
        }

    @Test
    fun `speech goes at once when no sound is playing`() =
        runTest {
            val harness = Harness(this)

            harness.send(RoundEnded(1))
            harness.sound.finish()
            runCurrent()
            harness.send(PhaseStarted(7, plank))

            assertEquals(listOf("sound", "speak Plank"), harness.log.actions)
            harness.end()
        }

    // Pause and stop (TTS-3)

    @Test
    fun `stopping speech stops the speaker and drops speech waiting for the sound`() =
        runTest {
            val harness = Harness(this)

            harness.send(RoundEnded(1), PhaseStarted(7, plank), Paused)
            assertEquals(listOf("sound", "stop"), harness.log.actions)

            harness.sound.finish()
            runCurrent()
            assertEquals(listOf("sound", "stop"), harness.log.actions)
            harness.end()
        }

    // Voice cues off (TTS-9)

    @Test
    fun `with voice cues off, the speaker is never used, and the sound plays`() =
        runTest {
            val harness = Harness(this, voiceCuesEnabled = false)

            harness.send(
                PhaseStarted(0, getReady),
                CountdownStarted,
                PhaseStarted(1, jumpingJacks),
                SecondChanged(1, jumpingJacks, 3),
                Paused,
                RoundEnded(1),
                Completed,
            )
            harness.end()

            assertEquals(listOf("sound"), harness.log.actions)
            assertEquals(0, harness.announcementsFinished)
        }

    // The end of play

    @Test
    fun `play returns when the events end, without stopping speech`() =
        runTest {
            val harness = Harness(this)

            harness.send(Completed)
            harness.end()

            assertTrue(harness.job.isCompleted)
            assertEquals(listOf("speak Workout complete. Well done!"), harness.log.actions)
        }

    @Test
    fun `cancelling play doesn't stop speech`() =
        runTest {
            val harness = Harness(this)

            harness.send(PhaseStarted(1, jumpingJacks))
            harness.job.cancel()
            harness.job.join()

            assertEquals(listOf("speak Jumping jacks"), harness.log.actions)
        }

    // prepare()

    @Test
    fun `prepare loads the sound, and prepares the speaker only with voice cues on`() =
        runTest {
            val log = AudioLog()
            val speaker = FakeSpeaker(log)
            val sound = FakeSoundPlayer(log)
            val player = CuePlayer(speaker, sound, ::testText)

            assertFalse(player.prepare(voiceCuesEnabled = false))
            assertEquals(0, speaker.prepareCalls)
            assertEquals(1, sound.prepareCalls)

            assertTrue(player.prepare(voiceCuesEnabled = true))
            assertEquals(1, speaker.prepareCalls)
            assertEquals(2, sound.prepareCalls)
        }

    @Test
    fun `prepare says voice cues can't be heard when the speaker isn't ready`() =
        runTest {
            val log = AudioLog()
            for (status in listOf(SpeakerStatus.INITIALIZING, SpeakerStatus.UNAVAILABLE)) {
                val player = CuePlayer(FakeSpeaker(log, status), FakeSoundPlayer(log), ::testText)

                assertFalse(player.prepare(voiceCuesEnabled = true), "$status")
            }
        }

    // With a real session

    @Test
    fun `with a real session of the example, every phrase is spoken on time, and it completes at 264,5 s`() =
        runTest {
            val log = AudioLog { testScheduler.currentTime }
            val speaker =
                FakeSpeaker(log, scope = backgroundScope, durationOf = { if (it == "Get ready in") 1_500L else null })
            val player = CuePlayer(speaker, FakeSoundPlayer(log), ::testText)
            val session =
                WorkoutSession(
                    Timeline.of(exampleWorkout),
                    Clock { testScheduler.currentTime },
                    backgroundScope,
                    waitForAnnouncement = true,
                )

            assertTrue(player.prepare(voiceCuesEnabled = true))
            val playing = launch { player.play(session, voiceCuesEnabled = true) }
            session.start()
            // In the foreground: the session's timer runs in backgroundScope (CLAUDE.md, coroutine tests).
            playing.join()

            val expected =
                listOf(
                    "0 speak Get ready in",
                    "1500 speak three",
                    "2500 speak two",
                    "3500 speak one",
                    "4500 speak Jumping jacks",
                    "31500 speak three",
                    "32500 speak two",
                    "33500 speak one",
                    "56500 speak three",
                    "57500 speak two",
                    "58500 speak one",
                    "64500 speak High knees",
                    "101500 speak three",
                    "102500 speak two",
                    "103500 speak one",
                    "104500 sound",
                    "134500 speak Plank",
                    "191500 speak three",
                    "192500 speak two",
                    "193500 speak one",
                    "224500 speak Crunches",
                    "261500 speak three",
                    "262500 speak two",
                    "263500 speak one",
                    "264500 speak Workout complete. Well done!",
                )
            assertEquals(expected, log.entries)
            assertEquals(SessionStatus.COMPLETED, session.state.value.status)
            assertEquals(264_500L, testScheduler.currentTime)
        }
}
