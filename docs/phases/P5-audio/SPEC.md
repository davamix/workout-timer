# P5 — Audio cues: Specification

| | |
|---|---|
| **Status** | Ready, v0.3 |
| **Phase** | P5, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P4 (done) |
| **Branch** | `phase/p5-audio` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

The voice and sound cues of a running workout. A pure `CuePlanner` in `:core` turns the engine's events into cues: what to say, when to play the round-end chime, and when to stop speaking. In `:app`, a `CuePlayer` plays those cues with Android's text-to-speech in English (United States) and a bundled chime, and tells the session when "Get ready in" has been spoken. A debug-only screen runs a workout with audio, so every cue can be checked by ear on the tablet before the runner screen exists.

Later phases build on it:

| Phase | Uses |
|---|---|
| P7 | the runner ViewModel prepares the `CuePlayer`, lets it play the session's cues, and creates the session with `waitForAnnouncement` set from the voice setting (§4.3.6) |
| P8 | finishes that wiring if P7 is merged before P5, and checks the cues in the acceptance run |

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.8.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| TTS-1 | "When an exercise starts and its duration is **longer than 10 s**, the exercise name is spoken. The rule is strict: a 10 s exercise is not announced." | The planner speaks the name when an exercise starts, only if it lasts more than 10 s. |
| TTS-2 | "When **3 s** remain in an exercise, a countdown is spoken: "three", "two", "one". There is one word per second, spoken as the display changes to 3, 2 and 1. An exercise of 3 s or less starts the countdown at its own duration." | The planner speaks a number word when an exercise's displayed time becomes 3, 2 or 1, including at its start for exercises of 3 s or less. |
| TTS-3 | "Pausing stops any speech in progress. Nothing is replayed on resume; the countdown carries on from the current second." | Pause gives a "stop speech" cue; the player stops the speaker and drops waiting speech. Resume gives no cue. |
| TTS-4 | "The cues work without a network connection. The app doesn't need the network at all." | The speaker picks an installed English (US) voice that needs no network. The app still has no `INTERNET` permission. |
| TTS-7 | "When the workout ends, "Workout complete. Well done!" is spoken." | The planner speaks it on `Completed`, which P4 sends when the last exercise ends and when Next is pressed on it. |
| TTS-9 | "A setting turns voice cues on or off. The choice is remembered. With voice cues off, the timer, the screen and the round-end sound work the same." | Applies the stored setting: with voice cues off the planner gives no speech cues, but the chime still plays, and get ready doesn't wait. P3 stores the setting; P6 has the settings screen. |
| TTS-10 | "**Get ready:** "Get ready in" is spoken first. When it finishes, the 3 s countdown starts, with "three", "two", "one" spoken in sync with the display. With voice cues off, the countdown starts immediately." | The planner speaks "Get ready in" when get ready starts. The player calls `announcementFinished()` when the speech ends, and the planner counts down from there. |
| TTS-11 | "Voice cues are always spoken in **English**, using the English (United States) voice, whatever the device language." | The speaker selects `Locale.US`. The spoken phrases are English resources that are never translated. |
| TTS-12 | "**Rests are silent:** no voice cues are spoken during exercise rests or round rests." | The planner gives no cue for rest phases. |
| SND-1 | "A short **sound** plays when a round ends, at the start of the round rest. It doesn't play after the last round. If the round rest is 0 s, the next exercise's name (`TTS-1`) is spoken after the sound finishes." | The planner plays the chime on `RoundEnded`, which P4 only sends when a round ends on time and another follows. The player holds any speech until the chime has finished. |
| SND-2 | "There are no other sound effects. Exercises are marked only by the voice cues." | The chime is the only sound. |
| EX-7 | "A **get-ready** countdown of **3 s** runs before the first exercise of the first round, and only there. The screen shows "Get ready", the countdown, and the name of the first exercise. The voice for it is described in `TTS-10`." | The voice part, as in TTS-10. The screen is P7's. |

### Specification text this phase implements (quoted)

From specification §5.3, the introduction and the note:

> Voice cues use Android's built-in text-to-speech service with the device's default engine (Google Speech Services), always in English.
>
> The voice and the round-end sound play at the media volume. While the app is open, the volume keys always change that volume, even between cues.

> Note on `TTS-1` and `TTS-2`: the 10 s threshold leaves at least 7 s between the spoken name and the start of the countdown, so the two cues never overlap.

From specification §4.3, sequencing rules 4 and 5:

> 4. When the last exercise of a round finishes: the round-end sound plays; **that round's rest** runs (the exercise's own rest is not added on top); the first exercise of the next round starts.
> 5. When the last exercise of the last round finishes, the workout ends: no rest runs and no round-end sound plays; "Workout complete. Well done!" is spoken.

From specification §5.1, what Next and Previous do:

> After moving, the workout stays **paused** at the start of the target exercise. Resume starts that exercise as normal, including its name announcement (`TTS-1`).
>
> Moving doesn't play the round-end sound (`SND-1`), even when it crosses into another round.
>
> Next on the last exercise ends the workout as if it had finished, so "Workout complete. Well done!" is spoken (`TTS-7`).

From specification §10, future ideas:

> A message when the English (United States) voice isn't available. In v1, voice cues are then silent; the timer and the round-end sound still work.

The §4.4 example, with its audio column:

> Workout "Example" has these defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.
>
> - Round 1 "Warm-up", own round rest 30 s: Jumping jacks 30 s; Arm circles 10 s, own rest 5 s; High knees, default duration.
> - Round 2 "Core": Plank 60 s, own rest 30 s; Crunches, default duration.

| # | Phase | Round | Name | Duration | Audio |
|---|---|---|---|---|---|
| 1 | Get ready | — | — | 3 s | "Get ready in" · 3-2-1 |
| 2 | Exercise | Warm-up | Jumping jacks | 30 s | "Jumping jacks" · 3-2-1 |
| 3 | Exercise rest | Warm-up | — | 15 s (default) | silent |
| 4 | Exercise | Warm-up | Arm circles | 10 s | 3-2-1 (no name: not longer than 10 s) |
| 5 | Exercise rest | Warm-up | — | 5 s (own) | silent |
| 6 | Exercise | Warm-up | High knees | 40 s (default) | "High knees" · 3-2-1 |
| 7 | Round rest | — | — | 30 s (own) | round-end sound, then silent |
| 8 | Exercise | Core | Plank | 60 s | "Plank" · 3-2-1 |
| 9 | Exercise rest | Core | — | 30 s (own) | silent |
| 10 | Exercise | Core | Crunches | 40 s (default) | "Crunches" · 3-2-1 |
| | **Completed** | | | **Total 4:23** | "Workout complete. Well done!" |

