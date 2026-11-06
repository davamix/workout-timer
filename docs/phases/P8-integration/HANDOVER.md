# P8 — Integration and v1.0: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
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

SPEC v0.2 is `Ready`: the user agreed with every assumption in §9. The phase branch exists, and T1 is done: a run can be timed from outside the app to about ±0.15 s. No app code has changed.

Found while writing the SPEC, on `main` at `6d2cc6d`:

- No wiring is left to do.
- `assembleRelease` works and gives an unsigned APK of 9.7 MB.
- `~/.gradle/gradle.properties` doesn't exist yet. The user has the instructions to create it and the keystore (SPEC §4.3.2).

## Next steps

1. `T2`: the signing config. Checking it with the real key waits until the user says the keystore is done.
2. `T3`: the end-to-end test.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | The method gives about ±0.15 s; numbers under "How to verify" |
| T2 | To do | Release build; the user creates the keystore |
| T3 | To do | End-to-end test |
| T4 | To do | Baseline and `ACCEPTANCE.md` |
| T5 | To do | Acceptance without sound |
| T6 | To do | Acceptance with the user |
| T7 | To do | The 60-minute run |
| T8 | To do | Close the documents |
| T9 | To do | Pull request |
| T10 | To do | Release after the merge, then `phase-close` |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

Lessons from earlier phases that apply here are in SPEC §4.5.

- **Voice cues were off in the debug app when P8 started.** T1 read `false` before changing anything, and put it back to `false` afterwards. Nobody knows yet what turned them off: P7's manual check 11 says to turn them back on. In T4, turn them on, run the whole instrumented suite, and read the setting again. If the suite leaves them off, a test isn't restoring them.

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

## Session log

Newest first.

### 2026-11-06

- Done: SPEC v0.1 and this handover, then the review: SPEC v0.2 (`Ready`), specification v0.11 (`NF-1`, Q2), plan v0.3 and CLAUDE.md (merged manifest, Q5). Branch created. T1, with `time_run` and `release_pkg` added to the `tablet` skill. Its throwaway probe is deleted.
- Notes: measured on the tablet for the SPEC: a `dumpsys window` poll through adb takes about 130 ms, and one `input` command about 750 ms. The first `input` took 1034 ms.
