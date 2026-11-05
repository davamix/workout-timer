package io.github.davamix.workouttimer.ui.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.davamix.workouttimer.audio.AudioLog
import io.github.davamix.workouttimer.audio.CuePlayer
import io.github.davamix.workouttimer.audio.FakeSoundPlayer
import io.github.davamix.workouttimer.audio.FakeSpeaker
import io.github.davamix.workouttimer.audio.Speaker
import io.github.davamix.workouttimer.audio.testText
import io.github.davamix.workouttimer.core.engine.Clock
import io.github.davamix.workouttimer.core.repository.WorkoutRepository
import io.github.davamix.workouttimer.core.settings.Settings
import io.github.davamix.workouttimer.data.exampleWorkout
import io.github.davamix.workouttimer.data.exercise
import io.github.davamix.workouttimer.data.round
import io.github.davamix.workouttimer.data.workout
import io.github.davamix.workouttimer.ui.MainDispatcherExtension
import io.github.davamix.workouttimer.ui.TestSettingsRepository
import io.github.davamix.workouttimer.ui.TestWorkoutRepository
import io.github.davamix.workouttimer.ui.completed.WorkoutSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * The runner's ViewModel with a real session and a real cue player on P5's audio fakes, in virtual time.
 *
 * With voice cues off, the §4.4 example runs: get ready 0–3 s, Jumping jacks 3–33 s, rest 33–48 s, Arm circles
 * 48–58 s, rest 58–63 s, High knees 63–103 s, round rest 103–133 s, Plank 133–193 s, rest 193–223 s, Crunches
 * 223–263 s.
 *
 * The session runs in `viewModelScope`, on the test `Main` dispatcher, so `advanceUntilIdle()` would run the whole
 * workout: tests move time with [at] instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunnerViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val log = AudioLog()
    private val speaker = FakeSpeaker(log)
    private val soundPlayer = FakeSoundPlayer(log)
    private val created = mutableListOf<RunnerViewModel>()

    @AfterEach
    fun clear() {
        created.forEach { it.viewModelScope.cancel() }
    }

    private fun TestScope.runner(
        voiceOn: Boolean = false,
        repository: WorkoutRepository = TestWorkoutRepository(listOf(exampleWorkout)),
        workoutId: String = exampleWorkout.id,
        speaker: Speaker = this@RunnerViewModelTest.speaker,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = RunnerViewModel(
        workoutId = workoutId,
        workoutRepository = repository,
        settingsRepository = TestSettingsRepository(Settings(voiceCuesEnabled = voiceOn)),
        clock = Clock { testScheduler.currentTime },
        cuePlayer = CuePlayer(speaker, soundPlayer, ::testText),
        savedStateHandle = savedState,
        audioDispatcher = main.dispatcher,
    ).also { created += it }

    /** Moves virtual time to [millis] after the test's start, and runs what is due then. */
    private fun TestScope.at(millis: Long) {
        advanceTimeBy(millis - currentTime)
        runCurrent()
    }

    private val RunnerViewModel.active: RunnerState.Active
        get() = checkNotNull(state.value as? RunnerState.Active) { "Not active: ${state.value}" }

    private val RunnerViewModel.view: RunnerView get() = active.view

    private val RunnerViewModel.title: String?
        get() = view.exercise?.name

    private val exampleSummary = WorkoutSummary("Example", totalSeconds = 263, roundCount = 2, exerciseCount = 5)

    // Starting

    @Test
    fun `it starts in get ready, at 3 s`() =
        runTest {
            val runner = runner()

            assertEquals(PhaseKind.GET_READY, runner.view.kind)
            assertEquals(3, runner.view.remainingSeconds)
            assertFalse(runner.view.paused)
        }

    @Test
    fun `while it reads the workout, it's starting with no name`() =
        runTest {
            val repository = TestWorkoutRepository(listOf(exampleWorkout), loaded = false)
            val runner = runner(repository = repository)
            assertEquals(RunnerState.Starting(), runner.state.value)

            repository.finishLoading()
            runCurrent()
            assertEquals(PhaseKind.GET_READY, runner.view.kind)
        }

    @Test
    fun `while the audio prepares, it's starting with the workout's name`() =
        runTest {
            val gated = GatedSpeaker(speaker)
            val runner = runner(voiceOn = true, speaker = gated)
            at(8_000)
            assertEquals(RunnerState.Starting("Example"), runner.state.value)

            gated.release()
            runCurrent()
            assertEquals(PhaseKind.GET_READY, runner.view.kind)
            assertEquals(3, runner.view.remainingSeconds)
        }

    @Test
    fun `with voice cues on, get ready waits for Get ready in, then counts down`() =
        runTest {
            val runner = runner(voiceOn = true)
            assertTrue("speak Get ready in" in log.actions)

            at(2_000)
            assertEquals(3, runner.view.remainingSeconds)

            speaker.finish("Get ready in")
            at(3_000)
            assertEquals(2, runner.view.remainingSeconds)

            at(5_000)
            assertEquals("Jumping jacks", runner.title)
            assertEquals(30, runner.view.remainingSeconds)
        }

    @Test
    fun `with voice cues off, get ready counts down at once and the speaker is never used`() =
        runTest {
            val runner = runner(voiceOn = false)

            at(1_000)
            assertEquals(2, runner.view.remainingSeconds)
            at(3_000)
            assertEquals("Jumping jacks", runner.title)

            assertEquals(0, speaker.prepareCalls)
            assertTrue(log.actions.none { it.startsWith("speak") || it.startsWith("unheard") }, "${log.actions}")
        }

    // Running

    @Test
    fun `the time counts down from the full duration to 1 in the last second`() =
        runTest {
            val runner = runner()

            at(3_500)
            assertEquals(30, runner.view.remainingSeconds)
            at(32_500)
            assertEquals(1, runner.view.remainingSeconds)
            at(33_000)
            assertEquals(PhaseKind.REST, runner.view.kind)
            assertEquals(15, runner.view.remainingSeconds)
        }

    @Test
    fun `the whole example runs with its cues, then completes with its summary`() =
        runTest {
            val speaker = FakeSpeaker(log, scope = this, durationOf = { 500L })
            val runner = runner(voiceOn = true, speaker = speaker)

            // "Get ready in" takes 0.5 s, so Jumping jacks runs from 3.5 s to 33.5 s.
            at(4_000)
            assertEquals(ExerciseLabel("Jumping jacks", 1, 3), runner.view.exercise)
            assertEquals(30, runner.view.remainingSeconds)
            at(33_000)
            assertEquals(1, runner.view.remainingSeconds)

            // The round rest runs from 103.5 s to 133.5 s.
            at(104_000)
            assertEquals(PhaseKind.REST, runner.view.kind)
            assertEquals(RoundLabel(2, 2, "Core"), runner.view.nextRound)
            assertEquals(NextExercise("Plank", 60), runner.view.next)

            at(263_000)
            assertEquals("Crunches", runner.title)
            at(263_500)
            assertEquals(RunnerState.Completed(exampleSummary), runner.state.value)
            assertTrue("speak Workout complete. Well done!" in log.actions, "${log.actions}")
            assertTrue("sound" in log.actions)
        }

    // Pause and resume (EX-4, EX-5)

    @Test
    fun `pausing an exercise keeps its time, and resuming counts on from it`() =
        runTest {
            val runner = runner()
            at(10_000)

            runner.pause()
            assertTrue(runner.view.paused)
            assertEquals(23, runner.view.remainingSeconds)

            at(20_000)
            assertEquals(23, runner.view.remainingSeconds)

            runner.resume()
            assertFalse(runner.view.paused)
            at(21_000)
            assertEquals(22, runner.view.remainingSeconds)
            assertEquals("Jumping jacks", runner.title)
        }

    @Test
    fun `pausing get ready keeps its time, and resuming counts on from it`() =
        runTest {
            val runner = runner()
            at(1_500)

            runner.pause()
            assertEquals(PhaseKind.GET_READY, runner.view.kind)
            assertEquals(2, runner.view.remainingSeconds)
            at(5_000)
            assertEquals(2, runner.view.remainingSeconds)

            runner.resume()
            at(6_000)
            assertEquals(1, runner.view.remainingSeconds)
            at(6_500)
            assertEquals("Jumping jacks", runner.title)
        }

    @Test
    fun `pausing a rest keeps its time, and resuming counts on from it`() =
        runTest {
            val runner = runner()
            at(40_000)

            runner.pause()
            assertEquals(PhaseKind.REST, runner.view.kind)
            assertEquals(8, runner.view.remainingSeconds)
            at(50_000)
            assertEquals(8, runner.view.remainingSeconds)

            runner.resume()
            at(51_000)
            assertEquals(7, runner.view.remainingSeconds)
        }

    // Next and Previous (EX-9)

    @Test
    fun `from get ready, Previous does nothing and Next goes to the first exercise`() =
        runTest {
            val runner = runner()
            at(1_000)
            runner.pause()
            assertFalse(runner.view.canGoPrevious)

            runner.previous()
            assertEquals(PhaseKind.GET_READY, runner.view.kind)

            runner.next()
            assertEquals("Jumping jacks", runner.title)
            assertEquals(30, runner.view.remainingSeconds)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `from an exercise, Next and Previous skip the rests, and Previous on the first restarts it`() =
        runTest {
            val runner = runner()
            at(50_000)
            runner.pause()
            assertEquals("Arm circles", runner.title)
            assertEquals(8, runner.view.remainingSeconds)

            runner.next()
            assertEquals("High knees", runner.title)
            assertEquals(40, runner.view.remainingSeconds)

            runner.previous()
            assertEquals("Arm circles", runner.title)
            assertEquals(10, runner.view.remainingSeconds)

            runner.previous()
            assertEquals("Jumping jacks", runner.title)
            runner.previous()
            assertEquals("Jumping jacks", runner.title)
            assertEquals(30, runner.view.remainingSeconds)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `from a rest, Next goes to the exercise after it`() =
        runTest {
            val runner = runner()
            at(40_000)
            runner.pause()

            runner.next()
            assertEquals("Arm circles", runner.title)
            assertEquals(10, runner.view.remainingSeconds)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `from a rest, Previous goes to the exercise before it`() =
        runTest {
            val runner = runner()
            at(40_000)
            runner.pause()

            runner.previous()
            assertEquals("Jumping jacks", runner.title)
            assertEquals(30, runner.view.remainingSeconds)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `Next on the last exercise completes the workout`() =
        runTest {
            val runner = runner()
            at(1_000)
            runner.pause()
            repeat(5) { runner.next() }
            assertEquals("Crunches", runner.title)

            runner.next()
            runCurrent()
            assertEquals(RunnerState.Completed(exampleSummary), runner.state.value)
        }

    @Test
    fun `resuming after Next starts the target exercise`() =
        runTest {
            val runner = runner()
            at(1_000)
            runner.pause()
            runner.next()

            runner.resume()
            at(2_000)
            assertEquals("Jumping jacks", runner.title)
            assertEquals(29, runner.view.remainingSeconds)
        }

    // Stop (EX-8)

    @Test
    fun `Stop asks while paused, Cancel keeps the workout, and Stop ends it`() =
        runTest {
            val runner = runner()
            at(10_000)

            runner.requestStop()
            assertFalse(runner.active.confirmingStop)

            runner.pause()
            runner.requestStop()
            assertTrue(runner.active.confirmingStop)

            runner.cancelStop()
            assertFalse(runner.active.confirmingStop)
            assertTrue(runner.view.paused)

            runner.requestStop()
            runner.confirmStop()
            runCurrent()
            assertEquals(RunnerState.Exited, runner.state.value)
        }

    @Test
    fun `confirming without the dialog open does nothing`() =
        runTest {
            val runner = runner()
            at(10_000)
            runner.pause()

            runner.confirmStop()
            assertTrue(runner.view.paused)
        }

    // Back (Q1)

    @Test
    fun `Back pauses a running workout, then opens the Stop dialog, then closes it`() =
        runTest {
            val runner = runner()
            at(10_000)

            runner.back()
            assertTrue(runner.view.paused)
            assertFalse(runner.active.confirmingStop)

            runner.back()
            assertTrue(runner.active.confirmingStop)

            runner.back()
            assertFalse(runner.active.confirmingStop)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `Back while starting leaves, and the workout never starts`() =
        runTest {
            val gated = GatedSpeaker(speaker)
            val runner = runner(voiceOn = true, speaker = gated)

            runner.back()
            assertEquals(RunnerState.Exited, runner.state.value)

            gated.release()
            at(5_000)
            assertEquals(RunnerState.Exited, runner.state.value)
            assertTrue(log.actions.isEmpty(), "${log.actions}")
        }

    @Test
    fun `Back when the workout can't run leaves`() =
        runTest {
            val runner = runner(workoutId = "missing")

            runner.back()
            assertEquals(RunnerState.Exited, runner.state.value)
        }

    // Foreground (EX-10)

    @Test
    fun `leaving the foreground pauses, and coming back doesn't resume`() =
        runTest {
            val runner = runner()
            at(10_000)

            runner.onScreenStopped()
            assertTrue(runner.view.paused)
            assertEquals(23, runner.view.remainingSeconds)

            at(30_000)
            runner.onScreenStarted()
            assertTrue(runner.view.paused)
            assertEquals(23, runner.view.remainingSeconds)
        }

    @Test
    fun `leaving the foreground keeps a paused workout as it is`() =
        runTest {
            val runner = runner()
            at(10_000)
            runner.pause()
            runner.next()

            runner.onScreenStopped()
            runner.onScreenStarted()
            assertEquals("Arm circles", runner.title)
            assertEquals(10, runner.view.remainingSeconds)
            assertTrue(runner.view.paused)
        }

    @Test
    fun `a workout ready while away waits for the foreground to start`() =
        runTest {
            val gated = GatedSpeaker(speaker)
            val runner = runner(voiceOn = true, speaker = gated)

            runner.onScreenStopped()
            gated.release()
            at(10_000)
            assertEquals(RunnerState.Starting("Example"), runner.state.value)
            assertTrue(log.actions.isEmpty(), "${log.actions}")

            runner.onScreenStarted()
            runCurrent()
            assertEquals(PhaseKind.GET_READY, runner.view.kind)
            assertEquals(3, runner.view.remainingSeconds)
        }

    // Rebuilt after Android closed the app (EX-11)

    @Test
    fun `a rebuilt runner exits at once without reading the workout`() =
        runTest {
            val savedState = SavedStateHandle(mapOf(RunnerViewModel.STARTED_KEY to true))
            val neverLoads = TestWorkoutRepository(listOf(exampleWorkout), loaded = false)

            val runner = runner(repository = neverLoads, savedState = savedState)
            assertEquals(RunnerState.Exited, runner.state.value)
        }

    @Test
    fun `a new runner marks itself started`() =
        runTest {
            val savedState = SavedStateHandle()

            runner(savedState = savedState)
            assertEquals(true, savedState.get<Boolean>(RunnerViewModel.STARTED_KEY))
        }

    // Unavailable

    @Test
    fun `an unknown workout can't run`() =
        runTest {
            assertEquals(RunnerState.Unavailable, runner(workoutId = "missing").state.value)
        }

    @Test
    fun `an invalid workout can't run`() =
        runTest {
            val invalid = workout(round("Only", exercise("Squats", duration = 0)), name = "Invalid")
            val runner = runner(repository = TestWorkoutRepository(listOf(invalid)), workoutId = invalid.id)

            assertEquals(RunnerState.Unavailable, runner.state.value)
        }
}