From specification §3, the target device:

> TTS engine: Google Speech Services (`com.google.android.tts`), the default and only engine installed. No TTS language is forced in settings, so by default it follows the system language (Spanish). Installed voices: Spanish (Spain) and English (United States).
>
> **Voice cues are always in English** (`TTS-11`), but the system language is Spanish. The app has to select English (United States) explicitly. That voice is installed on the device, so cues work offline.

## 3. Scope

### In scope

- **Cue planner** (`:core`): the `Cue` and `Utterance` types, and `CuePlanner`, which applies `TTS-1`–`TTS-12`, `SND-1`, `SND-2` and the voice setting.
- **Speaker** (`:app`): `TtsSpeaker`, the platform `TextToSpeech` with the default engine and an English (US) voice that works offline. It has a readiness check and utterance callbacks. It warms the engine up with the fixed phrases, so they're spoken without delay (Q6).
- **Preparing ahead:** the app starts preparing the audio when its activity is created, so a workout rarely waits for the TTS engine (Q6).
- **Round-end sound:** a PowerShell script that generates the chime as a WAV file (plan `D-10`), the generated file, and `MediaSoundPlayer`, which plays it.
- **Cue player** (`:app`): `CuePlayer` plays the planner's cues in order. It holds speech while the chime plays, stops speech on pause, and reports when "Get ready in" ends.
- **Spoken phrases:** English text in `strings_audio.xml`, marked as never translated.
- **`AppContainer`:** the speaker, the sound player and the cue player.
- **Debug screen:** a debug-build-only activity that runs a built-in workout with audio, so the cues can be checked by ear.
- **Tests:** JVM tests for the planner and the player; instrumented tests for the speaker, the sound player and the phrases on the tablet; manual checks by ear.

### Out of scope

| What | Phase |
|---|---|
| The engine's events, the get-ready wait and its 5 s timeout (already merged) | P4 |
| Storing the voice setting (already merged) | P3 |
| The settings screen with the voice switch | P6 |
| The runner ViewModel and screens, pausing when the app leaves the foreground (`EX-10`) | P7 |
| Wiring the cue player into the runner (§4.3.6): P7 if P5 is merged first, otherwise P8 | P7 or P8 |
| A message on screen when the English voice isn't available (Q5) | future idea (specification §10) |
| Voice speed or volume settings, Spanish voice cues | future ideas (specification §10) |
| Lowering other apps' audio during cues | discarded (`TTS-8`) |

## 4. Design

The plan's decisions this phase implements:

> **D-9:** The platform `TextToSpeech`, set to `Locale.US`. Utterance callbacks drive the get-ready countdown. *Reason:* implements `TTS-10` and `TTS-11` without extra libraries.
>
> **D-10:** The round-end sound is a short chime generated by a script in the repo. *Reason:* a public repo shouldn't raise questions about third-party audio licences.
>
> **D-2:** Two Gradle modules: `:core` holds the model, timeline, engine and cue logic in pure Kotlin. `:app` holds everything that needs Android.

And plan §2.3, how a workout runs (steps 5–7):

> 5. The **`CuePlanner`** turns events into cues, such as *speak "Squats"*, *play the round-end sound* or *stop speaking*. It applies `TTS-1`–`TTS-12`, `SND-1`, `SND-2` and the voice setting. Because it is pure Kotlin, a test can check the whole cue sequence of a workout.
> 6. The **`CuePlayer`** plays the cues in order. If the round-end sound and an exercise name fall at the same moment, the sound plays first (`SND-1`). Pausing stops speech (`TTS-3`).
> 7. **Get ready** (`TTS-10`) is the one place where audio drives the timer: the session waits in the get-ready phase while "Get ready in" is spoken; the player reports when the speech ends, and the 3 s countdown starts; if voice cues are off, the countdown starts immediately; a timeout keeps the workout from getting stuck if TTS fails.

### 4.1 Packages and files owned

```
core/src/main/kotlin/io/github/davamix/workouttimer/core/cues/
    Cue.kt                    Cue, Utterance
    CuePlanner.kt             CuePlanner and its constants: every rule in §4.3.1
core/src/test/kotlin/io/github/davamix/workouttimer/core/cues/
    CueFixtures.kt            the edge-case timeline, and helpers to run a machine and plan its cues
    CuePlannerTest.kt
    ExampleCuesTest.kt

app/src/main/kotlin/io/github/davamix/workouttimer/audio/
    Speaker.kt                Speaker, SpeakerStatus, SPEAKER_PREPARE_TIMEOUT_MILLIS
    TtsSpeaker.kt             Speaker on the platform TextToSpeech
    SoundPlayer.kt            SoundPlayer
    MediaSoundPlayer.kt       SoundPlayer on MediaPlayer, playing res/raw/round_end.wav
    SpokenText.kt             Utterance → English text, from strings_audio.xml
    CuePlayer.kt              CuePlayer, ROUND_END_SOUND_TIMEOUT_MILLIS
app/src/main/res/raw/round_end.wav            generated by tools/generate-round-end-sound.ps1; never edited by hand
app/src/main/res/values/strings_audio.xml     the spoken phrases, translatable="false"
app/src/test/kotlin/io/github/davamix/workouttimer/audio/
    AudioFakes.kt             FakeSpeaker and FakeSoundPlayer, writing to one shared AudioLog
    CuePlayerTest.kt
app/src/androidTest/kotlin/io/github/davamix/workouttimer/audio/
    TtsSpeakerDeviceTest.kt
    MediaSoundPlayerDeviceTest.kt
    SpokenTextDeviceTest.kt

app/src/debug/AndroidManifest.xml             the debug screen's activity, debug builds only
app/src/debug/kotlin/io/github/davamix/workouttimer/audiodebug/
    AudioDebugActivity.kt
    AudioDebugViewModel.kt
    AudioDebugScreen.kt
    DebugWorkouts.kt          the §4.4 example and the edge-case workout
app/src/debug/res/values/strings_audio_debug.xml

tools/generate-round-end-sound.ps1
```

**Shared files changed,** each with this phase's entries only:

| File | Change |
|---|---|
| `app/src/main/kotlin/io/github/davamix/workouttimer/di/AppContainer.kt` | `speaker`, `soundPlayer` and `cuePlayer`, `prepareAudio()`, and a debug-only audio log, like the storage one |
| `app/src/main/AndroidManifest.xml` | a `<queries>` entry for the `android.intent.action.TTS_SERVICE` intent. Apps targeting API 30 or higher need it to see the TTS engine. It has no effect on the tablet's API 26, and it isn't a permission. |
| `app/src/main/kotlin/io/github/davamix/workouttimer/MainActivity.kt` | `volumeControlStream = AudioManager.STREAM_MUSIC`, so the volume keys always set the cue volume (Q3); and `prepareAudio()` when it's created (§4.3.6, Q6) |
| `.gitattributes` | `*.wav binary` |

