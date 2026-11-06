# P8 — Integration and v1.0: Specification

| | |
|---|---|
| **Status** | Ready, v0.3 |
| **Phase** | P8, see [Implementation plan §5](../../IMPLEMENTATION_PLAN.md#5-phases) |
| **Depends on** | P3, P5, P6 and P7, all done. It also uses P1, P2 and P4, all done. |
| **Branch** | `phase/p8-integration` |
| **Handover** | [HANDOVER.md](HANDOVER.md) |

This document holds everything needed to implement the phase. Working on it shouldn't require reading the full specification or plan.

## 1. Goal

Version 1.0.0 of the app, accepted on the tablet and released. At the end of this phase there is a signed release APK, one test that goes through the whole app, a record showing that every requirement was checked on that build, a real 60-minute run within ±1 s, and a `v1.0.0` tag with a GitHub release carrying the APK.

This phase adds no features and no screens. Production Kotlin only changes if acceptance finds a defect (§4.3.7).

## 2. Requirements covered

P8 covers every requirement. The text is quoted from [SPECIFICATION.md](../../SPECIFICATION.md) (version v0.11). The last column names the acceptance scenarios in §6 that check it. Discarded IDs (`UI-8`, `UI-9`, `TTS-5`, `TTS-6`, `TTS-8`) aren't requirements.

### Workout execution

| ID | Requirement (quoted) | Accepted by |
|---|---|---|
| EX-1 | Starting a workout runs its phases following the sequencing rules in §4.3. | A6 |
| EX-2 | Each exercise lasts its configured duration. The remaining time counts down to zero, then the next phase starts automatically. | A6 |
| EX-3 | Each rest lasts its configured duration. A value of 0 s means no rest. | A6, A7 |
| EX-4 | The user can **pause** a running exercise and **resume** it. Resuming continues from the remaining time; it does not restart the exercise. | A8 |
| EX-5 | Pause and resume also work during rests and the get-ready countdown. | A8 |
| EX-6 | When the last exercise of the last round finishes, the workout ends and a completion screen is shown. | A6 |
| EX-7 | A **get-ready** countdown of **3 s** runs before the first exercise of the first round, and only there. The screen shows "Get ready", the countdown, and the name of the first exercise. The voice for it is described in `TTS-10`. | A6 |
| EX-8 | While paused, **Stop** asks for confirmation, then ends the workout and returns to the workout list. | A9 |
| EX-9 | While paused, **Next** and **Previous** move to another exercise without asking for confirmation (see the table below). | A9 |
| EX-10 | If the app leaves the foreground (Home button, switching apps, screen turned off), the running workout pauses automatically. The workout only runs while the app is on screen. | A10 |
| EX-11 | If Android closes the app while it's in the background (for example, to free memory), the paused workout is not restored. The app reopens on the workout list. | A11 |

What Next and Previous do (specification §5.1, quoted):

- They move between **exercises** and skip any rest in between.
- After moving, the workout stays **paused** at the start of the target exercise. Resume starts that exercise as normal, including its name announcement (`TTS-1`).
- Moving doesn't play the round-end sound (`SND-1`), even when it crosses into another round.
- Next on the last exercise ends the workout as if it had finished, so "Workout complete. Well done!" is spoken (`TTS-7`).

| Paused during | Next | Previous |
|---|---|---|
| Get ready | First exercise | Not available |
| An exercise | Next exercise; on the last exercise of the workout, ends the workout (completion screen) | Previous exercise; on the first exercise, restarts it |
| A rest | The exercise after the rest | The exercise before the rest |

### Workout screen

| ID | Requirement (quoted) | Accepted by |
|---|---|---|
| UI-1 | Shows the current **round name**. | A6 |
| UI-2 | Shows the current **exercise name**. | A6 |
| UI-3 | Shows the **time**: the remaining time of the **current phase only** (exercise, rest or get ready) as `mm:ss`, rounded up to the whole second. It shows `00:40` at the start of a 40 s exercise and `00:01` during its last second. The remaining time of the whole workout is not shown. | A6 |
| UI-4 | Has a control to **pause / resume**. | A8 |
| UI-5 | During a rest, shows "Rest" with its remaining time and the name and duration of the **next exercise**. If a new round starts after the rest, the next round's name is shown too. | A6 |
| UI-6 | Shows progress: exercise *n* of *m* in the round, round *n* of *m*. | A6 |
| UI-7 | Exercise, rest and paused states each have their own background colour, so the current state is clear from a distance. | A6, A8 |
| UI-10 | While a phase is running, the only button shown is **Pause**. | A6 |
| UI-11 | While paused, the buttons shown are **Resume**, **Previous**, **Next** and **Stop**, and the screen clearly says the workout is paused. | A8 |

### Audio cues

Quoted from specification §5.3: "The voice and the round-end sound play at the media volume. While the app is open, the volume keys always change that volume, even between cues." Accepted by A15.

| ID | Requirement (quoted) | Accepted by |
|---|---|---|
| TTS-1 | When an exercise starts and its duration is **longer than 10 s**, the exercise name is spoken. The rule is strict: a 10 s exercise is not announced. | A6, A7 |
| TTS-2 | When **3 s** remain in an exercise, a countdown is spoken: "three", "two", "one". There is one word per second, spoken as the display changes to 3, 2 and 1. An exercise of 3 s or less starts the countdown at its own duration. | A6, A7 |
| TTS-3 | Pausing stops any speech in progress. Nothing is replayed on resume; the countdown carries on from the current second. | A8 |
| TTS-4 | The cues work without a network connection. The app doesn't need the network at all. | A6 |
| TTS-7 | When the workout ends, "Workout complete. Well done!" is spoken. | A6, A9 |
| TTS-9 | A setting turns voice cues on or off. The choice is remembered. With voice cues off, the timer, the screen and the round-end sound work the same. | A14, A16 |
| TTS-10 | **Get ready:** "Get ready in" is spoken first. When it finishes, the 3 s countdown starts, with "three", "two", "one" spoken in sync with the display. With voice cues off, the countdown starts immediately. | A6, A14 |
| TTS-11 | Voice cues are always spoken in **English**, using the English (United States) voice, whatever the device language. | A6 |
| TTS-12 | **Rests are silent:** no voice cues are spoken during exercise rests or round rests. | A6 |
| SND-1 | A short **sound** plays when a round ends, at the start of the round rest. It doesn't play after the last round. If the round rest is 0 s, the next exercise's name (`TTS-1`) is spoken after the sound finishes. | A6, A7 |
| SND-2 | There are no other sound effects. Exercises are marked only by the voice cues. | A6 |

### Workout management

| ID | Requirement (quoted) | Accepted by |
|---|---|---|
| WM-1 | Each exercise has a **name** and a **duration** in seconds. A workout-level default duration applies to every exercise that doesn't set its own. | A3, A4 |
| WM-2 | Rests are set in seconds; 0 means no rest. The workout sets a default exercise rest and a default round rest. Each exercise can set its **own rest**, used after it. Each round can set its **own round rest**, used after it. | A3 |
| WM-3 | Several workouts can be saved on the device and are listed on the home screen. | A5 |
| WM-4 | Workouts can be created, edited, duplicated and deleted in the app. The editor is the only way to create workouts. | A3, A4, A5 |
| WM-5 | In the editor, rounds and exercises can be added, renamed, reordered, duplicated and removed. To repeat a round, duplicate it. | A4 |
| WM-6 | The editor and the workout list show the workout's total duration, from Start to completion: get ready, exercises and rests. | A3, A4 |
| WM-7 | Validation: the workout, each round and each exercise need a name; a workout has at least one round; a round has at least one exercise; durations are at least 1 s; rests are 0 s or more. No duration or rest can be longer than 99:59 (99 minutes 59 seconds). | A4 |
| WM-8 | Workouts are stored locally and survive app restarts and device reboots. There is no cloud backup: uninstalling the app deletes them. | A1, A16 |
| WM-9 | Deleting a workout asks for confirmation. | A5 |
| WM-10 | Leaving the editor with unsaved changes asks for confirmation before discarding them. | A4 |

### Non-functional

| ID | Requirement (quoted) | Accepted by |
|---|---|---|
| NF-1 | **Timer accuracy:** remaining time is computed from a monotonic clock, not by adding up timer ticks, so errors don't build up. A 60-minute workout ends within ±1 s of real elapsed time, and pausing doesn't change this. The time counts from the start of the get-ready countdown: preparing the audio and the "Get ready in" announcement (`TTS-10`) come before it, and aren't part of the workout's total (`WM-6`). | A17 |
| NF-2 | **Screen stays on** while the runner screen is open. | A13 |
| NF-3 | **Robust to configuration changes:** events such as flipping the tablet between the two landscape orientations don't reset, pause or skip the running workout. | A12 |
| NF-4 | **Offline and private:** no network permission, no accounts, no analytics. | A1, A6 |
| NF-5 | **Compatibility:** runs on Android 8.0 (API 26) on the target device. | A1 |
| NF-6 | **Readability:** the time and exercise name are readable from about 3 m away. Touch targets are large. | A6 |
| NF-7 | **Distribution:** installed as an APK from the development machine (`adb` / Android Studio). | A1 |
| NF-8 | **Landscape only:** the app follows the sensor between the two landscape orientations and never shows portrait. | A12 |
| NF-9 | **Language:** the UI is in English. All user-facing text lives in resource files, so a Spanish translation can be added later without code changes. | A2 |

## 3. Scope

### In scope

- **Wiring:** confirm that none is left (§4.3.1).
- **Release build:** signed with the user's own keystore, kept outside the repo. `versionName` becomes `1.0.0`.
- **End-to-end test:** one UI test that creates a workout, runs it and completes it (§4.3.4).
- **Acceptance:** the scenarios in §6 on the release APK, recorded per requirement in `ACCEPTANCE.md`, including the real 60-minute run.
- **Release:** the `v1.0.0` tag and a GitHub release with the APK attached (§4.3.6).
- **Closing the documents:** README, CLAUDE.md, the `tablet` skill, and the specification and plan status.

### Out of scope

- New features, and everything in specification §10 (future ideas).
- A release workflow in GitHub Actions. CI has no signing key, and it stays that way.
- Moving workouts from the debug app to the release app (§4.3.3).
- Removing P5's audio debug screen. It lives in `app/src/debug`, so it isn't in the release APK.
- Shrinking with R8 (Q4).

## 4. Design

### 4.1 Packages and files owned

| File | Change |
|---|---|
| `app/src/androidTest/kotlin/io/github/davamix/workouttimer/EndToEndTest.kt` | New (§4.3.4) |
| `docs/phases/P8-integration/ACCEPTANCE.md` | New: one row per requirement, with its result (§4.3.8) |
| `app/build.gradle.kts` (shared) | Signing config; `versionName = "1.0.0"`. `versionCode` stays 1. |
| `.claude/skills/tablet/helpers.sh` and `SKILL.md` (shared) | The release package name, and the helper that times a run (§4.2) |
| `README.md`, `CLAUDE.md` (shared) | Status, how to build and install a release, and lessons from this phase |
| `docs/SPECIFICATION.md`, `docs/IMPLEMENTATION_PLAN.md` (shared) | Their status becomes v1.0 (Q7), on `main` |

No new screen, so there's no layout to check. No library is added.

### 4.2 Interfaces and data

**Signing properties,** in the user-level file `C:\Users\davam_p3h1i13\.gradle\gradle.properties`, outside the repo. The user wrote it, and Claude doesn't read it.

```properties
workoutTimerKeystoreFile=C:/Users/davam_p3h1i13/workout-timer.keystore
workoutTimerKeystorePassword=<password>
workoutTimerKeyAlias=<alias>
workoutTimerKeyPassword=<password>
```

**Helpers** in `helpers.sh`:

- `release_pkg=io.github.davamix.workouttimer`. The helpers read `$pkg` when they run, so setting `pkg=$release_pkg` after loading the file points them at the release app (§4.3.3).
- `time_run`: polls the app's window from the PC, and prints the PC time whenever `FLAG_KEEP_SCREEN_ON` turns on or off (§4.3.5). Its exact form comes from T1.

### 4.3 Behaviour

#### 4.3.1 Wiring

Nothing is left to wire. This was read from `main` at `6d2cc6d` on 2026-11-06, and T4 confirms it on the final code.

| Connection | State |
|---|---|
| Real repositories (P3) | `AppContainer` creates `FileWorkoutRepository` and `DataStoreSettingsRepository`. `InMemory…` appears in `app/src/main` only in KDoc. |
| Audio in the runner (P4, P5) | `RunnerViewModel` reads the voice setting once, prepares the cue player, creates the session with `waitForAnnouncement = voiceOn`, and launches `cuePlayer.play`. |
| Audio prepared early (P5) | `MainActivity` calls `prepareAudio()` when created, and `SettingsViewModel` calls it when voice cues are turned on. |
| Screens (P6, P7) | The list's Start opens `RunnerRoute`. No placeholder screen or string is left. |

The handovers leave P8 these items only: the end-to-end test, checking `WM-8` through the UI, and the final checks of `NF-1` and `NF-3`.

#### 4.3.2 Signing

- **The keystore** (Q1, Q8) was created by the user in their own terminal, at `C:\Users\davam_p3h1i13\workout-timer.keystore`, so its password never passed through Claude. The key is RSA 4096. The certificate is public inside the APK, and the user chose to keep their name and country in it.
- **The properties file** is written in Notepad, not VS Code, because the Claude extension can see what's selected in the editor. A `\` is an escape character in a properties file, so the path uses forward slashes and the password has no backslash.
- **Keep a copy of the keystore and its password outside this PC.** Android only installs an update signed with the same key. Without the key, an update means uninstalling first, which deletes every workout (`WM-8`).
- **In `app/build.gradle.kts`:** the release signing config exists only when all four properties are set, read with `providers.gradleProperty`, which works with the configuration cache. Otherwise the release build stays unsigned. CI has no properties and must keep passing, because `./gradlew build` also assembles the release variant.
- **Checking it:** `apksigner verify --print-certs` (`D:\Android\Sdk\build-tools\37.0.0\apksigner.bat`) must show a v2 signature with the certificate `CN=Daniel Valcarce, C=ES`, SHA-256 `abd7e456e1edcd040377267afdcf0ebbd0ee7dd82c4f07a2466c366a074ad75f`. Android 8 uses v2. Never print the properties file.

#### 4.3.3 Release and debug side by side

| | Debug | Release |
|---|---|---|
| Package | `io.github.davamix.workouttimer.debug` | `io.github.davamix.workouttimer` |
| Launcher name | Workout Timer (debug) | Workout Timer |
| Audio debug screen | Yes | No |
| `run-as` | Yes | No, so its files can't be read and `end_process` doesn't work |
| Instrumented tests | Yes | No |
| Logs from the app | Warnings only | None (plan §3) |

- **The release app starts empty.** Each package has its own storage, so the workouts in the debug app stay there. Specification §2 rules out importing them.
- **On the release app these helpers work** once `pkg=$release_pkg` is set: `launch`, `stop_app`, `shot`, `texts`, `wait_for`, `tap_text`, `start_workout`, `keeps_screen_on`.
- **Installing:** `adb install -r <apk>`. `installRelease` would also work, but the acceptance should use the same command the user will use (`NF-7`).

#### 4.3.4 End-to-end test

`EndToEndTest` is a JUnit 4 class with one test, `createRunAndCompleteAWorkout`, using `createAndroidComposeRule<MainActivity>()`. It runs on the app's real storage, settings, navigation, clock and audio, with no fake. It stays on the debug build, because instrumented tests can't run on the release one.

1. **Before:** remember the voice setting and turn voice cues on. Nothing else is assumed about the list.
2. **Create:** press New workout. A new workout starts with no name and one round holding one unnamed exercise. Type the workout below, adding what's missing. Check that `editor-total` shows "Total 0:22", then Save.
3. **Check what was saved:** the list shows it with "Total 0:22". The saved workout in `container.workoutRepository`, compared with `withoutIds()`, has the expected structure.
4. **Run:** press its Start. Without touching anything else, wait for each exercise's name in `runner-title`, in order.
5. **Complete:** the completed screen shows "Well done!" and "Total time 0:22 · 2 rounds · 3 exercises". `container.speaker.status.value` is `READY`, so the real engine was connected. A test can't hear the cues; A6 covers that.
6. **Back to list:** the workout is still listed.
7. **After:** delete every workout with the test's name through the repository, so a failed run leaves nothing behind, and put the voice setting back.

The workout, called "End-to-end test", has default rests of 0 s apart from its own values:

| Round | Exercise | Duration | Rest after it |
|---|---|---|---|
| Round A | One | 3 s | 1 s |
| Round A | Two | 3 s | 1 s (round rest) |
| Round B | Three | 11 s, so its name is spoken | — |

Total: 3 s get ready + 17 s exercises + 2 s rests = 22 s.

Test pitfalls (P6, P7):

- IDs are random here, unlike in `WorkoutEditorScreenTest`. Match the editor's tags (`exercise-name-…`, `round-name-…`) by prefix with a small `SemanticsMatcher`.
- `LazyColumn` items exist only once scrolled into composition: use `performScrollToNode` on `editor-form`.
- Allow 20 s for the runner to start: preparing the TTS engine can take up to 10 s after a cold start, then there's "Get ready in" and the countdown.
- Rests of 1 s are too short to wait for reliably, so the test doesn't look for them.

#### 4.3.5 Measuring the 60-minute run (`NF-1`)

The run happens on the release APK, which has no logs and no `run-as`, so it's timed from outside on the PC's clock. The app isn't changed for it.

- **The signal:** the runner sets `FLAG_KEEP_SCREEN_ON` while it's open (`RunnerScreen.kt`), and `dumpsys window windows` shows it. Measured on 2026-11-06, a poll through adb takes about 130 ms.
- **What it includes:** the span from flag on to flag off also holds preparing the audio, "Get ready in", and the navigation to the completed screen. These fixed costs are measured with a short workout and subtracted:

  ```
  span     = flag off − flag on, on the PC's clock
  overhead = span − planned, on a 60 s workout with the same voice setting, 3 runs
  drift    = span − paused time − 3600 s − overhead
  ```

  `NF-1` passes when |drift| ≤ 1 s. Its time counts from the start of the get-ready countdown (Q2), so what comes before it mustn't count.
- **The workout, "One hour":** defaults of 50 s exercise, 10 s exercise rest and 10 s round rest. One round of 5 exercises, duplicated to make 12 rounds (`WM-5`), with an own duration of 57 s on the very last exercise. 3 + 12 × 290 + 11 × 10 + 7 = 3600 s. The list shows "Total 1:00:00".
- **Pauses:** two of about a minute each, near minutes 20 and 40, with `adb shell input tap` on the Pause and Resume buttons. Paused time is the PC time between the two `input` commands returning. One `input` command takes about 750 ms, measured on 2026-11-06. Only the spread matters, because the pause and the resume each carry the same delay. T1 measures it.
- **Polling:** every few seconds for most of the hour, so as not to load the tablet, then as fast as possible from a minute before the expected end.
- **Conditions:** voice cues on, Wi-Fi either way, the tablet on USB and left alone. Record the battery level before and after.

T1 proves all this on short runs before anyone spends an hour on it.

#### 4.3.6 Releasing

The tag has to point at the merged code, so these steps come after the user merges the pull request, and before `phase-close`.

1. `git switch main && git pull --ff-only`, and wait for CI to pass on the merge commit.
2. Compare with the commit that passed acceptance, recorded in `ACCEPTANCE.md`: `git diff --stat <accepted> HEAD -- . ':!docs' ':!*.md' ':!.claude'` must be empty. If it isn't, repeat the checks the difference affects. Comparing APK hashes wouldn't work, because AGP puts the git commit in the release APK (`META-INF/version-control-info.textproto`).
3. `./gradlew clean assembleRelease`, then `apksigner verify --print-certs`.
4. `adb install -r` over the accepted build. The workouts must still be there, which also proves that updating keeps them. Run one workout to the end.
5. Copy the APK to the scratchpad as `workout-timer-1.0.0.apk`.
6. **Ask the user for the go-ahead.** The repo is public, so the tag and the release are public.
7. Tag and publish:

   ```bash
   git tag -a v1.0.0 -m "Workout Timer 1.0.0"
   git push origin v1.0.0
   gh release create v1.0.0 <apk> --verify-tag --title "Workout Timer 1.0.0" --notes-file <file>
   ```

   The notes say what the app does, the device it targets, and how to install it with `adb install`, and they link to `ACCEPTANCE.md`.
8. Download the asset with `gh release download`, and check that its SHA-256 matches the local APK.
9. Use the `phase-close` skill.

#### 4.3.7 Defects found during acceptance

- **A requirement fails:** fix it on the phase branch with a test, then repeat every scenario the fix could affect. If `:core`'s engine, the runner or the audio changes after A17, run A17 again. If the fix contradicts a document, the usual order applies: specification, plan, SPEC.
- **A problem that breaks no requirement:** list it in `ACCEPTANCE.md` and ask the user whether to fix it now or leave it for later. Known issues left for later go in the release notes.

#### 4.3.8 `ACCEPTANCE.md`

It has a header with the commit, the APK's SHA-256, the date and the device. Below that, one row per requirement ID in §2's order: ID, a short name, the scenarios, the result, and a note. A requirement passes when all its scenarios pass. Whatever needs a person is marked as checked by the user. Nothing is marked as passing unless it was seen to pass.

### 4.4 Dependencies

No library is added. Tools used: `keytool` from Android Studio's JBR, and `apksigner` and `aapt2` from `D:\Android\Sdk\build-tools\37.0.0`. `gh` is logged in as the repo's owner.

### 4.5 What earlier phases left for this one

- **Tablet facts (P1, P3, P5, P7):**
  - The first launch after installing can take over a minute on a debug build. T2 measures the release build.
  - After a reboot, wait for the unlock and for the USB reset that follows it (`tablet` skill, "Rebooting").
  - A cold TTS `prepare()` takes 9–10 s, and the runner shows "Getting ready…" meanwhile.
- **`am kill` doesn't end the app on this tablet (P7).** `end_process` works only on the debug build, because it needs `run-as`. That's why A11 runs on debug (Q3).
- **uiautomator can't read a running workout (P7).** The time changes every second, so the screen is never idle. Read it while paused, or use `shot`.
- **Turning the tablet over (P7) doesn't recreate the activity.** It only changes the display's rotation, so A12 also forces a real configuration change. `RunnerActivityTest` covers recreation in tests.
- **Dialogs (P7):** `captureToImage()` can't capture them below API 28. Check them on the device.
- **`connectedDebugAndroidTest` deletes the debug app's workouts (P6, P7).** Use the `instrument` helper.
- **Library components in the release manifest (P5):** see Q5.

## 5. Security and privacy

Final check of plan §3 on the release APK, in A1 and T4:

| Rule | Check |
|---|---|
| No network | `aapt2 dump badging` lists no `INTERNET` permission. The only one it lists is `<package>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (Q5). |
| No cloud backup | `dumpsys package` shows no `ALLOW_BACKUP` flag (`D-11`) |
| Not debuggable | `dumpsys package` shows no `DEBUGGABLE` flag, and `run-as` is refused |
| Minimal surface | The merged release manifest holds exactly `MainActivity` (exported), `ProfileInstallReceiver` (exported, needs `android.permission.DUMP`) and `InitializationProvider` (not exported). Anything new fails the check (Q5). |
| Signing keys stay out of the repo | The keystore and the properties are outside it. `git grep` for the property names finds only their use in `build.gradle.kts` and the documents, with no values. Passwords never appear on a command line or in any output. |
| Release builds don't log | Logcat holds no `WorkoutStorage` or `WorkoutAudio` line during acceptance (A18) |
| Trusted dependencies | No library is added. CI still validates the wrapper, and its actions stay pinned. |

The APK goes on a public GitHub release (plan PQ-2). It holds no workouts and no secrets. The certificate holds the user's name and country, by their choice (Q8), and the commit it records is already public.

## 6. Testing

| Test | Type | Covers |
|---|---|---|
| All existing tests | Unit (`./gradlew build`) and instrumented | Each phase's own coverage of every requirement |
| `EndToEndTest` | UI, on the tablet | The whole app wired together (plan P8) |
| Acceptance scenarios A1–A18 | Manual, on the tablet | Every requirement (§2) |

### Manual checks on the tablet

Use the release APK, unless a scenario says otherwise. The tablet stays in Spanish. "User" marks what needs a person; Claude does the rest through adb. Ask before changing a tablet setting, and put it back afterwards.

| # | Who | Steps | Expected |
|---|---|---|---|
| A1 | Claude | `adb install -r` the signed APK. Check it as in §5, with `apksigner` and `aapt2`. Time the first launch and a later cold start (`am start -W`). | Package `…workouttimer`, version 1.0.0. Checks as in §5. The launcher has "Workout Timer", and this APK adds no audio debug entry. It opens on the empty list (`NF-4`, `NF-5`, `NF-7`, `WM-8`). |
| A2 | Claude | Visit every screen and read it with `texts`. Search the Kotlin sources for user-facing text outside resources. | All English on a Spanish tablet, and none outside resources (`NF-9`) |
| A3 | Claude | Create the §4.4 example below by hand, leaving linked values empty, then Save | The editor shows "Total 4:23" before saving. The list shows "Example" and "Total 4:23". |
| A4 | Claude | Edit it. Set the default duration to 45. Check the unused rests, and move High knees up one place. Add, rename, duplicate, reorder and remove an exercise and a round. Clear a name, set a duration to 0 and a rest to 6000, then Save. Remove the only exercise of a round, then the only round, and Save each time. Leave with Back: Keep editing, then Discard. | High knees and Crunches show 45, Jumping jacks keeps 30, and the total is 4:33. "Not used" moves with the exercise, and its value comes back. Each Save shows messages at the fields, or asks for at least one exercise or round, and saves nothing. "Discard changes?", then 4:23 again (`WM-1`, `WM-5`, `WM-6`, `WM-7`, `WM-10`). |
| A5 | Claude | Duplicate "Example". Delete the copy: Cancel, then Delete. | "Example (copy)" with 4:23. Cancel keeps it, Delete removes it (`WM-3`, `WM-4`, `WM-9`). |
| A6 | User listens and watches | Turn Wi-Fi off. Run `stop_app cold`, open the app and start the example at once. Follow it to the end without touching it, from about 3 m. Then use every screen at arm's length. Turn Wi-Fi back on. | "Getting ready…", then each phase and cue as in the example table below, in English, in sync with the display. Rests silent. Colours easy to tell apart, and the time and name easy to read. "Total time 4:23 · 2 rounds · 5 exercises" at the end. Buttons easy to hit. |
| A7 | User listens | Create "Edges": round 1 with rest 0 s holding Ten (10 s) and Eleven (11 s); round 2 holding Twelve (12 s), Three (3 s), Two (2 s) and One (1 s). Default rests 0 s. Run it. | Ten isn't named; Eleven is. The chime, then "Twelve", not over it. The 3, 2 and 1 s exercises count from "three", "two" and "one" (`EX-3`, `TTS-1`, `TTS-2`, `SND-1`). |
| A8 | User listens | Pause in get ready, in an exercise and in a rest, then resume each. Pause during a spoken name, and just after "two". | The time stays, then counts on. The paused colour and "PAUSED", with Resume, Previous, Next and Stop. Speech stops at the pause and nothing is replayed; "one" comes on time (`EX-4`, `EX-5`, `TTS-3`, `UI-4`, `UI-7`, `UI-11`). |
| A9 | User listens | While paused, go through the Next and Previous table. Cross into round 2 and resume. Next on the last exercise. Pause, Stop, Cancel, Stop, Stop. Back while running, while paused, and with the dialog open. | As the table says; Previous is disabled in get ready. No chime when crossing, and the name is spoken on resume. The completed screen and "Workout complete. Well done!" Stop returns to the list. Back pauses, then asks to stop, then closes the dialog (`EX-8`, `EX-9`, `TTS-7`). |
| A10 | Claude | While running, press Home and return. Repeat with Recents and the power button. | Paused each time, at the time it had (`EX-10`) |
| A11 | Claude, debug APK | Start a workout, `end_process`, reopen from Recents. Repeat, reopening from the launcher. | The workout list both times (`EX-11`) |
| A12 | User turns the tablet | Turn it over while running, while paused, and with the Stop dialog open. Hold it upright. Then Claude forces a configuration change while running, with `settings put system font_scale 1.15`, and puts it back to 1.0. | Nothing resets, pauses or skips, and the dialog stays open. It never shows portrait. After the forced change, the workout is still running at the right time (`NF-3`, `NF-8`). |
| A13 | Claude | `screen_timeout 30000`. Leave the runner paused for over a minute, then wait on the list. `screen_timeout never`. | The screen stays on in the runner, and goes off on the list (`NF-2`) |
| A14 | User listens | Turn voice cues off, force-stop the app, reopen Settings. Run Edges. Turn them back on. | Still off after reopening. Get ready counts down at once. Only the chime is heard (`TTS-9`, `TTS-10`). |
| A15 | User listens | Between cues, press the volume keys, then during a cue. | The media volume changes both times, and the voice and the chime follow it (specification §5.3) |
| A16 | User unlocks | With voice cues off and the workouts in place, `adb reboot`, wait, and open the app. Turn voice cues back on. | The workouts and the setting are still there (`WM-8`, `TTS-9`) |
| A17 | Claude; user listens at the end | Create "One hour". Measure the overhead, then run it as §4.3.5 says. | \|drift\| ≤ 1 s. The cues still play in the last minute, and the screen stayed on. The numbers go in `ACCEPTANCE.md` (`NF-1`). |
| A18 | Claude | Read the crash buffer and the app's logcat after each session of checks | No crash, and no `WorkoutAudio` or `WorkoutStorage` line |

The §4.4 example (specification, quoted). Workout "Example" has these defaults: exercise duration 40 s, exercise rest 15 s, round rest 60 s.

- Round 1 "Warm-up", own round rest 30 s:
  - Jumping jacks: 30 s
  - Arm circles: 10 s, own rest 5 s
  - High knees: default duration
- Round 2 "Core":
  - Plank: 60 s, own rest 30 s
  - Crunches: default duration

| # | Phase | Round | Name | Duration | Audio |
|---|---|---|---|---|---|
| 1 | Get ready | — | — | 3 s | "Get ready in" · 3-2-1 |
| 2 | Exercise | Warm-up | Jumping jacks | 30 s | "Jumping jacks" · 3-2-1 |
| 3 | Exercise rest | Warm-up | — | 15 s (default) | silent |
| 4 | Exercise | Warm-up | Arm circles | 10 s | 3-2-1 (no name: not longer than 10 s) |
| 5 | Exercise rest | Warm-up | — | 5 s (own) | silent |
| 6 | Exercise | Warm-up | High knees | 40 s (default) | "High knees" · 3-2-1 |
| 7 | Round rest | — | — | 30 s (own) | round-end sound, then silent |
| 8 | Exercise | Core | Plank | 60 s | "Plank" · 3-2-1 |
| 9 | Exercise rest | Core | — | 30 s (own) | silent |
| 10 | Exercise | Core | Crunches | 40 s (default) | "Crunches" · 3-2-1 |
| | **Completed** | | | **Total 4:23** | "Workout complete. Well done!" |

## 7. Tasks

In order. Each task should take one working session or less.

| ID | Task |
|---|---|
| T1 | **Measure the timing method (§4.3.5),** with a throwaway script and short workouts on the debug build: that the flag shows up in polling and turns on and off when expected; the overhead over 3 runs of a 60 s workout, with voice on and off; the spread of `input tap` over about 10 runs, and the Pause and Resume coordinates. Record the numbers in the handover. If the method can't give ±1 s, stop and ask. Add `time_run` and `release_pkg` to the `tablet` skill. |
| T2 | **Release build.** The user creates the keystore and the properties file (§4.3.2). Add the signing config and set `versionName`. Check `assembleRelease` with `apksigner`, install it next to the debug app, and time the first launch and a cold start. CI has no properties, so the pull request's CI run proves that `./gradlew build` still passes without them. |
| T3 | **End-to-end test** (§4.3.4). Run it a few times, cold and warm, to rule out flakiness. |
| T4 | **Baseline.** `./gradlew build`; every instrumented test passes on the tablet; the wiring (§4.3.1) and security (§5) checks; write `ACCEPTANCE.md` with every requirement. Note the commit under test. |
| T5 | **Acceptance without sound:** A1–A5, A10, A11, A13, A18. Claude alone. |
| T6 | **Acceptance with the user:** A6–A9, A12, A14–A16, A18. |
| T7 | **The 60-minute run:** A17, A18. It comes last among the checks, so a fix found earlier doesn't force a second hour (§4.3.7). |
| T8 | **Close the documents.** README: status, what the app is, how to build a signed release and install it. CLAUDE.md and the `tablet` skill: release commands and what this phase learned. Specification and plan: v1.0 (Q7), on `main`. |
| T9 | **Pull request** (skill `handover`), with `ACCEPTANCE.md` linked. Wait for CI and for the user to merge it. |
| T10 | **After the merge:** release as in §4.3.6, with the user's go-ahead, then the `phase-close` skill. |

## 8. Done criteria

The checks that must all pass before the phase is merged:

- [ ] T1–T8 done
- [ ] `./gradlew build` passes locally and in CI, without the signing properties in CI
- [ ] Every instrumented test passes on the tablet, including `EndToEndTest`
- [ ] The release APK is signed with the user's key, version 1.0.0, not debuggable, with no `INTERNET` permission
- [ ] `ACCEPTANCE.md` has every requirement, each one passing, including `NF-1` with its measured drift
- [ ] No secret in the repo
- [ ] HANDOVER.md is up to date

After the merge:

- [ ] `v1.0.0` is tagged on the merged commit, and the GitHub release carries the signed APK
- [ ] The plan's status table is updated

## 9. Open questions

None at the moment. New questions are numbered from Q9.

### Answered

| # | Question | Decision |
|---|---|---|
| Q1 | Who creates the keystore, and where? | The user creates it, in their own terminal, with the command in §4.3.2, at `C:\Users\davam_p3h1i13\.android-keys\`. They write the properties file too, so the password never passes through Claude, and they keep a copy of both outside this PC. Claude then builds with it, without ever reading the file. |
| Q2 | What does `NF-1`'s ±1 s measure? With voice on, "Get ready in" comes before the countdown (`TTS-10`), and it isn't in the planned total (`WM-6`, §4.4), so the real time from pressing Start is the total plus 1–2 s. | `NF-1` is about the timer drifting, so it counts from the moment the get-ready countdown starts. The announcement and the time to prepare the audio don't count. Specification v0.11 says so. |
| Q3 | Does every check have to run on the release APK? `EX-11` needs `run-as` to end the process the way Android does, and the release APK refuses it. | Everything runs on the release APK except A11, which runs on the debug APK from the same commit. Saved state has no code that differs between the two builds. The end-to-end test also stays on debug. |
| Q4 | Should v1.0 turn on R8? The release build has `isMinifyEnabled = false` since P1. Unshrunk, the APK is 9.7 MB. | It stays off. The APK size doesn't matter for an app installed by hand, and R8 would add keep rules to get right for a build the instrumented tests can't run on. If A6 shows the UI stuttering on the release build, raise it again. |
| Q5 | Plan §3 and CLAUDE.md didn't match the merged manifest. AndroidX adds `ProfileInstallReceiver` (exported, but only holders of `android.permission.DUMP` can reach it), `InitializationProvider` (not exported), and `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a permission the app declares and grants itself. | Keep all three, since `InitializationProvider` starts libraries the app uses, and none of them gives access to anything. `NF-4` still holds: there's no network permission. Plan v0.3 §3 and CLAUDE.md now say so. These three are the expected list in §5, so that a new one fails the check. |
| Q6 | How much of the 60-minute run needs the user? | Claude drives it through adb. The tablet speaks for an hour on USB, and nobody touches it. The user only listens to the last minute. If the user would rather work out with it, that's fine as long as they don't touch the screen outside the two pauses. |
| Q7 | Do the specification and the plan become v1.0? They still say "Draft". | Yes, at T8: specification v1.0 and plan v1.0, both based on what was released, with a change-log entry. New work after that starts a new version. |
| Q8 | From T2: the user's keystore is at `C:\Users\davam_p3h1i13\workout-timer.keystore`, not in `.android-keys`, and its certificate is `CN=Daniel Valcarce, C=ES`, not the generic "Workout Timer" this SPEC assumed. Anyone who downloads the APK can read the certificate. | Keep both. The user's name is already public in the repo's commits. The key can't change after this without uninstalling the app, which deletes its workouts. |

## 10. Change log

| Version | Date | Changes |
|---|---|---|
| 0.1 | 2026-11-06 | First draft |
| 0.2 | 2026-11-06 | Review applied: the user agreed with every draft assumption (Q1–Q7). Quotes specification v0.11 (`NF-1`, Q2). §4.3.2 adds how to write the properties file. §5 gives the expected manifest (Q5). Ready. |
| 0.3 | 2026-11-06 | From T2 (Q8): the keystore's real path and certificate, in §4.2, §4.3.2 and §5. |
