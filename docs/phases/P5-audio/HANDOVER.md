# P5 — Audio cues: Handover

| | |
|---|---|
| **Status** | In progress: all tasks done; pull request [#5](https://github.com/davamix/workout-timer/pull/5) is waiting for the user to merge it |
| **Spec** | [SPEC.md](SPEC.md), version v0.3 |
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
4. All tasks are done, and pull request #5 is open. First check it with `gh pr view 5`: if I've merged it, use the phase-close skill instead. If it has review comments, deal with them on the branch. For any fix that changes audio, the tablet must be connected over USB (tablet skill); it stays in Spanish.
5. Before the session ends, or once all tasks are done, use the handover skill. When the done criteria pass, open the pull request and wait for me to merge it.
```

## Current state

All tasks are done, and pull request [#5](https://github.com/davamix/workout-timer/pull/5) is open. CI passed on `792642e` (run 41903245858). The planner, the chime, the speaker with its warm-up, the cue player and the debug screen all work. `./gradlew build` passes with 318 unit tests (245 in `:core`, 73 in `:app`). `connectedDebugAndroidTest` gives 26 tests: 23 passed and 3 skipped (P3's manual-only `PersistenceCheck`), 0 failures. The user ran manual checks 1–9 on the tablet, and all pass. The TTS start-up delay found in T3 led to SPEC v0.3 (Q6).

## Next steps

1. Wait for the user to merge pull request #5. Deal with any review comments on the branch first.
2. After the merge, use the `phase-close` skill on `main`.

## Tasks

The task IDs come from SPEC.md §7.

| Task | Status | Notes |
|---|---|---|
| T1 | Done | `CuePlannerTest` 23 tests, `ExampleCuesTest` 10, matching SPEC §4.3.8 exactly. |
| T2 | Done | Chime of 520 ms. `MediaSoundPlayerDeviceTest` passes (4 tests). The user approved it as it is (manual check 1). |
| T3 | Done | `TtsSpeakerDeviceTest` passes (4 tests). The start-up times it measured are in SPEC §4.3.3, and led to Q6. |
| T4 | Done | `CuePlayerTest` 15 tests, including a real session of the example that completes at 264.5 s. `SpokenTextDeviceTest` passes (2 tests). |
| T5 | Done | Installed; screenshots checked. The edge-case workout ran end to end on the tablet. The merged release manifest exports only `MainActivity` among activities. |
| T6 | Done | The warm-up (SPEC v0.3, Q6). `TtsSpeakerDeviceTest` passes (6 tests), `SpokenTextDeviceTest` too (3 tests). Timings under "Known issues". |
| T7 | In review | The user ran manual checks 2–9 on the tablet, and all pass. KDoc reviewed: `CuePlayer.prepare()` now says how long it can wait. Pull request [#5](https://github.com/davamix/workout-timer/pull/5). |

## Decisions and deviations

Anything decided during implementation that isn't in SPEC.md, or that changes it. A change to a requirement goes into SPEC.md or SPECIFICATION.md first.

| Date | Decision | Reason |
|---|---|---|
| 2026-11-04 | The two test fakes share one file, `AudioFakes.kt`, with an `AudioLog` that both write to, instead of `FakeSpeaker.kt` and `FakeSoundPlayer.kt`. | Tests check the order of speech and sound, which needs one log. |
| 2026-11-04 | With voice cues off, `CuePlayer` doesn't even call `speaker.stop()` on `StopSpeech`. | SPEC §4.3.2 says the speaker is never called with voice off; the planner still gives `StopSpeech`. |
| 2026-11-04 | `TtsSpeaker` ignores an `onInit` that comes from an engine it has since replaced. | A late callback from an old engine must not mark the new one READY. |
| 2026-11-04 | `MediaSoundPlayer` calls the `onDone` of a play that a new play cuts off. | It counts as finished (the `SoundPlayer` KDoc says so), so a caller waiting on it is never stuck. |
| 2026-11-04 | The debug screen's log leaves out ticks that give no cue, and keeps 50 lines instead of 30. | With every tick, 30 lines covered only the last few seconds, and the start of a run scrolled away. |
| 2026-11-04 | `TtsSpeaker.selectedVoice` is set as soon as the voice is chosen, before the warm-up ends; `voiceName` stays `null` until READY. | `voiceName` keeps the `Speaker` contract ("`null` until READY"); the field is only for the device test. |
| 2026-11-04 | The voice chosen on the tablet is `en-US-language`, the engine's default English (US) voice. It's installed and needs no network. It sorts first among the equal-quality candidates by name. | It meets SPEC §4.3.3. The other installed offline voices are `en-us-x-{iob,iog,iol,iom,sfg,tpc,tpd,tpf}-local`, all of quality 400 and latency 200. |

## Known issues

- **TTS start-up, solved by the warm-up (SPEC v0.3, Q6).** T3's measurements are in SPEC §4.3.3. With the warm-up, measured in T6 on the tablet, with both the app and `com.google.android.tts` force-stopped first:

  | What | Time |
  |---|---|
  | `prepare()` from a cold engine, in `TtsSpeakerDeviceTest` | 9.1 s, 9.9 s and 10.0 s in three runs; READY each time |
  | Opening the debug screen, with no workout started | READY 9.7–11.7 s after `am start` (polled every 2 s); the cache folder is empty afterwards |
  | "three", after the warm-up | done 699 ms after `speak()`; it takes ~0.7 s to say |

  A cold `prepare()` is right at the 10 s limit. That's enough, because the activities start preparing when they're created: a workout's `prepare()` comes seconds later, and waits for the rest of the same preparation. If it does time out, the timer still runs: the first cues are silent until the speaker is READY, and get ready doesn't wait.
- **Library components in the release manifest:** besides `MainActivity`, the merged manifest has AndroidX's `ProfileInstallReceiver` (exported, but only callers holding `android.permission.DUMP` can reach it) and `InitializationProvider` (not exported). Both were there before P5.
- **`sun.misc.Unsafe` warnings:** ktlint prints them on JDK 25. They are harmless (noted in P1).

## Pending wiring

- **The runner side of the audio (P7 if P5 is merged first, otherwise P8):** as in SPEC §4.3.6. Read the voice setting once when the workout starts. Call `cuePlayer.prepare(voiceOn)`, create the `WorkoutSession` with `waitForAnnouncement = voiceOn`, launch `cuePlayer.play(session, voiceOn)` in `viewModelScope`, then call `session.start()`. The runner reads `session.state` only: the cue player is the only collector of `session.events`. This completes P4's pending wiring: "`waitForAnnouncement` stays `false` until the audio exists."
  - `prepare()` usually returns at once, but within ~10 s of a cold start it can wait up to 10 s. There's no session yet while it waits, so P7 decides what the screen shows meanwhile.
- **The settings screen (P6), a suggestion:** when the voice switch is turned on, call `container.prepareAudio()` (in a scope that outlives the screen, or `lifecycleScope` of the activity). `MainActivity` only prepares the speaker if voice cues are on when it's created, so otherwise the first workout after turning them on does the whole 9–10 s preparation itself.

## How to verify

- `./gradlew build`: compiles, runs the `:core` and `:app` unit tests, Android Lint, ktlint and the no-`INTERNET` check.
- Instrumented tests on the tablet: `./gradlew connectedDebugAndroidTest` runs them all, then uninstalls the app, so run `./gradlew installDebug` afterwards. To run one and keep the app's data, see the tablet skill: `TtsSpeakerDeviceTest`, `MediaSoundPlayerDeviceTest`, `SpokenTextDeviceTest`. To time a cold start, force-stop `com.google.android.tts` and the app first.
- Manual checks 1–9 in SPEC §6, with the debug screen "WT audio debug". Launch it from its icon, or with `adb shell am start -n io.github.davamix.workouttimer.debug/io.github.davamix.workouttimer.audiodebug.AudioDebugActivity`.
- Regenerate the chime with `pwsh tools/generate-round-end-sound.ps1`. `git status` shows no change unless its parameters changed.

## Session log

Newest first.

### 2026-11-04

- Done: SPEC v0.1 written. The review answered Q1–Q5 as assumed, which gave specification v0.8 and SPEC v0.2 `Ready`. Branch created. T1 to T5 implemented, tested and pushed. TTS start-up measured on the tablet, which showed the first phrase starts 3–4 s late. The user agreed to the warm-up proposal: SPEC v0.3 (Q6) pushed to `main` and merged, and T6 implemented and tested on the tablet. The user approved the chime as it is (manual check 1), and ran manual checks 2–9 on the tablet: all pass. KDoc reviewed. Pull request [#5](https://github.com/davamix/workout-timer/pull/5) opened; CI passed.
- Notes: the tablet's battery went from 13 % to 55 % on USB during the session.
