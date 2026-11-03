# P4 — Workout engine: Handover

| | |
|---|---|
| **Status** | Done. Merged in pull request [#4](https://github.com/davamix/workout-timer/pull/4) on 2026-11-03. |
| **Spec** | [SPEC.md](SPEC.md), version v0.3 |
| **Branch** | `phase/p4-engine` |
| **Last session** | 2026-11-03 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Phase done: no prompt needed.

## Current state

All six tasks are done and merged into `main` through pull request [#4](https://github.com/davamix/workout-timer/pull/4) on 2026-11-03. CI passed on the pull request: 1m58s on `d3613fd`, the last code change, and again on the handover commit `9148efc`.

- `./gradlew build` passes locally: 270 unit tests (212 in `:core`, 115 of them new; 58 in `:app`), 0 failures. Lint, ktlint and the no-INTERNET check are clean.
- `connectedDebugAndroidTest` on the tablet: 13 tests. 10 passed, and the 3 `PersistenceCheck` steps were skipped on purpose. On its own, `EngineClockDeviceTest` also passed 3 runs in a row.
- `:core`'s runtime classpath is still only `kotlin-stdlib` and `kotlinx-coroutines-core`.
- Specification v0.7 (P4 Q1 and Q2) and SPEC v0.3 (the command queue) are on `main`.

What's new (base package `io.github.davamix.workouttimer`):

| Where | Files | Main API |
|---|---|---|
| `:core` `core/engine` | `Clock.kt`, `SessionState.kt`, `SessionEvent.kt`, `SessionMachine.kt`, `WorkoutSession.kt` | `Clock`, `SessionStatus`, `SessionState`, `SessionEvent`, `WorkoutSession`, `ANNOUNCEMENT_TIMEOUT_MILLIS`; internal: `SessionMachine` |
| `:app` `di/AppContainer.kt` | — | `clock` (`SystemClock.elapsedRealtime()`) |

How P5 and P7 use it:

- Create `WorkoutSession(timeline, container.clock, viewModelScope, waitForAnnouncement)`, then call `start()`.
- The screen follows `state`. The audio collects `events` once.
- Commands can be called from any thread, and from inside a collector.

## Next steps

None for P4. These phases can start now, as all their dependencies are done:

- **P5** (audio cues), which depends on P4;
- **P6** (workout management UI), which depends on P2;
- **P7** (runner UI), which depends on P4.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `SessionRunTest` (19) |
| T2 | Done | `PauseResumeTest` (10), `SessionCommandsTest` (11) |
| T3 | Done | `NavigationCommandsTest` (32), `GetReadyHandshakeTest` (15, including the pause-while-waiting case listed under `PauseResumeTest` in SPEC §6). `SessionCommandsTest` covers all seven commands. |
| T4 | Done | `AccuracyTest` (14): a 61:03 workout (`hourLongTimeline`) in 13 seeded random runs. No fix was needed. A mutation check (scheduling the next phase from the tick time instead of the exact end) makes 13 of the 14 fail. |
| T5 | Done | `WorkoutSessionTest` (14), including commands from 8 threads at once and collectors that call back |
| T6 | Done | `AppContainer.clock`; `EngineClockDeviceTest` (2) passes on the tablet. Pull request #4, merged. |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-03 | `SessionMachine.state()` takes no `now`. It returns the state as of the last call, and `remainingSeconds` is the value last reported by `PhaseStarted` or `SecondChanged`. In SPEC v0.3. | The state and the events can never disagree. Every caller advances the machine to `now` first anyway. It is internal, so P5 and P7 aren't affected. |
| 2026-11-03 | `nextWakeAt()` is computed from the displayed second: *phase start + (duration − displayed + 1) × 1000*. | It needs no `now`, and it gives the next whole second of the phase, or its end when 1 s is displayed. |
| 2026-11-03 | `ANNOUNCEMENT_TIMEOUT_MILLIS` is declared in `SessionMachine.kt`, next to the code that applies it. | Same package as `WorkoutSession`, so the public API is as SPEC §4.2 lists it. |
| 2026-11-03 | `start()` also applies elapsed time first, like the other commands. | One rule for every command (SPEC §4.3). Before start it has no effect. |
| 2026-11-03 | The internal sleep hook is `sleepFor: (plannedMillis: Long) -> Long`, which turns each planned sleep into the actual one. The timer sleeps with `withTimeoutOrNull(sleepFor(millis)) { replan.receive() }`. In SPEC v0.3. | A command must be able to cut a sleep short. With a timeout around the replan signal, that needs no extra coroutine or `select`. Tests still make wake-ups late or early. |
| 2026-11-03 | `WorkoutSession` queues every command. Only one call at a time applies the queue and publishes the results; a call made while another is applying it just queues its command. In SPEC v0.3. | On `Dispatchers.Main.immediate`, sending an event can resume a collector inline. If that collector calls a command, as P5 might, the command would otherwise run in the middle of a batch and send its events out of order. The lock alone can't stop this, because it is re-entrant on the same thread. With the queue disabled, 2 `WorkoutSessionTest` cases fail. |
| 2026-11-03 | A command from another thread may return before it is applied, if the session is applying another one at that moment. It is applied as soon as that one finishes, in call order. In SPEC v0.3. | This follows from the queue. On one thread with no collector calling back, as in P7's UI, every command is applied before it returns. |
| 2026-11-03 | SPEC v0.3 went to `main`, and `main` was merged into the phase branch. | Phase SPEC versions are pushed to `main` (CLAUDE.md). The merge keeps the pull request's SPEC.md the same as `main`'s. |

## Known issues

- **For P5 and P7 tests:** when a session runs in `runTest`'s `backgroundScope`, `advanceUntilIdle()` doesn't run its timer, because it ignores work that only `backgroundScope` has scheduled. Wait in the foreground instead, for example with `session.state.first { it.status == COMPLETED }` or `session.events.toList()`. `advanceTimeBy()` does run it. See `WorkoutSessionTest.awaitEnd()`.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25. They are harmless (noted in P1).

## Pending wiring

- `waitForAnnouncement` stays `false` until the audio exists. P8, or P7 if P5 is merged first, passes the voice setting, and P5's cue player calls `announcementFinished()`.

## How to verify

```
./gradlew build                                               # 270 unit tests, lint, ktlint, no-INTERNET check
./gradlew :core:test --tests "*.engine.*"                     # this phase's 115 tests only
./gradlew :core:dependencies --configuration runtimeClasspath # only kotlin-stdlib and kotlinx-coroutines-core
```

On the tablet, without wiping the app's data (see the `tablet` skill):

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class io.github.davamix.workouttimer.di.EngineClockDeviceTest io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner | tail -8   # OK (2 tests)
```

## Session log

Newest first.

### 2026-11-03 (close)

- The user merged pull request #4. P4 is closed: done in plan §5.1, done criteria ticked in SPEC.md §8.

### 2026-11-03

- **Done:**
  - Wrote SPEC v0.1. The user accepted the draft assumptions for Q1 (no round-end sound on Next or Previous) and Q2 (the completed screen shows the planned total). They went into specification v0.7 and SPEC v0.2, which became `Ready`.
  - Implemented T1–T6.
  - Recorded the command queue in SPEC v0.3.
  - Opened pull request #4.
- **Notes:**
  - Mutation checks confirmed that `AccuracyTest` catches drift and that `WorkoutSessionTest` catches re-entrant commands.
  - `connectedDebugAndroidTest` uninstalls the app, so it was reinstalled with `installDebug` afterwards.
