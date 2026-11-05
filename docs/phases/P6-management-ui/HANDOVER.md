# P6 — Workout management UI: Handover

| | |
|---|---|
| **Status** | Done. Merged in pull request [#6](https://github.com/davamix/workout-timer/pull/6) on 2026-11-05. |
| **Spec** | [SPEC.md](SPEC.md), version v0.4 |
| **Branch** | `phase/p6-management-ui` |
| **Last session** | 2026-11-05 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Phase done: no prompt needed.

## Current state

All eight tasks are done and merged into `main` through pull request [#6](https://github.com/davamix/workout-timer/pull/6) on 2026-11-05. CI passed on the pull request (2m27s on `0a41add`, the last code and SPEC change, and again on the handover commit `71ec660`).

- **Screens:** the workout list, the editor and settings replace their P1 placeholders, and use P3's real storage through `AppContainer`.
- **Build:** `./gradlew build` passes, with 411 unit tests (166 in `:app`, 93 of them new; 245 in `:core`) and 0 failures. Lint, ktlint and the no-INTERNET check are clean.
- **Tablet:** `connectedDebugAndroidTest` runs 51 tests: 0 failures, and 3 skipped on purpose (P3's `PersistenceCheck`).
- **Manual checks** (SPEC §6) pass:
  - the user ran the steps on the screens, and judged the touch targets and the text easy to use at arm's length;
  - a force-stop keeps the workout and the voice setting;
  - the log shows no crash and no storage warning.
- **Unchanged:** the merged debug and release manifests have the same permissions and components as `main`, and `:core` is untouched.
- **SPEC v0.4** records the editor's layout as built (Q9).

On the tablet, the debug app holds the user's "Example" workout from the manual checks, and voice cues are on.

## Next steps

None for P6.

Phases that can start now, with all their dependencies `Done` (plan §5.1):

- **P7, Workout runner UI** (depends on P4). It has no SPEC.md or HANDOVER.md yet; the `phase-docs` skill writes them. It completes P5's audio wiring in the runner, and P6's "Pending wiring" below.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | SwiftKey isn't full screen in landscape. It's 372 dp tall, leaving 204 dp visible above it. Only `adjustResize` keeps the top bar on screen. The numbers are in SPEC §4.3.6 (v0.3). The probe was deleted. |
| T2 | Done | `TotalDurationTest` (12), `WorkoutFormTest` (43), `EditorErrorsTest` (3). The icon library adds no permission or component to the merged release manifest. |
| T3 | Done | `WorkoutListViewModelTest` (12). On the tablet, `WorkoutListScreenTest` (8) and the updated `NavigationSmokeTest` (6) pass. The list and its delete dialog were checked in screenshots. |
| T4 | Done | `WorkoutEditorViewModelTest` (19). Compose snapshot state works in JVM unit tests. |
| T5 | Done | The whole editor screen, including what SPEC §7 lists under T6 (move, duplicate, remove, add, the discard dialog, `BackHandler`, not found), because it lives in the same composables. Checked on the tablet in screenshots: a new workout, an edited one, the keyboard over a rest field, and the validation messages. `NavigationSmokeTest`'s editor test now leaves with Cancel. |
| T6 | Done | `WorkoutEditorScreenTest` (13) passes on the tablet. It creates the §4.4 example by typing, and covers editing, own and linked values, unused rests, structure, validation, leaving, not found and touch targets. The round header now has its own tag, `round-header-<id>`. |
| T7 | Done | `SettingsViewModelTest` (4); `SettingsScreenTest` (4) passes on the tablet. The screen was checked in a screenshot. |
| T8 | Done | `connectedDebugAndroidTest`: 51 tests, 0 failures, 3 skipped. Merged manifests unchanged against `main`. No user-facing literals in P6's Kotlin code. The user ran the manual checks on the tablet, and all pass; persistence and the log were checked over adb. KDoc on the public API. SPEC v0.4. Pull request [#6](https://github.com/davamix/workout-timer/pull/6). |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

Most settle points SPEC.md leaves open. The three about the editor's fields went into SPEC v0.4 (Q9), so the SPEC matches the code.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-04 | `formatTotalDuration` pads with `padStart` instead of `String.format`. | `String.format` without a locale triggers Lint's `DefaultLocale` warning, and the result shouldn't depend on the device's locale. |
| 2026-11-04 | `timeText` keeps only ASCII digits `0`–`9`, not every Unicode digit. | Other digits (Arabic-Indic, for example) would pass `isDigit()` but aren't what the number keyboard types. |
| 2026-11-04 | `WorkoutListViewModel.state` is shared with `SharingStarted.Eagerly`. | The repository's flow is a cheap state flow, and the list lives as long as the app. `state.value` is then current in unit tests without a collector. |
| 2026-11-04 | Callbacks that take an ID go through `NavBackStackEntry.ifResumed` in `AppNavHost`, a private helper. | It is `dropUnlessResumed` for callbacks with an argument (SPEC §4.3.9). |
| 2026-11-04 | Besides `edit` and `addRound`, the editor ViewModel has `addExercise`, `duplicateRound` and `duplicateExercise`. All four return the new item's ID. | The screen needs the ID to scroll to the new item and focus a new exercise (SPEC §4.3.3). They use the ViewModel's `newId`, so tests get fixed IDs. |
| 2026-11-04 | The editor ignores edits, and `requestClose`, while a save runs. | Edits made during a save would be lost when the editor closes after it. The save closes the editor anyway. |
| 2026-11-04 | Own-value fields (exercise duration and rest, round rest) have no floating label. The exercises are a table under one header row (Exercise · Duration · Rest), and the round rest has an inline label. The workout name, round name and defaults keep floating labels. | With a label, Material 3's value-based `OutlinedTextField` hides the placeholder until the field has focus, so a linked value wouldn't show its default. The `labelPosition` option that avoids this exists only on the `TextFieldState` overload. The table is also more compact above the keyboard. In SPEC v0.4 (Q9). |
| 2026-11-04 | The placeholder is the default's number alone, dimmed to 60 %. The unit "s" is the field's suffix. | Without a label, Material shows the suffix on an empty field too, so "40 s" read "40 s s". At full strength, a placeholder looked like a typed value. In SPEC v0.4 (Q9). |
| 2026-11-04 | "Not used" texts are "Not used: last exercise" and "Not used: last round", and seconds fields are 200 dp wide. | SPEC §4.2's "Not used: last exercise of the round" wraps to three lines in a field, and at 170 dp even "last round" wrapped. In SPEC v0.4 (Q9). |
| 2026-11-04 | The editor's content takes an `EditorActions` interface, which the ViewModel implements, instead of ten callbacks. | Shorter signatures for the composables. Tests still drive the real ViewModel. |
| 2026-11-04 | `assertTouchTargetsAtLeast` checks the touch bounds of the clickable nodes that are fully on screen, and skips the others. | In Material 3 1.4 an `IconButton` lays out at 40 dp; `minimumInteractiveComponentSize` widens only its touch area to 48 dp. Touch bounds are clipped to the screen, so a row half scrolled out of the editor would read as too small. |
| 2026-11-04 | System Back in the editor tests closes the soft keyboard first (`closeSoftKeyboard()`, then `pressBack()`). | With the keyboard up, Back only closes the keyboard. |
| 2026-11-04 | `SettingsViewModel` takes `prepareAudio` as a suspend lambda. Its factory runs `AppContainer.prepareAudio()` on `Dispatchers.Default`. | The same thread `MainActivity` uses, because connecting the speech engine shouldn't run on the main thread. The ViewModel stays testable with a plain lambda. |
| 2026-11-04 | The whole settings row toggles voice cues; the `Switch` itself takes no clicks. | One large touch target (NF-6), and one node for tests to find. |
| 2026-11-04 | Each round is one `LazyColumn` item (a card holding its exercise rows). The form scrolls to a round's card for the first error and for a new round. A new exercise's name field takes the focus, which scrolls it into view. | Rounds hold few exercises, so one item each is cheap. Item indexes stay simple: 0 is the workout, then one per round, then Add round. |

## Known issues

- **The keyboard takes a lot of the screen.** SwiftKey's letter keyboard is 372 dp tall in landscape, which leaves 204 dp for the editor: the top bar and about one exercise row. The number pad is about 210 dp tall, so more of the form stays visible while typing durations and rests. With `adjustResize`, the focused field always stays visible (SPEC §4.3.6).
- **Instrumented tests uninstall the app.** `connectedDebugAndroidTest` wipes the user's workouts in the debug app on the tablet. To keep them, run single classes with `am instrument` (tablet skill).
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25. They are harmless (noted in P1).

## Pending wiring

Connections this phase leaves for later phases:

- **P7, the runner:**
  - the list's Start opens `RunnerRoute(workoutId)`;
  - the runner must validate the workout before building a timeline, because `Timeline.of` throws on an invalid one (P3 Q2). It also decides what to show for an invalid or missing workout;
  - `formatTotalDuration()` (`ui/common/TotalDuration.kt`) is there for the completed screen's planned total;
  - `PlaceholderScreen` and `placeholder_note` are still used by the runner and completed placeholders. P7 deletes them with the last placeholder;
  - `NavigationSmokeTest` seeds a workout (`smoke-test-workout`), presses its Start, and then clicks the runner placeholder's Finish (`runner_finish`). P7 updates those two runner tests for the real runner.
- **P8:** the end-to-end test (create a workout in the editor, run it, complete it), and checking `WM-8` through the UI as part of acceptance.

## How to verify

```
./gradlew build                        # 411 unit tests (166 :app, 245 :core), lint, ktlint, no-INTERNET check
./gradlew connectedDebugAndroidTest    # tablet: 51 tests, 3 skipped (uninstalls the app afterwards)
./gradlew installDebug                 # then the manual checks in SPEC.md §6
```

To run P6's screen tests and keep the app's data (Git Bash, `MSYS_NO_PATHCONV=1`, see the tablet skill):

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class io.github.davamix.workouttimer.ui.editor.WorkoutEditorScreenTest io.github.davamix.workouttimer.debug.test/androidx.test.runner.AndroidJUnitRunner | tail -8
```

The other classes are `ui.list.WorkoutListScreenTest`, `ui.settings.SettingsScreenTest` and `NavigationSmokeTest`.

## Session log

Newest first.

### 2026-11-05 (close)

- The user merged pull request [#6](https://github.com/davamix/workout-timer/pull/6). P6 is closed: `Done` in plan §5.1, done criteria ticked in SPEC.md §8.

### 2026-11-05

- Done:
  - The user ran the manual checks on the tablet, and all pass. Over adb: "Example" and voice cues off survived a force-stop, and the log showed no crash or storage warning. Voice cues were turned back on afterwards.
  - SPEC v0.4 went to `main` and was merged into the branch: the editor's fields as built (Q9).
  - Pull request [#6](https://github.com/davamix/workout-timer/pull/6) opened. CI passed on `0a41add` (2m27s).
- Notes: `connectedDebugAndroidTest` wasn't rerun for the handover. It ran on the last code commit, and only documents changed since; another run would have deleted the user's workout.

### 2026-11-04 (implementation)

- Done:
  - T1: the keyboard probe ran on the tablet. SPEC v0.3 records its measurements, and the probe was deleted.
  - T2–T7 implemented, one commit per task. Each screen was checked in screenshots on the tablet.
  - T8: the full instrumented suite passes, and the merged manifests and the no-literal-text check are clean.
- Notes:
  - Compose snapshot state (`mutableStateOf`) works in JVM unit tests, so the editor ViewModel is tested without a device.
  - In Compose tests, the soft keyboard eats the first Back. Close it with `closeSoftKeyboard()` first.
  - The tablet's keyboard is SwiftKey. Its number pad is much shorter than its letter keyboard.

### 2026-11-04

- Done: SPEC.md v0.1 and this handover written. The user agreed to Q1–Q8 as assumed, so SPEC v0.2 is `Ready`. CLAUDE.md now says Compose UI tests use the `v2` rules. P6 marked `In progress` on `main`, and the branch created.
- Notes: the specification and the plan were read only to write the SPEC.