No build file changes: everything used is already a dependency (§4.4). `src/debug/kotlin` is a standard source folder of the debug build.

### 4.2 Interfaces and data

#### `:core`, package `io.github.davamix.workouttimer.core.cues`

```kotlin
/** Exercises lasting longer than this have their name spoken when they start (TTS-1). */
const val ANNOUNCE_NAME_ABOVE_SECONDS = 10

/** The countdown speaks the last this many seconds of an exercise and of get ready (TTS-2, TTS-10). */
const val COUNTDOWN_SECONDS = 3

/** Something the audio must do, decided by [CuePlanner] from one session event. */
sealed interface Cue {
    /** Speak [utterance], after anything already being spoken. */
    data class Speak(val utterance: Utterance) : Cue

    /** Play the round-end sound (SND-1). Speech that comes with it, or while it plays, waits until it has finished. */
    data object PlayRoundEndSound : Cue

    /** Stop any speech in progress and drop speech that is still waiting (TTS-3). The sound isn't cut. */
    data object StopSpeech : Cue
}

/** What can be spoken. The words are English text in the app's resources (TTS-11). */
sealed interface Utterance {
    /** "Get ready in" (TTS-10). When it has been spoken, the player calls `WorkoutSession.announcementFinished()`. */
    data object GetReadyIn : Utterance

    /** "three", "two" or "one" (TTS-2, TTS-10). [number] is 1 to [COUNTDOWN_SECONDS]. */
    data class Count(val number: Int) : Utterance

    /** An exercise's name, as written in the workout (TTS-1). */
    data class ExerciseName(val name: String) : Utterance

    /** "Workout complete. Well done!" (TTS-7). */
    data object WorkoutComplete : Utterance
}

/**
 * Turns a session's events into cues (plan §2.3 step 5). Pure and stateless: the cues for an event depend only on
 * the event and [voiceCuesEnabled].
 *
 * @param voiceCuesEnabled The voice setting (TTS-9), read once when the workout starts. When `false`, no
 *   [Cue.Speak] is given; everything else is the same.
 */
class CuePlanner(val voiceCuesEnabled: Boolean) {
    /** The cues for [event], in the order they must be played. Often empty. */
    fun cuesFor(event: SessionEvent): List<Cue>
}
```

`Count` checks its number on creation and throws `IllegalArgumentException` outside 1–3.

#### `:app`, package `io.github.davamix.workouttimer.audio`

```kotlin
/**
 * How long [Speaker.prepare] waits at most for the TTS engine. From a cold start, connecting and warming up take
 * about 9 s on the tablet (§4.3.3).
 */
const val SPEAKER_PREPARE_TIMEOUT_MILLIS = 10_000L

enum class SpeakerStatus {
    /** Not connected yet, connecting, or warming up. */
    INITIALIZING,

    /** Connected, with the English (US) voice selected. Speech can be heard. */
    READY,

    /** The engine failed, or has no English (US) voice. [Speaker.prepare] tries again. */
    UNAVAILABLE,
}

/** Speaks text with the English (United States) voice (TTS-11). Safe to call from any thread. */
interface Speaker {
    val status: StateFlow<SpeakerStatus>

    /** The selected voice's name, for the debug screen and logs. `null` until [SpeakerStatus.READY]. */
    val voiceName: String?

    /**
     * Gets ready to speak: connects to the default TTS engine, selects the voice and warms the engine up, unless
     * that's done or under way. When UNAVAILABLE, it starts again from scratch. Returns the status once READY or
     * UNAVAILABLE, or after [timeoutMillis] (it may still be INITIALIZING then, and carries on preparing). Several
     * calls may wait at once; they share one preparation.
     */
    suspend fun prepare(timeoutMillis: Long = SPEAKER_PREPARE_TIMEOUT_MILLIS): SpeakerStatus

    /**
     * Speaks [text] after anything already queued. [onDone] is called once, on any thread, when it has been spoken
     * or couldn't be (not READY, engine error), at once in the second case. It isn't called when [stop] cuts the
     * text off.
     */
    fun speak(text: String, onDone: () -> Unit = {})

    /** Stops speech in progress and drops queued speech. Their `onDone` callbacks are never called. */
    fun stop()
}

/** Plays the round-end chime (SND-1). Safe to call from any thread. */
interface SoundPlayer {
    /** Loads the sound, if it isn't loaded yet, so the first play starts without delay. */
    fun prepare()

    /**
     * Plays the chime from its start, cutting off one still playing. [onDone] is called once, on any thread, when it
     * has finished or couldn't play.
     */
    fun playRoundEnd(onDone: () -> Unit = {})
}

/** Utterance → its English text, from `strings_audio.xml` (TTS-11). */
class SpokenText(resources: Resources) {
    fun of(utterance: Utterance): String

    /**
     * The phrases that are the same in every workout, in the order a workout first speaks them: "Get ready in",
     * "three", "two", "one", "Workout complete. Well done!". The speaker warms up with them (§4.3.3).
     */
    val fixedPhrases: List<String>
}

/** If the chime's `onDone` doesn't come within this time, speech waiting for it goes ahead anyway. */
const val ROUND_END_SOUND_TIMEOUT_MILLIS = 2_000L

/**
 * Plays a workout's cues (plan §2.3 step 6). One workout at a time.
 */
class CuePlayer(
    private val speaker: Speaker,
    private val soundPlayer: SoundPlayer,
    private val textOf: (Utterance) -> String,
    private val roundEndSoundTimeoutMillis: Long = ROUND_END_SOUND_TIMEOUT_MILLIS,
) {
    /**
     * Gets the audio ready for a workout: loads the chime and, if [voiceCuesEnabled], prepares the speaker. Call it
     * before `session.start()`. Returns whether voice cues can be heard: `false` when they're off or the speaker
     * isn't READY.
     */
    suspend fun prepare(voiceCuesEnabled: Boolean): Boolean

    /**
     * Plays [session]'s cues until its events end. It must be the only collector of `session.events`. Speech
     * already handed to the speaker when it returns, or when it's cancelled, carries on, so "Workout complete. Well
     * done!" isn't cut off when the runner leaves.
     *
     * @param onCues Called with each event and its cues, before they're played. Only the debug screen uses it.
     */
    suspend fun play(
        session: WorkoutSession,
        voiceCuesEnabled: Boolean,
        onCues: (SessionEvent, List<Cue>) -> Unit = { _, _ -> },
    )

    /** The same, on any events, for tests. */
    internal suspend fun play(
        events: Flow<SessionEvent>,
        voiceCuesEnabled: Boolean,
        announcementFinished: () -> Unit,
        onCues: (SessionEvent, List<Cue>) -> Unit = { _, _ -> },
    )
}
```

