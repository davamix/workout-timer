# P2 — Domain model and timeline: Specification

| | |
|---|---|
| **Status** | Ready, v0.2 |
| **Phase** | P2, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P1 (done) |
| **Branch** | `phase/p2-domain` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

The workout model, and the pure logic that turns a workout into the ordered list of phases it runs through. All of it lives in `:core`, in plain Kotlin, fully covered by unit tests.

Later phases build on it:

| Phase | Uses |
|---|---|
| P3 | stores the model, implementing the `WorkoutRepository` interface defined here |
| P4 | runs the timeline |
| P5 | turns the timeline's phases into cues |
| P6 | edits the model, using validation, totals and the unused-value rules |
| P7 | displays the phases |

## 2. Requirements covered

Quoted from [SPECIFICATION.md](../../SPECIFICATION.md) v0.6.

| ID | Requirement (quoted) | What this phase does for it |
|---|---|---|
| WM-1 | "Each exercise has a **name** and a **duration** in seconds. A workout-level default duration applies to every exercise that doesn't set its own." | Model, plus effective-duration resolution |
| WM-2 | "Rests are set in seconds; 0 means no rest. The workout sets a default exercise rest and a default round rest. Each exercise can set its **own rest**, used after it. Each round can set its **own round rest**, used after it." | Model, plus effective-rest resolution |
| WM-6 | "The editor and the workout list show the workout's total duration, from Start to completion: get ready, exercises and rests." | The timeline total, and an estimate that also works for incomplete workouts in the editor. P6 shows them. |
| WM-7 | "Validation: the workout, each round and each exercise need a name; a workout has at least one round; a round has at least one exercise; durations are at least 1 s; rests are 0 s or more. No duration or rest can be longer than 99:59 (99 minutes 59 seconds)." | `validate()` returns every error found. P6 shows them. |
| EX-1 | "Starting a workout runs its phases following the sequencing rules in §4.3." | The timeline applies the §4.3 rules (quoted below). P4 runs it. |
| EX-3 | "Each rest lasts its configured duration. A value of 0 s means no rest." | Rest phases get their effective duration. 0 s rests produce no phase. |
| EX-7 | "A **get-ready** countdown of **3 s** runs before the first exercise of the first round, and only there. The screen shows "Get ready", the countdown, and the name of the first exercise. The voice for it is described in `TTS-10`." | The timeline starts with one 3 s get-ready phase that knows the first exercise. The voice is P5; the screen is P7. |
| EX-9 | "While paused, **Next** and **Previous** move to another exercise without asking for confirmation (see the table below)." | Next/previous targets for every phase, following the table quoted below. P4 moves; P7 shows the buttons. |

### Specification sections this phase implements (quoted)

**§4.1, rests:** "A rest of **0 seconds means no rest**: the next exercise or round starts immediately."

**§4.2, structure:**

```
Workout
├── name
├── default exercise duration (s)
├── default exercise rest (s, ≥ 0)
├── default round rest (s, ≥ 0)
└── rounds (ordered, at least 1)
    ├── name
    ├── round rest (s), optional: falls back to the default round rest
    └── exercises (ordered, at least 1)
        ├── name
        ├── duration (s), optional: falls back to the default exercise duration
        └── rest (s), optional: falls back to the default exercise rest
```

> - **Defaults:** an optional value that isn't set falls back to the workout default. Changing a default updates every exercise or round that doesn't set its own value. Those that set their own keep it.
> - **Unused values:** two values are never used. The last exercise of a round has no exercise rest, because the round rest runs instead. The last round has no round rest, because the workout ends. The editor marks these fields as not applicable but keeps their values, so they apply again if the exercise or round is moved.

**§4.3, sequencing rules:**

> 1. Pressing Start runs the **get-ready** countdown (3 s).
> 2. The first exercise of the first round starts.
> 3. Inside a round, each exercise is followed by **that exercise's rest**, then the next exercise starts.
> 4. When the last exercise of a round finishes:
>    - the round-end sound plays;
>    - **that round's rest** runs (the exercise's own rest is not added on top);
>    - the first exercise of the next round starts.
> 5. When the last exercise of the last round finishes, the workout ends:
>    - no rest runs and no round-end sound plays;
>    - "Workout complete. Well done!" is spoken.
> 6. A rest of 0 s is skipped entirely: the next exercise starts immediately.

