# P6 — Workout management UI: Handover

| | |
|---|---|
| **Status** | Not started |
| **Spec** | [SPEC.md](SPEC.md), version v0.1 |
| **Branch** | `phase/p6-management-ui` |
| **Last session** | 2026-11-04 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P6 (Workout management UI) of the workout timer.

1. Use the phase-start skill for P6. Read only docs/phases/P6-management-ui/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p6-management-ui. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. The tablet must be connected for T1 (the keyboard probe) and from T3 on (Compose UI tests and screenshots). Use the real repositories from AppContainer in the app; tests build the ViewModels with the in-memory ones. P2, P3 and P5 are merged, so no fakes are needed. P7 may run in parallel in another worktree: in shared files (the catalog, app/build.gradle.kts, AppNavHost, the manifest, NavigationSmokeTest), change only P6's own entries.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

SPEC.md v0.1 is written and waiting for review. No code yet.

## Next steps

1. Review SPEC.md v0.1, and answer Q1–Q8 in §9.
2. Once the SPEC is `Ready`, start with `T1`: the keyboard probe on the tablet.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | To do | |
| T2 | To do | |
| T3 | To do | |
| T4 | To do | |
| T5 | To do | |
| T6 | To do | |
| T7 | To do | |
| T8 | To do | |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

None yet.

## Pending wiring

Connections this phase leaves for later phases:

- **P7, the runner:**
  - the list's Start opens `RunnerRoute(workoutId)`;
  - the runner must validate the workout before building a timeline, because `Timeline.of` throws on an invalid one (P3 Q2). It also decides what to show for an invalid or missing workout;
  - `formatTotalDuration()` (`ui/common/TotalDuration.kt`) is there for the completed screen's planned total;
  - `PlaceholderScreen` and `placeholder_note` are still used by the runner and completed placeholders. P7 deletes them with the last placeholder.
- **P8:** the end-to-end test (create a workout in the editor, run it, complete it), and checking `WM-8` through the UI as part of acceptance.

## How to verify

```
./gradlew build                        # unit tests, lint, ktlint, no-INTERNET check
./gradlew connectedDebugAndroidTest    # Compose UI tests on the tablet (uninstalls the app afterwards)
./gradlew installDebug                 # then the manual checks in SPEC.md §6
```

## Session log

Newest first.

### 2026-11-04

- Done: SPEC.md v0.1 and this handover written. P6 set to `Spec draft` in plan §5.1.
- Notes: the specification and the plan were read only to write the SPEC.