`TtsSpeaker(context: Context, warmUpTexts: List<String>, onWarning: (message: String, error: Throwable?) -> Unit)` and `MediaSoundPlayer(context: Context, onWarning: …)` implement the interfaces. `warmUpTexts` are the phrases the engine is warmed up with; `AppContainer` passes `SpokenText.fixedPhrases`. `TtsSpeaker` also exposes `selectedVoice: android.speech.tts.Voice?` for its device test.

#### Spoken phrases: `app/src/main/res/values/strings_audio.xml`

```xml
<resources>
    <!-- Spoken by the English (US) voice whatever the device language (TTS-11): never translated. -->
    <string name="cue_get_ready_in" translatable="false">Get ready in</string>
    <string name="cue_three" translatable="false">three</string>
    <string name="cue_two" translatable="false">two</string>
    <string name="cue_one" translatable="false">one</string>
    <string name="cue_workout_complete" translatable="false">Workout complete. Well done!</string>
</resources>
```

`Utterance.ExerciseName` is spoken as the workout has it.

#### `AppContainer` entries

```kotlin
private val spokenText by lazy { SpokenText(context.resources) }
val speaker: Speaker by lazy { TtsSpeaker(context, spokenText.fixedPhrases, onWarning = ::logAudioWarning) }
val soundPlayer: SoundPlayer by lazy { MediaSoundPlayer(context, onWarning = ::logAudioWarning) }
val cuePlayer: CuePlayer by lazy { CuePlayer(speaker, soundPlayer, spokenText::of) }

/**
 * Gets the audio ready before any workout starts: loads the chime and, if voice cues are on, prepares the speaker.
 * The activities call it when they're created, so a workout rarely waits for the TTS engine (§4.3.6).
 */
suspend fun prepareAudio() {
    cuePlayer.prepare(settingsRepository.settings.first().voiceCuesEnabled)
}
```

`logAudioWarning` logs with tag `WorkoutAudio`, in debug builds only, like `logStorageWarning`. The speaker and the sound player live as long as the app, so they're never shut down or released.

### 4.3 Behaviour

#### 4.3.1 Planner rules

With voice cues on, each event gives these cues. P4's events are described in §4.5.

| Event | Cues | Rule |
|---|---|---|
| `PhaseStarted` of get ready | `Speak(GetReadyIn)` | TTS-10 |
| `CountdownStarted` | `Speak(Count(3))` | TTS-10 |
| `PhaseStarted` of an exercise lasting more than 10 s | `Speak(ExerciseName(name))` | TTS-1 |
| `PhaseStarted` of an exercise lasting 3 s or less | `Speak(Count(duration))` | TTS-2 |
| `PhaseStarted` of an exercise lasting 4–10 s | none | TTS-1 |
| `SecondChanged` in get ready or an exercise, to 3, 2 or 1 | `Speak(Count(n))` | TTS-2, TTS-10 |
| `SecondChanged` to more than 3 | none | |
| `PhaseStarted` or `SecondChanged` of a rest | none | TTS-12 |
| `RoundEnded` | `PlayRoundEndSound` | SND-1 |
| `Paused` | `StopSpeech` | TTS-3 |
| `Resumed` | none: nothing is replayed | TTS-3 |
| `Completed` | `Speak(WorkoutComplete)` | TTS-7 |
| `Stopped` | `StopSpeech` | |

- **Voice cues off:** the same table without any `Speak` cue. Only the chime and `StopSpeech` remain (TTS-9).
- **The rules follow from P4's events.** Every case below needs no state in the planner:
  - `SecondChanged` isn't sent for a phase's first second. So an exercise of 3 s or less gets its first number at `PhaseStarted`, and every other number at `SecondChanged`.
  - After a pause, the countdown carries on from the next `SecondChanged`: a word cut off by the pause isn't repeated (TTS-3).
  - After Next or Previous, resume sends `PhaseStarted` for the target, so its name or first number is spoken as for any start (specification §5.1).
  - Pausing while "Get ready in" is spoken cuts it off. On resume, P4 sends `CountdownStarted` at once, so the countdown goes on without the announcement.
  - Next and Previous never send `RoundEnded`, so they never play the chime. Nor does the last round's end (SND-1).
  - Next on the last exercise sends `Completed`, so "Workout complete. Well done!" is spoken then too (Q4).
  - After a wake-up more than a second late, only the current second is sent, so only its number is spoken.

#### 4.3.2 Cue player

- **One queue.** `play()` handles the session's events and its own signals (the chime finished, its timeout) one at a time, in one coroutine, so it needs no lock. Callbacks from the speaker and the sound player only post a signal into that queue. The exception is the `onDone` of "Get ready in", which calls `announcementFinished()` directly: P4 allows that from any thread (§4.5).
- **`Speak(GetReadyIn)`:** if the speaker is READY, speak it, and call `announcementFinished()` in its `onDone`. Otherwise call `announcementFinished()` at once, so the countdown starts without waiting for P4's 5 s timeout.
- **Other `Speak` cues:** give the text to the speaker, which queues it after anything still being spoken. If the speaker isn't READY, nothing is heard.
- **`PlayRoundEndSound`:** play the chime. Until its `onDone` comes, or `roundEndSoundTimeoutMillis` passes, any `Speak` cue waits, in order. Then the waiting speech goes ahead (SND-1).
- **`StopSpeech`:** call `speaker.stop()`, and drop speech waiting for the chime. A chime already playing isn't cut, as it's under a second long.
- **When the events end, or `play()` is cancelled:** speech waiting for the chime is dropped. Speech already handed to the speaker carries on.
- **Voice cues off:** the speaker is never called; the chime plays as with voice on.

#### 4.3.3 Speaker

- **Engine:** `TextToSpeech(context, listener)`, which uses the device's default engine, Google Speech Services on the tablet. Created in `prepare()`, using the application context.
- **Voice (TTS-4, TTS-11):** once the engine is initialised, select the best voice whose locale is English (United States), that doesn't need the network, and that is installed (its features don't include `KEY_FEATURE_NOT_INSTALLED`). Best means highest `quality`, then lowest `latency`. If no such voice is listed, fall back to `setLanguage(Locale.US)`, accept a result of `LANG_COUNTRY_AVAILABLE` or better, and log that the voice may need the network.
- **Warm-up (Q6):** once the voice is selected, the speaker synthesises each of `warmUpTexts`, in order, with `synthesizeToFile` into one temporary file in the app's cache folder (`context.cacheDir`). Each is queued with its own utterance ID, and the progress listener tracks them. When the last one is done, the file is deleted, and the speaker becomes READY. Nothing is heard.
  - Why: on the tablet, the first phrase after the engine connects starts 3–4 s late, while the engine loads the voice, and every new phrase ~250 ms late. A phrase the engine has synthesised before starts within ~30 ms (measured in T3, see the table below).
  - A warm-up phrase that fails, through an `ERROR` result, `onError` or `onStop`, is logged and counts as done. The speaker still becomes READY: speech works, just later.
  - `stop()` doesn't interrupt the warm-up. Nothing else can be queued while INITIALIZING, as `speak()` needs READY.
  - If the engine is replaced (reconnect), callbacks from the old engine's warm-up are ignored.
