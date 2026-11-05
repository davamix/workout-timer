# P7 — Workout runner UI: Specification

| | |
|---|---|
| **Status** | Ready, v0.4 |
| **Phase** | P7, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P4 (done). It also uses P2, P3, P5 and P6, all done. |
| **Branch** | `phase/p7-runner-ui` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

The last two placeholder screens are replaced:

- the **runner**: it runs a workout from the list's Start. Get ready, exercises and rests each have their own colour, and the screen shows the time left in the current phase, the round and the exercise, and what comes next. While running, the only button is Pause. While paused, the buttons are Resume, Previous, Next and Stop, and Stop asks for confirmation.
- the **completed** screen: "Well done!" and a summary of the workout.

The runner's ViewModel owns P4's `WorkoutSession` and wires in P5's audio. At the end of this phase, a workout runs on the tablet with its voice and sound cues. The runner keeps the screen on, pauses when the app leaves the foreground, isn't affected by turning the tablet over, and returns to the workout list if Android closed the app in the background.

Later phases build on it:

| Phase | Uses |
|---|---|
| P8 | the end-to-end test (create a workout in the editor, run it, complete it); the final checks of `NF-1` (a real 60-minute run) and `NF-3` |

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.10.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| UI-1 | "Shows the current **round name**." | The header: "Round 2/3 · Legs" (§4.3.2). |
| UI-2 | "Shows the current **exercise name**." | The title during an exercise (§4.3.2). |
| UI-3 | "Shows the **time**: the remaining time of the **current phase only** (exercise, rest or get ready) as `mm:ss`, rounded up to the whole second. It shows `00:40` at the start of a 40 s exercise and `00:01` during its last second. The remaining time of the whole workout is not shown." | `formatPhaseTime()` of the session's `remainingSeconds`, which P4 already rounds up (§4.3.2). |
| UI-4 | "Has a control to **pause / resume**." | Pause while running, Resume while paused (§4.3.3). |
| UI-5 | "During a rest, shows "Rest" with its remaining time and the name and duration of the **next exercise**. If a new round starts after the rest, the next round's name is shown too." | "REST", the time, "Next: Lunges (40 s)", and "Next round: Core" after a round rest (§4.3.2). |
| UI-6 | "Shows progress: exercise *n* of *m* in the round, round *n* of *m*." | The header: "Round 2/3" on the left, "Exercise 2/4" on the right during an exercise (§4.3.2). |
| UI-7 | "Exercise, rest and paused states each have their own background colour, so the current state is clear from a distance." | `PhaseColors` from P1, with get ready's own colour too (§4.3.9). |
| UI-10 | "While a phase is running, the only button shown is **Pause**." | §4.3.3. |
| UI-11 | "While paused, the buttons shown are **Resume**, **Previous**, **Next** and **Stop**, and the screen clearly says the workout is paused." | The paused layout, with "PAUSED" under the title and the paused colour (§4.3.2, §4.3.3). |
| EX-4 | "The user can **pause** a running exercise and **resume** it. Resuming continues from the remaining time; it does not restart the exercise." | The buttons call the session's `pause()` and `resume()`. P4 keeps the time. |
| EX-5 | "Pause and resume also work during rests and the get-ready countdown." | The Pause button is shown in every running phase. |
| EX-6 | "When the last exercise of the last round finishes, the workout ends and a completion screen is shown." | The runner opens the completed screen when the session completes (§4.3.7, §4.3.8). |
| EX-7 | "A **get-ready** countdown of **3 s** runs before the first exercise of the first round, and only there. The screen shows "Get ready", the countdown, and the name of the first exercise. The voice for it is described in `TTS-10`." | The get-ready look: "GET READY", 00:03 to 00:01, and "Next: Jumping jacks (30 s)" (§4.3.2). The voice is P5's, wired in §4.3.1. |
| EX-8 | "While paused, **Stop** asks for confirmation, then ends the workout and returns to the workout list." | The Stop dialog, then `session.stop()` and back to the list (§4.3.3, §4.3.7). |
| EX-9 | "While paused, **Next** and **Previous** move to another exercise without asking for confirmation (see the table below)." | The buttons call `next()` and `previous()`; Previous is disabled in get ready (§4.3.3). P4 applies the table. |
| EX-10 | "If the app leaves the foreground (Home button, switching apps, screen turned off), the running workout pauses automatically. The workout only runs while the app is on screen." | The runner pauses on its lifecycle's `ON_STOP`, except for a configuration change. A workout still starting waits until the screen is back (§4.3.1, §4.3.4). |
| EX-11 | "If Android closes the app while it's in the background (for example, to free memory), the paused workout is not restored. The app reopens on the workout list." | A runner that Android rebuilds finds a flag in its saved state and returns to the list (§4.3.5). |
| NF-2 | "**Screen stays on** while the runner screen is open." | `FLAG_KEEP_SCREEN_ON` on the window while the runner is shown (§4.3.6). |
| NF-3 | "**Robust to configuration changes:** events such as flipping the tablet between the two landscape orientations don't reset, pause or skip the running workout." | The ViewModel owns the session and survives recreation (plan D-7). The pause on `ON_STOP` skips configuration changes (§4.3.4). P8 checks it again. |
| NF-6 | "**Readability:** the time and exercise name are readable from about 3 m away. Touch targets are large." | Large text, high-contrast colours, buttons of at least 72 dp (§4.3.9). Checked from 3 m on the tablet. |
| NF-9 | "**Language:** the UI is in English. All user-facing text lives in resource files, so a Spanish translation can be added later without code changes." | Every text of the two screens is in `strings_runner.xml` and `strings_completed.xml`. |

P5's cues are wired in (§4.3.1), so `TTS-1`–`TTS-12`, `SND-1` and `SND-2` can be heard in a real workout. They stay P5's requirements. P7 checks by ear that they still work through the runner.

### Specification text this phase implements (quoted)

**§4.3, after the sequencing rules:** "Any phase can be paused. Resuming returns to the same phase with the same remaining time. While paused, you can also move to the next or previous exercise, or stop the workout (`EX-8`, `EX-9`)."

**§5.1, what Next and Previous do:**

> - They move between **exercises** and skip any rest in between.
> - After moving, the workout stays **paused** at the start of the target exercise. Resume starts that exercise as normal, including its name announcement (`TTS-1`).
> - Moving doesn't play the round-end sound (`SND-1`), even when it crosses into another round.
> - Next on the last exercise ends the workout as if it had finished, so "Workout complete. Well done!" is spoken (`TTS-7`).
>
> | Paused during | Next | Previous |
> |---|---|---|
> | Get ready | First exercise | Not available |
> | An exercise | Next exercise; on the last exercise of the workout, ends the workout (completion screen) | Previous exercise; on the first exercise, restarts it |
> | A rest | The exercise after the rest | The exercise before the rest |

**§5.2, workout screen:** "The screen must be readable at a glance from a few metres away while exercising."

**§4.4, the example** used by the tests and the manual checks. Workout "Example" has these defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.

> - Round 1 "Warm-up", own round rest 30 s:
>   - Jumping jacks: 30 s
>   - Arm circles: 10 s, own rest 5 s
>   - High knees: default duration
> - Round 2 "Core":
>   - Plank: 60 s, own rest 30 s
>   - Crunches: default duration
>
> | # | Phase | Round | Name | Duration | Audio |
> |---|---|---|---|---|---|
> | 1 | Get ready | — | — | 3 s | "Get ready in" · 3-2-1 |
> | 2 | Exercise | Warm-up | Jumping jacks | 30 s | "Jumping jacks" · 3-2-1 |
> | 3 | Exercise rest | Warm-up | — | 15 s (default) | silent |
> | 4 | Exercise | Warm-up | Arm circles | 10 s | 3-2-1 (no name: not longer than 10 s) |
> | 5 | Exercise rest | Warm-up | — | 5 s (own) | silent |
> | 6 | Exercise | Warm-up | High knees | 40 s (default) | "High knees" · 3-2-1 |
> | 7 | Round rest | — | — | 30 s (own) | round-end sound, then silent |
> | 8 | Exercise | Core | Plank | 60 s | "Plank" · 3-2-1 |
> | 9 | Exercise rest | Core | — | 30 s (own) | silent |
> | 10 | Exercise | Core | Crunches | 40 s (default) | "Crunches" · 3-2-1 |
> | | **Completed** | | | **Total 4:23** | "Workout complete. Well done!" |