The sound and the voice in rules 4 and 5 belong to P5. P2 provides the information they need: which exercise ends a round, and which ends the workout.

**§5.1, what Next and Previous do:**

> - They move between **exercises** and skip any rest in between.
> - After moving, the workout stays **paused** at the start of the target exercise. Resume starts that exercise as normal, including its name announcement (`TTS-1`).
>
> | Paused during | Next | Previous |
> |---|---|---|
> | Get ready | First exercise | Not available |
> | An exercise | Next exercise; on the last exercise of the workout, ends the workout (completion screen) | Previous exercise; on the first exercise, restarts it |
> | A rest | The exercise after the rest | The exercise before the rest |

P2 computes the targets. Staying paused is P4's job.

**§4.4, example** (the acceptance test for this phase):

> Workout "Example" has these defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.
>
> - Round 1 "Warm-up", own round rest 30 s: Jumping jacks 30 s; Arm circles 10 s, own rest 5 s; High knees, default duration
> - Round 2 "Core": Plank 60 s, own rest 30 s; Crunches, default duration
>
> | # | Phase | Round | Name | Duration |
> |---|---|---|---|---|
> | 1 | Get ready | — | — | 3 s |
> | 2 | Exercise | Warm-up | Jumping jacks | 30 s |
> | 3 | Exercise rest | Warm-up | — | 15 s (default) |
> | 4 | Exercise | Warm-up | Arm circles | 10 s |
> | 5 | Exercise rest | Warm-up | — | 5 s (own) |
> | 6 | Exercise | Warm-up | High knees | 40 s (default) |
> | 7 | Round rest | — | — | 30 s (own) |
> | 8 | Exercise | Core | Plank | 60 s |
> | 9 | Exercise rest | Core | — | 30 s (own) |
> | 10 | Exercise | Core | Crunches | 40 s (default) |
> | | **Completed** | | | **Total 4:23** |
>
> The total runs from Start to completion: get ready 3 s + exercises 180 s + exercise rests 50 s + round rest 30 s.

## 3. Scope

### In scope

- **Model:** `Workout`, `Round` and `Exercise`, with linked defaults (effective values), the unused-value rules, ID generation, and duplication with new IDs.
- **Validation:** WM-7.
- **Timeline:** the phase types, the builder, the total, an estimated total for incomplete workouts, and the next/previous targets.
- **Storage interface:** `WorkoutRepository` and `InMemoryWorkoutRepository`.

### Out of scope

| What | Phase |
|---|---|
| JSON format and file storage | P3 |
| Running the timeline in time: clock, pause, staying paused after Next/Previous | P4 |
| Sounds and voice, including the round-end sound and the final phrase | P5 |
| Editor and list screens, error messages, how workouts are sorted in the list, names given to duplicates | P6 |
| Runner screen | P7 |

## 4. Design

### 4.1 Packages and files owned

Everything lives in `:core`. The base package is `io.github.davamix.workouttimer.core`.

```
core/src/main/kotlin/io/github/davamix/workouttimer/core/
    model/Workout.kt              Workout, Round, Exercise
    model/EffectiveValues.kt      effective durations and rests; unused-value rules
    model/Ids.kt                  newId(); duplicate() for workouts, rounds and exercises
    model/Validation.kt           Limits, validate(), ValidationError, TimeField
    timeline/Phase.kt             Phase, RestKind, ExerciseInfo
    timeline/Timeline.kt          Timeline.of(), totalSeconds, next(), previous(), NavigationTarget
    timeline/EstimatedTotal.kt    Workout.estimatedTotalSeconds()
    repository/WorkoutRepository.kt
    repository/InMemoryWorkoutRepository.kt
core/src/test/kotlin/io/github/davamix/workouttimer/core/
    ExampleWorkouts.kt            the §4.4 example and small builders, for this and later phases' tests
    model/…Test.kt, timeline/…Test.kt, repository/…Test.kt
```

