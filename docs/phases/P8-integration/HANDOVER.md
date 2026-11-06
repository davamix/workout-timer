# P8 — Integration and v1.0: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.4 |
| **Branch** | `phase/p8-integration` |
| **Last session** | 2026-11-06 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P8 (Integration and v1.0) of the workout timer.

1. Use the phase-start skill for P8. Read only docs/phases/P8-integration/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p8-integration. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. The tablet must be connected. I'm needed for: T2 (I create the keystore and the properties file; never read or print them), T6 (listening, turning the tablet, unlocking after a reboot), and the last minute of T7. Ask before changing any tablet setting, and put it back afterwards. The debug app holds my workouts, so run instrumented tests with the tablet skill's instrument helper, not connectedDebugAndroidTest. Don't push the tag or create the release until I say so.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it. After the merge, T10 releases v1.0.0, then the phase-close skill closes the phase.
```

## Current state

T1–T6 are done, on SPEC v0.4. Acceptance runs on the release APK from `56b5747`, whose SHA-256 is in `ACCEPTANCE.md`. No defect has been found.

- A run can be timed from outside the app to about ±0.15 s.
- The release APK is version 1.0.0, signed with the user's key, and installed on the tablet next to the debug app. It holds the §4.4 "Example".
- Every instrumented test passes, including `EndToEndTest`.
- No production Kotlin has changed, and no wiring is left to do.
- `ACCEPTANCE.md`: 50 of the 51 rows pass. Only `NF-1` is left, with A17 and the last of A18.

## Next steps

1. `T7`, the 60-minute run (SPEC §4.3.5).
2. `T8`: close the documents.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | The method gives about ±0.15 s; numbers under "How to verify" |
| T2 | Done | Signed APK installed. `./gradlew build` still has to pass without the properties in CI, which the pull request's run proves. |
| T3 | Done | 10 of 10 runs pass |
| T4 | Done | 81 instrumented tests pass; wiring and security checks clean; `ACCEPTANCE.md` written |
| T5 | Done | A1, A3, A4, A5, A10, A11 and A13 pass, with no defect found. A2 and A18 finish in T6. |
| T6 | Done | A2, A6–A9, A12 and A14–A16 pass, with no defect found. The user confirmed what only ears and hands can check. |
| T7 | Doing | The 60-minute run |
| T8 | To do | Close the documents |
| T9 | To do | Pull request |
| T10 | To do | Release after the merge, then `phase-close` |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-06 | The keystore is at `C:\Users\davam_p3h1i13\workout-timer.keystore`, and its certificate is `CN=Daniel Valcarce, C=ES`. In SPEC v0.3 (Q8). | The user created it that way, and chose to keep their name, which is already public in the commits. |
| 2026-11-06 | `EndToEndTest` closes the soft keyboard before each click in the form. | `MainActivity` resizes for the keyboard, and focus moving from a name to a number swaps the letter keyboard for the shorter number pad a moment later. If that lands between `performScrollToNode` and `performClick`, the round is never added: 13 of 23 runs failed this way, at both a fresh install and warm. With the fix, 10 of 10 pass. Bounds logged before the click showed it: the button stayed at y=521 when the round was added, and stayed at y=941 or had empty bounds when it wasn't. The test still uses the real keyboard. |
| 2026-11-06 | `EndToEndTest` doesn't use `FLAG_ALT_FOCUSABLE_IM`. | It was tried first, to keep the keyboard away, but the keyboard still showed on this tablet, and 5 of 10 runs failed. |

## Known issues

Lessons from earlier phases that apply here are in SPEC §4.5.

- **For CLAUDE.md, in T8:** a click on a node with empty bounds raises no error, and lands at the screen's corner. Under `MainActivity`, close the keyboard before a click that follows a scroll.
- **For the `tablet` skill, in T8: driving the editor through adb.** Focusing a field scrolls the form, so "the Nth field on screen" changes between taps. Find a field by its place after one holding a known value. A round's controls are in its header, above its name field. Press Back only when `dumpsys input_method` shows `mInputShown=true`, because Back on a closed keyboard leaves the editor. T7 needs this to create "One hour".

## Pending wiring

None. P8 is the last phase, and SPEC §4.3.1 lists what was checked.

## How to verify

The acceptance scenarios are in SPEC §6, and their results will go in `ACCEPTANCE.md`.

**T1, timing a run from outside the app (SPEC §4.3.5).** On the debug build, with a 60 s workout, on 2026-11-06. `time_run` read the flag each time, and it turned on and off as expected.

| Runs | Span (s) | Overhead (s) |
|---|---|---|
| Voice on, 3 runs | 62.202, 62.232, 62.152 | 2.195 on average |
| Voice off, 3 runs | 61.060, 60.948, 60.927 | 0.978 on average |

The difference, about 1.2 s, is "Get ready in".

| Voice on, two pauses of about 10 s | Span (s) | Paused (s) | Drift (s) |
|---|---|---|---|
| Run 1 | 83.910 | 21.704 | +0.011 |
| Run 2 | 83.787 | 21.724 | −0.132 |
| Run 3 | 84.022 | 21.768 | +0.059 |

- One `input tap` took 0.725–0.784 s over 12 taps.
- The method is good to about ±0.15 s, well inside `NF-1`'s ±1 s.
- T7 repeats the overhead on the release APK before the long run, because these numbers come from the debug build.
- Pause is at (960, 1012) and Resume at (818, 1013), in pixels.
- No crash, and no `WorkoutAudio` or `WorkoutStorage` line, during the runs.

**T2, the signed release APK,** on 2026-11-06:

- `apksigner`: v2 signature, one signer, `CN=Daniel Valcarce, C=ES`, RSA 4096, SHA-256 `abd7e456e1edcd040377267afdcf0ebbd0ee7dd82c4f07a2466c366a074ad75f`.
- 9.7 MB. Version 1.0.0, label "Workout Timer". The only permission listed is `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- First launch after installing: 1.09 s. Cold starts: 1.08, 1.06 and 1.23 s (`am start -W`).
- `dumpsys package` flags: `HAS_CODE ALLOW_CLEAR_USER_DATA`, so neither debuggable nor backed up. `run-as` is refused.