In the timeline, these are phase indexes 0 to 9.

**§6, screens:**

| Screen | Purpose | Main actions |
|---|---|---|
| **Workout runner** | Runs the workout (see §5.1–§5.3) | Running: pause. Paused: resume, previous, next, stop. The system Back button pauses a running workout; on a paused one, it asks to stop, as Stop does (`EX-8`). |
| **Workout completed** | Shows a "Well done!" message and a short summary: the total time, and the number of rounds and exercises. All three are the workout's planned values: the total time is its planned duration (`WM-6`), not the time spent running, and the counts include every round and exercise. Pauses and skipped exercises don't change them. | Back to list |

"All screens are landscape." The tablet's screen is 960 × 600 dp in landscape. The specification's sketches, "low fidelity, layout only":

```
Exercise running:
+--------------------------------------------------------------+
| Round 2/3 · Legs                                Exercise 2/4 |
|                                                              |
|                            SQUATS                            |
|                                                              |
|                            00:27                             |
|                                                              |
|                                                              |
|                         [ || Pause ]                         |
|                                                              |
+--------------------------------------------------------------+

Rest running:
+--------------------------------------------------------------+
| Round 2/3 · Legs                                             |
|                                                              |
|                             REST                             |
|                                                              |
|                            00:12                             |
|                                                              |
|                     Next: Lunges (40 s)                      |
|                                                              |
|                         [ || Pause ]                         |
|                                                              |
+--------------------------------------------------------------+

Paused:
+--------------------------------------------------------------+
| Round 2/3 · Legs                                Exercise 2/4 |
|                                                              |
|                            SQUATS                            |
|                            PAUSED                            |
|                            00:27                             |
|                                                              |
|                                                              |
|  [ |< Previous ]  [ > Resume ]  [ Next >| ]      [ Stop ]    |
|                                                              |
+--------------------------------------------------------------+
```

### Plan text this phase implements (quoted)

Plan §5.2, P7:

> - **Goal:** the runner and completed screens.
> - **Scope:**
>   - **Runner ViewModel:** owns the session.
>   - **Runner screen states:** get ready, exercise, rest and paused, each with its colour.
>   - **Paused controls:** Resume, Previous, Next and Stop, with confirmation for Stop.
>   - **Completed screen.**
>   - **Screen and lifecycle:**
>     - keep the screen on;
>     - pause when the activity stops;
>     - after Android closes the app, it may rebuild the navigation stack. The runner then has no live session, so it returns to the workout list (`EX-11`).
> - **Done when:**
>   - UI tests cover pause, resume, next, previous and stop;
>   - on the tablet, the screen is readable from 3 m, and flipping between landscape orientations keeps the state.

Plan §2.3: "The ViewModel owns the session, so rotating between the two landscape orientations doesn't affect it (`NF-3`). The runner screen keeps the display on (`NF-2`) and pauses the session when the activity stops (`EX-10`)."

| ID | Decision |
|---|---|
| D-3 | "Manual dependency injection through an `AppContainer`." The runner's ViewModel gets the repositories, the clock and the cue player from it. |
| D-6 | "Time comes from a monotonic clock (`SystemClock.elapsedRealtime`) behind a `Clock` interface." The runner passes `container.clock` to the session. |
| D-7 | "The engine is a pure state machine. It exposes its state as a `StateFlow` and its events as a `Flow`. The runner screen's ViewModel owns it." |
| D-8 | "No background service. The workout pauses when the activity stops." |
| D-13 | "Each UI feature keeps its text in its own resource file (`strings_<feature>.xml`)." |

## 3. Scope

### In scope

- **Runner logic:** `RunnerView`, the pure mapping from the session's state to what the screen shows, and `formatPhaseTime()`.
- **Runner ViewModel:** reads and validates the workout, reads the voice setting, prepares the audio, creates and starts the session with the cue player, takes the commands, holds the Stop confirmation, pauses when the screen leaves the foreground, and handles a runner rebuilt after Android closed the app.
- **Runner screen:** the starting, unavailable, running and paused looks; the Stop dialog; system Back; keeping the screen on; pausing on `ON_STOP`; the icons of its buttons.
- **Completed screen:** "Well done!" and the summary, from its route.
- **Navigation:** the runner's and completed screen's entries in `AppNavHost`, and `CompletedRoute`'s arguments. The last placeholder code goes.
- **Device measurement first (T1):** lifecycle events, configuration changes, the screen's content area, and how to make Android close the app, all on the tablet.
- **Tests:** JVM unit tests for the mapping and the ViewModel; Compose UI tests on the tablet; P6's smoke test updated for the real runner.

### Out of scope

| What | Phase |
|---|---|
| The end-to-end test (create in the editor, run, complete) and the acceptance checklist | P8 |
| The final 60-minute accuracy run (`NF-1`) and the final check of `NF-3` | P8 |
| What the cues say and when, the chime, the TTS voice | P5 (done). P7 only wires the cue player in. |
| Timing, transitions and the Next and Previous targets | P4 and P2 (done) |
| Restoring a workout after Android closes the app | Not in v1 (`EX-11`) |
| Running in the background, notifications, lock-screen controls | Not in v1 (`D-8`, `EX-10`) |
| The remaining time of the whole workout; tap anywhere to pause | Discarded (`UI-9`, `UI-8`) |
| Hiding the status and navigation bars (immersive mode) | Not planned: the bars stay, so Home and Back work as everywhere else |
| Workout history; a message when the English voice is missing | Future ideas (specification §10) |
| The audio debug screen | P5's. It stays as it is, in debug builds only. |

## 4. Design

### 4.1 Packages and files owned

`app/src/main/kotlin/io/github/davamix/workouttimer/`:

| File | Contents |
|---|---|
| `ui/runner/RunnerView.kt` | `RunnerView` and its parts, `SessionState.toRunnerView()`, `formatPhaseTime()`. Pure Kotlin. |
| `ui/runner/RunnerViewModel.kt` | `RunnerViewModel`, `RunnerState`, `RunnerActions` |
| `ui/runner/RunnerScreen.kt` | Replaces the placeholder: `RunnerScreen` (the entry), `RunnerContent` (stateless), the Stop dialog |
| `ui/runner/RunnerIcons.kt` | The buttons' icons, drawn as `ImageVector`s (§4.3.9) |
| `ui/completed/CompletedScreen.kt` | Replaces the placeholder: `WorkoutSummary`, `CompletedScreen` and `CompletedContent` |

`app/src/main/res/values/`: `strings_runner.xml` and `strings_completed.xml`, both rewritten.

Tests:

| File | Contents |
|---|---|
| `app/src/test/.../ui/runner/RunnerViewTest.kt` | The mapping on the §4.4 example, and `formatPhaseTime()` |
| `app/src/test/.../ui/runner/RunnerViewModelTest.kt` | The ViewModel with virtual time, the in-memory repositories and P5's audio fakes |
| `app/src/test/.../ui/runner/GatedSpeaker.kt` | A `Speaker` whose `prepare()` waits until the test lets it go, delegating the rest to P5's `FakeSpeaker` |
| `app/src/androidTest/.../ui/runner/RunnerContentTest.kt` | `RunnerContent` with fixed states |
| `app/src/androidTest/.../ui/runner/RunnerScreenTest.kt` | `RunnerScreen` with a real ViewModel, in-memory repositories and silent audio |
| `app/src/androidTest/.../ui/runner/RunnerActivityTest.kt` | The runner inside `MainActivity`: recreation, stopping, the keep-screen-on flag |
| `app/src/androidTest/.../ui/runner/SilentAudio.kt` | A `Speaker` and a `SoundPlayer` that play nothing |
| `app/src/androidTest/.../ui/completed/CompletedScreenTest.kt` | The completed screen |

