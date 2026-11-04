# P6 — Workout management UI: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.3 |
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

SPEC.md is `Ready`. The review confirmed all eight draft assumptions (Q1–Q8). T1's keyboard measurements confirmed §4.3.6, and are recorded there in SPEC v0.3: `MainActivity` gets `adjustResize` in T5. No app code yet.

## Next steps

1. `T2`: the foundations (SPEC §7). Add the dependencies, `TotalDuration.kt`, `WorkoutForm.kt`, `EditorErrors.kt`, `MainDispatcherExtension` and `TestRepositories.kt`, with their unit tests.

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
| T7 | To do | |
| T8 | To do | |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

None of these change SPEC.md. They settle points it doesn't specify.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-04 | `formatTotalDuration` pads with `padStart` instead of `String.format`. | `String.format` without a locale triggers Lint's `DefaultLocale` warning, and the result shouldn't depend on the device's locale. |
| 2026-11-04 | `timeText` keeps only ASCII digits `0`–`9`, not every Unicode digit. | Other digits (Arabic-Indic, for example) would pass `isDigit()` but aren't what the number keyboard types. |
| 2026-11-04 | `WorkoutListViewModel.state` is shared with `SharingStarted.Eagerly`. | The repository's flow is a cheap state flow, and the list lives as long as the app. `state.value` is then current in unit tests without a collector. |
| 2026-11-04 | Callbacks that take an ID go through `NavBackStackEntry.ifResumed` in `AppNavHost`, a private helper. | It is `dropUnlessResumed` for callbacks with an argument (SPEC §4.3.9). |
| 2026-11-04 | Besides `edit` and `addRound`, the editor ViewModel has `addExercise`, `duplicateRound` and `duplicateExercise`. All four return the new item's ID. | The screen needs the ID to scroll to the new item and focus a new exercise (SPEC §4.3.3). They use the ViewModel's `newId`, so tests get fixed IDs. |
| 2026-11-04 | The editor ignores edits, and `requestClose`, while a save runs. | Edits made during a save would be lost when the editor closes after it. The save closes the editor anyway. |
| 2026-11-04 | Own-value fields (exercise duration and rest, round rest) have no floating label. The exercises are a table under one header row (Exercise · Duration · Rest), and the round rest has an inline label. The workout name, round name and defaults keep floating labels. | With a label, Material 3's value-based `OutlinedTextField` hides the placeholder until the field has focus, so a linked value wouldn't show its default. The `labelPosition` option that avoids this exists only on the `TextFieldState` overload. The table is also more compact above the keyboard. |
| 2026-11-04 | The placeholder is the default's number alone, dimmed to 60 %. The unit "s" is the field's suffix. | Without a label, Material shows the suffix on an empty field too, so "40 s" read "40 s s". At full strength, a placeholder looked like a typed value. |
| 2026-11-04 | "Not used" texts are "Not used: last exercise" and "Not used: last round", and seconds fields are 200 dp wide. | SPEC §4.2's "Not used: last exercise of the round" wraps to three lines in a field, and at 170 dp even "last round" wrapped. SPEC §4.2 says the wording may change. |
| 2026-11-04 | The editor's content takes an `EditorActions` interface, which the ViewModel implements, instead of ten callbacks. | Shorter signatures for the composables. Tests still drive the real ViewModel. |
| 2026-11-04 | `assertTouchTargetsAtLeast` checks the touch bounds of the clickable nodes that are fully on screen, and skips the others. | In Material 3 1.4 an `IconButton` lays out at 40 dp; `minimumInteractiveComponentSize` widens only its touch area to 48 dp. Touch bounds are clipped to the screen, so a row half scrolled out of the editor would read as too small. |
| 2026-11-04 | System Back in the editor tests closes the soft keyboard first (`closeSoftKeyboard()`, then `pressBack()`). | With the keyboard up, Back only closes the keyboard. |
| 2026-11-04 | Each round is one `LazyColumn` item (a card holding its exercise rows). The form scrolls to a round's card for the first error and for a new round. A new exercise's name field takes the focus, which scrolls it into view. | Rounds hold few exercises, so one item each is cheap. Item indexes stay simple: 0 is the workout, then one per round, then Add round. |

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

- Done: SPEC.md v0.1 and this handover written. The user agreed to Q1–Q8 as assumed, so SPEC v0.2 is `Ready`. CLAUDE.md now says Compose UI tests use the `v2` rules. P6 marked `In progress` on `main`, and the branch created.
- Notes: the specification and the plan were read only to write the SPEC.