- **Readiness:** READY once the voice is selected and the warm-up is done. UNAVAILABLE if initialisation fails, or there's no English (US) voice; this is logged. Speaking never throws.
- **Audio:** `AudioAttributes` with `USAGE_MEDIA` and `CONTENT_TYPE_SPEECH`, so the media volume controls the cues.
- **Queueing:** `speak()` uses `QUEUE_ADD` with a unique utterance ID, and keeps each `onDone` by ID. `UtteranceProgressListener.onDone` and `onError` call it and forget it; `onStop` forgets it without calling. `stop()` calls `TextToSpeech.stop()` and forgets every pending `onDone`.
- **Failure while speaking:** if `speak()` returns `ERROR`, the status becomes UNAVAILABLE and `onDone` is called at once. The next `prepare()` reconnects.
- **Timing on the tablet,** measured in T3 from a cold engine with a throwaway probe:

  | Step | Time |
  |---|---|
  | `TextToSpeech` connects (`onInit`) | 2.7–3.0 s on the first connection in a process; ~0.15 s for a second one |
  | Listing the voices | ~0.5 s |
  | Warm-up with the five fixed phrases | 5.7 s, of which 4.3 s for the first phrase |
  | Without warm-up: the first phrase | starts 2.8–4.2 s after `speak()` |
  | Without warm-up: a new phrase later | starts ~250 ms after `speak()` |
  | After warm-up: a fixed phrase | starts ~25 ms after `speak()` |
  | Speaking the phrases at volume 0, instead of to a file | 10.8 s, so not used |

  T6 measures `prepare()` with the warm-up from a cold engine, and records it in the handover.

#### 4.3.4 Round-end sound

- **Generated, not recorded (D-10):** `tools/generate-round-end-sound.ps1` runs on PowerShell 7 (`pwsh`), as Python isn't installed. It writes `app/src/main/res/raw/round_end.wav` and needs no other tool.
- **Format:** WAV, 16-bit PCM, mono, 44 100 Hz.
- **Sound:** a two-note rising chime, about 0.5 s and no more than 0.8 s long. Each note is a sine with two quieter overtones, a 5 ms attack and an exponential decay. The peak is at −3 dBFS, and the last 10 ms fade out to silence. The note frequencies and lengths are parameters at the top of the script, so the chime can be tuned by ear (T2).
- **Deterministic:** the script uses no randomness, so running it again gives an identical file, and `git status` shows no change.
- **Player:** `MediaSoundPlayer` creates one `MediaPlayer` in `prepare()` with `MediaPlayer.create(context, R.raw.round_end, audioAttributes, audioSessionId)`. Its attributes are `USAGE_MEDIA` and `CONTENT_TYPE_SONIFICATION`, the same volume as speech. `playRoundEnd()` seeks to 0 and starts; `OnCompletionListener` calls `onDone`. If the player can't be created or fails, it logs, and calls `onDone` at once.

#### 4.3.5 Timing

- **Speech latency:** after the warm-up (§4.3.3), "Get ready in", the number words and "Workout complete. Well done!" start ~25 ms after `speak()`. Exercise names can't be warmed up, as they come from the workout, so each starts ~250 ms after its exercise. That's fine: a name marks no change on the display. The manual checks (§6) judge by ear whether each number is heard as the display changes. If it isn't, that's recorded as a known issue and raised with the user.
- **The chime and a 3 s exercise:** with a 0 s round rest, an exercise of 3 s or less after the chime has its first number held for the chime's length. The chime is short enough for that number to end before the next one.

#### 4.3.6 How the runner uses it (P7 or P8)

```kotlin
// In the runner ViewModel, when the workout starts:
val voiceOn = container.settingsRepository.settings.first().voiceCuesEnabled   // read once (TTS-9)
container.cuePlayer.prepare(voiceOn)
val session = WorkoutSession(Timeline.of(workout), container.clock, viewModelScope, waitForAnnouncement = voiceOn)
viewModelScope.launch { container.cuePlayer.play(session, voiceOn) }
session.start()
```

- The voice setting is read once, when the workout starts. The settings screen can only be reached from the workout list, so it can't change during a workout.
- `waitForAnnouncement` and `play()` get the same value. If the speaker isn't READY, the player calls `announcementFinished()` at once, so get ready doesn't wait.
- The runner reads `session.state` only. `session.events` has a single collector: the cue player.
- **`prepare()` usually returns at once,** as the audio is prepared when the app opens (below). It can wait up to `SPEAKER_PREPARE_TIMEOUT_MILLIS` (10 s) when a workout starts within ~9 s of a cold start, or when voice cues were off when the app opened and have been turned on since. There's no session yet while it waits, so P7 decides what the screen shows meanwhile.
- The debug screen (§4.3.7) does the same, so it's a working example.

**Preparing when the app opens (Q6).** `MainActivity.onCreate` starts `container.prepareAudio()` (§4.2) in `lifecycleScope`, on `Dispatchers.Default`, so the main thread doesn't load the chime or connect the engine. The debug activity does the same. A later `prepare()` waits for that preparation instead of starting another. If the activity is destroyed first, only the waiting coroutine is cancelled: the warm-up runs on the engine's callbacks and carries on. It's started from the activity, not from `Application.onCreate`, because the system also starts the app's process without any screen, for example for AndroidX's profile-installer receiver, and audio isn't needed then.

#### 4.3.7 Debug screen

A debug-build activity, so the cues can be checked by ear before P7. It isn't in release builds.

- **Reaching it:** its own launcher icon, "WT audio debug", declared in `app/src/debug/AndroidManifest.xml` (Q2). It can also be started with `adb shell am start -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.audiodebug.AudioDebugActivity`. It's landscape like the app, and uses the app's theme.
- **Workouts:** built in code, in `DebugWorkouts.kt`:
  - **Example:** the §4.4 example, 4:23.
  - **Edge cases**, 0:30. Round "A", round rest 0 s: "Ten" 10 s with rest 0 s, then "Eleven" 11 s. Round "B": "Three" 3 s, "Two" 2 s and "One" 1 s, with rests of 0 s. It covers the 10 s threshold, short exercises, and the chime followed at once by a countdown.
