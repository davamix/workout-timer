# P7 — Workout runner UI: Handover

| | |
|---|---|
| **Status** | Not started |
| **Spec** | [SPEC.md](SPEC.md), version v0.1 |
| **Branch** | `phase/p7-runner-ui` |
| **Last session** | — |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P7 (Workout runner UI) of the workout timer.

1. Use the phase-start skill for P7. Read only docs/phases/P7-runner-ui/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p7-runner-ui. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. Every dependency is merged (P2–P6), so no fakes stand in for missing phases. The tablet must be connected for T1 and from T4 on. T1 needs me to turn the tablet over, and T7 needs me to read the screen from 3 m and listen to the cues. connectedDebugAndroidTest uninstalls the app and wipes its workouts: to keep them, run single classes with am instrument (tablet skill).
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

Not started. The runner and completed screens are still P1's placeholders: Start on the list opens a runner placeholder with a Finish button.

## Next steps

1. Get SPEC.md reviewed and marked `Ready` (its §9 has six open questions).
2. Then start with `T1`: the lifecycle, content-area and process-death measurements on the tablet (SPEC §7).

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | To do | Measure on the tablet: lifecycle events, recreation, content area, process death |
| T2 | To do | `RunnerView.kt` |
| T3 | To do | `RunnerViewModel` |
| T4 | To do | `RunnerContent`, icons, colours, sizes |
| T5 | To do | `RunnerScreen` entry, completed screen, navigation, placeholders removed |
| T6 | To do | Instrumented flows |
| T7 | To do | Manifests, text check, manual checks, KDoc, pull request |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

None yet.

## Pending wiring

Connections this phase leaves for later phases:

- **P8:** the end-to-end test (create a workout in the editor, run it from the list, complete it), and the final checks of `NF-1` (a real 60-minute run) and `NF-3`.

P7 completes the pending wiring that P4, P5 and P6 left for it (SPEC §4.5).

## How to verify

- `./gradlew build`: compile, unit tests, Lint, ktlint and the no-`INTERNET` check.
- `./gradlew connectedDebugAndroidTest` on the tablet, or single classes with `am instrument` to keep the app's data (tablet skill).
- The manual checks in SPEC §6.

## Session log

Newest first.
