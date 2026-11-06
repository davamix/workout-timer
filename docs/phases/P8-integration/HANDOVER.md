# P8 — Integration and v1.0: Handover

| | |
|---|---|
| **Status** | Not started |
| **Spec** | [SPEC.md](SPEC.md), version v0.1 |
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

SPEC v0.1 is written and waiting for review, with seven open questions in §9. No code has been written, and the phase branch doesn't exist yet.

Found while writing the SPEC, on `main` at `6d2cc6d`:

- No wiring is left to do.
- `assembleRelease` works and gives an unsigned APK of 9.7 MB.
- `~/.gradle/gradle.properties` doesn't exist yet.

## Next steps

1. The user reviews SPEC.md and answers §9.
2. Apply the review (skill `phase-docs`, "After the review"). Q2 and Q5 change the specification and the plan, which come first.
3. Once the user agrees, set SPEC.md and plan §5.1 to `Ready`, then start the phase with the `phase-start` skill.
4. Start with `T1`.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | To do | Measure the timing method |
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

None yet. Lessons from earlier phases that apply here are in SPEC §4.5.

## Pending wiring

None. P8 is the last phase, and SPEC §4.3.1 lists what was checked.

## How to verify

Nothing to verify yet. The acceptance scenarios are in SPEC §6, and their results will go in `ACCEPTANCE.md`.

## Session log

Newest first.

### 2026-11-06

- Done: SPEC v0.1 and this handover.
- Notes: measured on the tablet for the SPEC: a `dumpsys window` poll through adb takes about 130 ms, and one `input` command about 750 ms. The first `input` took 1034 ms.
