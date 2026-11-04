# P5 — Audio cues: Handover

| | |
|---|---|
| **Status** | Not started |
| **Spec** | [SPEC.md](SPEC.md), version v0.1 |
| **Branch** | `phase/p5-audio` |
| **Last session** | — |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P5 (Audio cues) of the workout timer.

1. Use the phase-start skill for P5. Read only docs/phases/P5-audio/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p5-audio. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. P2, P3 and P4 are merged, so no fakes are needed for them. T1 is pure Kotlin in :core. From T2 on, the tablet must be connected over USB (tablet skill): T2, T3 and T4 have instrumented tests, T5 installs the debug screen, and T6 needs me at the tablet to listen to the cues. The tablet stays in Spanish.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

Not started. SPEC.md is a draft under review.

## Next steps

1. Once SPEC.md is `Ready`, start with `T1`: the `Cue` and `Utterance` types and `CuePlanner` in `:core`, with `CuePlannerTest` and `ExampleCuesTest`.

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

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|

## Known issues

None yet.

## Pending wiring

- **The runner side of the audio (P7 if P5 is merged first, otherwise P8):** as in SPEC §4.3.6. Read the voice setting once when the workout starts. Call `cuePlayer.prepare(voiceOn)`, create the `WorkoutSession` with `waitForAnnouncement = voiceOn`, launch `cuePlayer.play(session, voiceOn)` in `viewModelScope`, then call `session.start()`. The runner reads `session.state` only: the cue player is the only collector of `session.events`. This completes P4's pending wiring: "`waitForAnnouncement` stays `false` until the audio exists."

## How to verify

- `./gradlew build`: compiles, runs the `:core` and `:app` unit tests, Android Lint, ktlint and the no-`INTERNET` check.
- Instrumented tests on the tablet (tablet skill, "Run one instrumented test and keep the app's data"): `TtsSpeakerDeviceTest`, `MediaSoundPlayerDeviceTest`, `SpokenTextDeviceTest`.
- Manual checks 1–8 in SPEC §6, with the debug screen "WT audio debug".

## Session log

Newest first.
