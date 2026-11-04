package io.github.davamix.workouttimer.core.cues

import io.github.davamix.workouttimer.core.engine.SessionMachine
import io.github.davamix.workouttimer.core.engine.T0
import io.github.davamix.workouttimer.core.engine.exampleStarts
import io.github.davamix.workouttimer.core.engine.exampleTimeline
import io.github.davamix.workouttimer.core.engine.runOnTime
import io.github.davamix.workouttimer.core.engine.runOnTimeUntil
import io.github.davamix.workouttimer.core.engine.startAndRunOnTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Whole workouts run on the engine's machine, with every event passed through the planner. The expected sequences
 * are SPEC §4.3.8, and the §4.4 example's audio column (TTS-1, TTS-2, TTS-7, TTS-10, TTS-12, SND-1, SND-2).
 */
class ExampleCuesTest {
    private val voiceOn = CuePlanner(voiceCuesEnabled = true)
    private val voiceOff = CuePlanner(voiceCuesEnabled = false)

    @Test
    fun `the example with voice on gives every cue of its audio column, on time`() {
        val cues = runWithAnnouncement(exampleTimeline, announcementMillis = 1_500).cueCodes(voiceOn)

        val expected =
            listOf(
                "0 Get ready in",
                "1500 three",
                "2500 two",
                "3500 one",
                "4500 Jumping jacks",
                "31500 three",
                "32500 two",
                "33500 one",
                // Arm circles lasts 10 s, so it has no name.
                "56500 three",
                "57500 two",
                "58500 one",
                "64500 High knees",
                "101500 three",
                "102500 two",
                "103500 one",
                "104500 sound",
                "134500 Plank",
                "191500 three",
                "192500 two",
                "193500 one",
                "224500 Crunches",
                "261500 three",
                "262500 two",
                "263500 one",
                "264500 Workout complete",
            )
        assertEquals(expected, cues)
        assertEquals(24, cues.count { !it.endsWith(" sound") })
    }

    @Test
    fun `the example with voice off gives only the round-end sound, at the start of the round rest`() {
        val cues = SessionMachine(exampleTimeline).startAndRunOnTime().cueCodes(voiceOff)

        assertEquals(listOf("103000 sound"), cues)
    }

    @Test
    fun `the edge cases with voice on gives every cue on time`() {
        val cues = runWithAnnouncement(edgeTimeline, announcementMillis = 1_000).cueCodes(voiceOn)

        val expected =
            listOf(
                "0 Get ready in",
                "1000 three",
                "2000 two",
                "3000 one",
                // Ten lasts 10 s, so it has no name.
                "11000 three",
                "12000 two",
                "13000 one",
                "14000 Eleven",
                "22000 three",
                "23000 two",
                "24000 one",
                // Round A ends with a 0 s round rest: the sound, then Three's first number.
                "25000 sound",
                "25000 three",
                "26000 two",
                "27000 one",
                "28000 two",
                "29000 one",
                "30000 one",
                "31000 Workout complete",
            )
        assertEquals(expected, cues)
    }

    // Pause, resume, Next, Previous and Stop (TTS-3, TTS-7, specification §5.1)

    @Test
    fun `pausing during Get ready in stops it, and resuming starts the countdown without repeating it`() {
        val machine = SessionMachine(exampleTimeline, waitForAnnouncement = true)

        val cues =
            (
                machine.at(T0) { start(it) } +
                    machine.at(T0 + 800) { pause(it) } +
                    machine.at(T0 + 10_000) { resume(it) } +
                    machine.runOnTimeUntil(T0 + 12_000)
            ).cueCodes(voiceOn)

        assertEquals(listOf("0 Get ready in", "800 stop", "10000 three", "11000 two", "12000 one"), cues)
    }

    @Test
    fun `pausing at the instant of two, then resuming, carries on with one and replays nothing`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        // Jumping jacks starts at 3 s and lasts 30 s, so its display shows 2 at 31 s.
        val two = exampleStarts[1] + 28_000
        val beforePause = machine.runOnTimeUntil(two)

        val cues =
            (
                machine.at(two) { pause(it) } +
                    machine.at(two + 10_000) { resume(it) } +
                    machine.runOnTimeUntil(two + 11_000)
            ).cueCodes(voiceOn)

        assertEquals("31000 two", beforePause.cueCodes(voiceOn).last())
        assertEquals(listOf("31000 stop", "42000 one"), cues)
    }

    @Test
    fun `moving with Next gives no cue, and resuming names the target exercise`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(T0 + 5_000)

        val cues =
            (
                machine.at(T0 + 5_000) { pause(it) } +
                    machine.at(T0 + 6_000) { next(it) } +
                    machine.at(T0 + 7_000) { next(it) } +
                    machine.at(T0 + 8_000) { resume(it) }
            ).cueCodes(voiceOn)

        // Jumping jacks, then Arm circles, then High knees.
        assertEquals(listOf("5000 stop", "8000 High knees"), cues)
    }

    @Test
    fun `Next from the last exercise of a round plays no sound, and resuming names the next round's first`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        val inHighKnees = exampleStarts[5] + 2_000
        machine.runOnTimeUntil(inHighKnees)

        val cues =
            (
                machine.at(inHighKnees) { pause(it) } +
                    machine.at(inHighKnees + 1_000) { next(it) } +
                    machine.at(inHighKnees + 2_000) { resume(it) }
            ).cueCodes(voiceOn)

        assertEquals(listOf("65000 stop", "67000 Plank"), cues)
    }

    @Test
    fun `Previous on the first exercise restarts it, and resuming names it again`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(T0 + 10_000)

        val cues =
            (
                machine.at(T0 + 10_000) { pause(it) } +
                    machine.at(T0 + 11_000) { previous(it) } +
                    machine.at(T0 + 12_000) { resume(it) }
            ).cueCodes(voiceOn)

        assertEquals(listOf("10000 stop", "12000 Jumping jacks"), cues)
    }

    @Test
    fun `Next on the last exercise says Workout complete`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(T0 + 500)
        machine.pause(T0 + 500)

        // From get ready: Jumping jacks, Arm circles, High knees, Plank, Crunches, then completion.
        val cues = (1..6).flatMap { n -> machine.at(T0 + 500 + n * 1_000L) { next(it) } }.cueCodes(voiceOn)

        assertEquals(listOf("6500 Workout complete"), cues)
    }

    @Test
    fun `Stop while paused only stops speech`() {
        val machine = SessionMachine(exampleTimeline)
        machine.start(T0)
        machine.runOnTimeUntil(T0 + 20_000)

        val cues =
            (
                machine.at(T0 + 20_000) { pause(it) } +
                    machine.at(T0 + 21_000) { stop(it) } +
                    machine.runOnTime()
            ).cueCodes(voiceOn)

        assertEquals(listOf("20000 stop", "21000 stop"), cues)
    }
}