**Shared file changed:** `core/build.gradle.kts`. It gains `kotlinx-coroutines-core` (for the `Flow` in `WorkoutRepository`) and `kotlinx-coroutines-test` (tests). Both are already declared in the version catalog.

### 4.2 Interfaces and data

All durations and rests are whole **seconds** (`Int`). IDs are `String`s (random UUIDs in the app, fixed values in tests).

```kotlin
// model
data class Workout(
    val id: String,
    val name: String,
    val defaultExerciseDurationSeconds: Int,
    val defaultExerciseRestSeconds: Int,
    val defaultRoundRestSeconds: Int,
    val rounds: List<Round>,
)

data class Round(
    val id: String,
    val name: String,
    val restSeconds: Int? = null,       // null: linked to the workout's default round rest
    val exercises: List<Exercise>,
)

data class Exercise(
    val id: String,
    val name: String,
    val durationSeconds: Int? = null,   // null: linked to the workout's default exercise duration
    val restSeconds: Int? = null,       // null: linked to the workout's default exercise rest
)

// Effective values: the own value if set, otherwise the workout default (§4.2 "Defaults")
fun Workout.durationOf(exercise: Exercise): Int
fun Workout.restAfter(exercise: Exercise): Int
fun Workout.restAfter(round: Round): Int

// Unused values (§4.2): false for the last exercise of a round, and for the last round
fun Round.isRestUsed(exercise: Exercise): Boolean
fun Workout.isRestUsed(round: Round): Boolean

// IDs and duplication (used by P6 for WM-4 and WM-5). Copies get fresh IDs all the way down.
fun newId(): String
fun Workout.duplicate(name: String, newId: () -> String = ::newId): Workout
fun Round.duplicate(newId: () -> String = ::newId): Round
fun Exercise.duplicate(newId: () -> String = ::newId): Exercise

// Validation (WM-7). An empty list means valid.
object Limits {
    const val MIN_DURATION_SECONDS = 1
    const val MIN_REST_SECONDS = 0
    const val MAX_SECONDS = 5999        // 99:59 (WM-7), the most mm:ss can show
}
fun Workout.validate(): List<ValidationError>

sealed interface ValidationError {
    data object WorkoutNameMissing : ValidationError
    data object NoRounds : ValidationError
    data class RoundNameMissing(val roundId: String) : ValidationError
    data class RoundHasNoExercises(val roundId: String) : ValidationError
    data class ExerciseNameMissing(val exerciseId: String) : ValidationError
    data class TimeOutOfRange(val field: TimeField, val min: Int, val max: Int) : ValidationError
}

sealed interface TimeField {
    data object DefaultExerciseDuration : TimeField
    data object DefaultExerciseRest : TimeField
    data object DefaultRoundRest : TimeField
    data class ExerciseDuration(val exerciseId: String) : TimeField
    data class ExerciseRest(val exerciseId: String) : TimeField
    data class RoundRest(val roundId: String) : TimeField
}
```

