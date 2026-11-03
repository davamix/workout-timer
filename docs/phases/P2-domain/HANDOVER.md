# P2 — Domain model and timeline: Handover

| | |
|---|---|
| **Status** | In progress: all tasks done; pull request [#2](https://github.com/davamix/workout-timer/pull/2) is waiting for the user to merge it |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
| **Branch** | `phase/p2-domain` |
| **Last session** | 2026-11-03 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P2 (Domain model and timeline) of the workout timer.

1. Use the phase-start skill for P2. Read only docs/phases/P2-domain/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p2-domain. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. This phase is pure Kotlin in the :core module. The tablet isn't needed, and :core must not get any Android dependency. All tasks are done and pull request #2 is open: check whether it has been merged or has review comments before doing anything else.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

All six tasks are implemented on `phase/p2-domain`. Pull request [#2](https://github.com/davamix/workout-timer/pull/2) is open.

- `./gradlew build` passes locally: 94 `:core` unit tests (0 failures), lint and ktlint clean, no compiler warnings.
- CI passes on pull request #2 (1m35s, on commit `d0e5b7a`, the last code change).
- The §4.4 example gives exactly the 10 phases of its table and a total of 263 s (4:23).
- `:core`'s runtime classpath holds only `kotlin-stdlib` and `kotlinx-coroutines-core`; there is no Android dependency.
- Every public declaration has KDoc for P3–P7.

What's in `:core` (package `io.github.davamix.workouttimer.core`):

| Package | Files | Main API |
|---|---|---|
| `model` | `Workout.kt`, `EffectiveValues.kt`, `Ids.kt`, `Validation.kt` | `Workout`, `Round`, `Exercise`; `durationOf`, `restAfter`, `isRestUsed`; `newId`, `duplicate`; `Limits`, `validate`, `ValidationError`, `TimeField` |
| `timeline` | `Phase.kt`, `Timeline.kt`, `EstimatedTotal.kt` | `GET_READY_SECONDS`, `ExerciseInfo`, `Phase`, `RestKind`; `Timeline.of`, `totalSeconds`, `next`, `previous`, `NavigationTarget`; `estimatedTotalSeconds` |
| `repository` | `WorkoutRepository.kt`, `InMemoryWorkoutRepository.kt` | `WorkoutRepository`, `InMemoryWorkoutRepository` |

Test fixtures live in `core/src/test/.../core/ExampleWorkouts.kt`: `exampleWorkout` (§4.4), and the builders `exercise()`, `round()` and `workout()`, which give fixed IDs `e-<name>`, `r-<name>` and `w-<name>`.

## Next steps

1. Wait for the user to merge pull request #2. Fix any review comments on `phase/p2-domain`, then run `./gradlew build` and push.
2. After the merge, close the phase:
   - on `main`, set P2 to `Done` in plan §5.1, then commit and push;
   - set this handover's status to `Done` with the PR link, replace the prompt with "Phase done: no prompt needed.", and set T6 to `Done`.
3. Phases P3, P4 and P6 can then start.

## Tasks

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `EffectiveValuesTest` (10), `UnusedValuesTest` (9), `DuplicateTest` (6). The `ExampleWorkouts.kt` builders were added here because the T1 tests need them. |
| T2 | Done | `ValidationTest` (18) |
| T3 | Done | `TimelineTest` (19), `ExampleWorkoutTest` (2). `exampleWorkout` added to `ExampleWorkouts.kt`. |
| T4 | Done | `NavigationTest` (12), `EstimatedTotalTest` (11) |
| T5 | Done | `InMemoryWorkoutRepositoryTest` (7) |
| T6 | In review | KDoc done, build passes, handover written, pull request #2 open. Done once merged. |

## Decisions and deviations

None of these change SPEC.md. They settle points it doesn't specify.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-03 | A name that is empty or only whitespace counts as missing (`isBlank()`). | WM-7 asks for a name, and a blank one would show nothing. |
| 2026-11-03 | `validate()` lists errors in workout order: the workout's name, its three defaults, `NoRounds`, then for each round its name, its rest and `RoundHasNoExercises`, followed by each of its exercises (name, duration, rest). Defaults are checked once, where they're defined; linked values aren't reported again. | The order is stable, so P6 can rely on it, and each field is reported once. |
| 2026-11-03 | `Round.isRestUsed(exercise)` and `Workout.isRestUsed(round)` match by ID. If the exercise or round isn't there, they throw `IllegalArgumentException`. | The SPEC doesn't cover that case, and a silent `true` or `false` would hide a bug in the caller. |
| 2026-11-03 | `Phase.Rest` checks `durationSeconds ≥ 1` on creation and throws `IllegalArgumentException` otherwise. | This enforces the SPEC's "always ≥ 1" note. |
| 2026-11-03 | `Timeline.of` and `estimatedTotalSeconds` share one internal function, `Workout.exerciseSteps()`. It drops rounds without exercises before deciding which round is last. So in the estimate, if only empty rounds follow a round, that round counts as the last one and its round rest isn't added. | The SPEC says "rounds without exercises are ignored", and the timeline and the estimate should apply the same rules. The estimate then matches what the workout would run once the empty rounds are filled in or removed. |
| 2026-11-03 | `estimatedTotalSeconds` adds up in `Long` and caps the result at `Int.MAX_VALUE`. | Unvalidated values could overflow an `Int`. Validated workouts are far below the cap. |
| 2026-11-03 | `kotlinx-coroutines-core` is an `api` dependency of `:core`, not `implementation`. | `Flow` is part of the public `WorkoutRepository` interface, so `:app` needs it on its compile classpath. |
| 2026-11-03 | `InMemoryWorkoutRepository` is backed by a `MutableStateFlow`, so `workouts` has state-flow semantics. Saving an identical workout or deleting an unknown ID doesn't emit, and a slow collector may skip intermediate lists but always gets the latest one. The `WorkoutRepository` KDoc says so. | This is how the SPEC's "emits again after each change" reads for a flow of current state. P3's file-backed implementation should behave the same. |

## Known issues

- **Test fixtures stay in `:core`.** `ExampleWorkouts.kt` is in `core/src/test`, so only `:core` tests can use it. That covers P4 and P5. If P3 or P6 need it in `:app` tests, `:core` would need the `java-test-fixtures` plugin. That is a shared build-file change, to be agreed in that phase.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25, from its embedded Kotlin compiler. They are harmless (also noted in P1).

## Pending wiring

None. P3 implements `WorkoutRepository` with file storage and adds it to `AppContainer`.

## How to verify

```
./gradlew build                                               # 94 :core unit tests, lint, ktlint
./gradlew :core:test --tests "*ExampleWorkoutTest"            # the §4.4 acceptance test
./gradlew :core:dependencies --configuration runtimeClasspath # only kotlin-stdlib and kotlinx-coroutines-core
```

There are no tablet checks: this phase has no UI.

## Session log

Newest first.

### 2026-11-03

- **Done:** T1–T6 in one session, one commit per task. P2 was marked `In progress` in plan §5.1 on `main`. Pull request #2 is open.
- **Phase docs:** SPEC.md and HANDOVER.md were enough. Only plan §5.1 was opened, to check P1's status and to update P2's.
- **Notes:**
  - Running `ktlintFormat` and `build` in the same Gradle call can report stale ktlint violations. Run `./gradlew :core:ktlintFormat` first, then `./gradlew build`.
  - `ktlint_official` breaks long chained expressions inside `sumOf { }` into hard-to-read lines. `estimatedTotalSeconds` uses a plain loop instead.
