# P7 — Workout runner UI: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.4 |
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

T1–T6 are done, and T7 is under way. A workout runs from the list's Start on the tablet, with its cues, to the completed screen. Every instrumented test passes on the tablet (80, run with `am instrument` to keep the app's data). The merged manifests are unchanged, and P7's Kotlin holds no user-facing text.

Manual checks (SPEC §6) done through adb on 2026-11-05, all as expected:

| Check | Result |
|---|---|
| 8 | Recents (back at 00:09) and the power button (back at 00:05) left the workout paused. Home did too, in T5's live check. |
| 4 | Next went to Arm circles, paused at 00:10. Previous went back to Jumping jacks at 00:30. |
| 5 | Stop, then Cancel: still paused |
| 6 | Back while paused opens the Stop dialog; Back again closes it |
| 10 | The cached process was ended with `run-as … kill -9`. Reopening from the launcher, and again from Recents, showed the workout list. |
| 13 | No crash, and no `WorkoutAudio` or `WorkoutStorage` warning |

The keep-screen-on flag is set while the runner is open, and cleared on the list.

## Next steps

1. The manual checks that need the user (SPEC §6):
   - **1:** the whole §4.4 example by ear, voice on.
   - **2:** reading from 3 m.
   - **3:** speech stops on pause.
   - **7:** turning the tablet over while running, paused, and with the Stop dialog open.
   - **9:** the screen timeout, at 30 s for a while; it needs the user's agreement to change the setting.
   - **11:** voice off, chime only.
   - **12:** the cold start, "Getting ready…", then "Get ready in".
2. Then `connectedDebugAndroidTest` (it uninstalls the app and deletes the "Example" workout, so after the manual checks), KDoc review, `./gradlew build`, the handover skill, and the pull request.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | Results in SPEC v0.3. The probe was deleted. |
| T2 | Done | `RunnerView.kt`; `RunnerViewTest` (18 tests) |
| T3 | Done | `RunnerViewModel.kt`; `RunnerViewModelTest` (28 tests), `GatedSpeaker`. No new dependency: `createSavedStateHandle()` comes with `navigation-compose`. |
| T4 | Done | `RunnerContent`, `RunnerIcons`, `PhaseColors.OnPhase` and `OnPaused`, `strings_runner.xml`; `RunnerContentTest` (14 tests, on the tablet). Paused layout changed with the user (Q7, SPEC v0.4). |
| T5 | Done | `RunnerScreen` entry, `CompletedScreen` and `CompletedContent`, `CompletedRoute`, `AppNavHost`; placeholders removed; `NavigationSmokeTest` updated; `CompletedScreenTest` (5 tests). On the tablet, with `RunnerContentTest`: 25 tests pass. |
| T6 | Done | `RunnerScreenTest` (6), `RunnerActivityTest` (4), `SilentAudio.kt`. Without the `isChangingConfigurations` check, `recreationKeepsTheWorkoutRunning` fails, so it guards NF-3. All 80 instrumented tests pass on the tablet. |
| T7 | Doing | Manifests and text check done. Manual checks through adb done (see Current state); the user's part is next. |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-05 | `confirmStop()` does nothing unless the Stop dialog is open. The Stop dialog closes by itself if the workout stops being paused. | A stop always goes through the confirmation (`EX-8`), and the dialog never shows over a running workout. |
| 2026-11-05 | Back while starting cancels the start: no session is created, even if the audio becomes ready afterwards. | Leaving must not start a workout in the background (`EX-10`). |
| 2026-11-05 | The time and the title use `BasicText` with `TextAutoSize.StepBased`: the title in a fixed 80 dp box (72 → 36 sp), the time in the height left over (220 → 48 sp). In SPEC v0.4. | One layout fits every state in 542 dp. The exercise, the state that matters most from 3 m, gets the largest time. |
| 2026-11-05 | Stop is at the end of the paused row of buttons (Q7, SPEC v0.4, specification v0.10). | With Stop on its own row, the paused time shrank to about 50 sp in a round rest. The user chose this from screenshots of both layouts. |
| 2026-11-05 | The unavailable screen sits in a `Surface`. | Without it, nothing set the content colour, and its text was black on the dark background (found in the gallery screenshots). |

## Known issues

- **`am kill` doesn't end the app on this tablet,** even when it's cached. To check `EX-11`, end the cached process with `run-as … kill -9` instead (SPEC §4.3.5; the tablet skill now says so).
- **The tablet's screen timeout is "never"** (`screen_off_timeout` 2147483647). Manual check 9 (`NF-2`) needs it at 30 s for a while, with the user's agreement, and then put back.
- **`captureToImage()` can't capture dialogs below API 28.** The Stop dialog's look is checked on the device in T6 and T7 instead.
- **Checking the screens' look:** a throwaway instrumented test (not committed) drew each `RunnerState` with `RunnerContent`, saved `captureToImage()` as PNGs in the app's cache, and `adb exec-out run-as … cat cache/<file>` pulled them. It's faster than driving the app to each state. A few `gallery-*.png` files are left in the debug app's cache; the next `connectedDebugAndroidTest` uninstall removes them.
- **Piping Gradle through `grep | tail` hides its exit code,** so `./gradlew … | grep … && adb shell am instrument …` runs the old test APK when the build fails. With `set -o pipefail`, `grep -v` returns 1 when nothing is left to print, which looks like a failed build. Write Gradle's output to a file and check `$?` instead. (Proposed for CLAUDE.md and the tablet skill.)

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