**Shared files this phase changes**, only in their runner and completed parts:

- `navigation/Routes.kt`: `CompletedRoute` becomes a data class carrying the summary (§4.2).
- `navigation/AppNavHost.kt`: the runner's and completed screen's entries (§4.3.7).
- `ui/theme/Color.kt`: `PhaseColors` gains the text colour of each state. Its four background colours may change after the 3 m check, within the contrast rule (§4.3.9).
- `ui/common/PlaceholderScreen.kt` is deleted, with `placeholder_note` in `res/values/strings.xml`. P6 left them for P7 to remove with the last placeholder.
- `app/src/androidTest/.../NavigationSmokeTest.kt`: its two runner tests (§6).
- Only if needed (§4.4): `gradle/libs.versions.toml` and `app/build.gradle.kts`, for `lifecycle-viewmodel-savedstate`.

Not changed: `:core`, `AppContainer` (it already has everything the runner needs), `MainActivity` and the manifest.

### 4.2 Interfaces and data

#### Screens, as `AppNavHost` calls them

```kotlin
// ui/runner/RunnerScreen.kt
@Composable
fun RunnerScreen(
    workoutId: String,
    onCompleted: (WorkoutSummary) -> Unit,   // EX-6: open the completed screen
    onExit: () -> Unit,                      // back to the workout list (EX-8, EX-11, Back)
    viewModel: RunnerViewModel = viewModel(factory = RunnerViewModel.factory(workoutId)),
)

/** Draws [state]. Every action goes to [actions]. */
@Composable
fun RunnerContent(state: RunnerState, actions: RunnerActions)

// ui/completed/CompletedScreen.kt
@Composable
fun CompletedScreen(summary: WorkoutSummary, onBackToList: () -> Unit)
```

The completed screen has no ViewModel: its route carries everything it shows (§4.3.8). `CompletedScreen` is then just the entry that `AppNavHost` calls, and `CompletedContent(summary, onBackToList)` draws it.

#### Routes: `navigation/Routes.kt`

```kotlin
@Serializable
data class CompletedRoute(
    val workoutName: String,
    val totalSeconds: Int,
    val roundCount: Int,
    val exerciseCount: Int,
)
```

`RunnerRoute(workoutId)` doesn't change. `AppNavHost` maps between `CompletedRoute` and `WorkoutSummary`.

#### What the screen shows: `ui/runner/RunnerView.kt`, pure Kotlin

```kotlin
/** The look of a phase (UI-7). Paused is [RunnerView.paused], on top of it. */
enum class PhaseKind { GET_READY, EXERCISE, REST }

/**
 * What the runner shows for one session state (§4.3.2). Built only for RUNNING and PAUSED states.
 *
 * @property remainingSeconds The session's, already rounded up (UI-3). The screen formats it with [formatPhaseTime].
 * @property round The header's round (UI-1, UI-6). For a rest, the round of the exercise before it (Q4).
 * @property exercise During an exercise: its name, and its position in the round (UI-2, UI-6). Otherwise null.
 * @property next During get ready and rests: the exercise that comes next (EX-7, UI-5). Otherwise null.
 * @property nextRound During a round rest: the round that starts after it (UI-5). Otherwise null.
 * @property canGoPrevious The session's: false in get ready (EX-9).
 */
data class RunnerView(
    val kind: PhaseKind,
    val paused: Boolean,
    val remainingSeconds: Int,
    val round: RoundLabel,
    val exercise: ExerciseLabel?,
    val next: NextExercise?,
    val nextRound: RoundLabel?,
    val canGoPrevious: Boolean,
)

data class RoundLabel(val number: Int, val count: Int, val name: String)
data class ExerciseLabel(val name: String, val number: Int, val count: Int)
data class NextExercise(val name: String, val durationSeconds: Int)

fun SessionState.toRunnerView(): RunnerView

/** A phase's remaining time as `mm:ss` (UI-3): 40 gives "00:40", 1 gives "00:01", 5999 gives "99:59". Pads with `padStart`. */
fun formatPhaseTime(seconds: Int): String
```

The fields come from P2's `ExerciseInfo` (§4.5): `roundNumber`, `roundCount`, `roundName`, `numberInRound`, `countInRound`, `name` and `durationSeconds`.

#### Runner ViewModel: `ui/runner/RunnerViewModel.kt`

```kotlin
/** What the runner shows, or what it has finished with. */
sealed interface RunnerState {
    /** Reading the workout, then preparing the audio (§4.3.1). [workoutName] once the workout is read. */
    data class Starting(val workoutName: String? = null) : RunnerState

    /** No saved workout has this ID, or it isn't valid, so it can't run (§4.3.1). */
    data object Unavailable : RunnerState

    /** The workout is running or paused. [confirmingStop]: the Stop dialog is open (EX-8). */
    data class Active(val view: RunnerView, val confirmingStop: Boolean = false) : RunnerState

    /** The workout completed (EX-6). The screen calls `onCompleted(summary)` once. */
    data class Completed(val summary: WorkoutSummary) : RunnerState

    /**
     * The runner is done without completing: stopped (EX-8), left while starting or unavailable, or rebuilt after
     * Android closed the app (EX-11). The screen calls `onExit()` once.
     */
    data object Exited : RunnerState
}

/** What the runner screen can ask for. The ViewModel does it; `RunnerContent` only calls it. */
interface RunnerActions {
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun requestStop()   // opens the Stop dialog, while paused
    fun confirmStop()   // stops the workout
    fun cancelStop()    // closes the dialog; still paused
    fun back()          // system Back, and "Back to list" when unavailable (§4.3.3)
}

class RunnerViewModel(
    workoutId: String,
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    clock: Clock,
    cuePlayer: CuePlayer,
    savedStateHandle: SavedStateHandle,
    audioDispatcher: CoroutineDispatcher = Dispatchers.Default,   // where cuePlayer.prepare() runs; tests pass theirs
) : ViewModel(), RunnerActions {
    val state: StateFlow<RunnerState>

    /** The screen left the foreground (EX-10): pauses a running workout. Not called for a configuration change. */
    fun onScreenStopped()

    /** The screen is back in the foreground. A workout still starting may start now; a paused one stays paused. */
    fun onScreenStarted()

    companion object {
        /** Builds it with the app container's repositories, clock and cue player, and the entry's saved state. */
        fun factory(workoutId: String): ViewModelProvider.Factory
    }
}
```

The factory follows P6's pattern: `viewModelFactory { initializer { … } }`, with the container from `(this[APPLICATION_KEY] as WorkoutTimerApp).container` and the saved state from `createSavedStateHandle()`.

The ViewModel takes the real `CuePlayer`. Unit tests build one with P5's `FakeSpeaker` and `FakeSoundPlayer`, so they also check the wiring. Instrumented tests build one with `SilentAudio.kt`.

#### Completed summary: `ui/completed/CompletedScreen.kt`

```kotlin
/**
 * What the completed screen shows (specification §6). [totalSeconds] is the planned total, the timeline's (P4 Q2).
 * [roundCount] and [exerciseCount] are the workout's (Q2).
 */
data class WorkoutSummary(
    val workoutName: String,
    val totalSeconds: Int,
    val roundCount: Int,
    val exerciseCount: Int,
)
```

#### Main texts

They are suggestions. `strings_runner.xml` and `strings_completed.xml` hold the final wording, which may change during implementation without a new SPEC version. Only quoted specification text is fixed: "Well done!", and the button names Pause, Resume, Previous, Next and Stop.

| Where | Text |
|---|---|
| Header | "Round 2/3 · Legs", "Exercise 2/4" |
| Titles | "GET READY", "REST"; an exercise's name as written (Q6) |
| Paused label | "PAUSED" |
| Below the time | "Next: Lunges (40 s)", "Next round: Core" |
| Buttons | "Pause", "Resume", "Previous", "Next", "Stop" |
| Stop dialog | "Stop the workout?", "You'll go back to the workout list.", "Stop", "Cancel" |
| Starting | "Getting ready…" under the workout's name |
| Unavailable | "This workout can't be started.", "Back to list" |
| Completed | "Well done!", "Example is complete.", "Total time 4:23 · 2 rounds · 5 exercises", "Back to list" |