- **Controls:** a choice of workout; the voice cues switch, which reads and writes the stored setting (`settingsRepository`), so it also tests TTS-9; Start, Pause, Resume, Next, Previous and Stop, each enabled only where it applies.
- **Shown:** the speaker status and voice name; the session status, the phase, and its remaining seconds in large type, to judge the countdown against; and a log of the last 50 events with their cues and times since Start. Ticks that give no cue are left out of the log.
- **Behaviour:** an `AudioDebugViewModel` owns the session and starts it as in §4.3.6. It doesn't pause when the app leaves the foreground, as that's P7's `EX-10`.
- **Text:** in `app/src/debug/res/values/strings_audio_debug.xml`.

#### 4.3.8 Expected cue sequences

These are the oracles for `ExampleCuesTest`. Times are seconds after Start.

**§4.4 example, voice on, with "Get ready in" taking 1.5 s.** The countdown starts at 1.5 s, so every phase starts 1.5 s later than in P4's on-time run, and the workout completes at 264.5 s.

| Time | Cue | | Time | Cue |
|---|---|---|---|---|
| 0 | "Get ready in" | | 101.5 | "three" |
| 1.5 | "three" | | 102.5 | "two" |
| 2.5 | "two" | | 103.5 | "one" |
| 3.5 | "one" | | 104.5 | round-end sound |
| 4.5 | "Jumping jacks" | | 134.5 | "Plank" |
| 31.5 | "three" | | 191.5 | "three" |
| 32.5 | "two" | | 192.5 | "two" |
| 33.5 | "one" | | 193.5 | "one" |
| 56.5 | "three" (Arm circles, no name) | | 224.5 | "Crunches" |
| 57.5 | "two" | | 261.5 | "three" |
| 58.5 | "one" | | 262.5 | "two" |
| 64.5 | "High knees" | | 263.5 | "one" |
| | | | 264.5 | "Workout complete. Well done!" |

That's 24 speech cues and one sound, and nothing during the rests.

**§4.4 example, voice off:** get ready doesn't wait, so the times are P4's. There is exactly one cue: the round-end sound at 103 s.

**Edge cases, voice on, with "Get ready in" taking 1 s:**

| Time | Cue |
|---|---|
| 0 | "Get ready in" |
| 1, 2, 3 | "three", "two", "one" |
| 4 | nothing: "Ten" lasts 10 s, not more |
| 11, 12, 13 | "three", "two", "one" |
| 14 | "Eleven" |
| 22, 23, 24 | "three", "two", "one" |
| 25 | round-end sound, then "three" ("Three" starts with a 0 s round rest) |
| 26, 27 | "two", "one" |
| 28, 29 | "two", "one" ("Two") |
| 30 | "one" ("One") |
| 31 | "Workout complete. Well done!" |

### 4.4 Dependencies

No new libraries.

| Library | Version | Where | Why |
|---|---|---|---|
| `kotlinx-coroutines-core` | 1.11.0 | `:core` (already `api`), `:app` | `Flow`, `StateFlow`, `Channel` |
| `kotlinx-coroutines-test` | 1.11.0 | `:core` and `:app` tests (already there) | virtual time for `CuePlayerTest` |
| `androidx.activity:activity-compose` | 1.13.0 | `:app` (already there) | the debug activity; `by viewModels()` |
| AndroidX Lifecycle | 2.11.0 | `:app` (already there, through `lifecycle-runtime-compose`) | `ViewModel` and `viewModelScope` for the debug screen |
| JUnit Jupiter, JUnit 4, AndroidX Test | as in the catalog | tests (already there) | |

Platform APIs, all available on API 26: `android.speech.tts.TextToSpeech`, `Voice` and `UtteranceProgressListener`; `android.media.MediaPlayer` and `AudioAttributes`.

From earlier phases it uses:

- **P4:** `WorkoutSession`, `SessionEvent`, `SessionState`, `ANNOUNCEMENT_TIMEOUT_MILLIS`; in `:core` tests, the internal `SessionMachine` and `EngineFixtures.kt` (`T0`, `exampleTimeline`, `Timed`, `startAndRunOnTime()`).
- **P2:** `Timeline`, `Phase`, `ExerciseInfo`, and the `ExampleWorkouts.kt` fixtures.
- **P3:** `SettingsRepository` from `AppContainer`, for the debug screen's switch.

No fakes are needed: P2, P3 and P4 are merged.

### 4.5 From earlier phases

What P4 settled about the interfaces this phase uses, from its SPEC and handover:

- **Events** (`SessionEvent` KDoc):
  - `PhaseStarted` is sent when a phase begins from its start: at `start()`, when the phase before it ends, or on the first `resume()` after `next()` or `previous()` moved to it. Its displayed time is its full duration.
  - `SecondChanged` isn't sent for a phase's first second. After a wake-up more than a second late, only the current value is sent.
  - `RoundEnded(roundNumber)` is sent when the last exercise of a round ends on time and another round follows, just before the next phase's `PhaseStarted`. Moving with Next or Previous never sends it.
  - `Completed` is sent when the last exercise ends, or Next is pressed on it. `Completed` and `Stopped` are the last events.
- **Single collector:** "Collect it once: each event goes to one collector. Events sent before the collector starts are kept, so `start` may come first." The flow completes after `Completed` or `Stopped`, or when the session's scope is cancelled.
- **The get-ready wait:** with `waitForAnnouncement = true`, get ready stays at 3 s until `announcementFinished()`, or until `ANNOUNCEMENT_TIMEOUT_MILLIS` (5 s). A call that comes late, twice or while paused is ignored. Pausing while waiting ends the wait, and resume starts the countdown at once.
- **Threads:** commands may be called from any thread, including from a collector of `events`. They apply in call order. "A command from another thread may return before it is applied, if the session is applying another one at that moment." So `announcementFinished()` from a TTS callback thread is safe.
- **Pending wiring left by P4:** "`waitForAnnouncement` stays `false` until the audio exists. P8, or P7 if P5 is merged first, passes the voice setting, and P5's cue player calls `announcementFinished()`." P5 provides the cue player side. The runner side stays pending (§4.3.6).
- **Test pitfall:** when a session runs in `runTest`'s `backgroundScope`, `advanceUntilIdle()` doesn't run its timer. Wait in the foreground instead, for example with `session.state.first { … }`, or use `advanceTimeBy()`. This applies to `CuePlayerTest`. It's also in CLAUDE.md.