```kotlin
// timeline
const val GET_READY_SECONDS = 3

/** One exercise as it appears in the run, with everything the screen (UI-1, UI-2, UI-6) and the cues need. */
data class ExerciseInfo(
    val exerciseId: String,
    val name: String,
    val durationSeconds: Int,
    val roundId: String,
    val roundName: String,
    val roundNumber: Int,      // 1-based
    val roundCount: Int,
    val numberInRound: Int,    // 1-based
    val countInRound: Int,
) {
    val isFirstInRound: Boolean      // numberInRound == 1
    val isLastInRound: Boolean       // numberInRound == countInRound
    val isLastInWorkout: Boolean     // isLastInRound && roundNumber == roundCount
}

sealed interface Phase {
    val durationSeconds: Int
    data class GetReady(val first: ExerciseInfo) : Phase          // durationSeconds = GET_READY_SECONDS
    data class Exercise(val exercise: ExerciseInfo) : Phase       // durationSeconds = exercise.durationSeconds
    data class Rest(
        val kind: RestKind,
        override val durationSeconds: Int,                        // always ≥ 1: 0 s rests produce no phase
        val previous: ExerciseInfo,                               // the exercise before the rest
        val next: ExerciseInfo,                                   // the exercise after it (UI-5)
    ) : Phase
}

enum class RestKind { EXERCISE, ROUND }

class Timeline private constructor(val phases: List<Phase>) {
    val totalSeconds: Int                            // WM-6: sum of all phase durations
    fun next(from: Int): NavigationTarget            // EX-9; `from` is a phase index
    fun previous(from: Int): Int?                    // EX-9; null means "not available"
    companion object {
        fun of(workout: Workout): Timeline           // requires workout.validate().isEmpty()
    }
}

sealed interface NavigationTarget {
    data class GoTo(val phaseIndex: Int) : NavigationTarget   // always an Exercise phase
    data object Complete : NavigationTarget                   // Next on the last exercise
}

/** WM-6 for the editor: works on incomplete workouts too, so the total can update while editing. */
fun Workout.estimatedTotalSeconds(): Int
```

```kotlin
// repository
interface WorkoutRepository {
    val workouts: Flow<List<Workout>>     // every saved workout; emits again after each change; order not specified
    suspend fun get(id: String): Workout?
    suspend fun save(workout: Workout)    // insert, or replace the workout with the same id
    suspend fun delete(id: String)        // does nothing if the id is unknown
}

class InMemoryWorkoutRepository(initial: List<Workout> = emptyList()) : WorkoutRepository
```

The repository doesn't validate. The editor validates before saving (P6).

### 4.3 Behaviour

**Building the timeline** (`Timeline.of`) applies the §4.3 rules in one pass:

```
phases = [GetReady(first exercise)]
for each round r, for each exercise e in r:
    phases += Exercise(e)
    if e is not the last exercise of r:      rest = restAfter(e),  kind EXERCISE
    else if r is not the last round:         rest = restAfter(r),  kind ROUND   (e's own rest is ignored)
    else:                                    no rest (workout ends)
    if rest > 0: phases += Rest(kind, rest, previous = e, next = the following exercise)
```

- `Timeline.of` throws `IllegalArgumentException` if the workout isn't valid. Callers validate first.
- `ExerciseInfo` numbers are 1-based. They count every exercise and round of the workout, so they match "exercise *n* of *m*" and "round *n* of *m*" (UI-6).

**Next and previous** follow the §5.1 table:

| Current phase | `next(from)` | `previous(from)` |
|---|---|---|
| `GetReady` | `GoTo` the first exercise | `null` |
| `Exercise`, not the last one | `GoTo` the next exercise phase | `GoTo` the previous exercise phase, or the same index on the first exercise (restart) |
| `Exercise`, the last one | `Complete` | as above |
| `Rest` | `GoTo` the exercise after it | `GoTo` the exercise before it |

`previous()` returns an index (`Int?`), not a `NavigationTarget`, because it can never complete the workout. An index out of range throws `IndexOutOfBoundsException`.

**Estimated total** (`estimatedTotalSeconds`) gives the editor a running figure for workouts that aren't valid yet:

- rounds without exercises are ignored;
- negative values count as 0;
- a workout with no exercises gives 0;
- otherwise it applies the same rules as the timeline.

For any valid workout it equals `Timeline.of(workout).totalSeconds`; a test checks this.

**Validation** checks every value, including unused ones, because they become active again if the exercise or round is moved (§4.2). It reports every problem, not just the first.

### 4.4 Dependencies

| Library | Version | Where | Why |
|---|---|---|---|
| `kotlinx-coroutines-core` | 1.11.0 | `:core` main | `Flow` in `WorkoutRepository` |
| `kotlinx-coroutines-test` | 1.11.0 | `:core` test | testing the repository |
| JUnit Jupiter | 6.1.3 (BOM) | `:core` test | already set up in P1 |