### 4.3 Behaviour

#### 4.3.1 Starting a workout

When the runner opens, its ViewModel does this, in `viewModelScope`:

1. **Rebuilt after Android closed the app?** If the saved state's `started` flag is set, the state becomes `Exited` at once, and nothing else happens (§4.3.5).
2. Otherwise it sets the flag, and the state is `Starting()`.
3. **Reads the workout:** `workoutRepository.get(workoutId)`. If there's none, or `validate()` reports errors, the state becomes `Unavailable`. `Timeline.of` throws on an invalid workout (P3 Q2), so it's never called with one.
4. The state becomes `Starting(workout.name)`. It reads the voice setting once: `settingsRepository.settings.first().voiceCuesEnabled` (`TTS-9`).
5. **Prepares the audio:** `withContext(audioDispatcher) { cuePlayer.prepare(voiceOn) }`. That's `Dispatchers.Default`, as `MainActivity` and P6's settings screen do, because loading the chime and connecting the TTS engine shouldn't run on the main thread. It usually returns at once. Within about 10 s of a cold start it can wait up to 10 s (P5).
6. **Waits for the foreground.** If the screen left the foreground meanwhile (`onScreenStopped()` without `onScreenStarted()` since), it waits until it's back. A workout never starts while the app is away (`EX-10`).
7. **Starts:**

   ```kotlin
   val session = WorkoutSession(Timeline.of(workout), clock, viewModelScope, waitForAnnouncement = voiceOn)
   val playing = viewModelScope.launch { cuePlayer.play(session, voiceOn) }
   // collect session.state into RunnerState.Active (§4.3.2) and the outcome (§4.3.7)
   session.start()
   ```

   `waitForAnnouncement` and `play()` get the same value (P5). The runner reads only `session.state`: the cue player is the only collector of `session.events`.

**While starting** (Q3), the screen has get ready's background, the workout's name once it's read, a progress indicator and "Getting ready…". There's no Pause button, because nothing runs yet. Back returns to the list (`Exited`). Leaving cancels `viewModelScope`, and with it the wait for the audio. The TTS warm-up itself carries on (P5).

**Unavailable:** the theme's background, "This workout can't be started." and "Back to list". The button and system Back both return to the list. In practice it can't happen: the editor only saves valid workouts, and the list only shows saved ones.

#### 4.3.2 What the screen shows

Every value comes from `session.state` through `toRunnerView()`. The screen never counts time itself: P4 publishes a new state at least once per displayed second.

| Phase | Background | Header, left | Header, right | Title | Below the time |
|---|---|---|---|---|---|
| Get ready | get ready | the first exercise's round: "Round 1/2 · Warm-up" | — | "GET READY" | "Next: Jumping jacks (30 s)" (`EX-7`) |
| Exercise | exercise | its round: "Round 1/2 · Warm-up" | "Exercise 1/3" | its name: "Jumping jacks" | — |
| Exercise rest | rest | its round | — | "REST" | "Next: Arm circles (10 s)" |
| Round rest | rest | the round that just ended (Q4) | — | "REST" | "Next round: Core", then "Next: Plank (60 s)" (`UI-5`) |
| Paused, in any of these | paused | as the phase | as the phase | as the phase, with "PAUSED" under it (`UI-11`) | as the phase |