**T3, the end-to-end test:** `gradle installDebug installDebugAndroidTest && instrument EndToEndTest`. It takes about 55 s, and the tablet speaks. 5 of 5 runs passed right after installing with a cold speech engine, then 5 of 5 warm.

**Unit tests** on `caf02d3` plus T3: 212 in `:app` and 245 in `:core`, no failures.

**T4, the baseline,** on commit `11873a9`, on 2026-11-06:

- Every instrumented test passes on the tablet: `OK (81 tests)` in 172 s, with `gradle installDebug installDebugAndroidTest && instrument`. `PersistenceCheck`'s 3 steps are skipped, as always.
- Voice cues in the debug app were off when P8 started. To see whether a test turns them off, T4 turned them on and ran the whole suite: they were still on afterwards, so no test does. They're on now.
- The merged release manifest holds exactly what SPEC §5 expects. There's no secret in a tracked file, no user-facing text in the Kotlin sources, and no network or analytics library. `app/src/main` and `core/src/main` haven't changed since the phase started.
- `ACCEPTANCE.md` holds all 50 requirement IDs, compared with the specification by script, plus the §5.3 volume rule.

## Session log

Newest first.

### 2026-11-06

- Done: SPEC v0.1 and this handover, then the review: SPEC v0.2 (`Ready`), specification v0.11 (`NF-1`, Q2), plan v0.3 and CLAUDE.md (merged manifest, Q5). Branch created. T1, with `time_run` and `release_pkg` added to the `tablet` skill; its throwaway probe is deleted. T2, with the user's keystore (SPEC v0.3). T3. T4. T5.
- Notes: measured on the tablet for the SPEC: a `dumpsys window` poll through adb takes about 130 ms, and one `input` command about 750 ms. The first `input` took 1034 ms.