From P3: instrumented test names are camelCase (also in CLAUDE.md). Storage warnings go through an `onWarning` callback that `AppContainer` logs in debug builds only; the audio classes follow the same pattern.

## 5. Security and privacy

With reference to plan §3:

- **No permissions.** TTS and playing a bundled resource need none. The `<queries>` entry only makes the TTS engine visible on API 30 and higher; it isn't a permission. The build's no-`INTERNET` check still applies (NF-4).
- **Offline voice.** The speaker picks a voice that doesn't need the network (TTS-4), so exercise names are synthesised on the tablet.
- **Debug screen only in debug builds.** Its activity is exported, with a launcher icon, but only `app/src/debug/AndroidManifest.xml` declares it. The release manifest still exports only `MainActivity`; T5 checks the merged release manifest.
- **Debug-only logging** (plan §3): audio warnings go through `logAudioWarning`, which logs only in debug builds.
- **Never blocks the workout.** If TTS fails, `announcementFinished()` is called at once, and P4's 5 s timeout is the backstop. If the warm-up fails, the speaker is READY anyway. If the chime's callback never comes, waiting speech goes ahead after 2 s. Speaking never throws.
- **Warm-up file.** The warm-up writes a temporary file in the app's private cache folder and deletes it; no permission is needed.
- **Our own sound.** The chime is generated by a script in the repo (D-10), so there's no third-party audio.

## 6. Testing

JVM tests run with `./gradlew build` and in CI. `:core` tests drive the internal `SessionMachine` at chosen times, as P4's tests do, and pass each event through the planner. `:app` tests use fakes for the speaker and the sound player, and `runTest` with virtual time. Instrumented tests run on the tablet; use the `tablet` skill to run one without wiping the app's data.

| Test | Type | Covers |
|---|---|---|
| `CuePlannerTest`, one event at a time:<br>• get ready: "Get ready in" at `PhaseStarted`, "three" at `CountdownStarted`, "two" and "one" at `SecondChanged`<br>• exercise names: 10 s gets none, 11 s gets its name<br>• exercises of 3, 2 and 1 s get `Count(duration)` at `PhaseStarted`; 4 s gets nothing at start and "three" at 3<br>• `SecondChanged` to 3, 2, 1 gives the words; to 4 or more, nothing<br>• rests give nothing, whatever their kind or length<br>• `RoundEnded` gives the sound; `Paused` and `Stopped` give `StopSpeech`; `Resumed` gives nothing; `Completed` gives "Workout complete"<br>• voice off: the same events give no `Speak`, and the sound and `StopSpeech` stay<br>• `Count` rejects 0 and 4 | unit | TTS-1, TTS-2, TTS-3, TTS-7, TTS-9, TTS-10, TTS-12, SND-1, SND-2 |
| `ExampleCuesTest`, a machine run through the planner:<br>• the §4.4 example, voice on, "Get ready in" taking 1.5 s: exactly the §4.3.8 table, cues and times<br>• the §4.4 example, voice off: only the sound, at 103 s<br>• the edge cases, voice on: exactly the §4.3.8 table<br>• pause during "Get ready in", then resume: `StopSpeech`, then "three" at once, and no "Get ready in" again<br>• pause in an exercise's countdown at the instant of "two", resume 10 s later: `StopSpeech`, then "one" exactly 1 s after the resume, and nothing replayed<br>• pause on Jumping jacks, Next twice, resume: no cue while moving; on resume, "High knees"<br>• Next from High knees into round 2: no sound; resume gives "Plank"<br>• Previous on the first exercise, then resume: its name again<br>• Next on Crunches: "Workout complete"<br>• Stop while paused: `StopSpeech` only | unit | TTS-1, TTS-2, TTS-3, TTS-7, TTS-9, TTS-10, TTS-12, SND-1, SND-2, EX-7 |
| `CuePlayerTest`, with fakes:<br>• the speaker gets the texts in cue order<br>• "Get ready in": `announcementFinished()` is called when the fake reports done; at once when the speaker isn't READY, or when `speak` fails<br>• a sound followed by a name or a number: the speech waits for the sound's `onDone`, then goes in order<br>• if the sound's `onDone` never comes, the speech goes after `ROUND_END_SOUND_TIMEOUT_MILLIS`<br>• `StopSpeech` calls `speaker.stop()` and drops speech waiting for the sound<br>• voice off: the speaker is never called, and the sound plays<br>• `play()` returns when the events end, without calling `stop()`<br>• `prepare()`: with voice off it doesn't touch the speaker; it returns whether the speaker is READY<br>• with a real `WorkoutSession` of the §4.4 example in virtual time, and a fake speaker that takes 1.5 s to say "Get ready in": all 24 phrases in order, and the session completes at 264.5 s | unit | TTS-3, TTS-9, TTS-10, SND-1; plan §2.3 steps 6–7 |
| `TtsSpeakerDeviceTest`, on the tablet in Spanish, with the fixed phrases as `warmUpTexts`:<br>• `prepare()` reaches READY within `SPEAKER_PREPARE_TIMEOUT_MILLIS`<br>• the selected voice's locale is `en_US`, and it doesn't need the network<br>• once READY, the warm-up file is gone<br>• once READY, "three" is done within 1.5 s of `speak()`<br>• `speak()` calls `onDone`<br>• `stop()` during a long sentence: `onDone` isn't called<br>• `speak()` before `prepare()` calls `onDone` at once | instrumented | TTS-4, TTS-10, TTS-11 |
| `MediaSoundPlayerDeviceTest`: `playRoundEnd()` calls `onDone` within 2 s; the sound lasts 0.8 s or less | instrumented | SND-1 |
| `SpokenTextDeviceTest`: with a Spanish (`es-ES`) configuration, every `Utterance` gives the English phrase from §4.2, and `fixedPhrases` gives the five fixed phrases in order | instrumented | TTS-11 |

### Manual checks on the tablet

The tablet stays in Spanish. The user listens; adb can't check sound. Use the debug screen, "WT audio debug". Record each result in the handover.

