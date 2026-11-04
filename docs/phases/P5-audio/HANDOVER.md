# P5 — Audio cues: Handover

| | |
|---|---|
| **Status** | In progress |
| **Spec** | [SPEC.md](SPEC.md), version v0.2 |
| **Branch** | `phase/p5-audio` |
| **Last session** | 2026-11-04 |

This document tracks the phase as it's built. Update it at the end of every session, so the next one can continue from here.

## Prompt to start or continue

Paste this prompt into a new Claude Code session to start or continue the phase. When the phase is done, replace it with "Phase done: no prompt needed."

```text
Start or continue phase P5 (Audio cues) of the workout timer.

1. Use the phase-start skill for P5. Read only docs/phases/P5-audio/HANDOVER.md and SPEC.md; they contain everything needed.
2. Carry on from "Next steps" in the handover. Work through the tasks in SPEC.md §7 in order, on branch phase/p5-audio. Commit after each task, once ./gradlew build passes.
3. Follow SPEC.md. If something in it is wrong or unclear, stop and ask me before changing course. Record every decision or deviation in the handover.
4. T1 to T5 are done. Two things wait for me: my verdict on the chime (manual check 1), and my answer on the TTS warm-up proposal under "Known issues". The tablet must be connected over USB (tablet skill), and T6 needs me at the tablet to listen to the cues. The tablet stays in Spanish.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

T1 to T5 are done and pushed. The planner, the chime, the speaker, the cue player and the debug screen all work. Every unit test passes, and so do all the instrumented tests on the tablet. The debug screen ran the edge-case workout end to end on the tablet. Two things are open. On the tablet, the first utterance after the TTS engine connects starts about 3–4 s late, so "Get ready in" can overrun P4's 5 s timeout (see "Known issues"). And the user hasn't listened to the chime yet.

## Next steps

1. Get the user's answer on the warm-up proposal in "Known issues". If agreed, write SPEC v0.3 first (§4.2, §4.3.3, §4.3.6, §6, §7), push it to `main` and merge `main` into the branch. Then implement it, with a test in `TtsSpeakerDeviceTest`.
2. Manual check 1: the user plays `app/src/main/res/raw/round_end.wav` on the PC. If they want changes, tune the parameters at the top of `tools/generate-round-end-sound.ps1`, then run `pwsh tools/generate-round-end-sound.ps1`.
3. T6: manual checks 2–8 with the user at the tablet, using "WT audio debug". Then KDoc review, the handover skill, and the pull request.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `CuePlannerTest` 23 tests, `ExampleCuesTest` 10, matching SPEC §4.3.8 exactly. |
| T2 | Done | Chime of 520 ms. `MediaSoundPlayerDeviceTest` passes (4 tests). The user hasn't listened to it yet (manual check 1). |
| T3 | Done | `TtsSpeakerDeviceTest` passes (4 tests). Measured start-up times are under "Known issues". |
| T4 | Done | `CuePlayerTest` 15 tests, including a real session of the example that completes at 264.5 s. `SpokenTextDeviceTest` passes (2 tests). |
| T5 | Done | Installed; screenshots checked. The edge-case workout ran end to end on the tablet. The merged release manifest exports only `MainActivity` among activities. |
| T6 | To do | Needs the user at the tablet. |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-04 | The two test fakes share one file, `AudioFakes.kt`, with an `AudioLog` that both write to, instead of `FakeSpeaker.kt` and `FakeSoundPlayer.kt`. | Tests check the order of speech and sound, which needs one log. |
| 2026-11-04 | With voice cues off, `CuePlayer` doesn't even call `speaker.stop()` on `StopSpeech`. | SPEC §4.3.2 says the speaker is never called with voice off; the planner still gives `StopSpeech`. |
| 2026-11-04 | `TtsSpeaker` ignores an `onInit` that comes from an engine it has since replaced. | A late callback from an old engine must not mark the new one READY. |
| 2026-11-04 | `MediaSoundPlayer` calls the `onDone` of a play that a new play cuts off. | It counts as finished (the `SoundPlayer` KDoc says so), so a caller waiting on it is never stuck. |
| 2026-11-04 | The debug screen's log leaves out ticks that give no cue, and keeps 50 lines instead of 30. | With every tick, 30 lines covered only the last few seconds, and the start of a run scrolled away. |
| 2026-11-04 | The voice chosen on the tablet is `en-US-language`, the engine's default English (US) voice. It's installed and needs no network. It sorts first among the equal-quality candidates by name. | It meets SPEC §4.3.3. The other installed offline voices are `en-us-x-{iob,iog,iol,iom,sfg,tpc,tpd,tpf}-local`, all of quality 400 and latency 200. |

## Known issues

- **The TTS engine starts slowly, and the first utterance is late. A proposal is waiting for the user.** Measured on the tablet, from a cold engine, with a throwaway probe:

  | Step | Time |
  |---|---|
  | `TextToSpeech` connects (`onInit`) | 2.7–3.0 s, on the first connection in a process; ~0.15 s for a second one |
  | Listing the voices | ~0.5 s |
  | First utterance after that | starts 2.8–4.2 s after `speak()` |
  | A new phrase later | starts ~250 ms after `speak()` |
  | A phrase already spoken once | starts ~20–40 ms after `speak()` (the engine caches it) |

  So "Get ready in", the first utterance, can end after P4's 5 s announcement timeout. On the debug screen it ended at 4.1 s; in the probe, at 5.5 s. The countdown would then start while it's still being spoken. The first countdown words are also ~250 ms behind the display.

  **Proposal:**
  1. After selecting the voice, `prepare()` warms the engine up. It synthesises the five fixed phrases to a temporary file in the app's cache with `synthesizeToFile`, then deletes the file, and is READY only after that. Measured: 5.7 s from a cold engine. Afterwards "Get ready in" and the numbers start ~25 ms after `speak()`. Speaking them at volume 0 instead took 10.8 s.
  2. Start preparing when the app starts, if voice cues are on, so the ~9 s cold start is usually over before a workout begins.
  3. Raise `SPEAKER_PREPARE_TIMEOUT_MILLIS` from 5 s to 10 s, so a workout started right after launch waits for the voice instead of losing "Get ready in".

  This changes SPEC §4.2, §4.3.3 and §4.3.6, and adds a P5 entry in a shared file (`WorkoutTimerApp` or `AppContainer`).
- **Library components in the release manifest:** besides `MainActivity`, the merged manifest has AndroidX's `ProfileInstallReceiver` (exported, but only callers holding `android.permission.DUMP` can reach it) and `InitializationProvider` (not exported). Both were there before P5.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25. They are harmless (noted in P1).

## Pending wiring

- **The runner side of the audio (P7 if P5 is merged first, otherwise P8):** as in SPEC §4.3.6. Read the voice setting once when the workout starts. Call `cuePlayer.prepare(voiceOn)`, create the `WorkoutSession` with `waitForAnnouncement = voiceOn`, launch `cuePlayer.play(session, voiceOn)` in `viewModelScope`, then call `session.start()`. The runner reads `session.state` only: the cue player is the only collector of `session.events`. This completes P4's pending wiring: "`waitForAnnouncement` stays `false` until the audio exists."

## How to verify

- `./gradlew build`: compiles, runs the `:core` and `:app` unit tests, Android Lint, ktlint and the no-`INTERNET` check.
- Instrumented tests on the tablet (tablet skill, "Run one instrumented test and keep the app's data"): `TtsSpeakerDeviceTest`, `MediaSoundPlayerDeviceTest`, `SpokenTextDeviceTest`.
- Manual checks 1–8 in SPEC §6, with the debug screen "WT audio debug". Launch it from its icon, or with `adb shell am start -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.audiodebug.AudioDebugActivity`.
- Regenerate the chime with `pwsh tools/generate-round-end-sound.ps1`. `git status` shows no change unless its parameters changed.

## Session log

Newest first.

### 2026-11-04

- Done: SPEC v0.1 written. The review answered Q1–Q5 as assumed, which gave specification v0.8 and SPEC v0.2 `Ready`. Branch created. T1 to T5 implemented, tested and pushed.
- Notes: TTS start-up was measured on the tablet (see "Known issues"), and a warm-up is proposed; the user's answer is pending. The chime is waiting for the user to listen. The tablet's battery was at 13 %.
