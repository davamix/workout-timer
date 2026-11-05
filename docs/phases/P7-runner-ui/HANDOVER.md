# P7 — Workout runner UI: Handover

| | |
|---|---|
| **Status** | Done. Merged in pull request [#7](https://github.com/davamix/workout-timer/pull/7) on 2026-11-05. |
| **Spec** | [SPEC.md](SPEC.md), version v0.4 |
| **Branch** | `phase/p7-runner-ui` |
| **Last session** | 2026-11-05 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Phase done: no prompt needed.

## Current state

Merged into `main` in pull request [#7](https://github.com/davamix/workout-timer/pull/7) on 2026-11-05. CI passed on the branch's last commits (`79a2f54` and `7bdd9d4`).

- **What works:** a workout runs on the tablet from the list's Start to the completed screen, with its cues.
  - It shows "Getting ready…" while the audio prepares.
  - Get ready, exercises and rests each have their colour, and the time stays large.
  - Pause, Resume, Next, Previous and Stop (with its confirmation) all work, and Back pauses, then asks to stop.
  - The workout pauses when the app leaves the screen, and turning the tablet over doesn't affect it.
  - It returns to the list if Android ended the app in the background.
  - The screen stays on while the runner is open.
- **Checks:**
  - `./gradlew build` passes: unit tests `:app` 212, `:core` 245, 0 failures.
  - All 80 instrumented tests pass on the tablet, at `f0d9b2b`. They ran as the whole suite with `am instrument`, to keep the user's workouts; only this file has changed since.
  - All 13 manual checks of SPEC §6 pass. Some were run through adb; the full run by ear, readability from 3 m, turning the tablet over, voice off and the cold start were done by the user.
  - The merged manifests are unchanged: no manifest, dependency or build file changed.

## Next steps

None for P7.

P8 (Integration and v1.0) can start now: all its dependencies (P3, P5, P6, P7) are `Done`. Its SPEC.md and HANDOVER.md don't exist yet; the `phase-docs` skill writes them.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | Results in SPEC v0.3. The probe was deleted. |
| T2 | Done | `RunnerView.kt`; `RunnerViewTest` (18 tests) |
| T3 | Done | `RunnerViewModel.kt`; `RunnerViewModelTest` (28 tests), `GatedSpeaker`. No new dependency: `createSavedStateHandle()` comes with `navigation-compose`. |
| T4 | Done | `RunnerContent`, `RunnerIcons`, `PhaseColors.OnPhase` and `OnPaused`, `strings_runner.xml`; `RunnerContentTest` (14 tests, on the tablet). Paused layout changed with the user (Q7, SPEC v0.4). |
| T5 | Done | `RunnerScreen` entry, `CompletedScreen` and `CompletedContent`, `CompletedRoute`, `AppNavHost`; placeholders removed; `NavigationSmokeTest` updated; `CompletedScreenTest` (5 tests) |
| T6 | Done | `RunnerScreenTest` (6), `RunnerActivityTest` (4), `SilentAudio.kt`. Without the `isChangingConfigurations` check, `recreationKeepsTheWorkoutRunning` fails, so it guards NF-3. All 80 instrumented tests pass on the tablet. |
| T7 | Done | Manifests and text checked; manual checks passed (below); pull request #7, merged |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-05 | `confirmStop()` does nothing unless the Stop dialog is open. The Stop dialog closes by itself if the workout stops being paused. | A stop always goes through the confirmation (`EX-8`), and the dialog never shows over a running workout. |
| 2026-11-05 | Back while starting cancels the start: no session is created, even if the audio becomes ready afterwards. | Leaving must not start a workout in the background (`EX-10`). |
| 2026-11-05 | The time and the title use `BasicText` with `TextAutoSize.StepBased`: the title in a fixed 80 dp box (72 → 36 sp), the time in the height left over (220 → 48 sp). In SPEC v0.4. | One layout fits every state in 542 dp. The exercise, the state that matters most from 3 m, gets the largest time. |
| 2026-11-05 | Stop is at the end of the paused row of buttons (Q7, SPEC v0.4, specification v0.10). | With Stop on its own row, the paused time shrank to about 50 sp in a round rest. The user chose this from screenshots of both layouts. |
| 2026-11-05 | The unavailable screen sits in a `Surface`. | Without it, nothing set the content colour, and its text was black on the dark background (found in the gallery screenshots). |
| 2026-11-05 | The instrumented tests ran as the whole suite with `am instrument`, not `connectedDebugAndroidTest` as SPEC T6 and §8 name it. | It runs the same tests on the same tablet, but keeps the debug app's workouts, which the manual checks used. `connectedDebugAndroidTest` would have uninstalled the app and deleted them. |

## Known issues

- **`am kill` doesn't end the app on this tablet,** even when it's cached. To check `EX-11`, end the cached process with `run-as … kill -9` instead (SPEC §4.3.5; the tablet skill now says so).
- **The tablet's screen timeout is "never"** (`screen_off_timeout` 2147483647). Manual check 9 (`NF-2`) set it to 30 s with the user's agreement, and put it back.
- **`captureToImage()` can't capture dialogs below API 28.** The Stop dialog was checked on the device instead.
- **Checking the screens' look:** a throwaway instrumented test (not committed) drew each `RunnerState` with `RunnerContent`, saved `captureToImage()` as PNGs in the app's cache, and `adb exec-out run-as … cat cache/<file>` pulled them. It's faster than driving the app to each state. A few `gallery-*.png` files are left in the debug app's cache; the next uninstall removes them.
- **uiautomator can't dump a running workout:** the time changes every second, so the screen never goes idle. Dump while paused, or take a screenshot.
- **Piping Gradle through `grep | tail` hides its exit code,** so `./gradlew … | grep … && adb shell am instrument …` runs the old test APK when the build fails. With `set -o pipefail`, `grep -v` returns 1 when nothing is left to print, which looks like a failed build. Write Gradle's output to a file and check `$?` instead. (Proposed for CLAUDE.md and the tablet skill.)

## Pending wiring

Connections this phase leaves for later phases:

- **P8:** the end-to-end test (create a workout in the editor, run it from the list, complete it), and the final checks of `NF-1` (a real 60-minute run) and `NF-3`.

P7 completes the pending wiring that P4, P5 and P6 left for it (SPEC §4.5).

## How to verify

- `./gradlew build`: compile, unit tests, Lint, ktlint and the no-`INTERNET` check.
- Instrumented tests: `./gradlew connectedDebugAndroidTest` on the tablet. To keep the app's data, run `./gradlew installDebug installDebugAndroidTest`, then `adb shell am instrument -w io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner` (tablet skill).
- The manual checks in SPEC §6. Results on 2026-11-05:

  | Check | Result |
  |---|---|
  | 1, 2, 3 | The user ran the §4.4 example with voice cues on. The cues matched the display, the chime played at the round rest, and "Workout complete. Well done!" was spoken before the completed screen. Pausing stopped speech. The text read clearly from about 3 m. |
  | 4 | Next went to Arm circles, paused at 00:10. Previous went back to Jumping jacks at 00:30. |
  | 5, 6 | Stop, then Cancel: still paused. Back while paused opens the Stop dialog; Back again closes it. |
  | 7 | The user turned the tablet over while running, while paused, and with the dialog open: nothing reset, paused or skipped. |
  | 8 | Recents, Home and the power button each left the workout paused |
  | 9 | With a 30 s timeout: the screen stayed on after 70 s paused in the runner, and was off after 45 s on the list. The timeout was put back to "never". |
  | 10 | After the cached process was ended, reopening from the launcher and from Recents showed the workout list |
  | 11 | Voice cues off: get ready counted down at once; only the chime was heard |
  | 12 | Cold start (app and TTS engine force-stopped): "Getting ready…", then get ready with "Get ready in" |
  | 13 | No crash, and no `WorkoutAudio` or `WorkoutStorage` warning |

## Session log

Newest first.

### 2026-11-05 (close)

- The user merged pull request #7. P7 is closed: `Done` in plan §5.1, done criteria ticked in SPEC.md §8.

### 2026-11-05

- Done:
  - **Setup:** SPEC v0.1 written and reviewed. v0.2 marked `Ready`, with specification v0.9 (Back on the runner, the completed screen's counts). Phase marked in progress; branch created.
  - **T1:** the lifecycle probe and the process-death check, recorded in SPEC v0.3. The tablet skill's way of ending the app was corrected on `main`.
  - **T2–T6:** the runner's view mapping, ViewModel, content, entry, completed screen, navigation and tests.
  - **SPEC v0.4 (Q7):** Stop moved into the paused row of buttons, with specification v0.10.
  - **T7:** the checks, the manual checks with the user, and pull request #7.
- Notes:
  - Home's `onStop` comes 300–560 ms after `onPause`; Recents' and the power button's 50–70 ms after.
  - A throwaway screenshot gallery (`captureToImage()` to PNGs) caught a layout problem (Q7) and a colour bug before any manual check.
  - Two lessons worth adding to CLAUDE.md or the skills are proposed to the user:
    - Gradle's exit code is lost through pipes.
    - A navigation call driven by a ViewModel state must wait for `RESUMED` rather than use `dropUnlessResumed`.
