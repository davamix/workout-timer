# P7 — Workout runner UI: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.3 |
| **Branch** | `phase/p7-runner-ui` |
| **Last session** | 2026-11-05 |

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

T1–T3 are done. The measurements confirm the design (SPEC v0.3). The runner's logic is in place and unit-tested: `RunnerView` (what each state shows) and `RunnerViewModel` (starting, commands, Stop dialog, Back, foreground, rebuilt runner, completed summary), with the real session and cue player. Nothing draws them yet: the runner and completed screens are still P1's placeholders, and `WorkoutSummary` sits in the placeholder's `CompletedScreen.kt` until T5.

## Next steps

1. `T4`: `RunnerContent` for every state of SPEC §4.3.2 (starting, unavailable, running and paused looks, the Stop dialog), `RunnerIcons.kt`, the text colours in `PhaseColors`, and `strings_runner.xml`. Fit the sizes into the measured 960 × 542 dp, with screenshots on the tablet. Test: `RunnerContentTest`.
2. Then `T5`: the `RunnerScreen` entry, the completed screen and navigation.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | Results in SPEC v0.3. The probe was deleted. |
| T2 | Done | `RunnerView.kt`; `RunnerViewTest` (18 tests) |
| T3 | Done | `RunnerViewModel.kt`; `RunnerViewModelTest` (28 tests), `GatedSpeaker`. No new dependency: `createSavedStateHandle()` comes with `navigation-compose`. |
| T4 | To do | `RunnerContent`, icons, colours, sizes |
| T5 | To do | `RunnerScreen` entry, completed screen, navigation, placeholders removed |
| T6 | To do | Instrumented flows |
| T7 | To do | Manifests, text check, manual checks, KDoc, pull request |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-05 | `confirmStop()` does nothing unless the Stop dialog is open. The Stop dialog closes by itself if the workout stops being paused. | A stop always goes through the confirmation (`EX-8`), and the dialog never shows over a running workout. |
| 2026-11-05 | Back while starting cancels the start: no session is created, even if the audio becomes ready afterwards. | Leaving must not start a workout in the background (`EX-10`). |

## Known issues

- **`am kill` doesn't end the app on this tablet,** even when it's cached. To check `EX-11`, end the cached process with `run-as … kill -9` instead (SPEC §4.3.5; the tablet skill now says so).
- **The tablet's screen timeout is "never"** (`screen_off_timeout` 2147483647). Manual check 9 (`NF-2`) needs it at 30 s for a while, with the user's agreement, and then put back.

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

### 2026-11-05

- Done: SPEC v0.1 written, reviewed, and v0.2 marked `Ready`, with specification v0.9 (Back on the runner, the completed screen's counts). Phase marked in progress; branch `phase/p7-runner-ui` created. T1: the lifecycle probe, run twice (the user turned the tablet over during the first run), and the process-death check from the launcher and from Recents. Results in SPEC v0.3, merged from `main`. The tablet skill's way of ending the app was wrong (`am kill`); fixed on `main`.
- Notes: Home's `onStop` comes 300–560 ms after `onPause`; Recents' and the power button's about 50–70 ms after.