Assertions use JUnit Jupiter (`org.junit.jupiter.api.Assertions`, `assertThrows`). No other libraries.

## 5. Security and privacy

- Pure logic: no I/O, no Android and no network.
- Validation is what keeps impossible values out: negative or zero durations, negative rests, and values too large to display (over 99:59, WM-7). Upper limits also keep totals far from `Int` overflow.
- The repository interface returns copies of immutable data classes, so callers can't change stored state behind its back.

## 6. Testing

All tests are JVM unit tests in `:core`, run by `./gradlew build` and in CI.

| Test | Covers |
|---|---|
| `EffectiveValuesTest`: an unset value uses the default; an own value overrides it; changing a default changes only linked values | WM-1, WM-2, §4.2 Defaults |
| `UnusedValuesTest`: the last exercise's rest and the last round's rest are unused; after reordering, the kept values are used again | §4.2 Unused values |
| `ValidationTest`: one test per rule; a valid workout gives no errors; several problems are all reported; unused values are still validated; 5999 s is accepted and 6000 s is rejected | WM-7 |
| `DuplicateTest`: every ID is new at every level; all other values are kept | WM-4/WM-5 support for P6 |
| `TimelineTest`: one test per §4.3 rule:<br>• get ready comes first and lasts 3 s<br>• the exercise rest sits between exercises<br>• the round rest replaces the last exercise's own rest<br>• no rest after the last exercise of the workout<br>• 0 s exercise and round rests are skipped<br>• a single-exercise workout gives get ready + exercise<br>• `ExerciseInfo` numbering and flags<br>• rest phases know their previous and next exercise | EX-1, EX-3, EX-7, §4.3 |
| `ExampleWorkoutTest`: the §4.4 example gives exactly the 10 phases of the table (kinds, round names, exercise names, durations), and a total of 263 s (4:23) | §4.4, WM-6 |
| `NavigationTest`: every cell of the Next/Previous table, on the §4.4 example and on a single-exercise workout | EX-9 |
| `EstimatedTotalTest`: equals the timeline total for valid workouts; incomplete workouts (empty rounds, no exercises, negative values) | WM-6 |
| `InMemoryWorkoutRepositoryTest`: save, get, replace, delete, unknown IDs; `workouts` emits after each change | Storage interface |

### Manual checks on the tablet

None. This phase has no UI or device behaviour.

## 7. Tasks

| ID | Task |
|---|---|
| T1 | Model types, effective values, unused-value rules, IDs and duplication. Tests: `EffectiveValuesTest`, `UnusedValuesTest`, `DuplicateTest`. |
| T2 | Validation (`Limits`, `validate()`, errors). Test: `ValidationTest`. |
| T3 | Timeline: `Phase`, `ExerciseInfo`, `Timeline.of`, `totalSeconds`; the `ExampleWorkouts` fixture. Tests: `TimelineTest`, `ExampleWorkoutTest`. |
| T4 | Navigation targets and the estimated total. Tests: `NavigationTest`, `EstimatedTotalTest`. |
| T5 | `WorkoutRepository` and `InMemoryWorkoutRepository`, with the coroutines dependencies. Test: `InMemoryWorkoutRepositoryTest`. |
| T6 | KDoc on the public API for P3–P7; `./gradlew build`; handover; pull request |

## 8. Done criteria

- [ ] All tasks done
- [ ] Unit tests and lint pass locally and in CI
- [ ] Every §4.3 rule, every cell of the Next/Previous table, and the §4.4 example have passing tests
- [ ] `:core` still has no Android dependency
- [ ] HANDOVER.md is up to date, and the plan's status table is updated

## 9. Open questions

None at the moment.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | Must exercises have a name? | Yes. Added to WM-7 (specification v0.6). |
| Q2 | Is there a maximum duration or rest? | Yes, 99:59 (5999 s) for every duration and rest. Added to WM-7 (specification v0.6). |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-03 | First draft |
| 0.2 | 2026-11-03 | Q1 and Q2 answered (exercise names required; 99:59 maximum), quoting specification v0.6. Status `Ready`. |