- **The time** is `formatPhaseTime(remainingSeconds)`: always `mm:ss`, two digits each. A phase lasts at most 99:59 (`WM-7`), so the minutes never need a third digit. Get ready shows 00:03, 00:02 and 00:01. While "Get ready in" is spoken, it stays at 00:03 (P4's `awaitingAnnouncement`).
- **Durations after "Next:"** are in seconds, "(40 s)" or "(90 s)", as the editor takes them (P6 Q3).
- **After Next or Previous,** the session is paused at the start of the target exercise: the screen shows that exercise with its full duration, paused (`EX-9`).
- **Long names:** a title stays on one line. It shrinks to a minimum size, then ends in an ellipsis. Round names in the header and names after "Next:" are cut with an ellipsis.

Get ready and a round rest, suggested from the specification's sketches:

```
Get ready:
+--------------------------------------------------------------+
| Round 1/2 · Warm-up                                          |
|                                                              |
|                          GET READY                           |
|                            00:03                             |
|                                                              |
|                  Next: Jumping jacks (30 s)                  |
|                                                              |
|                         [ || Pause ]                         |
+--------------------------------------------------------------+

Round rest:
+--------------------------------------------------------------+
| Round 1/2 · Warm-up                                          |
|                                                              |
|                             REST                             |
|                            00:30                             |
|                                                              |
|                       Next round: Core                       |
|                      Next: Plank (60 s)                      |
|                                                              |
|                         [ || Pause ]                         |
+--------------------------------------------------------------+
```

#### 4.3.3 Controls

- **Running** (`UI-10`): only Pause, large and centred under the time.
- **Paused** (`UI-11`): one row of buttons, as in the sketch: Previous, Resume and Next, then a gap, and Stop at the right end (Q7). With Stop in the same row rather than under it, the time keeps room to stay large while paused. Resume sits a little left of where Pause was.
- **Previous in get ready** is shown disabled (Q5), from `canGoPrevious`, so the layout doesn't change between phases. The specification says it's "Not available" there (`EX-9`).
- **Next and Previous** call the session at once, without asking (`EX-9`).
- **Stop** opens a dialog (`EX-8`): "Stop the workout?", with Stop and Cancel. Stop calls `session.stop()`, and the runner returns to the list (§4.3.7). Cancel, Back or a tap outside the dialog closes it, and the workout stays paused. The dialog's state is in the ViewModel, so it survives a configuration change.
- **Commands that don't apply** are ignored by the session (P4), and by the ViewModel when it isn't `Active`. `requestStop()` does nothing while running, because Stop isn't shown then.
- **System Back** (Q1), through `BackHandler` while the state is neither `Completed` nor `Exited`:

  | State | Back |
  |---|---|
  | Starting, Unavailable | returns to the list (`Exited`) |
  | Running | pauses, as the Pause button does |
  | Paused | opens the Stop dialog |
  | Stop dialog open | closes it (the dialog handles it) |

  So an accidental Back never ends a workout without the confirmation `EX-8` asks for.

#### 4.3.4 Leaving the foreground, and configuration changes (`EX-10`, `NF-3`)

- `RunnerScreen` calls `onScreenStopped()` on its lifecycle's `ON_STOP`, and `onScreenStarted()` on `ON_START`, with `LifecycleEventEffect`. The lifecycle is the navigation entry's, which follows the activity's. `ON_STOP` comes when the app leaves the screen: Home, Recents, another app, and the screen turned off (measured in T1, below).
- `onScreenStopped()` calls `session.pause()` if it's running. A paused workout stays as it is. When the app comes back, the workout is paused and shows the paused controls: nothing resumes by itself.
- **A configuration change also stops the activity,** just before it's recreated. The screen then doesn't call `onScreenStopped()`: it checks `LocalActivity.current?.isChangingConfigurations`, which Android sets for exactly this case, and which its documentation suggests checking in `onStop`. On the tablet it's `true` in `onPause`, `onStop` and `onDestroy` (T1). The ViewModel, the session and its timer survive the recreation (D-7), so nothing resets, pauses or skips (`NF-3`), and the new screen shows the same state.
- There's no foreground service and nothing runs in the background (D-8).

**Measured in T1** on the tablet, with a probe logging the activity's callbacks. Home, Recents and the power button were each tried three times, the flip once:

| What | Callbacks |
|---|---|
| Home | `onPause`, then `onStop` 300–560 ms later. `isChangingConfigurations` is `false`. |
| Recents | `onPause`, then `onStop` 60–70 ms later; `false` |
| Power button (screen off) | `onPause`, then `onStop` 50–70 ms later; `false`. The tablet has no lock screen, so waking it resumes the app directly. |
| Notification shade, pulled down and back up | none |
| Turning the tablet over (180°), and back | none. Only the display's rotation changes (1 → 3 → 1): no configuration change, no recreation. |
| `activity.recreate()`, as `ActivityScenario.recreate()` does | `onPause`, `onStop` and `onDestroy`, all with `isChangingConfigurations` `true`, then a new activity with saved state |

So all three `EX-10` cases give `ON_STOP`, and turning the tablet over never reaches the runner. The `isChangingConfigurations` check still matters for the recreation `RunnerActivityTest` forces, and for any other configuration change that recreates the activity while the runner is shown.

#### 4.3.5 After Android closes the app (`EX-11`)

- When Android ends the app's process in the background, it can later rebuild the activity from its saved state, and Navigation rebuilds the back stack: the list with the runner on top. The runner's ViewModel is then new, and there's no session.
- The ViewModel keeps one flag in its `SavedStateHandle`: `started`, set the first time it opens (§4.3.1, step 2). A rebuilt runner finds it set and becomes `Exited`, so the screen returns to the list. A runner opened from Start never has it.
- A configuration change keeps the ViewModel, so it never gets here.
- Nothing else is saved: the specification says the paused workout isn't restored.
- **Measured in T1,** with the runner placeholder: after the cached process was ended, reopening the app from the launcher or from Recents started a new process, and Navigation rebuilt the list with the runner on top. So this case does happen on the tablet.
- **Making it happen** for the manual check: `adb shell am kill` doesn't end the process on this tablet, even once it's cached. Instead, press Home, open another app (Settings), and press Home again, so the timer is no longer the "previous" app. Then run `adb shell run-as io.github.davamix.workouttimer.debug kill -9 <pid>`, with the PID from `pidof`. The system logs "Process … has died: cch CAC", as for a low-memory kill, and the task stays in Recents.

#### 4.3.6 Keeping the screen on (`NF-2`)

- While `RunnerScreen` is in the composition, from `Starting` on, the activity's window has `FLAG_KEEP_SCREEN_ON`. A `DisposableEffect` adds it, and its `onDispose` clears it. That holds while running and while paused.
- The flag needs no permission, unlike a wake lock (`NF-4`).
- On a configuration change, the old screen clears the flag on the old window, and the new one sets it on the new window.
- The completed screen doesn't keep the screen on. `NF-2` is about the runner.

#### 4.3.7 Finishing, and navigation

- **Completed** (`EX-6`): when the session's state is `COMPLETED`, from the last exercise ending or from Next on it, the ViewModel waits for `cuePlayer.play()` to return, then becomes `Completed(summary)`. `play()` returns once it has handled the `Completed` event, so "Workout complete. Well done!" has been handed to the speaker, and it carries on after the runner goes (P5).
- **The summary:** the workout's name; `timeline.totalSeconds`, the planned total (specification §6, P4 Q2); the number of rounds (`ExerciseInfo.roundCount`) and of exercise phases in the timeline (Q2). The §4.4 example gives "Example", 263, 2 and 5.
- **Stopped** (`EX-8`): when the state is `STOPPED`, the ViewModel becomes `Exited`.
- **The screen reports each outcome once,** from a `LaunchedEffect` on it, **after waiting until its lifecycle is `RESUMED`**. A runner rebuilt after Android closed the app (§4.3.5) is composed before its navigation entry is resumed. With `dropUnlessResumed`, its `onExit()` would be dropped and the screen would stay empty. So `AppNavHost` passes plain lambdas for `onCompleted` and `onExit`. Buttons that navigate, such as the completed screen's "Back to list", keep `dropUnlessResumed` (P6, §4.5).
- **`AppNavHost`:**
  - `onCompleted(summary)` navigates to `CompletedRoute(…)` with `popUpTo<RunnerRoute> { inclusive = true }`, as the placeholder does. Back on the completed screen then returns to the list.
  - `onExit()` pops back to `WorkoutListRoute`.
  - The completed screen's `onBackToList` stays as it is: `popBackStack(WorkoutListRoute, inclusive = false)`.
- Leaving the runner by any route clears its ViewModel. `viewModelScope` is cancelled, which stops the session's timer and the cue player's `play()`.

#### 4.3.8 Completed screen

```
+--------------------------------------------------------------+
|                                                              |
|                          Well done!                          |
|                                                              |
|                     Example is complete.                     |
|           Total time 4:23 · 2 rounds · 5 exercises           |
|                                                              |
|                       [ Back to list ]                       |
|                                                              |
+--------------------------------------------------------------+
```

- The total uses P6's `formatTotalDuration()`: "4:23", or "1:01:03" from an hour.
- Rounds and exercises use plural resources (`pluralStringResource`): "1 round", "2 rounds".
- "Back to list" and system Back both return to the list. The runner is no longer on the back stack.
- It has the theme's background, with "Well done!" large, in the theme's primary colour.
- Everything it shows comes in its route, so it also comes back as it was if Android closes the app while it's open.

#### 4.3.9 Look: colours, text and touch targets (`UI-7`, `NF-6`)

**Colours.** The backgrounds are P1's `PhaseColors`. Each state's text has a contrast of at least 4.5:1 with its background:

| State | Background | Text | Contrast |
|---|---|---|---|
| Get ready | `#6A1B9A`, purple | white | 9.4:1 |
| Exercise | `#2E7D32`, green | white | 5.1:1 |
| Rest | `#1565C0`, blue | white | 5.8:1 |
| Paused | `#EF6C00`, orange | black | 6.8:1. White would be only 3.1:1. |

P7 adds the text colours to `PhaseColors`. After the 3 m check (T7), the colours may change, as long as each state keeps a clearly different background and the 4.5:1 rule.

**Buttons.** The main action (Pause, Resume) is filled in the state's text colour, with the background colour as its content. Previous, Next and Stop are outlined in the text colour. Disabled Previous is dimmed. Each button shows an icon and its text. The icon is decorative (`contentDescription = null`), because the text names the action.

**Icons.** `material-icons-core` has only `PlayArrow` of the five, and the extended icon library is very large. `RunnerIcons.kt` draws all five as `ImageVector`s from simple shapes: two bars (Pause), a triangle (Resume), a bar and a triangle (Previous), a triangle and a bar (Next), and a square (Stop). It's our own drawing, so no third-party artwork comes into the repo, as with the sound (D-10).

**Text sizes,** as fitted on the tablet in T4. T7 confirms them from 3 m:

| Element | Size |
|---|---|
| Time | Bold. It fills the height the other elements leave, between 48 and 220 sp: about 200 sp in an exercise and 170 sp in a round rest while running; about 200 sp and 120 sp while paused. |
| Title (name, "GET READY", "REST") | Bold, one line in an 80 dp box: up to 72 sp, shrinking to 36 sp for long names, then cut with an ellipsis |
| "PAUSED" | 36 sp, bold |
| Header, "Next:" lines | 30 sp, one line each, cut with an ellipsis |
| Button labels | 28 sp, with a 36 dp icon |

Rules:

- The time is always the largest text. Its size changes with the layout, between an exercise and a rest and when pausing, never while it counts.
- Its digits all have the same width, so the time doesn't shift as it counts. Roboto's do; keep it so.
- Every state fits on screen without scrolling, in the content area of 960 × 542 dp measured in T1: 600 dp minus a 24 dp status bar and a 33 dp navigation bar.
- Sizes are in `sp`, like the rest of the app.

**Touch targets.** Every runner button is at least 72 dp tall and wide, and Pause and Resume at least 96 dp tall. The Stop dialog's buttons are at least 56 dp tall. They are used in the middle of an exercise, from arm's length. P6's minimum is 48 dp.

#### 4.3.10 Text (`NF-9`)

Every text and content description of the two screens is in `strings_runner.xml` and `strings_completed.xml`, with format arguments for numbers and names, and plurals for counts. The Kotlin code builds no user-facing text. `formatPhaseTime()` and `formatTotalDuration()` produce only digits and colons.

### 4.4 Dependencies

**No new library is expected.** The runner uses:

| Library | Version | For |
|---|---|---|
| `activity-compose` | 1.13.0 | `LocalActivity` (since 1.10), `BackHandler` |
| `lifecycle-runtime-compose` | 2.11.0 | `LifecycleEventEffect`, `collectAsStateWithLifecycle` |
| `lifecycle-viewmodel-compose` | 2.11.0 | `viewModel()`, `viewModelFactory`, `createSavedStateHandle()` |
| Material 3, from Compose BOM 2026.09.00 | | `AlertDialog`, `Button`, `OutlinedButton`, `CircularProgressIndicator` |

`SavedStateHandle` and `createSavedStateHandle()` come from `lifecycle-viewmodel-savedstate`, which `navigation-compose` brings in. If it isn't on `:app`'s compile classpath, add `androidx.lifecycle:lifecycle-viewmodel-savedstate` to the catalog with `version.ref = "lifecycle"`, and record it in the handover.

**Interfaces from other phases**, all merged:

| Phase | What |
|---|---|
| P4 | `WorkoutSession` (its constructor, `start`, `pause`, `resume`, `next`, `previous`, `stop`, `state`), `SessionState`, `SessionStatus` |
| P2 | `Timeline.of`, `Timeline.totalSeconds`, `Phase` (`GetReady`, `Exercise`, `Rest`), `RestKind`, `ExerciseInfo`, `Workout.validate()` |
| P5 | `CuePlayer.prepare` and `CuePlayer.play`; `Speaker` and `SoundPlayer`, for the test doubles |
| P3 | `container.workoutRepository` and `container.settingsRepository` |
| P6 | `formatTotalDuration()`; the test helpers `MainDispatcherExtension`, `TestRepositories.kt`, `UiTestWorkouts.kt` and `TouchTargets.kt` |
| P1 | `PhaseColors`, the theme |

### 4.5 From earlier phases

What earlier phases settled about the code this phase uses, from their SPECs and handovers:

- **P4, the session:**
  - "A command that doesn't apply in the current status does nothing." Commands apply in call order, and each one first applies the time that has passed.
  - "On one thread with no collector calling back, as in P7's UI, every command is applied before it returns." A command from another thread may return before it's applied. The cue player calls `announcementFinished()` from a TTS thread, and the session handles that.
  - `remainingSeconds` is "rounded up to the whole second (UI-3): the phase's duration at its start, and 1 during its last second. 0 once COMPLETED". While `awaitingAnnouncement`, "`remainingSeconds` stays at 3".
  - `canGoPrevious` is "whether `previous()` would move: the session is paused and not in get ready".
  - The `scope` passed to the session runs its timer: "Cancelling it stops the timer and completes events."
  - The announcement timeout is 5 s (`ANNOUNCEMENT_TIMEOUT_MILLIS`): get ready never waits longer for "Get ready in".
  - The completed screen's total is the planned one, `timeline.totalSeconds` (P4 Q2, specification v0.7).
- **P4 and P5, pending wiring, completed here:**
  - P4: "`waitForAnnouncement` stays `false` until the audio exists. P8, or P7 if P5 is merged first, passes the voice setting, and P5's cue player calls `announcementFinished()`."
  - P5: "Read the voice setting once when the workout starts. Call `cuePlayer.prepare(voiceOn)`, create the `WorkoutSession` with `waitForAnnouncement = voiceOn`, launch `cuePlayer.play(session, voiceOn)` in `viewModelScope`, then call `session.start()`. The runner reads `session.state` only: the cue player is the only collector of `session.events`." And: "`prepare()` usually returns at once, but within ~10 s of a cold start it can wait up to 10 s. There's no session yet while it waits, so P7 decides what the screen shows meanwhile." §4.3.1 does it.
- **P5, the cue player:**
  - "Speech already handed to the speaker when it returns, or when it's cancelled, carries on, so 'Workout complete. Well done!' isn't cut off when the runner leaves."
  - Pausing and stopping stop speech (`TTS-3`). With voice cues off, the speaker is never called, not even to stop.
  - If the speaker isn't READY, the cue player calls `announcementFinished()` at once, so get ready doesn't wait.
  - Measured cold start, with the warm-up: `prepare()` took 9.1–10.0 s. `MainActivity` starts preparing when it's created, so a workout started later waits only for the rest of it.
  - `CuePlayer` plays one workout at a time. It's a single instance in `AppContainer`.
- **P6, pending wiring, completed here:**
  - "the list's Start opens `RunnerRoute(workoutId)`". Already wired in `AppNavHost`.
  - "the runner must validate the workout before building a timeline, because `Timeline.of` throws on an invalid one (P3 Q2). It also decides what to show for an invalid or missing workout." See §4.3.1.
  - "`formatTotalDuration()` (`ui/common/TotalDuration.kt`) is there for the completed screen's planned total."
  - "`PlaceholderScreen` and `placeholder_note` are still used by the runner and completed placeholders. P7 deletes them with the last placeholder."
  - "`NavigationSmokeTest` seeds a workout (`smoke-test-workout`), presses its Start, and then clicks the runner placeholder's Finish (`runner_finish`). P7 updates those two runner tests for the real runner."
- **P6, patterns to follow:**
  - Each screen has an entry composable taking its ViewModel, and a stateless `…Content`. With many callbacks, the content takes an actions interface (`EditorActions`), which the ViewModel implements.
  - Callbacks that navigate on a click go through `dropUnlessResumed`, or `NavBackStackEntry.ifResumed` in `AppNavHost` when they take an argument, so a double tap can't open a screen twice. State-driven navigation is different (§4.3.7).
  - `SettingsViewModel` runs `prepareAudio` on `Dispatchers.Default`, through its factory.
  - `formatTotalDuration` pads with `padStart`: `String.format` without a locale triggers Lint's `DefaultLocale`.
  - `assertTouchTargetsAtLeast(min)` checks the touch bounds of the clickable nodes fully on screen. Material 3 icon buttons lay out at 40 dp but take touches over 48 dp.
- **P6, tests:**
  - `TestWorkoutRepository` can hold back its data (`loaded = false`), and its `get()` then waits until `finishLoading()`.
  - The in-memory `InMemoryWorkoutRepository` and `InMemorySettingsRepository` are in `:core`'s main code, so instrumented tests can use them too.
  - `uiExampleWorkout` in `UiTestWorkouts.kt` is the §4.4 example for instrumented tests.
  - Texts are matched through `getString(R.string.…)`, never as literals. Rows carry test tags.
  - "`connectedDebugAndroidTest` wipes the user's workouts in the debug app on the tablet. To keep them, run single classes with `am instrument` (tablet skill)."
- **P5 and P6, audio fakes:** `AudioFakes.kt` (`app/src/test/.../audio/`) has `AudioLog`, `FakeSpeaker` (it logs `speak <text>`, and its speech finishes when the test calls `finish(text)`), `FakeSoundPlayer` and `testText(utterance)`. They're P5's test code: P7 uses them and doesn't change them. Its own `GatedSpeaker` wraps a `FakeSpeaker`.
- **P2, the timeline:** rest phases are never 0 s (rests of 0 s produce no phase); `Phase.Rest` has `kind`, `previous` and `next`; `ExerciseInfo` numbers are 1-based.
- **P3:** `FileWorkoutRepository.get(id)` returns `null` for an unknown ID and doesn't throw: damaged files are skipped when loading.
- **P1:** the first launch after installing takes 30–75 s on the tablet, while Android 8 compiles the debug APK. Later cold starts take about 2 s.

**Testing pitfall, from P4.** In the ViewModel tests, the session runs in `viewModelScope` on the test `Main` dispatcher, not in `backgroundScope`. So `advanceUntilIdle()` does run its timer, and runs the whole workout to its end. Move time with `advanceTimeBy()` and `runCurrent()` instead. The clock is `Clock { testScheduler.currentTime }`.

## 5. Security and privacy

Plan §3 applies. For this phase:

- **No new permission or component.** `FLAG_KEEP_SCREEN_ON` needs no permission; a wake lock would need `WAKE_LOCK`, so it isn't used. `./gradlew build` already fails on `INTERNET`. Also compare the merged debug and release manifests with `main`'s.
- **Saved state holds almost nothing:** the runner's `started` flag, and the routes' arguments (the workout's ID; the completed screen's workout name and numbers). Android keeps it in the app's private saved state.
- **Nothing leaves the app,** and no workout data is written: the runner only reads the workout and the voice setting.
- **No logging.** P7 adds no logs.
- **Tests clean up.** Tests on the app's real storage seed workouts with fixed IDs and delete them afterwards. `RunnerActivityTest` turns voice cues off for its run, and restores the stored value afterwards.

## 6. Testing

Unit tests run in `./gradlew build` and in CI. The ViewModel tests register `MainDispatcherExtension`, use the in-memory repositories from `TestRepositories.kt`, P5's audio fakes, and virtual time (§4.5's pitfall).