1. **Chime (T2):** play `round_end.wav` on the PC. It's short and pleasant, and clearly a "round done" sound. Tune the script's parameters until the user is happy.
2. **Example, voice on:** run it to the end (4:23 plus the announcement). Every cue in the specification's §4.4 audio column is heard, in English, at the right moment. Each number matches the display changing to 3, 2 and 1. Rests are silent. The chime plays once, at the start of the Warm-up round rest. "Workout complete. Well done!" plays at the end. (TTS-1, TTS-2, TTS-7, TTS-10, TTS-11, TTS-12, SND-1, SND-2)
3. **Edge cases, voice on:** "Ten" isn't named and "Eleven" is. After "Eleven", the chime plays, then "three" for the 3 s exercise, after the chime and not over it. The 2 s and 1 s exercises count from "two" and "one". (TTS-1, TTS-2, SND-1)
4. **Pause and resume:** pause while a name is being spoken: the speech stops at once. Resume: it isn't repeated. Pause just after "two": on resume, "one" comes on time, and nothing is repeated. Pause during "Get ready in": on resume, the countdown carries on. (TTS-3)
5. **Next and Previous:** pause, press Next into round 2, then resume: no chime, and "Plank" is spoken. (Specification §5.1)
6. **Voice off:** turn the switch off and run the example: no speech at all, the countdown in get ready starts immediately, and the chime still plays. Close the app, swipe it away, reopen the debug screen: the switch is still off. Turn it back on. (TTS-9)
7. **Offline:** turn Wi-Fi off and run the edge-case workout: every cue is heard. (TTS-4)
8. **Volume:** between cues, the volume keys change the media volume, and both the voice and the chime follow it. (Q3)
9. **Start-up (Q6):** with voice on, force-stop the app and the TTS engine (`adb shell am force-stop` on both packages). Open the debug screen and start the example at once: "Preparing" shows for a few seconds, then "Get ready in" is heard in full before the countdown, and "three" comes as the display changes to 3. Force-stop both again, open the debug screen, wait 15 s, and start: "Get ready in" starts at once. (TTS-10)

## 7. Tasks

| ID | Task |
|---|---|
| T1 | `Cue`, `Utterance` and `CuePlanner` in `:core`, with `CueFixtures.kt`. Tests: `CuePlannerTest`, `ExampleCuesTest`. |
| T2 | The round-end sound: `tools/generate-round-end-sound.ps1`, the generated `round_end.wav`, `*.wav binary` in `.gitattributes`. `SoundPlayer` and `MediaSoundPlayer`. Test: `MediaSoundPlayerDeviceTest`. The user listens to the chime (manual check 1). |
| T3 | `Speaker`, `SpeakerStatus` and `TtsSpeaker`; the `<queries>` entry in the manifest. Test: `TtsSpeakerDeviceTest`. Measure `prepare()` on the tablet from a cold engine and record it. |
| T4 | `SpokenText` and `strings_audio.xml`; `CuePlayer`; the `AppContainer` entries and `logAudioWarning`; `volumeControlStream` in `MainActivity`. Tests: `CuePlayerTest`, `SpokenTextDeviceTest`. |
| T5 | The debug screen: manifest, activity, ViewModel, screen, workouts and strings. Install it on the tablet and take a screenshot. Check that the merged release manifest exports only `MainActivity`. |
| T6 | The warm-up (Q6): `warmUpTexts` and the warm-up in `TtsSpeaker`; `SPEAKER_PREPARE_TIMEOUT_MILLIS` of 10 s; `SpokenText.fixedPhrases`; `prepareAudio()` in `AppContainer`, called by `MainActivity` and the debug activity. Tests: the new cases in `TtsSpeakerDeviceTest` and `SpokenTextDeviceTest`. Measure `prepare()` with the warm-up from a cold engine, and record it in the handover. |
| T7 | Manual checks 2–9 with the user, and fixes for anything they find. KDoc on the public API for P7. `./gradlew build`, handover, pull request. |

## 8. Done criteria

- [x] All tasks done
- [x] Unit tests and lint pass locally and in CI
- [x] `ExampleCuesTest` matches the §4.3.8 sequences exactly
- [x] `TtsSpeakerDeviceTest`, `MediaSoundPlayerDeviceTest` and `SpokenTextDeviceTest` pass on the tablet
- [x] Manual checks 1–9 pass: on the tablet, still in Spanish, every cue is heard in English at the right moment
- [x] `:core` still has no Android dependency; the release manifest exports only `MainActivity`, and has no `INTERNET` permission
- [x] HANDOVER.md is up to date, with the runner wiring under "Pending wiring", and the plan's status table is updated

## 9. Open questions

None at the moment.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | Where do the spoken phrases live? They're user-facing text, but they must stay English even after the UI is translated (TTS-11). | **In `res/values/strings_audio.xml`**, marked `translatable="false"`, not as constants. `:core` gives an `Utterance`, and `:app` turns it into text (§4.2). |
| Q2 | How is the debug screen reached? | **A separate activity with its own launcher icon**, "WT audio debug", in debug builds only (§4.3.7). The release build doesn't contain it. A shell can't start an activity that isn't exported, so it's exported either way. |
| Q3 | Should the volume keys always control the cue volume? On Android 8, they change the ringer volume when nothing is playing, which happens between cues. | **Yes.** `MainActivity` sets `volumeControlStream` to the media stream; the debug activity does the same. Added to specification §5.3 (v0.8). |
| Q4 | Next on the last exercise ends the workout (specification §5.1). Is "Workout complete. Well done!" spoken then too? | **Yes**, as TTS-7 says it is spoken when the workout ends. P4 sends the same `Completed` event in both cases. Added to specification §5.1 (v0.8). |
| Q5 | If the English (US) voice isn't available, voice cues are silent. Should the app tell the user? | **Not in v1.** It's logged in debug builds, and the debug screen shows the speaker status. A message is a future idea, added to specification §10 (v0.8). |
| Q6 | Found in T3: on the tablet, the first phrase after the TTS engine connects starts 3–4 s late. So "Get ready in" can still be playing after P4's 5 s timeout, and the countdown would start over it. The first numbers are also ~250 ms behind the display. What should be done? | **Warm the engine up, prepare when the app opens, and wait longer.** `prepare()` synthesises the fixed phrases to a temporary file before it's READY (§4.3.3). The activities start preparing when they're created (§4.3.6). `SPEAKER_PREPARE_TIMEOUT_MILLIS` goes from 5 s to 10 s, so a workout started right after a cold start waits for the voice rather than losing "Get ready in". New task T6. |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-04 | First draft, quoting specification v0.7 |
| 0.2 | 2026-11-04 | Q1 to Q5 answered as assumed: the phrases live in `strings_audio.xml`; the debug screen has its own launcher icon; the volume keys control the media volume; Next on the last exercise says "Workout complete"; no message when the voice is missing. Quoting specification v0.8. Status `Ready`. |
| 0.3 | 2026-11-04 | Q6, from T3's measurements: the speaker warms the engine up with `SpokenText.fixedPhrases` before it's READY; `prepareAudio()` starts preparing when the activities are created; `SPEAKER_PREPARE_TIMEOUT_MILLIS` is 10 s. T3's timings in §4.3.3. New task T6 and manual check 9; the old T6 is now T7. Brought in line with two recorded deviations: the fakes share `AudioFakes.kt`, and the debug log keeps 50 lines without ticks that give no cue. |
