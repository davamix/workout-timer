# P3 — Storage: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
| **Branch** | `phase/p3-storage` |
| **Last session** | 2026-11-03 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P3 (Storage) of the workout timer.

1. Use the phase-start skill for P3. Read only docs/phases/P3-storage/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p3-storage. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. T1–T3 are JVM only. The tablet is needed from T4 (instrumented tests) and in T5 (manual checks, including a reboot that I must unlock): check it with the tablet skill before starting those. P2 is merged, so no fakes are needed. P6 may run in parallel and also edits app/build.gradle.kts and AppContainer.kt: add only P3's lines, and if P6 is already merged with its own :app unit-test setup, reuse it.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

Nothing implemented yet. SPEC.md v0.2 is `Ready`; its open questions are all answered with the draft assumptions.

## Next steps

1. Start `T1`: add the `:app` unit-test setup and `kotlinx-serialization-json` to `app/build.gradle.kts`, then write `WorkoutFileFormat.kt` and `WorkoutFileFormatTest`.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | To do | |
| T2 | To do | |
| T3 | To do | |
| T4 | To do | |
| T5 | To do | |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

None yet.

## Pending wiring

None expected. P3 adds both repositories to `AppContainer`; P5, P6 and P7 use them from there.

## How to verify

```
./gradlew build                          # unit tests in :core and :app, lint, ktlint
./gradlew connectedDebugAndroidTest      # instrumented tests on the tablet (uninstalls the app afterwards)
```

The manual persistence check is in SPEC.md §6.

## Session log

Newest first.