| Test | Type | Covers |
|---|---|---|
| `RunnerViewTest`:<br>• every phase of the §4.4 example's timeline, as the §4.3.2 table says: get ready (round 1/2 Warm-up, next Jumping jacks 30 s); Jumping jacks (exercise 1/3); the rest after it (next Arm circles 10 s); the round rest at index 6 (round Warm-up, next round Core 2/2, next Plank 60 s); Crunches (round 2/2, exercise 2/2)<br>• `paused` and `canGoPrevious` come from the state<br>• `formatPhaseTime`: 40 → "00:40", 1 → "00:01", 0 → "00:00", 59 → "00:59", 60 → "01:00", 5999 → "99:59" | unit | UI-1, UI-2, UI-3, UI-5, UI-6, EX-7 |
| `RunnerViewModelTest`:<br>• **start:** `Starting`, then the name while the audio prepares (`GatedSpeaker`), then `Active` in get ready at 3 s<br>• **voice on:** get ready stays at 3 until the fake speaker finishes "Get ready in", then counts down. **Voice off:** it counts down at once, and the speaker is never called.<br>• **the whole §4.4 example** in virtual time: 0.5 s into Jumping jacks shows 30, its last second shows 1; the round rest shows next round Core; at the end, `Completed("Example", 263, 2, 5)`, and only after the speaker logged "Workout complete. Well done!"<br>• **pause and resume** in get ready, an exercise and a rest: the time is kept, then counts on<br>• **Next and Previous** from get ready, an exercise, a rest, the first and the last exercise, as the `EX-9` table says; paused at the target's full duration; Next on the last gives `Completed`<br>• **Stop:** `requestStop` while running does nothing; while paused it opens the dialog; `cancelStop` closes it, still paused; `confirmStop` gives `Exited`<br>• **Back:** as the §4.3.3 table says, in each state<br>• **foreground:** `onScreenStopped` pauses a running workout and leaves a paused one; `onScreenStarted` doesn't resume. Stopped while starting, the workout doesn't start when the audio is ready, only after `onScreenStarted`.<br>• **rebuilt** (`EX-11`): with `started` already in the saved state, `Exited` at once, and the repository isn't read (a `TestWorkoutRepository` that never loads). A new runner sets the flag.<br>• **unavailable:** an unknown ID; a workout with a 0 s duration | unit | EX-4–EX-11, UI-3, UI-4, the P4 and P5 wiring |
| `RunnerContentTest`, with fixed states:<br>• each look of §4.3.2 shows its texts: header, title, time, "Next:" lines, "PAUSED"<br>• running shows only Pause; paused shows Resume, Previous, Next and Stop; Previous is disabled in get ready<br>• each button calls its action; the Stop dialog's Stop and Cancel call theirs<br>• the background colour of each state, read from a pixel of `captureToImage()`<br>• starting and unavailable<br>• touch targets of at least 72 dp | instrumented (Compose) | UI-1–UI-7, UI-10, UI-11, EX-7, EX-8, NF-6 |
| `RunnerScreenTest`, with a real ViewModel, in-memory repositories and `SilentAudio`:<br>• Pause, then Resume, then Pause: the time is kept<br>• paused in get ready: Next, Next, Previous move as in `EX-9`, showing each exercise paused at its full duration<br>• Stop, Cancel: still paused; Stop, Stop: `onExit` is called<br>• Next on the last exercise: `onCompleted` with the summary<br>• system Back while running pauses; again, it opens the Stop dialog<br>• a 1 s-exercise workout runs to `onCompleted` by itself | instrumented (Compose) | EX-4, EX-5, EX-6, EX-8, EX-9, Q1 |
| `RunnerActivityTest`, in `MainActivity` on the real storage:<br>• **recreation** (`scenario.recreate()`) while running: still running, same phase, and the time keeps counting down (`NF-3`)<br>• recreation with the Stop dialog open: it's still open<br>• **stopping** (`scenario.moveToState(CREATED)`, then `RESUMED`): the workout is paused (`EX-10`)<br>• **keep screen on:** the window has `FLAG_KEEP_SCREEN_ON` while the runner is open, and not after Stop returns to the list (`NF-2`) | instrumented | NF-2, NF-3, EX-10 |
| `CompletedScreenTest`: "Well done!", the name, "Total time 4:23", "2 rounds", "5 exercises"; "1 round" and "1 exercise" for a one-exercise workout; Back to list calls back; touch targets | instrumented (Compose) | EX-6, specification §6, NF-6, NF-9 |
| `NavigationSmokeTest`, updated: Start on the seeded workout opens the runner; Pause, then Next until the completed screen shows (at most twice); Back to list returns to the list. Back on the completed screen skips the runner. | instrumented | navigation (§4.3.7) |

