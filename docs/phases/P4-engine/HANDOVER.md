# P4 — Workout engine: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
| **Branch** | `phase/p4-engine` |
| **Last session** | — |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P4 (Workout engine) of the workout timer.

1. Use the phase-start skill for P4. Read only docs/phases/P4-engine/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p4-engine. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. T1–T5 are pure Kotlin in :core (package core/engine) and need no tablet. Most tests drive the internal SessionMachine with chosen times; the driver tests use runTest with Clock { testScheduler.currentTime }. Only T6 touches :app (AppContainer.clock) and needs the tablet connected, for EngineClockDeviceTest. No fakes are needed: P2 is merged. P5 and P7 build on the public API in SPEC.md §4.2, so record any change to it.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

Nothing implemented yet. SPEC.md v0.2 is `Ready`.

## Next steps

1. Start `T1`: in `core/src/main/kotlin/io/github/davamix/workouttimer/core/engine/`, add `Clock`, `SessionStatus`, `SessionState`, `SessionEvent` and the internal `SessionMachine` with `start`, `advanceTo`, `state` and `nextWakeAt` (no announcement wait yet). Add `EngineFixtures.kt` and `SessionRunTest`, including the §4.4 event sequence in SPEC.md §4.3.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `SessionRunTest` (19) |
| T2 | To do | |
| T3 | To do | |
| T4 | To do | |
| T5 | To do | |
| T6 | To do | |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-03 | `SessionMachine.state()` takes no `now`, unlike SPEC §4.2's `state(now)`. It returns the state as of the last call, and `remainingSeconds` is the value last reported by `PhaseStarted` or `SecondChanged`. | The state and the events can never disagree. Every caller advances the machine to `now` first anyway. It is internal, so P5 and P7 aren't affected. |
| 2026-11-03 | `nextWakeAt()` is computed from the displayed second: *phase start + (duration − displayed + 1) × 1000*. | It needs no `now`, and it gives the next whole second of the phase, or its end when 1 s is displayed. |

## Known issues

None yet.

## Pending wiring

- `waitForAnnouncement` stays `false` until the audio exists. P8 (or P7, if P5 is merged first) passes the voice setting, and P5's cue player calls `announcementFinished()`.

## How to verify

```
./gradlew build                                               # :core and :app unit tests, lint, ktlint
./gradlew :core:test --tests "*.engine.*"                     # this phase's tests only
./gradlew :core:dependencies --configuration runtimeClasspath # still only kotlin-stdlib and kotlinx-coroutines-core
```

`EngineClockDeviceTest` runs on the tablet (T6); the `tablet` skill shows how to run one instrumented test.

## Session log

Newest first.