**Holding get ready in instrumented tests.** Get ready lasts 3 s. To act in it reliably, `SilentAudio`'s speaker is READY and never finishes "Get ready in", and the test runs with voice cues on. Get ready then waits for the announcement, up to the 5 s timeout (P4). Tests pause first, and everything after that is paused and doesn't depend on timing.

**Waiting for the runner.** In tests through `MainActivity`, the runner can stay in `Starting` for up to 10 s after a cold start, while the TTS engine prepares (P5). Wait for it with `waitUntil(timeoutMillis = 15_000)`.

**Finding things:** the time, the title, the header parts and each button carry test tags (`runner-time`, `runner-title`, …). Texts are matched through `getString(R.string.…)`.

### Manual checks on the tablet

Run them with `./gradlew installDebug`, after the instrumented tests, which uninstall the app. Create the §4.4 example first, or keep it from P6's checks. Steps 2 and 7–9 need the user at the tablet, and step 1 needs them listening.

| # | Step | Expected |
|---|---|---|
| 1 | Start the §4.4 example with voice cues on, and follow it to the end without touching it | Each phase's colour, title, time, header and "Next:" lines as in §4.3.2. The cues come in sync with the display (P5): "Get ready in" and 3-2-1; the names longer than 10 s; the chime at the round rest; "Workout complete. Well done!" Then "Total time 4:23 · 2 rounds · 5 exercises". |
| 2 | Read the screen from 3 m in each state (the user) | The time and the name are easy to read, and the four states are easy to tell apart (`NF-6`, `UI-7`) |
| 3 | Pause in get ready, in an exercise and in a rest; then resume each | The time stays, then counts on from there (`EX-4`, `EX-5`). Speech stops at the pause (`TTS-3`). |
| 4 | While paused, go through the `EX-9` table with Next and Previous | As the table says. Previous is disabled in get ready. Next on the last exercise opens the completed screen, with "Workout complete. Well done!" |
| 5 | Pause, Stop, Cancel; then Stop, Stop | Cancel keeps the workout paused; Stop returns to the list (`EX-8`) |
| 6 | Press system Back while running, then again | The first pauses; the second opens the Stop dialog (Q1) |
| 7 | Turn the tablet over while running, while paused, and with the Stop dialog open | Nothing resets, pauses or skips; the dialog stays open (`NF-3`) |
| 8 | While running: press Home, then return. Repeat with Recents and with the power button. | Each time, the workout is paused on return, at the time it had (`EX-10`) |
| 9 | The tablet's screen timeout is set to never (`screen_off_timeout` is 2147483647, read in T1). With the user's agreement, set it to 30 s (`adb shell settings put system screen_off_timeout 30000`), and put it back to 2147483647 afterwards. Leave the runner paused for over a minute, then wait on the list. | The screen stays on in the runner. On the list, it turns off after 30 s (`NF-2`). |
| 10 | Start a workout, then end the app's process as §4.3.5 says (Home, Settings, Home, `run-as … kill -9`). Reopen it from Recents. Repeat, and reopen it from the launcher. | The workout list, not the runner, both times (`EX-11`) |
| 11 | Turn voice cues off, then start a workout | Get ready counts down at once. Only the chime is heard (`TTS-9`). Turn them back on. |
| 12 | Force-stop the app and `com.google.android.tts`, open the app, and start a workout at once | "Getting ready…" until the audio is ready, then get ready with "Get ready in" (Q3) |
| 13 | Read the app's logcat during steps 1–12 | No crash, and no unexpected `WorkoutAudio` or `WorkoutStorage` warning |

## 7. Tasks

| ID | Task |
|---|---|
| T1 | **Measure on the tablet** (tablet skill, "Measuring device behaviour"):<br>• **Lifecycle:** a throwaway `probe/LifecycleProbe.kt` launches `MainActivity` and registers `ActivityLifecycleCallbacks` on the application. They log each callback with the time and `isChangingConfigurations`. Meanwhile, with `adb shell input keyevent`: Home, then `am start` back; Recents (`KEYCODE_APP_SWITCH`), then back; the power button, then on again and unlocked; and the notification shade (`adb shell cmd statusbar expand-notifications`). The user turns the tablet over twice. The probe also calls `activity.recreate()` once, as `ActivityScenario.recreate()` will in `RunnerActivityTest`, to see `isChangingConfigurations` in that `onStop`.<br>• **Content area:** the probe also logs the activity's content size in dp, and the status and navigation bar insets.<br>• **Process death,** on the installed app rather than the probe, because the probe runs in the app's process: start a workout (the runner placeholder), press Home, run `adb shell am kill io.github.davamix.workouttimer.debug`, and check with `pidof` that the process has gone. Reopen from Recents, then from the launcher, and take a screenshot each time. If `am kill` doesn't end it, try the developer option "Background process limit".<br>Record the results in §4.3.4, §4.3.5 and §4.3.9, and in the handover. If they contradict the design, stop and agree a SPEC change first. Delete the probe. |
| T2 | **What the screen shows:** `RunnerView.kt` (`toRunnerView()`, `formatPhaseTime()`). Test: `RunnerViewTest`. |
| T3 | **Runner ViewModel:** `RunnerViewModel.kt` with `RunnerState`, `RunnerActions` and `WorkoutSummary`: starting (§4.3.1), the commands and the Stop dialog (§4.3.3), the foreground (§4.3.4), the `started` flag (§4.3.5), the outcomes (§4.3.7), and the factory. Add `lifecycle-viewmodel-savedstate` only if needed (§4.4). Tests: `RunnerViewModelTest`, with `GatedSpeaker`. |
| T4 | **Runner screen content:** `RunnerContent` for every state of §4.3.2, the Stop dialog, `RunnerIcons.kt`, the text colours in `PhaseColors`, and `strings_runner.xml`. Fit the sizes of §4.3.9 into the content area T1 measured, checked with screenshots of each state on the tablet. Test: `RunnerContentTest`. |
| T5 | **Runner screen entry and navigation:** `RunnerScreen` (keeping the screen on, the lifecycle calls, `BackHandler`, the outcomes after `RESUMED`); the completed screen and `strings_completed.xml`; `CompletedRoute` and both entries in `AppNavHost`. Delete `PlaceholderScreen` and `placeholder_note`. Update `NavigationSmokeTest`. Tests: `CompletedScreenTest`. |
| T6 | **Instrumented flows:** `RunnerScreenTest`, with `SilentAudio.kt`, and `RunnerActivityTest`. Run every instrumented test on the tablet (`connectedDebugAndroidTest`). |
| T7 | **Finish:** compare the merged manifests with `main`'s; check that P7's Kotlin code holds no user-facing text; run the manual checks (§6) with the user, and adjust the sizes and colours if the 3 m check asks for it. Then KDoc on the public API for P8, `./gradlew build`, handover, pull request. |

## 8. Done criteria

The checks that must all pass before the phase is merged.

- [ ] All tasks done
- [ ] Unit tests and lint pass locally and in CI
- [ ] Instrumented tests pass on the tablet (`connectedDebugAndroidTest`), including the updated `NavigationSmokeTest`
- [ ] UI tests cover pause, resume, next, previous and stop (plan §5.2)
- [ ] Manual checks on the tablet pass, including reading the screen from 3 m and turning the tablet over while a workout runs (plan §5.2)
- [ ] The merged manifests gain no permission and no component
- [ ] P7's Kotlin code holds no user-facing text: it's all in `strings_runner.xml` and `strings_completed.xml`
- [ ] No placeholder code is left (`PlaceholderScreen`, `placeholder_note`, `runner_finish`)
- [ ] `:core` is unchanged
- [ ] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

None at the moment. New questions are numbered from Q8.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | What does the system Back button do on the runner? The specification didn't say. Leaving at once would end a workout without the confirmation `EX-8` asks for. | **It's one step towards leaving:** while running, it pauses; while paused, it opens the Stop dialog; with the dialog open, it closes it. While starting, or when the workout can't run, it returns to the list (§4.3.3). Added to specification §6 (v0.9). |
| Q2 | The completed screen shows "rounds and exercises done" (specification §6). After Next has skipped some, are those the workout's counts, or only the exercises actually run? | **The workout's counts,** like the total time, which is the planned one (P4 Q2). The §4.4 example always shows 2 rounds and 5 exercises. Specification §6 says so since v0.9. |
| Q3 | What does the runner show while the audio is being prepared? It's usually instant, but up to 10 s within about 10 s of a cold start (P5). | **Get ready's background, the workout's name, a progress indicator and "Getting ready…"**, with no buttons. Back returns to the list (§4.3.1). |
| Q4 | What does the header show during a round rest? The current round has just ended, and the next one is named below the time (`UI-5`). | **The round that just ended,** as during its exercises: "Round 1/2 · Warm-up", with "Next round: Core" and "Next: Plank (60 s)" below the time (§4.3.2). |
| Q5 | Previous isn't available in get ready (`EX-9`). Is it hidden or disabled? | **Shown disabled,** so the paused layout is the same in every phase (§4.3.3). |
| Q6 | The sketches show the exercise name in capitals ("SQUATS"). Are names shown in capitals, or as written? | **As written,** because long names read better in mixed case. The fixed words "GET READY", "REST" and "PAUSED" are in capitals (§4.2, main texts). |
| Q7 | Found in T4, on the tablet: in the 960 × 542 dp content area, with Stop on its own row under Previous, Resume and Next as sketched, the paused time shrank to about 125 sp in an exercise, and to about 50 sp in a round rest, which also shows two "Next" lines. | **Stop goes at the right end of the same row,** after a gap. The paused time is then about 200 sp in an exercise and 120 sp in a round rest. Resume sits about 95 dp left of the centre. The user chose it from screenshots of both layouts. Specification §6's paused sketch shows it since v0.10 (§4.3.3). |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-05 | First draft, based on specification v0.8 and plan v0.2 |
| 0.2 | 2026-11-05 | Q1 to Q6 answered as assumed. Quotes specification v0.9, which adds Back on the runner (Q1) and the completed screen's counts (Q2). Status `Ready`. |
| 0.3 | 2026-11-05 | T1 measurements, the design unchanged: all three `EX-10` cases give `ON_STOP`; turning the tablet over doesn't recreate the activity; `recreate()` sets `isChangingConfigurations`; the content area is 960 × 542 dp; Android rebuilds the runner after ending the app's process, and how to make it end it (§4.3.4, §4.3.5, §4.3.9, manual checks 9 and 10). |
| 0.4 | 2026-11-05 | Q7, found in T4: while paused, Stop is at the end of the row of buttons, not under it, so the time stays large (§4.3.3, the paused sketch). Text sizes as fitted on the tablet (§4.3.9). Quotes specification v0.10. |
